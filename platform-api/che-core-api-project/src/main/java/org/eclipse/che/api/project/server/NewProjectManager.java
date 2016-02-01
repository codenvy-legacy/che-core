/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
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
import org.eclipse.che.api.core.UnauthorizedException;
import org.eclipse.che.api.core.model.project.SourceStorage;
import org.eclipse.che.api.core.model.project.type.Attribute;
import org.eclipse.che.api.core.model.project.type.ProjectType;
import org.eclipse.che.api.core.model.workspace.ProjectConfig;
import org.eclipse.che.api.core.model.workspace.UsersWorkspace;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.core.util.LineConsumerFactory;
import org.eclipse.che.api.project.server.handlers.CreateProjectHandler;
import org.eclipse.che.api.project.server.handlers.ProjectCreatedHandler;
import org.eclipse.che.api.project.server.handlers.ProjectHandlerRegistry;
import org.eclipse.che.api.project.server.handlers.ProjectTypeChangedHandler;
import org.eclipse.che.api.project.server.type.AttributeValue;
import org.eclipse.che.api.project.server.type.BaseProjectType;
import org.eclipse.che.api.project.server.type.ProjectTypeRegistry;
import org.eclipse.che.api.project.server.type.Variable;
import org.eclipse.che.api.project.shared.dto.SourceEstimation;
import org.eclipse.che.api.vfs.Path;
import org.eclipse.che.api.vfs.VirtualFile;
import org.eclipse.che.api.vfs.VirtualFileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.MoreObjects.firstNonNull;
import static org.eclipse.che.dto.server.DtoFactory.newDto;

/**
 * @author andrew00x
 * @author Artem Zatsarynnyi
 */
@Singleton
public final class NewProjectManager {
    private static final Logger LOG = LoggerFactory.getLogger(NewProjectManager.class);

    private final VirtualFileSystem vfs;

    private final EventService           eventService;
    private final ProjectTypeRegistry    projectTypeRegistry;
    private final ProjectHandlerRegistry handlers;
    private final WorkspaceHolder        workspaceHolder;

    private final Map<String, ProjectImpl> projects;

    private final ProjectImporterRegistry importers;


    @Inject
    @SuppressWarnings("unchecked")
    public NewProjectManager(VirtualFileSystem vfs,
                             EventService eventService,
                             ProjectTypeRegistry projectTypeRegistry,
                             ProjectHandlerRegistry handlers,
                             ProjectImporterRegistry importers,
                             WorkspaceHolder workspaceHolder
                            )
            throws ServerException, NotFoundException, ProjectTypeConstraintException, InvalidValueException,
                   ValueStorageException {

        this.vfs = vfs;
        this.eventService = eventService;
        this.projectTypeRegistry = projectTypeRegistry;
        this.handlers = handlers;
        this.importers = importers;

        this.workspaceHolder = workspaceHolder;

        this.projects = new HashMap<>();

        initProjects();


    }


//    public String getWorkspaceId() {
//        return workspaceHolder.getWorkspace().getId();
//    }

    public FolderEntry getProjectsRoot() throws ServerException, NotFoundException {
        return new FolderEntry(vfs.getRoot());
    }


    public ProjectTypeRegistry getProjectTypeRegistry() {
        return this.projectTypeRegistry;
    }

    public ProjectHandlerRegistry getHandlers() {
        return handlers;
    }

    public VirtualFileSystem getVfs() {
        return this.vfs;
    }


    /**
     * @return all the projects
     */
    public List<ProjectImpl> getProjects() {

        return new ArrayList(projects.values());
    }

    /**
     * @param path
     *         where to find
     * @return child projects
     */
    public List<String> getProjects(String path) {

        Path root = Path.of(absolutizePath(path));
        List<String> children = new ArrayList<>();

//        if (!root.isAbsolute())
//            throw new RuntimeException("Absolute path expected: %s" + path);

        // TODO better algo?
        for (String key : projects.keySet()) {
            if (Path.of(key).isChild(root)) {
                children.add(key);
            }
        }

        return children;
    }

