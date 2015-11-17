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

import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.workspace.ModuleConfig;
import org.eclipse.che.api.core.model.workspace.ProjectConfig;
import org.eclipse.che.api.core.model.workspace.WorkspaceConfig;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.core.notification.EventSubscriber;
import org.eclipse.che.api.core.rest.HttpJsonHelper;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.project.server.handlers.CreateModuleHandler;
import org.eclipse.che.api.project.server.handlers.CreateProjectHandler;
import org.eclipse.che.api.project.server.handlers.PostImportProjectHandler;
import org.eclipse.che.api.project.server.handlers.ProjectCreatedHandler;
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
import org.eclipse.che.api.vfs.server.VirtualFileSystemRegistry;
import org.eclipse.che.api.vfs.server.observation.VirtualFileEvent;
import org.eclipse.che.api.workspace.server.WorkspaceService;
import org.eclipse.che.api.workspace.shared.dto.ModuleConfigDto;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.api.workspace.shared.dto.SourceStorageDto;
import org.eclipse.che.api.workspace.shared.dto.UsersWorkspaceDto;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceConfigDto;
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
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.UriBuilder;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.google.common.base.MoreObjects.firstNonNull;
import static org.eclipse.che.api.project.server.Constants.CODENVY_DIR;

/**
 * @author andrew00x
 * @author Artem Zatsarynnyi
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
    private final String                            apiEndpoint;

    @Inject
    @SuppressWarnings("unchecked")
    public DefaultProjectManager(VirtualFileSystemRegistry fileSystemRegistry,
                                 EventService eventService,
                                 ProjectTypeRegistry projectTypeRegistry,
                                 ProjectHandlerRegistry handlers,
                                 @Named("api.endpoint") String apiEndpoint) {

        this.fileSystemRegistry = fileSystemRegistry;
        this.eventService = eventService;
        this.projectTypeRegistry = projectTypeRegistry;
        this.handlers = handlers;
        this.apiEndpoint = apiEndpoint;

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

    private static String projectPath(String path) {
        int end = path.indexOf("/");
        if (end == -1) {
            return path;
        }
        return path.substring(0, end);
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

    @Override
    public List<Project> getProjects(String workspace) throws ServerException, NotFoundException, ForbiddenException {
        final FolderEntry myRoot = getProjectsRoot(workspace);
        final List<Project> projects = new ArrayList<>();
        for (FolderEntry folder : myRoot.getChildFolders()) {
            final Project project = getProject(workspace, folder.getPath());
            if (project != null) {
                projects.add(project);
            }
        }
        return projects;
    }

    @Override
    public Project getProject(String workspace, String projectPath) throws ForbiddenException, ServerException, NotFoundException {
        final FolderEntry myRoot = getProjectsRoot(workspace);
        final VirtualFileEntry child = myRoot.getChild(projectPath.startsWith("/") ? projectPath.substring(1) : projectPath);
        if (child != null && child.isFolder() && isProjectFolder((FolderEntry)child)) {
            return new Project((FolderEntry)child, this);
        }
        return null;
    }

    @Override
    public Project createProject(String workspace,
                                 String name,
                                 ProjectConfig projectConfig,
                                 Map<String, String> options)
            throws ConflictException, ForbiddenException, ServerException, NotFoundException {
        final FolderEntry myRoot = getProjectsRoot(workspace);
        final FolderEntry projectFolder = myRoot.createFolder(name);
        final Project project = new Project(projectFolder, this);

        final CreateProjectHandler generator = handlers.getCreateProjectHandler(projectConfig.getType());

        if (generator != null) {
            final Map<String, AttributeValue> valueMap = new HashMap<>();
            if (projectConfig.getAttributes() != null) {
                final Map<String, List<String>> attributes = projectConfig.getAttributes();
                for (String key : attributes.keySet()) {
                    valueMap.put(key, new AttributeValue(attributes.get(key)));
                }
            }
            generator.onCreateProject(project.getBaseFolder(), valueMap, options);
        }

        project.updateConfig(projectConfig);

        final ProjectMisc misc = project.getMisc();
        misc.setCreationDate(System.currentTimeMillis());
        misc.save(); // Important to save misc!!

        final ProjectCreatedHandler projectCreatedHandler = handlers.getProjectCreatedHandler(projectConfig.getType());
        if (projectCreatedHandler != null) {
            projectCreatedHandler.onProjectCreated(project.getBaseFolder());
        }
        return project;
    }

    @Override
    public Project updateProject(String workspace, String path, ProjectConfig newConfig)
            throws ForbiddenException, ServerException, NotFoundException, ConflictException, IOException {
        Project project = getProject(workspace, path);
        String oldProjectType = null;
        List<String> oldMixinTypes = new ArrayList<>();
        // If a project does not exist in the target path, create a new one
        if (project == null) {
            FolderEntry projectsRoot = getProjectsRoot(workspace);
            VirtualFileEntry child = projectsRoot.getChild(path);
            if (child != null && child.isFolder() && child.getParent().isRoot()) {
                project = new Project((FolderEntry)child, this);
            } else {
                throw new NotFoundException(String.format("Project '%s' doesn't exist in workspace '%s'.", path, workspace));
            }
        } else {
            try {
                ProjectConfig config = project.getConfig();
                oldProjectType = config.getType();
                oldMixinTypes = config.getMixinTypes();
            } catch (ProjectTypeConstraintException | ValueStorageException e) {
                // here we allow changing bad project type on registered
                LOG.warn(e.getMessage());
            }
        }
        project.updateConfig(newConfig);
        // handle project type changes
        // post actions on changing project type
        // base or mixin
        if (!newConfig.getType().equals(oldProjectType)) {
            ProjectTypeChangedHandler projectTypeChangedHandler = handlers
                    .getProjectTypeChangedHandler(newConfig.getType());
            if (projectTypeChangedHandler != null) {
                projectTypeChangedHandler.onProjectTypeChanged(project.getBaseFolder());
            }
        }
        List<String> mixinTypes = firstNonNull(newConfig.getMixinTypes(), Collections.<String>emptyList());
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

    @Override
    public ModuleConfigDto addModule(String workspaceId,
                                     String pathToParent,
                                     ModuleConfigDto createdModuleDto,
                                     Map<String, String> options) throws ConflictException,
                                                                         ForbiddenException,
                                                                         ServerException,
                                                                         NotFoundException {
        if (createdModuleDto == null) {
            throw new ConflictException("Module not found and module configuration is not defined");
        }

        String[] pathToParentParts = pathToParent.split(String.format("(?=[%s])", File.separator));

        String pathToProject = pathToParentParts[0];

        ProjectConfigDto projectFromWorkspaceDto = getProjectFromWorkspace(workspaceId, pathToProject);

        if (projectFromWorkspaceDto == null) {
            throw new NotFoundException("Parent Project not found " + pathToProject);
        }

        boolean addModuleToProjectFolder = pathToParentParts.length == 1;

        VirtualFileEntry parentFolder = null;

        if (addModuleToProjectFolder) {
            parentFolder = getProjectsRoot(workspaceId).getChild(projectFromWorkspaceDto.getPath());

            projectFromWorkspaceDto.getModules().add(createdModuleDto);
        } else {
            String absolutePathToParent = pathToParent.startsWith("/") ? pathToParent : '/' + pathToParent;

            for (ModuleConfigDto moduleConfig : projectFromWorkspaceDto.getModules()) {

                ModuleConfigDto parentModule = findModuleRecursive(moduleConfig, absolutePathToParent);

                if (parentModule != null) {
                    parentModule.getModules().add(createdModuleDto);

                    parentFolder = getProjectsRoot(workspaceId).getChild(parentModule.getPath());

                    break;
                }
            }
        }

        if (parentFolder == null) {
            throw new NotFoundException("Parent folder not found to create module in it.");
        }

        String createdModuleName = createdModuleDto.getName();

        VirtualFileEntry moduleFolder = ((FolderEntry)parentFolder).getChild(createdModuleName);

        if (moduleFolder == null) {
            moduleFolder = ((FolderEntry)parentFolder).createFolder(createdModuleName);
        }

        Project createdModule = new Project((FolderEntry)moduleFolder, this);

        Map<String, AttributeValue> projectAttributes = new HashMap<>();

        Map<String, List<String>> attributes = createdModuleDto.getAttributes();

        if (attributes != null) {
            for (String key : attributes.keySet()) {
                projectAttributes.put(key, new AttributeValue(attributes.get(key)));
            }
        }

        CreateProjectHandler generator = this.getHandlers().getCreateProjectHandler(createdModuleDto.getType());

        if (generator != null) {
            generator.onCreateProject(createdModule.getBaseFolder(), projectAttributes, options);
        }

        ProjectMisc misc = createdModule.getMisc();
        misc.setCreationDate(System.currentTimeMillis());
        misc.save(); // Important to save misc!!

        CreateModuleHandler moduleHandler = this.getHandlers().getCreateModuleHandler(createdModuleDto.getType());

        if (moduleHandler != null) {
            moduleHandler.onCreateModule((FolderEntry)parentFolder,
                                         createdModule.getPath(),
                                         createdModuleDto.getType(),
                                         options);
        }

        createdModuleDto.setPath(createdModule.getPath());

        updateProjectInWorkspace(workspaceId, projectFromWorkspaceDto);

        return createdModuleDto;
    }

    private ModuleConfigDto findModuleRecursive(ModuleConfigDto moduleConfig, String pathToModule) {
        if (pathToModule.equals(moduleConfig.getPath())) {
            return moduleConfig;
        }

        for (ModuleConfigDto configDto : moduleConfig.getModules()) {
            if (pathToModule.equals(configDto.getPath())) {
                return configDto;
            }

            ModuleConfigDto foundConfig = findModuleRecursive(configDto, pathToModule);

            if (foundConfig != null) {
                return foundConfig;
            }
        }

        return null;
    }

    @Override
    public FolderEntry getProjectsRoot(String workspace) throws ServerException, NotFoundException {
        return new FolderEntry(workspace, fileSystemRegistry.getProvider(workspace).getMountPoint(true).getRoot());
    }

    @Override
    public ProjectConfig getProjectConfig(Project project) throws ServerException, ProjectTypeConstraintException, ValueStorageException {
        ProjectConfigDto projectConfigDto = getProjectFromWorkspace(project.getWorkspace(), project.getPath());
        if (projectConfigDto == null) {
            projectConfigDto = DtoFactory.getInstance().createDto(ProjectConfigDto.class);
        }

        ProjectTypes types = new ProjectTypes(project, projectConfigDto.getType(), projectConfigDto.getMixinTypes(), this);
        types.addTransient();

        final Map<String, List<String>> attributes = new HashMap<>();

        for (ProjectType t : types.getAll().values()) {
            for (Attribute attr : t.getAttributes()) {
                if (attr.isVariable()) {
                    Variable var = (Variable)attr;
                    final ValueProviderFactory factory = var.getValueProviderFactory();

                    List<String> val;
                    if (factory != null) {
                        val = factory.newInstance(project.getBaseFolder()).getValues(var.getName());

                        if (val == null) {
                            throw new ProjectTypeConstraintException(
                                    "Value Provider must not produce NULL value of variable " + var.getId());
                        }
                    } else {
                        val = projectConfigDto.getAttributes().get(attr.getName());
                    }

                    if (val == null || val.isEmpty()) {
                        if (var.isRequired())
                            throw new ProjectTypeConstraintException(
                                    "No Value nor ValueProvider defined for required variable " + var.getId());
                        // else just not add it
                    } else {
                        attributes.put(var.getName(), val);
                    }
                } else {  // Constant
                    attributes.put(attr.getName(), attr.getValue().getList());
                }
            }
        }

        return DtoFactory.getInstance()
                         .createDto(ProjectConfigDto.class)
                         .withDescription(projectConfigDto.getDescription())
                         .withType(types.getPrimary().getId())
                         .withAttributes(attributes)
                         .withMixinTypes(types.mixinIds())
                         .withModules(projectConfigDto.getModules());
    }

    @Override
    public void updateProjectConfig(Project project, ProjectConfig config)
            throws ServerException, ValueStorageException, ProjectTypeConstraintException, InvalidValueException {

        List<ModuleConfigDto> modules = new ArrayList<>();

        for (ModuleConfig moduleConfig : config.getModules()) {
            getModulesRecursive(moduleConfig, modules);
        }

        final ProjectConfigDto projectConfig = DtoFactory.getInstance().createDto(ProjectConfigDto.class)
                                                         .withPath(project.getPath())
                                                         .withName(project.getName())
                                                         .withModules(modules)
                                                         .withSource(DtoFactory.getInstance().createDto(SourceStorageDto.class));

        ProjectTypes types = new ProjectTypes(project, config.getType(), config.getMixinTypes(), this);
        types.removeTransient();

        projectConfig.setType(types.getPrimary().getId());
        projectConfig.setDescription(config.getDescription());

        ArrayList<String> ms = new ArrayList<>();
        ms.addAll(types.getMixins().keySet());
        projectConfig.setMixinTypes(ms);

        // update attributes
        HashMap<String, List<String>> checkVariables = new HashMap<>();
        for (String attributeName : config.getAttributes().keySet()) {
            List<String> attributeValue = config.getAttributes().get(attributeName);

            // Try to find definition in all the types
            Attribute definition = null;
            for (ProjectType t : types.getAll().values()) {
                definition = t.getAttribute(attributeName);
                if (definition != null) {
                    break;
                }
            }

            // initialize provided attributes
            if (definition != null && definition.isVariable()) {
                Variable var = (Variable)definition;

                final ValueProviderFactory valueProviderFactory = var.getValueProviderFactory();

                // calculate provided values
                if (valueProviderFactory != null) {
                    valueProviderFactory.newInstance(project.getBaseFolder()).setValues(var.getName(), attributeValue);
                }

                if (attributeValue == null && var.isRequired()) {
                    throw new ProjectTypeConstraintException("Required attribute value is initialized with null value " + var.getId());
                }

                // store non-provided values into JSON
                if (valueProviderFactory == null) {
                    projectConfig.getAttributes().put(definition.getName(), attributeValue);
                }

                checkVariables.put(attributeName, attributeValue);
            }
        }

        for (ProjectType t : types.getAll().values()) {
            for (Attribute attr : t.getAttributes()) {
                if (attr.isVariable()) {
                    // check if required variables initialized
//                    if(attr.isRequired() && attr.getValue() == null) {
                    if (!checkVariables.containsKey(attr.getName()) && attr.isRequired()) {
                        throw new ProjectTypeConstraintException("Required attribute value is initialized with null value " + attr.getId());
                    }
                } else {
                    // add constants
                    projectConfig.getAttributes().put(attr.getName(), attr.getValue().getList());
                }
            }
        }

        // TODO: .codenvy folder creation should be removed when all project's meta-info will be stored on Workspace API
        try {
            final VirtualFileEntry codenvyDir = project.getBaseFolder().getChild(CODENVY_DIR);
            if (codenvyDir == null || !codenvyDir.isFolder()) {
                project.getBaseFolder().createFolder(CODENVY_DIR);
            }
        } catch (ForbiddenException | ConflictException e) {
            throw new ServerException(e.getServiceError());
        }

        updateProjectInWorkspace(project.getWorkspace(), projectConfig);
    }

    private void getModulesRecursive(ModuleConfig moduleConfig, List<ModuleConfigDto> projectModules) {
        List<ModuleConfigDto> modules = new ArrayList<>();

        for (ModuleConfig config : moduleConfig.getModules()) {
            getModulesRecursive(config, modules);
        }

        ModuleConfigDto module = DtoFactory.getInstance()
                                           .createDto(ModuleConfigDto.class)
                                           .withName(moduleConfig.getName())
                                           .withPath(moduleConfig.getPath())
                                           .withType(moduleConfig.getType())
                                           .withModules(modules)
                                           .withAttributes(moduleConfig.getAttributes())
                                           .withDescription(moduleConfig.getDescription())
                                           .withMixinTypes(moduleConfig.getMixinTypes());

        projectModules.add(module);
    }

    private UsersWorkspaceDto getWorkspace(String wsId) throws ServerException {
        final String href = UriBuilder.fromUri(apiEndpoint)
                                      .path(WorkspaceService.class).path(WorkspaceService.class, "getById")
                                      .build(wsId).toString();
        final Link link = DtoFactory.getInstance().createDto(Link.class).withMethod("GET").withHref(href);

        try {
            return HttpJsonHelper.request(UsersWorkspaceDto.class, link);
        } catch (IOException | ApiException e) {
            throw new ServerException(e);
        }
    }

    private ProjectConfigDto getProjectFromWorkspace(String wsId, String projectPath) throws ServerException {
        final UsersWorkspaceDto usersWorkspaceDto = getWorkspace(wsId);
        final String path = projectPath.startsWith("/") ? projectPath : "/" + projectPath;
        for (ProjectConfigDto projectConfig : usersWorkspaceDto.getProjects()) {
            if (path.equals(projectConfig.getPath())) {
                return projectConfig;
            }
        }
        return null;
    }

    private void updateWorkspace(String wsId, WorkspaceConfigDto workspaceConfig) throws ServerException {
        final String href = UriBuilder.fromUri(apiEndpoint)
                                      .path(WorkspaceService.class).path(WorkspaceService.class, "update")
                                      .build(wsId).toString();
        final Link link = DtoFactory.getInstance().createDto(Link.class).withMethod("PUT").withHref(href);

        try {
            HttpJsonHelper.request(WorkspaceConfigDto.class, link, workspaceConfig);
        } catch (IOException | ApiException e) {
            throw new ServerException(e.getMessage());
        }
    }

    private void updateProjectInWorkspace(String wsId, ProjectConfigDto projectConfig) throws ServerException {
        final String href = UriBuilder.fromUri(apiEndpoint)
                                      .path(WorkspaceService.class).path(WorkspaceService.class, "updateProject")
                                      .build(wsId).toString();
        final Link link = DtoFactory.getInstance().createDto(Link.class).withMethod("PUT").withHref(href);

        try {
            HttpJsonHelper.request(UsersWorkspaceDto.class, link, projectConfig);
        } catch (NotFoundException e) {
            final String addProjectHref = UriBuilder.fromUri(apiEndpoint)
                                                    .path(WorkspaceService.class).path(WorkspaceService.class, "addProject")
                                                    .build(wsId).toString();
            final Link addProjectLink = DtoFactory.getInstance().createDto(Link.class).withMethod("POST").withHref(addProjectHref);
            try {
                HttpJsonHelper.request(UsersWorkspaceDto.class, addProjectLink, projectConfig);
            } catch (IOException | ApiException e1) {
                throw new ServerException(e1.getMessage());
            }
        } catch (IOException | ApiException e) {
            throw new ServerException(e.getMessage());
        }
    }

    @Override
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
            // If have access to the project then must have access to its meta-information.
            // If don't have access then treat that as server error.
            throw new ServerException(e.getServiceError());
        }
    }

    @Override
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
                FolderEntry codenvy = (FolderEntry)project.getBaseFolder().getChild(CODENVY_DIR);
                if (codenvy == null) {
                    try {
                        codenvy = project.getBaseFolder().createFolder(CODENVY_DIR);
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

    @Override
    public List<? extends ModuleConfig> getProjectModules(Project parent) throws ServerException,
                                                                                 ForbiddenException,
                                                                                 ConflictException,
                                                                                 IOException,
                                                                                 NotFoundException {

        ProjectConfigDto project = getProjectFromWorkspace(parent.getWorkspace(), parent.getPath());

        return project.getModules();
    }

    @Override
    public VirtualFileSystemRegistry getVirtualFileSystemRegistry() {
        return fileSystemRegistry;
    }

    @Override
    public ProjectTypeRegistry getProjectTypeRegistry() {
        return this.projectTypeRegistry;
    }

    @Override
    public ProjectHandlerRegistry getHandlers() {
        return handlers;
    }

    @Override
    public Map<String, AttributeValue> estimateProject(String workspace, String path, String projectTypeId)
            throws ServerException, ForbiddenException, NotFoundException, ValueStorageException, ProjectTypeConstraintException {
        ProjectType projectType = projectTypeRegistry.getProjectType(projectTypeId);
        if (projectType == null) {
            throw new NotFoundException("Project Type " + projectTypeId + " not found.");
        }

        final VirtualFileEntry baseFolder = getProjectsRoot(workspace).getChild(path.startsWith("/") ? path.substring(1) : path);
        if (!baseFolder.isFolder()) {
            throw new NotFoundException("Not a folder: " + path);
        }

        Map<String, AttributeValue> attributes = new HashMap<>();

        for (Attribute attr : projectType.getAttributes()) {
            if (attr.isVariable() && ((Variable)attr).getValueProviderFactory() != null) {

                Variable var = (Variable)attr;
                // getValue throws ValueStorageException if not valid
                attributes.put(attr.getName(), var.getValue((FolderEntry)baseFolder));
            }
        }

        return attributes;
    }

    // ProjectSuggestion
    public List<SourceEstimation> resolveSources(String workspace, String path, boolean transientOnly)
            throws ServerException, ForbiddenException, NotFoundException, ProjectTypeConstraintException {
        final List<SourceEstimation> estimations = new ArrayList<>();

        for (ProjectType type : projectTypeRegistry.getProjectTypes(ProjectTypeRegistry.CHILD_TO_PARENT_COMPARATOR)) {
            if (transientOnly && type.isPersisted()) {
                continue;
            }

            final HashMap<String, List<String>> attributes = new HashMap<>();

            try {
                for (Map.Entry<String, AttributeValue> attr : estimateProject(workspace, path, type.getId()).entrySet()) {
                    attributes.put(attr.getKey(), attr.getValue().getList());
                }

                if (!attributes.isEmpty()) {
                    estimations.add(DtoFactory.getInstance().createDto(SourceEstimation.class)
                                              .withType(type.getId())
                                              .withAttributes(attributes));
                }
            } catch (ValueStorageException e) {
                LOG.warn(e.getLocalizedMessage(), e);
            }
        }
        if (estimations.isEmpty()) {
            estimations.add(DtoFactory.getInstance().createDto(SourceEstimation.class)
                                      .withType(BaseProjectType.ID));
        }

        return estimations;
    }

    @Override
    public Project convertFolderToProject(String workspace, String path, ProjectConfig projectConfig)
            throws ConflictException, ForbiddenException, ServerException, NotFoundException, IOException {

        final VirtualFileEntry projectEntry = getProjectsRoot(workspace).getChild(path);
        if (projectEntry == null || !projectEntry.isFolder())
            throw new NotFoundException("Not found or not a folder " + path);

        FolderEntry projectFolder = (FolderEntry)projectEntry;

        final Project project = new Project(projectFolder, this);

        // Update config
        if (projectConfig != null && projectConfig.getType() != null) {
            //TODO: need add checking for concurrency attributes name in giving config and in estimation
            for (Map.Entry<String, AttributeValue> entry : estimateProject(workspace, path, projectConfig.getType()).entrySet()) {
                projectConfig.getAttributes().put(entry.getKey(), entry.getValue().getList());
            }
            project.updateConfig(projectConfig);
        } else {  // try to get config (it will throw exception in case config is not valid)
            projectConfig = project.getConfig();
        }

        if (projectConfig.getType() != null) {
            PostImportProjectHandler postImportProjectHandler =
                    handlers.getPostImportProjectHandler(projectConfig.getType());
            if (postImportProjectHandler != null) {
                postImportProjectHandler.onProjectImported(project.getBaseFolder());
            }
        }

        final ProjectMisc misc = project.getMisc();
        misc.setCreationDate(System.currentTimeMillis());
        misc.save(); // Important to save misc!!

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
            ((FileEntry)entry).rename(newName, newMediaType);
        } else {
            final Project project = getProject(workspace, path);

            entry.rename(newName);

            if (project != null) {
                // get UsersWorkspaceDto
                final UsersWorkspaceDto usersWorkspace = getWorkspace(workspace);
                // replace path in all projects
                final String oldProjectPath = path.startsWith("/") ? path : "/" + path;
                usersWorkspace.getProjects()
                              .stream()
                              .filter(projectConfigDto -> projectConfigDto.getPath().startsWith(oldProjectPath))
                              .forEach(projectConfigDto -> {
                                  if (oldProjectPath.equals(projectConfigDto.getPath())) {
                                      projectConfigDto.setName(newName);
                                  }
                                  projectConfigDto.setPath(projectConfigDto.getPath().replaceFirst(oldProjectPath, entry.getPath()));
                              });
                // update workspace with a new WorkspaceConfig
                updateWorkspace(workspace, org.eclipse.che.api.workspace.server.DtoConverter.asDto((WorkspaceConfig)usersWorkspace));
            }

            final String projectName = projectPath(path);
            // We should not edit Modules if resource to rename is project
            if (!projectName.equals(path) && entry.isFolder()) {
                final Project rootProject = getProject(workspace, projectName);
                // TODO: rework
//                if (rootProject != null) {
//                    // We need module path without projectName, f.e projectName/module1/oldModuleName -> module1/oldModuleName
//                    String oldModulePath = path.replaceFirst(projectName + "/", "");
//                    // Calculates new module path, f.e module1/oldModuleName -> module1/newModuleName
//                    String newModulePath = oldModulePath.substring(0, oldModulePath.lastIndexOf("/") + 1) + newName;
//
//                    rootProject.getModules().update(oldModulePath, newModulePath);
//                }
            }
        }
        return entry;
    }

    @Override
    public boolean delete(String workspace, String path)
            throws ServerException, ForbiddenException, NotFoundException, ConflictException {
        final FolderEntry root = getProjectsRoot(workspace);
        final VirtualFileEntry entry = root.getChild(path);
        if (entry == null) {
            return false;
        }

        final ProjectConfig project = getProjectFromWorkspace(workspace, path);

        if (project != null) {
            // Remove whole project
            final String projectName = project.getName();
            String projectType = null;
            try {
                projectType = project.getType();
                deleteProjectFromWorkspace(workspace, project.getName());
            } catch (ServerException e) {
                // Let delete even project in invalid state.
                LOG.warn(String.format("Removing not valid project ws : %s, project path: %s ", workspace, path) + e.getMessage(), e);
            }
            entry.remove();
            LOG.info("EVENT#project-destroyed# PROJECT#{}# TYPE#{}# WS#{}# USER#{}#",
                     projectName,
                     projectType != null ? projectType : "unknown",
                     EnvironmentContext.getCurrent().getWorkspaceName(),
                     EnvironmentContext.getCurrent().getUser().getName());
        } else {
            // find if path is a module
            ModuleConfig parentConfig = null;
            if (!path.startsWith(File.separator)) {
                path = File.separator + path;
            }
            String[] parts = path.split(String.format("(?=[%s])", File.separator));
            ProjectConfigDto projectConfig = getProjectFromWorkspace(workspace, parts[0]);
            ModuleConfig tmp = projectConfig;
            boolean isModuleFound = false;
            if (tmp != null) {
                StringBuilder strBuilder = new StringBuilder(parts[0]);
                for (int i = 1; i < parts.length; i++) {
                    strBuilder.append(parts[i]);
                    Optional<? extends ModuleConfig> optional = findModuleWithPath(tmp, strBuilder.toString());
                    if (optional.isPresent()) {
                        isModuleFound = true;
                        tmp = optional.get();
                    } else {
                        isModuleFound = false;
                        break;
                    }
                }
                parentConfig = tmp;
            }

            if (isModuleFound) {
                RemoveModuleHandler removeModuleHandler = this.getHandlers().getRemoveModuleHandler(parentConfig.getType());
                if (removeModuleHandler != null) {
                    removeModuleHandler.onRemoveModule((FolderEntry)entry, path, parentConfig);
                }

                Iterator<? extends ModuleConfig> it = parentConfig.getModules().iterator();
                while (it.hasNext()) {
                    if (it.next().getPath().equals(path)) {
                        it.remove();
                    }
                }
                updateProjectInWorkspace(workspace, projectConfig);
            }

            eventService.publish(new ProjectItemModifiedEvent(ProjectItemModifiedEvent.EventType.DELETED, workspace,
                                                              projectPath(entry.getPath()), entry.getPath(), entry.isFolder()));
            entry.remove();
        }
        return true;
    }

    private void deleteProjectFromWorkspace(String wsId, String projectName) throws ServerException {
        final String href = UriBuilder.fromUri(apiEndpoint)
                                      .path(WorkspaceService.class).path(WorkspaceService.class, "deleteProject")
                                      .build(wsId, projectName).toString();
        final Link link = DtoFactory.getInstance().createDto(Link.class).withMethod("DELETE").withHref(href);

        try {
            HttpJsonHelper.request(null, link);
        } catch (IOException | ApiException e) {
            throw new ServerException(e);
        }
    }

    @Override
    public boolean isProjectFolder(FolderEntry folder) throws ServerException {
        try {
            return getProjectFromWorkspace(folder.getWorkspace(), folder.getPath()) != null;
        } catch (ApiException e) {
            throw new ServerException(e);
        }
    }

    @Override
    public boolean isModuleFolder(FolderEntry folder) throws ServerException {
        String pathToModuleFolder = folder.getPath();

        String[] pathToModuleParts = pathToModuleFolder.split(String.format("(?=[%s])", File.separator));

        ProjectConfigDto projectFromWorkspace = getProjectFromWorkspace(folder.getWorkspace(), pathToModuleParts[0]);

        if (projectFromWorkspace == null) {
            return false;
        }

        for (ModuleConfigDto moduleConfigDto : projectFromWorkspace.getModules()) {
            ModuleConfigDto foundModule = findModuleRecursive(moduleConfigDto, pathToModuleFolder);

            if (foundModule != null) {
                return true;
            }
        }

        return false;
    }

    private Optional<? extends ModuleConfig> findModuleWithPath(ModuleConfig parent, String modulePath) {
        return parent.getModules().stream().filter(module -> module.getPath().equals(modulePath)).findAny();
    }

}
