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
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.core.notification.EventSubscriber;
import org.eclipse.che.api.project.server.handlers.CreateModuleHandler;
import org.eclipse.che.api.project.server.handlers.CreateProjectHandler;
import org.eclipse.che.api.project.server.handlers.GetModulesHandler;
import org.eclipse.che.api.project.server.handlers.ProjectHandlerRegistry;
import org.eclipse.che.api.project.server.handlers.ProjectTypeChangedHandler;
import org.eclipse.che.api.project.server.handlers.RemoveModuleHandler;
import org.eclipse.che.api.project.server.notification.ProjectItemModifiedEvent;
import org.eclipse.che.api.project.server.type.Attribute;
import org.eclipse.che.api.project.server.type.AttributeValue;
import org.eclipse.che.api.project.server.type.BaseProjectType;
import org.eclipse.che.api.project.server.type.ProjectType;
import org.eclipse.che.api.project.server.type.ProjectTypeRegistry;
import org.eclipse.che.api.project.server.type.Variable;
import org.eclipse.che.api.project.shared.dto.SourceEstimation;

import org.eclipse.che.api.vfs.server.Path;
import org.eclipse.che.api.vfs.server.VirtualFileSystemRegistry;
import org.eclipse.che.api.vfs.server.observation.VirtualFileEvent;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.lang.Pair;
import org.eclipse.che.commons.lang.cache.Cache;
import org.eclipse.che.commons.lang.cache.SLRUCache;
import org.eclipse.che.dto.server.DtoFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

import static com.google.common.base.MoreObjects.firstNonNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author andrew00x
 */