    /**
     * @param projectPath
     * @return project or null if not found
     */
    public ProjectImpl getProject(String projectPath) {

        return projects.get(absolutizePath(projectPath));

    }

    /**
     * @param path
     * @return the project owned this path.
     * @throws NotFoundException
     *         if not such a project found
     */
    public ProjectImpl getOwnerProject(String path) throws NotFoundException {

        // it is a project
        if (projects.containsKey(path))
            return projects.get(path);

        // otherwise try to find matched parent
        Path test;
        while ((test = Path.of(path).getParent()) != null) {
            ProjectImpl project = projects.get(test.toString());
            if (project != null)
                return project;

            path = test.toString();
        }

        // path is out of projects
        throw new NotFoundException("Owner project not found " + path);


    }

    public boolean isProject(String path) throws NotFoundException {
        return getOwnerProject(path).getPath().equals(path);
    }

    /**
     * create project with optional adding it as a module to other
     *
     * @param projectConfig
     *         - project configuration
     * @param options
     *         - options for generator
     * @return new project
     * @throws ConflictException
     * @throws ForbiddenException
     * @throws ServerException
     * @throws NotFoundException
     */
    public ProjectImpl createProject(ProjectConfig projectConfig,
                                     Map<String, String> options)
            throws ConflictException,
                   ForbiddenException,
                   ServerException,
                   NotFoundException {
        return createProject(projectConfig, options, null);

    }


    /**
     * create project with optional adding it as a module to other
     *
     * @param projectConfig
     *         - project configuration
     * @param options
     *         - options for generator
     * @param addAsModuleTo
     *         - path to the project to add this as a module or null if no parent
     * @return new project
     * @throws ConflictException
     * @throws ForbiddenException
     * @throws ServerException
     * @throws NotFoundException
     */
    public ProjectImpl createProject(ProjectConfig projectConfig,
                                     Map<String, String> options,
                                     String addAsModuleTo) throws ConflictException,
                                                                  ForbiddenException,
                                                                  ServerException,
                                                                  NotFoundException {

        if (projectConfig.getPath() == null)
            throw new ConflictException("Path for new project should be defined ");

        String path = absolutizePath(projectConfig.getPath());

        FolderEntry projectFolder = new FolderEntry(vfs.getRoot().createFolder(path));

        ProjectImpl project = new ProjectImpl(projectFolder, projectConfig, true, this);

        CreateProjectHandler generator = handlers.getCreateProjectHandler(projectConfig.getType());

        if (generator != null) {
            Map<String, AttributeValue> valueMap = new HashMap<>();

            Map<String, List<String>> attributes = projectConfig.getAttributes();

            if (attributes != null) {
                for (Map.Entry<String, List<String>> entry : attributes.entrySet()) {
                    valueMap.put(entry.getKey(), new AttributeValue(entry.getValue()));
                }
            }

            generator.onCreateProject(projectFolder, valueMap, options);
        }

        // cache newly created project
        projects.put(path, project);

        ProjectCreatedHandler projectCreatedHandler = handlers.getProjectCreatedHandler(projectConfig.getType());

        if (projectCreatedHandler != null) {
            projectCreatedHandler.onProjectCreated(projectFolder);
        }

        if (addAsModuleTo != null) {
            ProjectImpl parent = getProject(addAsModuleTo);
            if (parent == null)
                throw new NotFoundException("Parent project not found at path: " + addAsModuleTo);
            parent.addModule(path);
        }

        return project;
    }

