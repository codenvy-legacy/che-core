/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.project.server;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.project.server.handlers.GetItemHandler;
import org.eclipse.che.api.project.server.type.Attribute;
import org.eclipse.che.api.project.server.type.AttributeValue;
import org.eclipse.che.api.project.server.type.ProjectType;
import org.eclipse.che.api.project.server.type.Variable;
import org.eclipse.che.api.project.shared.Builders;
import org.eclipse.che.api.project.shared.Runners;
import org.eclipse.che.api.project.shared.dto.SourceEstimation;

import org.eclipse.che.api.vfs.server.VirtualFile;
import org.eclipse.che.api.vfs.shared.dto.AccessControlEntry;
import org.eclipse.che.api.vfs.shared.dto.Principal;
import org.eclipse.che.api.vfs.shared.dto.VirtualFileSystemInfo.BasicPermissions;
import org.eclipse.che.dto.server.DtoFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Server side representation for codenvy project.
 *
 * @author andrew00x
 * @author Eugene Voevodin
 */
public class Project {
    private final FolderEntry    baseFolder;
    private final ProjectManager manager;
    private final Modules        modules;

    public Project(FolderEntry baseFolder, ProjectManager manager) {
        this.baseFolder = baseFolder;
        this.manager = manager;
        modules = new Modules();
    }

    /** Gets id of workspace which this project belongs to. */
    public String getWorkspace() {
        return baseFolder.getWorkspace();
    }

    /** Gets name of project. */
    public String getName() {
        return baseFolder.getName();
    }

    /** Gets path of project. */
    public String getPath() {
        return baseFolder.getPath();
    }

    /** Gets base folder of project. */
    public FolderEntry getBaseFolder() {
        return baseFolder;
    }

    /** Gets creation date of project in unix format or {@code -1} if creation date is unknown. */
    public long getCreationDate() throws ServerException {
        return getMisc().getCreationDate();
    }

    /** Gets most recent modification date of project in unix format or {@code -1} if modification date is unknown. */
    public long getModificationDate() throws ServerException {
        return getMisc().getModificationDate();
    }

    /** @see ProjectMisc */
    public ProjectMisc getMisc() throws ServerException {
        return manager.getProjectMisc(this);
    }

    /** @see ProjectMisc */
    public void saveMisc(ProjectMisc misc) throws ServerException {
        manager.saveProjectMisc(this, misc);
    }


    public ProjectConfig getConfig() throws ServerException, ValueStorageException, ProjectTypeConstraintException,
                                            InvalidValueException {

        final ProjectJson projectJson = ProjectJson.load(this);

        ProjectTypes types = new ProjectTypes(projectJson.getType(), projectJson.getMixinTypes());
        types.addTransient();

        final Map<String, AttributeValue> attributes = new HashMap<>();

        for (ProjectType t : types.all.values()) {

            for (Attribute attr : t.getAttributes()) {

                if (attr.isVariable()) {
                    Variable var = (Variable)attr;
                    final ValueProviderFactory factory = var.getValueProviderFactory();

                    List<String> val;
                    if (factory != null) {

                        val = factory.newInstance(baseFolder).getValues(var.getName());

                        if (val == null)
                            throw new ProjectTypeConstraintException(
                                    "Value Provider must not produce NULL value of variable " + var.getId());
                    } else {
                        val = projectJson.getAttributes().get(attr.getName());
                    }

                    if (val == null || val.isEmpty()) {
                        if (var.isRequired())
                            throw new ProjectTypeConstraintException(
                                    "No Value nor ValueProvider defined for required variable " + var.getId());
                        // else just not add it

                    } else {
                        attributes.put(var.getName(), new AttributeValue(val));

                    }

                } else {  // Constant

                    attributes.put(attr.getName(), attr.getValue());
                }
            }
        }


        Builders builders =
                (projectJson.getBuilders() == null) ? new Builders(types.primary.getDefaultBuilder()) : projectJson.getBuilders();
        Runners runners = (projectJson.getRunners() == null) ? new Runners(types.primary.getDefaultRunner()) : projectJson.getRunners();

//        return new ProjectConfig(projectJson.getDescription(), projectJson.getType(),
//                attributes, runners, builders, projectJson.getMixinTypes());

        return new ProjectConfig(projectJson.getDescription(), types.primary.getId(),
                                 attributes, runners, builders, types.mixinIds());
    }