@Singleton
public final class DefaultProjectManager implements ProjectManager {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultProjectManager.class);

    private static final int CACHE_NUM  = 1 << 2;
    private static final int CACHE_MASK = CACHE_NUM - 1;
    private static final int SEG_SIZE   = 32;

    private final Lock[]                                     miscLocks;
    private final Cache<Pair<String, String>, ProjectMisc>[] miscCaches;

    private final VirtualFileSystemRegistry         fileSystemRegistry;
    private final EventService                      eventService;
    private final EventSubscriber<VirtualFileEvent> vfsSubscriber;
    private final ProjectTypeRegistry               projectTypeRegistry;
    private final ProjectHandlerRegistry            handlers;


    @Inject
    @SuppressWarnings("unchecked")
    public DefaultProjectManager(VirtualFileSystemRegistry fileSystemRegistry,
                                 EventService eventService,
                                 ProjectTypeRegistry projectTypeRegistry,
                                 ProjectHandlerRegistry handlers) {

        this.fileSystemRegistry = fileSystemRegistry;
        this.eventService = eventService;
        this.projectTypeRegistry = projectTypeRegistry;
        //this.handler = handler;
        this.handlers = handlers;


        this.miscCaches = new Cache[CACHE_NUM];
        this.miscLocks = new Lock[CACHE_NUM];
        for (int i = 0; i < CACHE_NUM; i++) {
            miscLocks[i] = new ReentrantLock();
            miscCaches[i] = new SLRUCache<Pair<String, String>, ProjectMisc>(SEG_SIZE, SEG_SIZE) {
                @Override
                protected void evict(Pair<String, String> key, ProjectMisc value) {
                    if (value.isUpdated()) {
                        final int index = key.hashCode() & CACHE_MASK;
                        miscLocks[index].lock();
                        try {
                            writeProjectMisc(value.getProject(), value);
                        } catch (Exception e) {
                            LOG.error(e.getMessage(), e);
                        } finally {
                            miscLocks[index].unlock();
                        }
                        super.evict(key, value);
                    }
                }
            };
        }

        vfsSubscriber = new EventSubscriber<VirtualFileEvent>() {
            @Override
            public void onEvent(VirtualFileEvent event) {
                final String workspace = event.getWorkspaceId();
                final String path = event.getPath();
                if (path.endsWith(Constants.CODENVY_MISC_FILE_RELATIVE_PATH)) {
                    return;
                }
                switch (event.getType()) {
                    case CONTENT_UPDATED:
                    case CREATED:
                    case DELETED:
                    case MOVED:
                    case RENAMED: {
                        final int length = path.length();
                        for (int i = 1; i < length && (i = path.indexOf('/', i)) > 0; i++) {
                            final String projectPath = path.substring(0, i);
                            try {
                                final Project project = getProject(workspace, projectPath);
                                if (project != null) {
                                    getProjectMisc(project).setModificationDate(System.currentTimeMillis());
                                }
                            } catch (Exception e) {
                                LOG.error(e.getMessage(), e);
                            }
                        }
                        break;
                    }
                }
            }
        };
    }


    /**
     * Gets the list of projects in {@code workspace}.
     *
     * @param workspace
     *         id of workspace
     * @return the list of projects in specified workspace.
     * @throws ServerException
     *         if an error occurs
     */
    public List<Project> getProjects(String workspace) throws ServerException, NotFoundException {
        final FolderEntry myRoot = getProjectsRoot(workspace);
        final List<Project> projects = new ArrayList<>();
        for (FolderEntry folder : myRoot.getChildFolders()) {
            if (folder.isProjectFolder()) {
                projects.add(new Project(folder, this));
            }
        }
        return projects;
    }

    /**
     * Gets single project by id of workspace and project's path in this workspace.
     *
     * @param workspace
     *         id of workspace
     * @param projectPath
     *         project's path
     * @return requested project or {@code null} if project was not found
     * @throws ForbiddenException
     *         if user which perform operation doesn't have access to the requested project
     * @throws ServerException
     *         if other error occurs
     */
    public Project getProject(String workspace, String projectPath) throws ForbiddenException, ServerException, NotFoundException {
        final FolderEntry myRoot = getProjectsRoot(workspace);
        final VirtualFileEntry child = myRoot.getChild(projectPath.startsWith("/") ? projectPath.substring(1) : projectPath);
        if (child != null && child.isFolder() && ((FolderEntry)child).isProjectFolder()) {
            return new Project((FolderEntry)child, this);
        }
        return null;
    }


    /**
     * Creates new project.
     *
     * @param workspace
     *         id of workspace
     * @param name
     *         project's name
     * @param projectConfig
     *         project description
     * @return newly created project
     * @throws ConflictException
     *         if operation causes conflict, e.g. name conflict if project with specified name already exists
     * @throws ForbiddenException
     *         if user which perform operation doesn't have required permissions
     * @throws ServerException
     *         if other error occurs
     */
    public Project createProject(String workspace,
                                 String name,
                                 ProjectConfig projectConfig,
                                 Map<String, String> options,
                                 String visibility) throws ConflictException, ForbiddenException, ServerException, NotFoundException {
        final FolderEntry myRoot = getProjectsRoot(workspace);
        return createProjectInFolder(myRoot, name, projectConfig, options, visibility);
    }

    /**
     * Create a new project inside an existing folder and initialize its meta-data.
     * This is meant for internal use only.
     */
    private Project createProjectInFolder(FolderEntry parentFolder, String name, ProjectConfig projectConfig,
            Map<String, String> options, String visibility)
                    throws ConflictException, ForbiddenException, ServerException, NotFoundException {
        // Create a folder for the project
        final FolderEntry projectFolder = parentFolder.createFolder(name);
        final Project project = new Project(projectFolder, this);
        // If a creation handler is defined for the project type, call it
        final CreateProjectHandler generator = handlers.getCreateProjectHandler(projectConfig.getTypeId());
        if (generator != null) {
            generator.onCreateProject(project.getBaseFolder(), projectConfig.getAttributes(), options);
        }
        // Save the configuration
        project.updateConfig(projectConfig);
        finalizeProjectCreation(project, visibility);
        return project;
    }

    /**
     * Finalize the creation of a new project after writing its initial configuration.
     * This is meant for internal use only.
     */
    private void finalizeProjectCreation(Project project, String visibility) throws ServerException, ForbiddenException {
        // Set creation date
        final ProjectMisc misc = project.getMisc();
        misc.setCreationDate(System.currentTimeMillis());
        misc.save(); // Important to save misc!!
        // Set an explicit visibility if necessary
        if (visibility != null) {
            project.setVisibility(visibility);
        }
    }

    /**
     * Update the given project
     * @param workspace The workspace that contains the project to update.
     * @param path The path to the project.
     * @param newConfig The new configuration of the project
     * @param newVisibility Then new visibility of the project. If not, visibility is not changed.
     * @return The updated project.
     */
    @Override
    public Project updateProject(String workspace, String path, ProjectConfig newConfig, String newVisibility)
            throws ForbiddenException, ServerException, NotFoundException, ConflictException, IOException {
        Project project = getProject(workspace, path);
        String oldProjectType = null;
        List<String> oldMixinTypes = new ArrayList<>();
        // If a project does not exist in the target path, create a new one
        if (project == null) {
            FolderEntry projectsRoot = getProjectsRoot(workspace);
            VirtualFileEntry child = projectsRoot.getChild(path);
            if (child != null && child.isFolder() && child.getParent().isRoot()) {
                project = new Project((FolderEntry) child, this);
            } else {
                throw new NotFoundException(
                        String.format("Project '%s' doesn't exist in workspace '%s'.", path, workspace));
            }
        } else {
            try {
                ProjectConfig config = project.getConfig();
                oldProjectType = config.getTypeId();
                oldMixinTypes = config.getMixinTypes();
            } catch (ProjectTypeConstraintException | ValueStorageException e) {
                // here we allow changing bad project type on registered
                LOG.warn(e.getMessage());
            }
        }
        // Update the visibility if asked to do so
        if (newVisibility != null && !newVisibility.isEmpty()) {
            project.setVisibility(newVisibility);
        }
        project.updateConfig(newConfig);
        // handle project type changes
        // post actions on changing project type
        // base or mixin
        if (!newConfig.getTypeId().equals(oldProjectType)) {
            ProjectTypeChangedHandler projectTypeChangedHandler = handlers
                    .getProjectTypeChangedHandler(newConfig.getTypeId());
            if (projectTypeChangedHandler != null) {
                projectTypeChangedHandler.onProjectTypeChanged(project.getBaseFolder());
            }
        }
        List<String> mixinTypes = firstNonNull(newConfig.getMixinTypes(), Collections.<String> emptyList());
        for (String mixin : mixinTypes) {
            if (!oldMixinTypes.contains(mixin)) {
                ProjectTypeChangedHandler projectTypeChangedHandler = handlers.getProjectTypeChangedHandler(mixin);
                if (projectTypeChangedHandler != null) {
                    projectTypeChangedHandler.onProjectTypeChanged(project.getBaseFolder());
                }
            }
        }
        return project;
    }

    /**
     * Adds module to parent project. If module does not exist creates it before.
     *
     * @param workspace
     * @param projectPath
     *         - parent project path
     * @param modulePath
     *         - path for the module to add
     * @param moduleConfig
     *         - module configuration (optional, needed only if module does not exist)
     * @param options
     *         - options for module creation (optional, same as moduleConfig)
     * @param visibility
     *         - visibility for the module (optional, same as moduleConfig)
     * @return
     * @throws ConflictException
     * @throws ForbiddenException
     * @throws ServerException
     * @throws NotFoundException
     */
    public Project addModule(String workspace, String projectPath, String modulePath, ProjectConfig moduleConfig, Map<String,
            String> options, String visibility)
            throws ConflictException, ForbiddenException, ServerException, NotFoundException {

        Project parentProject = getProject(workspace, projectPath);
        if (parentProject == null)
            throw new NotFoundException("Parent Project not found " + projectPath);

        // Normalize the existing module paths and verify that there are no conflicts
        Path absModulePathObj = Project.getAbsoluteModulePath(modulePath, parentProject);
        Set<Path> existingAbsModulePaths = parentProject.getModules().getAbsolute();
        if (existingAbsModulePaths.contains(absModulePathObj)) {
            throw new ConflictException("Module " + modulePath + " already exists");
        }

        String absModulePath = absModulePathObj.toString();

        VirtualFileEntry moduleFolder = getProjectsRoot(workspace).getChild(absModulePath);
        if (moduleFolder != null && moduleFolder.isFile())
            throw new ConflictException("Item exists on " + absModulePath + " but is not a folder or project");

        Project module;
        // there are no source folder for module
        // create folder and make it project and update config
        if (moduleFolder == null) {
            if (moduleConfig == null)
                throw new ConflictException("Module not found on " + absModulePath + " and module configuration is not defined");
            String parentPath = absModulePathObj.getParent().toString();
            String name = absModulePathObj.getName();
            final VirtualFileEntry parentFolder = getProjectsRoot(workspace).getChild(parentPath);
            if (parentFolder == null || !parentFolder.isFolder())
                throw new NotFoundException("Parent Folder not found " + parentPath);
            // Create the new module's project
            module = createProjectInFolder((FolderEntry)parentFolder, name, moduleConfig, options, visibility);
        } else if (!((FolderEntry)moduleFolder).isProjectFolder()) {
            //  folder exists but is not a project, just update config
            if (moduleConfig == null)
                throw new ConflictException("Folder at " + absModulePath + " is not a project and module configuration is not defined");
            module = new Project((FolderEntry)moduleFolder, this);
            module.updateConfig(moduleConfig);
        } else {
            // project module exists
            module = getProject(workspace, absModulePath);
        }

        // finally adds the module to parent
        parentProject.getModules().add(modulePath);

        CreateModuleHandler moduleHandler = this.getHandlers().getCreateModuleHandler(parentProject.getConfig().getTypeId());
        if (moduleHandler != null) {
            moduleHandler.onCreateModule(parentProject.getBaseFolder(), absModulePath, module.getConfig(), options);
        }
        return module;


    }

    /**
     * Gets root folder of project tree.
     *
     * @param workspace
     *         id of workspace
     * @return root folder
     * @throws ServerException
     *         if an error occurs
     */
    public FolderEntry getProjectsRoot(String workspace) throws ServerException, NotFoundException {
        return new FolderEntry(workspace, fileSystemRegistry.getProvider(workspace).getMountPoint(true).getRoot());
    }

    /**
     * Gets ProjectMisc.
     *
     * @param project
     *         project
     * @return ProjectMisc
     * @throws ServerException
     *         if an error occurs
     * @see ProjectMisc
     */
    public ProjectMisc getProjectMisc(Project project) throws ServerException {
        final String workspace = project.getWorkspace();
        final String path = project.getPath();
        final Pair<String, String> key = Pair.of(workspace, path);
        final int index = key.hashCode() & CACHE_MASK;
        miscLocks[index].lock();
        try {
            ProjectMisc misc = miscCaches[index].get(key);
            if (misc == null) {
                miscCaches[index].put(key, misc = readProjectMisc(project));
            }
            return misc;
        } finally {
            miscLocks[index].unlock();
        }
    }


    public Set<Project> getProjectModules(Project parent)
            throws ServerException, ForbiddenException, ConflictException, IOException, NotFoundException {
        final List<String> modulePaths = new LinkedList<>();
        final Set<Project> modules = new LinkedHashSet<>();
        for (String p : parent.getModules().get()) {
            String modulePath = p.startsWith("/") ? p : parent.getPath() + "/" + p;
            modulePaths.add(modulePath);
        }

        //get modules via handler
        GetModulesHandler modulesHandler = handlers.getModulesHandler(parent.getConfig().getTypeId());
        if (modulesHandler != null) {
            modulesHandler.onGetModules(parent.getBaseFolder(), modulePaths);
        }

        for (String modulePath : modulePaths) {
            Project module = this.getProject(parent.getWorkspace(), modulePath);
            if (module != null && module.getBaseFolder().isProjectFolder()) {
                modules.add(module);
            }
        }
        return modules;
    }

    private ProjectMisc readProjectMisc(Project project) throws ServerException {
        try {
            ProjectMisc misc;
            final FileEntry miscFile = (FileEntry)project.getBaseFolder().getChild(Constants.CODENVY_MISC_FILE_RELATIVE_PATH);
            if (miscFile != null) {
                try (InputStream in = miscFile.getInputStream()) {
                    final Properties properties = new Properties();
                    properties.loadFromXML(in);
                    misc = new ProjectMisc(properties, project);
                } catch (IOException e) {
                    throw new ServerException(e.getMessage(), e);
                }
            } else {
                misc = new ProjectMisc(project);
            }
            return misc;
        } catch (ForbiddenException e) {
            // If have access to the project then must have access to its meta-information. If don't have access then treat that as
            // server error.
            throw new ServerException(e.getServiceError());
        }
    }

    /**
     * Gets ProjectMisc.
     *
     * @param project
     *         project
     * @param misc
     *         ProjectMisc
     * @throws ServerException
     *         if an error occurs
     * @see ProjectMisc
     */
    public void saveProjectMisc(Project project, ProjectMisc misc) throws ServerException {
        if (misc.isUpdated()) {
            final String workspace = project.getWorkspace();
            final String path = project.getPath();
            final Pair<String, String> key = Pair.of(workspace, path);
            final int index = key.hashCode() & CACHE_MASK;
            miscLocks[index].lock();
            try {
                miscCaches[index].remove(key);
                writeProjectMisc(project, misc);
                miscCaches[index].put(key, misc);
            } finally {
                miscLocks[index].unlock();
            }
        }
    }

    private void writeProjectMisc(Project project, ProjectMisc misc) throws ServerException {
        try {
            final ByteArrayOutputStream bout = new ByteArrayOutputStream();
            try {
                misc.asProperties().storeToXML(bout, null);
            } catch (IOException e) {
                throw new ServerException(e.getMessage(), e);
            }
            FileEntry miscFile = (FileEntry)project.getBaseFolder().getChild(Constants.CODENVY_MISC_FILE_RELATIVE_PATH);
            if (miscFile != null) {
                miscFile.updateContent(bout.toByteArray(), null);
            } else {
                FolderEntry codenvy = (FolderEntry)project.getBaseFolder().getChild(Constants.CODENVY_DIR);
                if (codenvy == null) {
                    try {
                        codenvy = project.getBaseFolder().createFolder(Constants.CODENVY_DIR);
                    } catch (ConflictException e) {
                        // Already checked existence of folder ".codenvy".
                        throw new ServerException(e.getServiceError());
                    }
                }
                try {
                    codenvy.createFile(Constants.CODENVY_MISC_FILE, bout.toByteArray(), null);
                } catch (ConflictException e) {
                    // Not expected, existence of file already checked
                    throw new ServerException(e.getServiceError());
                }
            }
            LOG.debug("Save misc file of project {} in {}", project.getPath(), project.getWorkspace());
        } catch (ForbiddenException e) {
            // If have access to the project then must have access to its meta-information. If don't have access then treat that as
            // server error.
            throw new ServerException(e.getServiceError());
        }
    }


    @PostConstruct
    void start() {
        eventService.subscribe(vfsSubscriber);
    }

    @PreDestroy
    void stop() {
        eventService.unsubscribe(vfsSubscriber);
        for (int i = 0, length = miscLocks.length; i < length; i++) {
            miscLocks[i].lock();
            try {
                miscCaches[i].clear();
            } finally {
                miscLocks[i].unlock();
            }
        }
    }


    public VirtualFileSystemRegistry getVirtualFileSystemRegistry() {
        return fileSystemRegistry;
    }

    public ProjectTypeRegistry getProjectTypeRegistry() {
        return this.projectTypeRegistry;
    }

    public ProjectHandlerRegistry getHandlers() {
        return handlers;
    }

    public Map<String, AttributeValue> estimateProject(String workspace, String path, String projectTypeId)
            throws ServerException, ForbiddenException, NotFoundException, ValueStorageException,
                   ProjectTypeConstraintException {


        ProjectType projectType = projectTypeRegistry.getProjectType(projectTypeId);
        if (projectType == null)
            throw new NotFoundException("Project Type " + projectTypeId + " not found.");

        final VirtualFileEntry baseFolder = getProjectsRoot(workspace).getChild(path.startsWith("/") ? path.substring(1) : path);
        if (!baseFolder.isFolder()) {
            throw new NotFoundException("Not a folder: " + path);
        }

        Map<String, AttributeValue> attributes = new HashMap<>();

        for (Attribute attr : projectType.getAttributes()) {

            if (attr.isVariable() && ((Variable)attr).getValueProviderFactory() != null) {

                Variable var = (Variable)attr;

                // getValue throws ValueStorageException if not valid
                AttributeValue value = var.getValue((FolderEntry)baseFolder);
                if (var.isRequired() && value.getList().isEmpty()) {
                    throw new ValueStorageException("Can't get required value for " + attr.getName());
                }
                attributes.put(attr.getName(), value);
            }

        }

        return attributes;

    }

    // ProjectSuggestion
    public List<SourceEstimation> resolveSources(String workspace, String path, boolean transientOnly)
            throws ServerException, ForbiddenException, NotFoundException,
                   ProjectTypeConstraintException {
        final List<SourceEstimation> estimations = new ArrayList<>();

        for (ProjectType type : projectTypeRegistry.getProjectTypes(ProjectTypeRegistry.CHILD_TO_PARENT_COMPARATOR)) {

            if (transientOnly && type.isPersisted())
                continue;

            final HashMap<String, List<String>> attributes = new HashMap<>();


            try {
                for (Map.Entry<String, AttributeValue> attr : estimateProject(workspace, path, type.getId()).entrySet()) {
                    List<String> values = attr.getValue().getList();
                    if (values != null && !values.isEmpty()) {
                        attributes.put(attr.getKey(), values);
                    }
                }

                if (!attributes.isEmpty()) {
                    estimations.add(
                            DtoFactory.getInstance().createDto(SourceEstimation.class)
                                      .withType(type.getId())
                                      .withPrimaryable(type.canBePrimary())
                                      .withAttributes(attributes));

                }

            } catch (ValueStorageException e) {
                LOG.warn(e.getLocalizedMessage());
            }

        }
        if (estimations.isEmpty()) {
            estimations.add(
                    DtoFactory.getInstance().createDto(SourceEstimation.class)
                              .withType(BaseProjectType.ID));
        }


        return estimations;
    }


    /**
     * Converts existed Folder to Project
     * - using projectConfig if it is not null or use internal metainformation (/.codenvy)
     *
     * @param workspace
     * @param projectConfig
     * @param visibility
     * @return
     * @throws ConflictException
     * @throws ForbiddenException
     * @throws ServerException
     * @throws ProjectTypeConstraintException
     */
    @Override
    public Project convertFolderToProject(String workspace, String path, ProjectConfig projectConfig, String visibility)
            throws ConflictException, ForbiddenException, ServerException, NotFoundException {


        final VirtualFileEntry projectEntry = getProjectsRoot(workspace).getChild(path);
        if (projectEntry == null || !projectEntry.isFolder())
            throw new NotFoundException("Not found or not a folder " + path);

        FolderEntry projectFolder = (FolderEntry)projectEntry;

        final Project project = new Project(projectFolder, this);

        // Update config
        if (projectConfig != null && projectConfig.getTypeId() != null) {
            //TODO: need add checking for concurrency attributes name in giving config and in estimation
            Map<String, AttributeValue> estimateProject = estimateProject(workspace, path, projectConfig.getTypeId());
            projectConfig.getAttributes().putAll(estimateProject);
            project.updateConfig(projectConfig);
        } else {  // try to get config (it will throw exception in case config is not valid)
            project.getConfig();
        }

        finalizeProjectCreation(project, visibility);

        return project;
    }

    @Override
    public VirtualFileEntry rename(String workspace, String path, String newName, String newMediaType)
            throws ForbiddenException, ServerException, ConflictException, NotFoundException {
        final FolderEntry root = getProjectsRoot(workspace);
        final VirtualFileEntry entry = root.getChild(path);
        if (entry == null) {
            return null;
        }
        if (entry.isFile() && newMediaType != null) {
            // Use the same rules as in method createFile to make client side simpler.
            ((FileEntry) entry).rename(newName, newMediaType);
        } else {
            entry.rename(newName);
            String projectName = projectPath(path);
            /* We should not edit Modules if resource to rename is project */
            if (!projectName.equals(path) && entry.isFolder() && ((FolderEntry) entry).isProjectFolder()) {
                Project project = getProject(workspace, projectName);

                /*
                 * We need module path without projectName, f.e projectName/module1/oldModuleName ->
                 * module1/oldModuleName
                 */
                String oldModulePath = path.replaceFirst(projectName + "/", "");
                /* Calculates new module path, f.e module1/oldModuleName -> module1/newModuleName */
                String newModulePath = oldModulePath.substring(0, oldModulePath.lastIndexOf("/") + 1) + newName;

                project.getModules().update(oldModulePath, newModulePath);
            }
        }
        return entry;
    }

    @Override
    public boolean delete(String workspace, String path, String modulePath)
            throws ServerException, ForbiddenException, NotFoundException, ConflictException {
        final FolderEntry root = getProjectsRoot(workspace);
        final VirtualFileEntry entry = root.getChild(path);
        if (entry == null) {
            return false;
        }
        if (entry.isFolder() && ((FolderEntry) entry).isProjectFolder()) {
            // In case of folder extract some information about project for logger before delete project.
            Project project = new Project((FolderEntry) entry, this);
            // remove module only
            if (modulePath != null) {
                RemoveModuleHandler removeModuleHandler = this.getHandlers().getRemoveModuleHandler(project.getConfig().getTypeId());
                if (removeModuleHandler != null) {
                    removeModuleHandler.onRemoveModule(project.getBaseFolder(), modulePath, project.getConfig());
                }
                Set<String> modules = project.getModules().get();
                if (modules == null || modules.isEmpty()) {
                    return false;
                }
                if (modules.contains(modulePath)) {
                    return project.getModules().remove(modulePath);
                } else {
                    String absoluteModulePath = project.getPath() + "/" + modulePath;
                    return project.getModules().remove(absoluteModulePath);
                }
            }
            final String name = project.getName();
            String projectType = null;
            try {
                projectType = project.getConfig().getTypeId();
            } catch (ServerException | ValueStorageException | ProjectTypeConstraintException e) {
                // Let delete even project in invalid state.
                entry.remove();
                LOG.info("EVENT#project-destroyed# PROJECT#{}# TYPE#{}# WS#{}# USER#{}#", name, "unknown",
                        EnvironmentContext.getCurrent().getWorkspaceName(),
                        EnvironmentContext.getCurrent().getUser().getName());
                LOG.warn(String.format("Removing not valid project ws : %s, project path: %s ", workspace, path)
                        + e.getMessage(), e);
            }
            entry.remove();
            LOG.info("EVENT#project-destroyed# PROJECT#{}# TYPE#{}# WS#{}# USER#{}#", name, projectType,
                    EnvironmentContext.getCurrent().getWorkspaceName(),
                    EnvironmentContext.getCurrent().getUser().getName());
        } else {
            eventService.publish(new ProjectItemModifiedEvent(ProjectItemModifiedEvent.EventType.DELETED, workspace,
                    projectPath(entry.getPath()), entry.getPath(), entry.isFolder()));
            entry.remove();
        }
        return true;
    }

    private static String projectPath(String path) {
        int end = path.indexOf("/");
        if (end == -1) {
            return path;
        }
        return path.substring(0, end);
    }

}