    /**
     *
     * @param newConfig - new config
     * @param path - path to project for update, if null then path from DTO will be used
     * @return updated config
     * @throws ForbiddenException
     * @throws ServerException
     * @throws NotFoundException
     * @throws ConflictException
     * @throws IOException
     */
    public ProjectImpl updateProject(ProjectConfig newConfig) throws ForbiddenException,
                                                                     ServerException,
                                                                     NotFoundException,
                                                                     ConflictException,
                                                                     IOException {

        // find project to update
        String apath;
//        if(path != null) {
//            apath = absolutizePath(path);
//        } else
        if(newConfig.getPath() != null) {
            apath = absolutizePath(newConfig.getPath());
        } else {
            throw new ConflictException("Project path is not defined");
        }

        ProjectImpl oldProject = getProject(apath);

        // If a project does not exist in the target path, create a new one
        if (oldProject == null)
            throw new NotFoundException(String.format("Project '%s' doesn't exist.", apath));

        // merge



        // store old types to
        String oldProjectType = oldProject.getType().getId();
        List<String> oldMixins = new ArrayList<>(oldProject.getMixins().keySet());



        // the new project
        ProjectImpl project = new ProjectImpl(oldProject.getBaseFolder(), newConfig, true, this);

        projects.put(apath, project);



        // handle project type changes
        // post actions on changing project type
        // base or mixin
        if (!newConfig.getType().equals(oldProjectType)) {
            ProjectTypeChangedHandler projectTypeChangedHandler = handlers.getProjectTypeChangedHandler(newConfig.getType());
            if (projectTypeChangedHandler != null) {
                projectTypeChangedHandler.onProjectTypeChanged(project.getBaseFolder());
            }
        }
        List<String> mixins = firstNonNull(newConfig.getMixins(), Collections.<String>emptyList());
        for (String mixin : mixins) {
            if (!oldMixins.contains(mixin)) {
                ProjectTypeChangedHandler projectTypeChangedHandler = handlers.getProjectTypeChangedHandler(mixin);
                if (projectTypeChangedHandler != null) {
                    projectTypeChangedHandler.onProjectTypeChanged(project.getBaseFolder());
                }
            }
        }

        return project;
    }


    private void deleteProject(String path) {
        projects.remove(path);
        // remove ref from modules if any
        for (ProjectImpl p : projects.values()) {
            p.getModules().remove(path);
        }
        // TODO fire
    }

    public ProjectImpl importProject(String path, SourceStorage sourceStorage)
            throws ServerException, IOException, ForbiddenException, UnauthorizedException, ConflictException, NotFoundException {

        final ProjectImporter importer = importers.getImporter(sourceStorage.getType());
        if (importer == null) {
            throw new ServerException(String.format("Unable import sources project from '%s'. Sources type '%s' is not supported.",
                                                    sourceStorage.getLocation(), sourceStorage.getType()));
        }
        // Preparing websocket output publisher to broadcast output of import process to the ide clients while importing
        final LineConsumerFactory outputOutputConsumerFactory = () -> new ProjectImportOutputWSLineConsumer(path,
                                                                                                            workspaceHolder.getWorkspace()
                                                                                                                           .getId(),
                                                                                                            300
        );

        // Not all importers uses virtual file system API. In this case virtual file system API doesn't get events and isn't able to set
        // correct creation time. Need do it manually.
        //VirtualFileEntry virtualFile = getVirtualFile(path, force);


        VirtualFileEntry vf = getProjectsRoot().getChild(path);
        if (vf != null && vf.isFile())
            throw new NotFoundException("Item on base path found and is not a folder" + path);
        else if(vf == null)
            vf = getProjectsRoot().createFolder(path);


        importer.importSources((FolderEntry)vf, sourceStorage, outputOutputConsumerFactory);

        //String pathToFolder = vf.getPath().toString();
        String name = path.substring(path.lastIndexOf("/")).substring(1);
        // String projectName = projectPath.substring(1);


        ProjectImpl project = new ProjectImpl((FolderEntry)vf, new ImportedProjectConf(name, path, sourceStorage), true, this);

        projects.put(path, project);

        return project;

    }