    /**
     * Updates Project Config making all necessary validations
     *
     * @param update
     * @throws ServerException
     * @throws ValueStorageException
     * @throws ProjectTypeConstraintException
     * @throws InvalidValueException
     */
    public final void updateConfig(ProjectConfig update) throws ServerException, ValueStorageException,
                                                                ProjectTypeConstraintException, InvalidValueException {

        final ProjectJson projectJson = new ProjectJson();

        ProjectTypes types = new ProjectTypes(update.getTypeId(), update.getMixinTypes());
        types.removeTransient();

        projectJson.setType(types.primary.getId());
        projectJson.setBuilders(update.getBuilders());
        projectJson.setRunners(update.getRunners());
        projectJson.setDescription(update.getDescription());


        ArrayList<String> ms = new ArrayList<>();
        ms.addAll(types.mixins.keySet());
        projectJson.setMixinTypes(ms);

        // update attributes

        HashMap<String, AttributeValue> checkVariables = new HashMap<>();
        for (String attributeName : update.getAttributes().keySet()) {

            AttributeValue attributeValue = update.getAttributes().get(attributeName);

            // Try to Find definition in all the types
            Attribute definition = null;
            for (ProjectType t : types.all.values()) {
                definition = t.getAttribute(attributeName);
                if (definition != null)
                    break;
            }

            // initialize provided attributes
            if (definition != null && definition.isVariable()) {
                Variable var = (Variable)definition;

                final ValueProviderFactory valueProviderFactory = var.getValueProviderFactory();

                // calculate provided values
                if (valueProviderFactory != null) {
                    valueProviderFactory.newInstance(baseFolder).setValues(var.getName(), attributeValue.getList());
                }

                if (attributeValue == null && var.isRequired())
                    throw new ProjectTypeConstraintException("Required attribute value is initialized with null value " + var.getId());


                // store non-provided values into JSON
                if (valueProviderFactory == null)
                    projectJson.getAttributes().put(definition.getName(), attributeValue.getList());

                checkVariables.put(attributeName, attributeValue);

            }
        }

        for (ProjectType t : types.all.values()) {
            for (Attribute attr : t.getAttributes()) {
                if (attr.isVariable()) {
                    // check if required variables initialized
//                    if(attr.isRequired() && attr.getValue() == null) {
                    if (!checkVariables.containsKey(attr.getName()) && attr.isRequired()) {
                        throw new ProjectTypeConstraintException("Required attribute value is initialized with null value " + attr.getId());

                    }
                } else {
                    // add constants
                    projectJson.getAttributes().put(attr.getName(), attr.getValue().getList());
                }

            }
        }


        // Default builders and runners
        // NOTE we take it from Primary type only (for the time)
        // TODO? let's see for Machine API
        if (projectJson.getBuilders().getDefault() == null)
            projectJson.getBuilders().setDefault(types.primary.getDefaultBuilder());

        if (projectJson.getRunners().getDefault() == null)
            projectJson.getRunners().setDefault(types.primary.getDefaultRunner());


        projectJson.save(this);

    }

    /**
     * Gets visibility of this project, either 'private' or 'public'. Project is considered to be 'public' if any user has read access to
     * it.
     */
    public String getVisibility() throws ServerException {
        final List<AccessControlEntry> acl = baseFolder.getVirtualFile().getACL();
        if (acl.isEmpty()) {
            return "public";
        }
        final Principal guest = DtoFactory.getInstance().createDto(Principal.class).withName("any").withType(Principal.Type.USER);
        for (AccessControlEntry ace : acl) {
            if (guest.equals(ace.getPrincipal()) && ace.getPermissions().contains("read")) {
                return "public";
            }
        }
        return "private";
    }

    /**
     * Updates project privacy.
     *
     * @see #getVisibility()
     */
    public void setVisibility(String projectVisibility) throws ServerException, ForbiddenException {
        switch (projectVisibility) {
            case "private":
                final List<AccessControlEntry> acl = new ArrayList<>(1);
                final Principal developer = DtoFactory.getInstance().createDto(Principal.class)
                                                      .withName("workspace/developer")
                                                      .withType(Principal.Type.GROUP);
                acl.add(DtoFactory.getInstance().createDto(AccessControlEntry.class)
                                  .withPrincipal(developer)
                                  .withPermissions(Arrays.asList("all")));
                baseFolder.getVirtualFile().updateACL(acl, true, null);
                break;
            case "public":
                // Remove ACL. Default behaviour of underlying virtual filesystem: everyone can read but can't update.
                baseFolder.getVirtualFile().updateACL(Collections.<AccessControlEntry>emptyList(), true, null);
                break;
        }
    }

