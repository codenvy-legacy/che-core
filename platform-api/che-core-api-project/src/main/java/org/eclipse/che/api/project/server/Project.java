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
import org.eclipse.che.api.vfs.server.VirtualFile;
import org.eclipse.che.api.vfs.shared.dto.AccessControlEntry;
import org.eclipse.che.api.vfs.shared.dto.Principal;
import org.eclipse.che.api.vfs.shared.dto.VirtualFileSystemInfo.BasicPermissions;
import org.eclipse.che.dto.server.DtoFactory;

import javax.ws.rs.core.MediaType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import static org.eclipse.che.api.project.server.Constants.CODENVY_DIR;

/**
 * Server side representation for codenvy project.
 *
 * @author andrew00x
 * @author Eugene Voevodin
 */
public class Project {
    static final String   ALL_PERMISSIONS      = BasicPermissions.ALL.value();
    static final String[] ALL_PERMISSIONS_LIST = {BasicPermissions.READ.value(), BasicPermissions.WRITE.value(),
                                                  BasicPermissions.UPDATE_ACL.value(), "build", "run"};
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

    public String getContentRoot() throws ServerException {
        return getMisc().getContentRoot();
    }

    /** @see ProjectMisc */
    public ProjectMisc getMisc() throws ServerException {
        return manager.getProjectMisc(this);
    }

    /** @see ProjectMisc */
    public void saveMisc(ProjectMisc misc) throws ServerException {
        manager.saveProjectMisc(this, misc);
    }

    public ProjectConfig getConfig() throws ServerException, ValueStorageException, ProjectTypeConstraintException, InvalidValueException {
        return manager.getProjectConfig(this);
    }

    /**
     * Updates Project Config making all necessary validations.
     *
     * @param config
     * @throws ServerException
     * @throws ValueStorageException
     * @throws ProjectTypeConstraintException
     * @throws InvalidValueException
     */
    public final void updateConfig(ProjectConfig config) throws ServerException,
                                                                ValueStorageException,
                                                                ProjectTypeConstraintException,
                                                                InvalidValueException {
        manager.updateProjectConfig(this, config);
    }

    /**
     * Gets visibility of this project, either 'private' or 'public'.Project is considered to be 'public' if any user has read access to
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

    /**
     * Gets security restriction applied to this project. Method returns empty {@code List} is project doesn't have any security
     * restriction.
     */
    public List<AccessControlEntry> getPermissions() throws ServerException {
        return getPermissions(baseFolder.getVirtualFile());
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

    public VirtualFileEntry getItem(String path) throws ProjectTypeConstraintException,
                                                        ValueStorageException, ServerException, NotFoundException, ForbiddenException {
        final VirtualFileEntry entry = getVirtualFileEntry(path);
        GetItemHandler handler = manager.getHandlers().getGetItemHandler(getConfig().getTypeId());
        if (handler != null)
            handler.onGetItem(entry);
        return entry;
    }

    /**
     * @return list of paths to modules
     */
    public Modules getModules() {
        return modules;
    }

    private VirtualFileEntry getVirtualFileEntry(String path) throws NotFoundException, ForbiddenException, ServerException {
        final FolderEntry root = manager.getProjectsRoot(this.getWorkspace());
        final VirtualFileEntry entry = root.getChild(path);
        if (entry == null) {
            throw new NotFoundException(String.format("Path '%s' doesn't exist.", path));
        }
        return entry;
    }

    public class Modules {
        private final String MODULES_PATH = CODENVY_DIR + "/modules";

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
            for (String modulePath : all) {
                if (modulePath.startsWith(oldPath + "/")) {
                    all.remove(modulePath);

                    String subModulePath = modulePath.replaceFirst(oldPath, newPath);
                    all.add(subModulePath);
                }
            }

            write(all);
        }

        public boolean remove(String path) throws ForbiddenException, ServerException, ConflictException {
            Set<String> all = read();
            if (all.contains(path)) {
                all.remove(path);
                write(all);
                return true;
            }
            return false;
        }

        public Set<String> get() throws ForbiddenException, ServerException {
            return read();
        }

        private Set<String> read() throws ForbiddenException, ServerException {
            HashSet<String> modules = new HashSet<>();
            VirtualFileEntry file = baseFolder.getChild(MODULES_PATH);

            if (file == null || file.isFolder()) {
                return modules;
            }

            try (BufferedReader in = new BufferedReader(new InputStreamReader(((FileEntry)file).getInputStream()))) {
                while (in.ready()) {
                    modules.add(in.readLine());
                }
            } catch (IOException e) {
                throw new ServerException(e);
            }

            return modules;
        }

        private void write(Set<String> modules) throws ForbiddenException, ServerException, ConflictException {
            VirtualFileEntry file = baseFolder.getChild(MODULES_PATH);

            if (file == null && !modules.isEmpty()) {
                file = ((FolderEntry)baseFolder.getChild(".codenvy")).createFile("modules", new byte[0], MediaType.TEXT_PLAIN);
            }

            String all = "";
            for (String path : modules) {
                all += (path + "\n");
            }

            if (file != null) {
                ((FileEntry)file).updateContent(all.getBytes());
            }
        }
    }
}