    public Map<String, AttributeValue> estimateProject(String path, String projectTypeId)
            throws ServerException, ForbiddenException, NotFoundException, ValueStorageException, ProjectTypeConstraintException {
        ProjectType projectType = projectTypeRegistry.getProjectType(projectTypeId);
        if (projectType == null) {
            throw new NotFoundException("Project Type " + projectTypeId + " not found.");
        }

        final VirtualFileEntry baseFolder = getProjectsRoot().getChild(path.startsWith("/") ? path.substring(1) : path);
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
    public List<SourceEstimation> resolveSources(String path, boolean transientOnly)
            throws ServerException, ForbiddenException, NotFoundException, ProjectTypeConstraintException {
        final List<SourceEstimation> estimations = new ArrayList<>();
        boolean isPresentPrimaryType = false;
        for (ProjectType type : projectTypeRegistry.getProjectTypes(ProjectTypeRegistry.CHILD_TO_PARENT_COMPARATOR)) {
            if (transientOnly && type.isPersisted()) {
                continue;
            }

            final HashMap<String, List<String>> attributes = new HashMap<>();

            try {
                for (Map.Entry<String, AttributeValue> attr : estimateProject(path, type.getId()).entrySet()) {
                    attributes.put(attr.getKey(), attr.getValue().getList());
                }

                if (!attributes.isEmpty()) {
                    estimations.add(newDto(SourceEstimation.class).withType(type.getId()).withAttributes(attributes));
                    ProjectType projectType = projectTypeRegistry.getProjectType(type.getId());
                    if (projectType.isPrimaryable()) {
                        isPresentPrimaryType = true;
                    }
                }
            } catch (ValueStorageException e) {
                LOG.warn(e.getLocalizedMessage(), e);
            }
        }
        if (!isPresentPrimaryType) {
            estimations.add(newDto(SourceEstimation.class).withType(BaseProjectType.ID));
        }

        return estimations;
    }

    /**
     * deletes item including project
     * @param path
     * @throws ServerException
     * @throws ForbiddenException
     * @throws NotFoundException
     * @throws ConflictException
     */
    public void delete(String path) throws ServerException,
                                           ForbiddenException,
                                           NotFoundException,
                                           ConflictException {



        String apath = absolutizePath(path);

        // delete item
        VirtualFile item = vfs.getRoot().getChild(Path.of(apath));
        if(item == null)
            return;

        item.delete();
        if(projects.get(apath) == null) {
            // fire event for file/folder
            return;
        }


        // process project
        for(String childPath : getProjects(apath)) {
            deleteProject(childPath);
        }
        deleteProject(apath);

    }

    public void deleteModule(String ownerPath, String modulePath) {
        ProjectImpl owner = projects.get(absolutizePath(ownerPath));
        if(owner != null)
            owner.getModules().remove(modulePath);
    }


//    public ProjectConfigDto addModule(String pathToParent,
//                                      ProjectConfig createdModuleDto,
//                                      Map<String, String> options) throws ConflictException,
//                                                                          ForbiddenException,
//                                                                          ServerException,
//                                                                          NotFoundException {
//        if (createdModuleDto == null) {
//            throw new ConflictException("Module not found and module configuration is not defined");
//        }
//
//        String[] pathToParentParts = pathToParent.split(String.format("(?=[%s])", File.separator));
//
//        String pathToProject = pathToParentParts[0];
//
//        ProjectConfigDto projectFromWorkspaceDto = getProjectFromWorkspace(pathToProject);
//
//        if (projectFromWorkspaceDto == null) {
//            throw new NotFoundException("Parent Project not found " + pathToProject);
//        }
//
//        String absolutePathToParent = pathToParent.startsWith("/") ? pathToParent : '/' + pathToParent;
//
//        ProjectConfigDto parentModule = projectFromWorkspaceDto.findModule(absolutePathToParent);
//
//        if (parentModule == null) {
//            parentModule = projectFromWorkspaceDto;
//        }
//
//        parentModule.getModules().add(createdModuleDto);
//
//        VirtualFileEntry parentFolder = getProjectsRoot().getChild(absolutePathToParent);
//
//        if (parentFolder == null) {
//            throw new NotFoundException("Parent folder not found for this node " + pathToParent);
//        }
//
//        String createdModuleName = createdModuleDto.getName();
//
//        VirtualFileEntry moduleFolder = ((FolderEntry)parentFolder).getChild(createdModuleName);
//
//        if (moduleFolder == null) {
//            moduleFolder = ((FolderEntry)parentFolder).createFolder(createdModuleName);
//        }
//
//        Project createdModule = new Project((FolderEntry)moduleFolder, this);
//
//        Map<String, AttributeValue> projectAttributes = new HashMap<>();
//
//        Map<String, List<String>> attributes = createdModuleDto.getAttributes();
//
//        if (attributes != null) {
//            for (String key : attributes.keySet()) {
//                projectAttributes.put(key, new AttributeValue(attributes.get(key)));
//            }
//        }
//
//        CreateProjectHandler generator = this.getHandlers().getCreateProjectHandler(createdModuleDto.getType());
//
//        if (generator != null) {
//            generator.onCreateProject(createdModule.getBaseFolder(), projectAttributes, options);
//        }
//
//        ProjectMisc misc = createdModule.getMisc();
//        misc.setCreationDate(System.currentTimeMillis());
//        misc.save(); // Important to save misc!!
//
//        CreateModuleHandler moduleHandler = this.getHandlers().getCreateModuleHandler(createdModuleDto.getType());
//
//        if (moduleHandler != null) {
//            moduleHandler.onCreateModule((FolderEntry)parentFolder,
//                                         createdModule.getPath(),
//                                         createdModuleDto.getType(),
//                                         options);
//        }
//
//        createdModuleDto.setPath(createdModule.getPath());
//
//        AttributeFilter attributeFilter = filterProvider.get();
//
//        attributeFilter.addPersistedAttributesToProject(createdModuleDto, (FolderEntry)moduleFolder);
//
//        updateProjectInWorkspace(workspaceId, projectFromWorkspaceDto);
//
//        attributeFilter.addRuntimeAttributesToProject(createdModuleDto, (FolderEntry)moduleFolder);
//
//        return createdModuleDto;
//    }


///////////////////


//    @Override
//    public ProjectConfigDto getProjectFromWorkspace(@NotNull String wsId, @NotNull String projectPath) throws ServerException {
//        final UsersWorkspaceDto usersWorkspaceDto = getWorkspace(wsId);
//        final String path = projectPath.startsWith("/") ? projectPath : "/" + projectPath;
//        for (ProjectConfigDto projectConfig : usersWorkspaceDto.getProjects()) {
//            if (path.equals(projectConfig.getPath())) {
//                return projectConfig;
//            }
//        }
//        return null;
//    }
//
//    public List<ProjectConfigDto> getAllProjectsFromWorkspace(@NotNull String workspaceId) throws ServerException {
//        UsersWorkspaceDto usersWorkspaceDto = getWorkspace(workspaceId);
//
//        return usersWorkspaceDto.getProjects();
//    }
//
//    private void updateWorkspace(String wsId, WorkspaceConfigDto workspaceConfig) throws ServerException {
//        final String href = UriBuilder.fromUri(apiEndpoint)
//                                      .path(WorkspaceService.class).path(WorkspaceService.class, "update")
//                                      .build(wsId).toString();
//        final Link link = newDto(Link.class).withMethod("PUT").withHref(href);
//
//        try {
//            httpJsonRequestFactory.fromLink(link)
//                                  .setBody(workspaceConfig)
//                                  .request();
//        } catch (IOException | ApiException e) {
//            throw new ServerException(e.getMessage());
//        }
//    }
//
//    private void updateProjectInWorkspace(String wsId, ProjectConfigDto projectConfig) throws ServerException {
//        final String href = UriBuilder.fromUri(apiEndpoint)
//                                      .path(WorkspaceService.class).path(WorkspaceService.class, "updateProject")
//                                      .build(wsId).toString();
//        final Link link = newDto(Link.class).withMethod("PUT").withHref(href);
//
//        try {
//            httpJsonRequestFactory.fromLink(link)
//                                  .setBody(projectConfig)
//                                  .request();
//        } catch (NotFoundException e) {
//            final String addProjectHref = UriBuilder.fromUri(apiEndpoint)
//                                                    .path(WorkspaceService.class).path(WorkspaceService.class, "addProject")
//                                                    .build(wsId).toString();
//            final Link addProjectLink = newDto(Link.class).withMethod("POST").withHref(addProjectHref);
//            try {
//                httpJsonRequestFactory.fromLink(addProjectLink)
//                                      .setBody(projectConfig)
//                                      .request();
//            } catch (IOException | ApiException e1) {
//                throw new ServerException(e1.getMessage());
//            }
//        } catch (IOException | ApiException e) {
//            throw new ServerException(e.getMessage());
//        }
//    }
//


//    @Override
//    public Project convertFolderToProject(String path, ProjectConfig projectConfig)
//            throws ConflictException, ForbiddenException, ServerException, NotFoundException, IOException {
//
//        final VirtualFileEntry projectEntry = getProjectsRoot().getChild(path);
//        if (projectEntry == null || !projectEntry.isFolder())
//            throw new NotFoundException("Not found or not a folder " + path);
//
//        FolderEntry projectFolder = (FolderEntry)projectEntry;
//
//        final Project project = new Project(projectFolder, this);
//
//        // Update config
//        if (projectConfig != null && projectConfig.getType() != null) {
//            //TODO: need add checking for concurrency attributes name in giving config and in estimation
//            for (Map.Entry<String, AttributeValue> entry : estimateProject(path, projectConfig.getType()).entrySet()) {
//                projectConfig.getAttributes().put(entry.getKey(), entry.getValue().getList());
//            }
//            project.updateConfig(projectConfig);
//        } else {  // try to get config (it will throw exception in case config is not valid)
//            projectConfig = project.getConfig();
//        }
//
//        if (projectConfig.getType() != null) {
//            PostImportProjectHandler postImportProjectHandler =
//                    handlers.getPostImportProjectHandler(projectConfig.getType());
//            if (postImportProjectHandler != null) {
//                postImportProjectHandler.onProjectImported(project.getBaseFolder());
//            }
//        }
//
//        final ProjectMisc misc = project.getMisc();
//        misc.setCreationDate(System.currentTimeMillis());
//        misc.save(); // Important to save misc!!
//
//        return project;
//    }

//    @Override
//    public VirtualFileEntry rename(String workspace, String path, String newName, String newMediaType)
//            throws ForbiddenException, ServerException, ConflictException, NotFoundException {
//        final FolderEntry root = getProjectsRoot(workspace);
//        final VirtualFileEntry entry = root.getChild(path);
//        if (entry == null) {
//            return null;
//        }
//
//        if (entry.isFile() && newMediaType != null) {
//            // Use the same rules as in method createFile to make client side simpler.
//            ((FileEntry)entry).rename(newName, newMediaType);
//        } else {
//            final Project project = getProject(workspace, path);
//
//            entry.rename(newName);
//
//            if (project != null) {
//                // get UsersWorkspaceDto
//                final UsersWorkspaceDto usersWorkspace = getWorkspace(workspace);
//                // replace path in all projects
//                final String oldProjectPath = path.startsWith("/") ? path : "/" + path;
//                usersWorkspace.getProjects()
//                              .stream()
//                              .filter(projectConfigDto -> projectConfigDto.getPath().startsWith(oldProjectPath))
//                              .forEach(projectConfigDto -> {
//                                  if (oldProjectPath.equals(projectConfigDto.getPath())) {
//                                      projectConfigDto.setName(newName);
//                                  }
//                                  projectConfigDto.setPath(projectConfigDto.getPath().replaceFirst(oldProjectPath, entry.getPath()));
//                              });
//                // update workspace with a new WorkspaceConfig
//                updateWorkspace(workspace, org.eclipse.che.api.workspace.server.DtoConverter.asDto((WorkspaceConfig)usersWorkspace));
//            }
//
//            final String projectName = projectPath(path);
//            // We should not edit Modules if resource to rename is project
//            if (!projectName.equals(path) && entry.isFolder()) {
//                final Project rootProject = getProject(workspace, projectName);
//                // TODO: rework
////                if (rootProject != null) {
////                    // We need module path without projectName, f.e projectName/module1/oldModuleName -> module1/oldModuleName
////                    String oldModulePath = path.replaceFirst(projectName + "/", "");
////                    // Calculates new module path, f.e module1/oldModuleName -> module1/newModuleName
////                    String newModulePath = oldModulePath.substring(0, oldModulePath.lastIndexOf("/") + 1) + newName;
////
////                    rootProject.getModules().update(oldModulePath, newModulePath);
////                }
//            }
//        }
//        return entry;
//    }
//

//    @Override
//    public void deleteModule(String workspaceId, String pathToParent, String pathToModule) throws ServerException,
//                                                                                                  NotFoundException,
//                                                                                                  ForbiddenException,
//                                                                                                  ConflictException {
//        VirtualFileEntry entryToDelete = getEntryToDelete(workspaceId, pathToModule);
//
//        pathToModule = pathToModule.startsWith("/") ? pathToModule.substring(1) : pathToModule;
//
//        String pathToProject = pathToModule.contains("/") ? pathToModule.substring(0, pathToModule.indexOf("/")) : pathToModule;
//
//        ProjectConfigDto project = getProjectFromWorkspace(workspaceId, pathToProject);
//
//        deleteModuleFromProject(project, (FolderEntry)entryToDelete, workspaceId);
//    }
//
//    private void deleteModuleFromProject(ProjectConfigDto project, FolderEntry entryToDelete, String workspaceId) throws ServerException,
//                                                                                                                         NotFoundException,
//                                                                                                                         ConflictException,
//                                                                                                                         ForbiddenException {
//        String pathToModule = entryToDelete.getPath();
//        String pathToParentModule = pathToModule.substring(0, pathToModule.lastIndexOf("/"));
//
//        ProjectConfigDto parentModule = project.findModule(pathToParentModule);
//        ProjectConfigDto moduleToDelete = project.findModule(pathToModule);
//
//        if (parentModule == null) {
//            parentModule = project;
//        }
//
//        if (moduleToDelete == null) {
//            throw new NotFoundException("Module " + pathToModule + " not found");
//        }
//
//        parentModule.getModules().remove(moduleToDelete);
//
//        updateProjectInWorkspace(workspaceId, project);
//
//        RemoveModuleHandler moduleHandler = this.getHandlers().getRemoveModuleHandler(moduleToDelete.getType());
//
//        if (moduleHandler != null) {
//            moduleHandler.onRemoveModule(entryToDelete.getParent(), moduleToDelete);
//        }
//
//        deleteEntryAndFireEvent(entryToDelete, workspaceId);
//    }
//
//    private void doDeleteProject(String wsId, String projectName) throws ServerException {
//        final String href = UriBuilder.fromUri(apiEndpoint)
//                                      .path(WorkspaceService.class).path(WorkspaceService.class, "deleteProject")
//                                      .build(wsId, projectName).toString();
//        final Link link = newDto(Link.class).withMethod("DELETE").withHref(href);
//
//        try {
//            httpJsonRequestFactory.fromLink(link)
//                                  .request();
//        } catch (IOException | ApiException exception) {
//            throw new ServerException(exception.getLocalizedMessage(), exception);
//        }
//    }
//
//    @Override
//    public boolean isProjectFolder(FolderEntry folder) throws ServerException {
//        try {
//            return getProjectFromWorkspace(folder.getWorkspace(), folder.getPath()) != null;
//        } catch (ApiException e) {
//            throw new ServerException(e);
//        }
//    }
//
//    @Override
//    public boolean isModuleFolder(FolderEntry folder) throws ServerException {
//        String pathToModuleFolder = folder.getPath();
//
//        String[] pathToModuleParts = pathToModuleFolder.split(String.format("(?=[%s])", File.separator));
//
//        ProjectConfigDto projectFromWorkspace = getProjectFromWorkspace(folder.getWorkspace(), pathToModuleParts[0]);
//
//        return projectFromWorkspace != null && projectFromWorkspace.findModule(pathToModuleFolder) != null;
//    }


    // =====================================
    //  Private methods
    // =====================================


    protected void initProjects()
            throws ServerException, NotFoundException, ProjectTypeConstraintException, InvalidValueException,
                   ValueStorageException {

        // take from vfs and config and merge
        // the difference between two lists (from config and from VFS)
        // is that config's one contains not only projects placed on root
        // but also  sub-projects
        UsersWorkspace workspace = workspaceHolder.getWorkspace();
        List<? extends ProjectConfig> projectConfigs = workspace.getProjects();
        for (ProjectConfig projectConfig : projectConfigs) {

            String path = absolutizePath(projectConfig.getPath());
            projects.put(path, new ProjectImpl(folder(path), projectConfig, false, this));

            initSubProjectsRecursively(projectConfig);


        }

        // only projects expected
        for (VirtualFile projectRoot : vfs.getRoot().getChildren()) {

            if (projectRoot.isFile()) {
                LOG.error("Plain file (not a folder) is not expected at the root of VFS: %s", projectRoot.getPath());
                continue; //  strange
            }

            if (!projects.containsKey(projectRoot.getPath().toString()))
                projects.put(projectRoot.getPath().toString(), new ProjectImpl(new FolderEntry(projectRoot), null, false, this));

        }

    }

    private void initSubProjectsRecursively(ProjectConfig parent)
            throws ServerException, ProjectTypeConstraintException, InvalidValueException, NotFoundException,
                   ValueStorageException {
        for (ProjectConfig pc : parent.getModules()) {

            projects.put(absolutizePath(pc.getPath()), new ProjectImpl(folder(absolutizePath(pc.getPath())), pc, false, this));
            initSubProjectsRecursively(pc);
        }

    }

    private FolderEntry folder(String path) throws ServerException {

        VirtualFile vf = vfs.getRoot().getChild(Path.of(path));
        return (vf == null) ? null : new FolderEntry(vf);
    }

    String absolutizePath(String path) {

        return (path.startsWith("/")) ? path : "/".concat(path);
    }


    // =================================
    // Inner classes
    // =================================

    private static class ImportedProjectConf implements ProjectConfig {

        private final String        name;
        private final String        path;
        private final SourceStorage source;

        private ImportedProjectConf(String name, String path, SourceStorage source) {
            this.name = name;
            this.path = path;
            this.source = source;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getPath() {
            return path;
        }

        @Override
        public String getDescription() {
            return "";
        }

        @Override
        public String getType() {
            return BaseProjectType.ID;
        }

        @Override
        public List<String> getMixins() {
            return new ArrayList<>();
        }

        @Override
        public Map<String, List<String>> getAttributes() {
            return new HashMap<>();
        }

        @Override
        public List<? extends ProjectConfig> getModules() {
            return new ArrayList<>();
        }

        @Override
        public SourceStorage getSource() {
            return source;
        }

        @Override
        public String getContentRoot() {
            return null;
        }
    }


}