    static final String   ALL_PERMISSIONS      = BasicPermissions.ALL.value();
    static final String[] ALL_PERMISSIONS_LIST = {BasicPermissions.READ.value(), BasicPermissions.WRITE.value(),
                                                  BasicPermissions.UPDATE_ACL.value(), "build", "run"};

    /**
     * Gets security restriction applied to this project. Method returns empty {@code List} is project doesn't have any security
     * restriction.
     */
    public List<AccessControlEntry> getPermissions() throws ServerException {
        return getPermissions(baseFolder.getVirtualFile());
    }

    private List<AccessControlEntry> getPermissions(VirtualFile virtualFile) throws ServerException {
        while (virtualFile != null) {
            final List<AccessControlEntry> acl = virtualFile.getACL();
            if (!acl.isEmpty()) {
                for (AccessControlEntry ace : acl) {
                    final List<String> permissions = ace.getPermissions();
                    // replace "all" shortcut with list
                    if (permissions.remove(ALL_PERMISSIONS)) {
                        final Set<String> set = new LinkedHashSet<>(permissions);
                        Collections.addAll(set, ALL_PERMISSIONS_LIST);
                        permissions.clear();
                        permissions.addAll(set);
                    }
                }
                return acl;
            } else {
                virtualFile = virtualFile.getParent();
            }
        }
        return new ArrayList<>(4);
    }

    /**
     * Sets permissions to project.
     *
     * @param acl
     *         list of {@link org.eclipse.che.api.vfs.shared.dto.AccessControlEntry}
     */
    public void setPermissions(List<AccessControlEntry> acl) throws ServerException, ForbiddenException {
        final VirtualFile virtualFile = baseFolder.getVirtualFile();
        if (virtualFile.getACL().isEmpty()) {
            // Add permissions from closest parent file if project don't have own.
            final List<AccessControlEntry> l = new LinkedList<>();
            l.addAll(acl);
            l.addAll(getPermissions(virtualFile.getParent()));
            virtualFile.updateACL(l, true, null);
        } else {
            baseFolder.getVirtualFile().updateACL(acl, false, null);
        }
    }

    public VirtualFileEntry getItem(String path) throws ProjectTypeConstraintException,
                                                        ValueStorageException, ServerException, NotFoundException, ForbiddenException {
        final VirtualFileEntry entry = getVirtualFileEntry(path);
        GetItemHandler handler = manager.getHandlers().getGetItemHandler(getConfig().getTypeId());
        if (handler != null)
            handler.onGetItem(entry);
        //getConfig().getMixinTypes()
        return entry;
    }


    /**
     * @return list of paths to modules
     */
    public Modules getModules() {
        return modules;
    }


    private VirtualFileEntry getVirtualFileEntry(String path)
            throws NotFoundException, ForbiddenException, ServerException {
        final FolderEntry root = manager.getProjectsRoot(this.getWorkspace());
        final VirtualFileEntry entry = root.getChild(path);
        if (entry == null) {
            throw new NotFoundException(String.format("Path '%s' doesn't exist.", path));
        }
        return entry;
    }


    public class Modules {

        private final String MODULES_PATH = ".codenvy/modules";

        public void remove(String path) throws ForbiddenException, ServerException, ConflictException {

            Set<String> all = read();
            if (all.contains(path)) {
                all.remove(path);
                write(all);
            }

        }

        public void add(String path) throws ForbiddenException, ServerException, ConflictException {
            Set<String> all = read();
            all.add(path);
            write(all);
        }

        public void update(String oldPath, String newPath) throws ForbiddenException, ServerException, ConflictException {
            Set<String> all = new CopyOnWriteArraySet<>(read());

            all.remove(oldPath);
            all.add(newPath);

            //update subModule paths
            for (String modulePath: all) {
                if (modulePath.startsWith(oldPath + "/")) {
                    all.remove(modulePath);

                    String subModulePath = modulePath.replaceFirst(oldPath, newPath);
                    all.add(subModulePath);
                }
            }

            write(all);
        }

        public Set<String> get() throws ForbiddenException, ServerException {
            return read();
        }

        private Set<String> read() throws ForbiddenException, ServerException {
            HashSet<String> modules = new HashSet<>();
            VirtualFileEntry file = null;

            file = baseFolder.getChild(MODULES_PATH);

            if (file == null || file.isFolder())
                return modules;

            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(((FileEntry)file).getInputStream()));
                while (in.ready()) {
                    modules.add(in.readLine());
                }
                in.close();
            } catch (IOException e) {
                throw new ServerException(e);
            }

            return modules;
        }

        private void write(Set<String> modules) throws ForbiddenException, ServerException, ConflictException {

            VirtualFileEntry file = null;
            file = baseFolder.getChild(MODULES_PATH);

            if (file == null && !modules.isEmpty())
                file = ((FolderEntry)baseFolder.getChild(".codenvy")).createFile("modules", new byte[0], "text/plain");

//                if(modules.isEmpty() && file != null)
//                    file.remove();

            String all = "";
            for (String path : modules) {
                all += (path + "\n");
            }

            ((FileEntry)file).updateContent(all.getBytes());

        }


    }


    private class ProjectTypes {

        ProjectType primary;
        Map<String, ProjectType> mixins = new HashMap<>();
        Map<String, ProjectType> all    = new HashMap<>();

        ProjectTypes(String pt, List<String> mss) throws ProjectTypeConstraintException {
            if (pt == null)
                throw new ProjectTypeConstraintException("No primary type defined for " + getWorkspace() + " : " + getPath());

            primary = manager.getProjectTypeRegistry().getProjectType(pt);
            if (primary == null)
                throw new ProjectTypeConstraintException("No project type registered for " + pt);
            if (!primary.canBePrimary())
                throw new ProjectTypeConstraintException("Project type " + primary.getId() + " is not allowable to be primary type");
            all.put(primary.getId(), primary);

            if (mss == null)
                mss = new ArrayList<>();

            // temporary storage to detect duplicated attributes
            HashMap<String, Attribute> tmpAttrs = new HashMap<>();
            for (Attribute attr : primary.getAttributes()) {
                tmpAttrs.put(attr.getName(), attr);
            }


            for (String m : mss) {
                if (!m.equals(primary.getId())) {

                    ProjectType mixin = manager.getProjectTypeRegistry().getProjectType(m);
                    if (mixin == null)
                        throw new ProjectTypeConstraintException("No project type registered for " + m);
                    if (!mixin.canBeMixin())
                        throw new ProjectTypeConstraintException("Project type " + mixin + " is not allowable to be mixin");

                    // detect duplicated attributes
                    for (Attribute attr : mixin.getAttributes()) {
                        if (tmpAttrs.containsKey(attr.getName()))
                            throw new ProjectTypeConstraintException(
                                    "Attribute name conflict. Duplicated attributes detected " + getPath() +
                                    " Attribute " + attr.getName() + " declared in " + mixin.getId() + " already declared in " +
                                    tmpAttrs.get(attr.getName()).getProjectType());

                        tmpAttrs.put(attr.getName(), attr);
                    }


                    // Silently remove repeated items from mixins if any
                    mixins.put(m, mixin);
                    all.put(m, mixin);

                }

            }

        }

        void removeTransient() {

            HashSet<String> toRemove = new HashSet<>();
            for (ProjectType mt : all.values()) {
                if (!mt.isPersisted())
                    toRemove.add(mt.getId());
            }

            for (String id : toRemove) {
                all.remove(id);
                mixins.remove(id);
            }

        }

        void addTransient() throws ServerException {
            List<SourceEstimation> estimations;
            try {
                estimations = manager.resolveSources(baseFolder.getWorkspace(), baseFolder.getPath(), true);
            } catch (Exception e) {
                throw new ServerException(e);
            }
            for (SourceEstimation est : estimations) {
                ProjectType type = manager.getProjectTypeRegistry().getProjectType(est.getType());

                // NOTE: Only mixable types allowed
                if (type.canBeMixin()) {
                    all.put(type.getId(), type);
                    mixins.put(type.getId(), type);
                }

            }
        }

        List<String> mixinIds() {
            return new ArrayList<>(mixins.keySet());
        }


    }

}
