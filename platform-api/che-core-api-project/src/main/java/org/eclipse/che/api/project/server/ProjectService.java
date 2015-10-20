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

import com.google.common.base.Strings;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

import org.apache.commons.fileupload.FileItem;
import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.UnauthorizedException;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.core.rest.Service;
import org.eclipse.che.api.core.rest.annotations.Description;
import org.eclipse.che.api.core.rest.annotations.GenerateLink;
import org.eclipse.che.api.core.rest.annotations.Required;
import org.eclipse.che.api.core.util.LineConsumerFactory;
import org.eclipse.che.api.project.server.handlers.PostImportProjectHandler;
import org.eclipse.che.api.project.server.handlers.ProjectHandlerRegistry;
import org.eclipse.che.api.project.server.notification.ProjectItemModifiedEvent;
import org.eclipse.che.api.project.server.type.AttributeValue;
import org.eclipse.che.api.project.server.type.ProjectTypeRegistry;
import org.eclipse.che.api.project.shared.EnvironmentId;
import org.eclipse.che.api.project.shared.dto.CopyOptions;
import org.eclipse.che.api.project.shared.dto.GeneratorDescription;
import org.eclipse.che.api.project.shared.dto.ImportProject;
import org.eclipse.che.api.project.shared.dto.ImportResponse;
import org.eclipse.che.api.project.shared.dto.ImportSourceDescriptor;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.project.shared.dto.MoveOptions;
import org.eclipse.che.api.project.shared.dto.NewProject;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.project.shared.dto.ProjectModule;
import org.eclipse.che.api.project.shared.dto.ProjectProblem;
import org.eclipse.che.api.project.shared.dto.ProjectReference;
import org.eclipse.che.api.project.shared.dto.ProjectUpdate;
import org.eclipse.che.api.project.shared.dto.RunnerEnvironment;
import org.eclipse.che.api.project.shared.dto.RunnerEnvironmentLeaf;
import org.eclipse.che.api.project.shared.dto.RunnerEnvironmentTree;
import org.eclipse.che.api.project.shared.dto.RunnerSource;
import org.eclipse.che.api.project.shared.dto.RunnersDescriptor;
import org.eclipse.che.api.project.shared.dto.Source;
import org.eclipse.che.api.project.shared.dto.SourceEstimation;
import org.eclipse.che.api.project.shared.dto.TreeElement;
import org.eclipse.che.api.vfs.server.ContentStream;
import org.eclipse.che.api.vfs.server.VirtualFile;
import org.eclipse.che.api.vfs.server.VirtualFileSystemImpl;
import org.eclipse.che.api.vfs.server.search.QueryExpression;
import org.eclipse.che.api.vfs.server.search.SearcherProvider;
import org.eclipse.che.api.vfs.shared.dto.AccessControlEntry;
import org.eclipse.che.api.vfs.shared.dto.Principal;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.lang.ws.rs.ExtMediaType;
import org.eclipse.che.dto.server.DtoFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toMap;

/**
 * @author andrew00x
 * @author Eugene Voevodin
 * @author Artem Zatsarynnyy
 * @author Valeriy Svydenko
 */
@Api(value = "/project",
     description = "Project manager")
@Path("/project/{ws-id}")
@Singleton // important to have singleton
public class ProjectService extends Service {
    private static final Logger  LOG                   = LoggerFactory.getLogger(ProjectService.class);
    private static final Pattern RUNNER_NAME_VALIDATOR = Pattern.compile("[\\w-]+((:/)?[^/\\\\]+)?");

    @Inject
    private ProjectManager              projectManager;
    @Inject
    private ProjectImporterRegistry     importers;
    @Inject
    private SearcherProvider            searcherProvider;
    @Inject
    private EventService                eventService;
    @Inject
    private ProjectHandlerRegistry      projectHandlerRegistry;

    private final ExecutorService executor = Executors.newFixedThreadPool(1 + Runtime.getRuntime().availableProcessors(),
                                                                          new ThreadFactoryBuilder()
                                                                                  .setNameFormat("ProjectService-IndexingThread-")
                                                                                  .setDaemon(true).build());

    @PreDestroy
    void stop() {
        executor.shutdownNow();
    }


    /**
     * Class for internal use. Need for marking not valid project.
     * This need for giving possibility to end user to fix problems in project settings.
     * Will be useful then we will migrate IDE2 project to the IDE3 file system.
     */
    private class NotValidProject extends Project {
        public NotValidProject(FolderEntry baseFolder, ProjectManager manager) {
            super(baseFolder, manager);
        }

        @Override
        public ProjectConfig getConfig() throws ServerException, ValueStorageException {
            throw new ServerException("Looks like this is not valid project. We will mark it as broken");
        }
    }

    @ApiOperation(value = "Get list of projects in root folder",
                  response = ProjectReference.class,
                  responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 500, message = "Server error")})
    @GenerateLink(rel = Constants.LINK_REL_GET_PROJECTS)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<ProjectReference> getProjects(@ApiParam("ID of workspace to get projects") @PathParam("ws-id") String workspace)
            throws IOException, ServerException, ConflictException, ForbiddenException, NotFoundException {
        final List<Project> projects = projectManager.getProjects(workspace);

        final List<ProjectReference> projectReferences = new ArrayList<>(projects.size());

        for (Project project : projects) {

            try {
                projectReferences.add(DtoConverter.toReferenceDto2(project,
                                                                   getServiceContext().getServiceUriBuilder(),
                                                                   getServiceContext().getBaseUriBuilder()));
            } catch (RuntimeException e) {
                // Ignore known error for single project.
                // In result we won't have them in explorer tree but at least 'bad' projects won't prevent to show 'good' projects.
                LOG.error(e.getMessage(), e);
                NotValidProject notValidProject = new NotValidProject(project.getBaseFolder(), projectManager);
                projectReferences.add(DtoConverter.toReferenceDto2(notValidProject,
                                                                   getServiceContext().getServiceUriBuilder(),
                                                                   getServiceContext().getBaseUriBuilder()));
            }
        }
        FolderEntry projectsRoot = projectManager.getProjectsRoot(workspace);
        List<VirtualFileEntry> children = projectsRoot.getChildren();
        for (VirtualFileEntry child : children) {
            if (child.isFolder()) {
                FolderEntry folderEntry = (FolderEntry)child;
                if (!folderEntry.isProjectFolder()) {
                    NotValidProject notValidProject = new NotValidProject(folderEntry, projectManager);

                    projectReferences.add(DtoConverter.toReferenceDto2(notValidProject,
                                                                       getServiceContext().getServiceUriBuilder(),
                                                                       getServiceContext().getBaseUriBuilder()));
                }
            }
        }
        return projectReferences;
    }

    @ApiOperation(value = "Get project by ID of workspace and project's path",
            response = ProjectDescriptor.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Project with specified path doesn't exist in workspace"),
            @ApiResponse(code = 403, message = "Access to requested project is forbidden"),
            @ApiResponse(code = 500, message = "Server error")})
    @GET
    @Path("/{path:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public ProjectDescriptor getProject(@ApiParam(value = "ID of workspace to get projects", required = true)
                                        @PathParam("ws-id") String workspace,
                                        @ApiParam(value = "Path to requested project", required = true)
                                        @PathParam("path") String path)
            throws NotFoundException, ForbiddenException, ServerException, ConflictException {
        Project project = projectManager.getProject(workspace, path);
        if (project == null) {
            FolderEntry projectsRoot = projectManager.getProjectsRoot(workspace);
            VirtualFileEntry child = projectsRoot.getChild(path);
            if (child != null && child.isFolder() && child.getParent().isRoot()) {
                project = new NotValidProject((FolderEntry)child, projectManager);
            } else {
                throw new NotFoundException(String.format("Project '%s' doesn't exist in workspace '%s'.", path, workspace));
            }
        }

        try {
            return DtoConverter.toDescriptorDto2(project,
                                                 getServiceContext().getServiceUriBuilder(),
                                                 getServiceContext().getBaseUriBuilder(),
                                                 projectManager.getProjectTypeRegistry(),
                                                 workspace);
        } catch (InvalidValueException e) {
            NotValidProject notValidProject = new NotValidProject(project.getBaseFolder(), projectManager);
            return DtoConverter.toDescriptorDto2(notValidProject,
                                                 getServiceContext().getServiceUriBuilder(),
                                                 getServiceContext().getBaseUriBuilder(),
                                                 projectManager.getProjectTypeRegistry(),
                                                 workspace);
        }
    }

    @ApiOperation(value = "Creates a new project",
                  response = ProjectDescriptor.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 403, message = "Operation is forbidden"),
            @ApiResponse(code = 409, message = "Project with specified name already exist in workspace"),
            @ApiResponse(code = 500, message = "Server error")})

    @POST
    @GenerateLink(rel = Constants.LINK_REL_CREATE_PROJECT)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ProjectDescriptor createProject(@ApiParam(value = "ID of workspace to create project", required = true)
                                           @PathParam("ws-id") String workspace,
                                           @ApiParam(value = "Name for new project", required = true)
                                           @Required
                                           @Description("project name")
                                           @QueryParam("name") String name,
                                           @ApiParam(value = "New project", required = true)
                                           @Description("descriptor of project") NewProject newProject)
            throws ConflictException, ForbiddenException, ServerException, NotFoundException, BadRequestException {
        requiredNotNull(workspace, "Workspace id");
        requiredNotNull(newProject, "Project descriptor");
        checkProjectName(name);
        checkProjectRunners(newProject.getRunners());

        final GeneratorDescription generatorDescription = newProject.getGeneratorDescription();
        Map<String, String> options;
        if (generatorDescription == null) {
            options = Collections.emptyMap();
        } else {
            options = generatorDescription.getOptions();
        }

        final Project project = projectManager.createProject(workspace, name,
                                                             DtoConverter.fromDto2(newProject, projectManager.getProjectTypeRegistry()),
                                                             options,
                                                             newProject.getVisibility());

        final ProjectDescriptor descriptor = DtoConverter.toDescriptorDto2(project,
                                                                           getServiceContext().getServiceUriBuilder(),
                                                                           getServiceContext().getBaseUriBuilder(),
                                                                           projectManager.getProjectTypeRegistry(),
                                                                           workspace);

        eventService.publish(new ProjectCreatedEvent(project.getWorkspace(), project.getPath()));

        logProjectCreatedEvent(descriptor.getName(), descriptor.getType());

        return descriptor;
    }

    @ApiOperation(value = "Get project modules",
                  notes = "Get project modules. Roles allowed: system/admin, system/manager.",
                  response = ProjectDescriptor.class,
                  responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 403, message = "User not authorized to call this operation"),
            @ApiResponse(code = 404, message = "Not found"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/modules/{path:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ProjectDescriptor> getModules(@ApiParam(value = "Workspace ID", required = true)
                                              @PathParam("ws-id") String workspace,
                                              @ApiParam(value = "Path to a project", required = true)
                                              @PathParam("path") String path)
            throws NotFoundException, ForbiddenException, ServerException, ConflictException, IOException {

        Project parent = projectManager.getProject(workspace, path);
        if (parent == null) {
            throw new NotFoundException("Project " + path + " was not found");
        }
        final List<ProjectDescriptor> modules = new LinkedList<>();
        for (Project module : projectManager.getProjectModules(parent)) {
                modules.add(DtoConverter.toDescriptorDto2(module,
                                                          getServiceContext().getServiceUriBuilder(),
                                                          getServiceContext().getBaseUriBuilder(),
                                                          projectManager.getProjectTypeRegistry(),
                                                          workspace));
        }
        return modules;
    }

    @ApiOperation(value = "Create a new module",
                  notes = "Create a new module in a specified project",
                  response = ProjectDescriptor.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 403, message = "User not authorized to call this operation"),
            @ApiResponse(code = 404, message = "Not found"),
            @ApiResponse(code = 409, message = "Module already exists"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @POST
    @Path("/{path:.*}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ProjectDescriptor createModule(@ApiParam(value = "Workspace ID", required = true)
                                          @PathParam("ws-id") String workspace,
                                          @ApiParam(value = "Path to a target directory", required = true)
                                          @PathParam("path") String parentPath,
                                          @ApiParam(value = "New module name", required = true)
                                          @QueryParam("path") String path,
                                          NewProject newProject)
            throws NotFoundException, ConflictException, ForbiddenException, ServerException {

        Project module = projectManager.addModule(workspace, parentPath, path,
                (newProject == null) ? null : DtoConverter
                        .fromDto2(newProject, projectManager.getProjectTypeRegistry()),
                (newProject == null) ? null : newProject.getGeneratorDescription().getOptions(),
                (newProject == null) ? null : newProject.getVisibility());


        final ProjectDescriptor descriptor = DtoConverter.toDescriptorDto2(module,
                                                                           getServiceContext().getServiceUriBuilder(),
                                                                           getServiceContext().getBaseUriBuilder(),
                                                                           projectManager.getProjectTypeRegistry(),
                                                                           workspace);

        eventService.publish(new ProjectCreatedEvent(module.getWorkspace(), module.getPath()));

        logProjectCreatedEvent(descriptor.getName(), descriptor.getType());

        return descriptor;
    }

    @ApiOperation(value = "Updates existing project",
                  response = ProjectDescriptor.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Project with specified path doesn't exist in workspace"),
            @ApiResponse(code = 403, message = "Operation is forbidden"),
            @ApiResponse(code = 409, message = "Update operation causes conflicts"),
            @ApiResponse(code = 500, message = "Server error")})
    @PUT
    @Path("/{path:.*}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ProjectDescriptor updateProject(@ApiParam(value = "ID of workspace", required = true)
                                           @PathParam("ws-id") String workspace,
                                           @ApiParam(value = "Path to updated project", required = true)
                                           @PathParam("path") String path,
                                           ProjectUpdate update)
            throws NotFoundException, ConflictException, ForbiddenException, ServerException, IOException, BadRequestException {
        requiredNotNull(workspace, "Workspace id");
        requiredNotNull(path, "Project path");
        checkProjectRunners(update.getRunners());
        ProjectConfig newConfig = DtoConverter.fromDto2(update, projectManager.getProjectTypeRegistry());
        String newVisibility = update.getVisibility();
        Project project = projectManager.updateProject(workspace, path, newConfig, newVisibility);
        return DtoConverter.toDescriptorDto2(project,
                                             getServiceContext().getServiceUriBuilder(),
                                             getServiceContext().getBaseUriBuilder(),
                                             projectManager.getProjectTypeRegistry(),
                                             workspace);
    }

    @ApiOperation(value = "Make sure the folder is supposed to be project of a certain type",
                  response = Map.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Project with specified path doesn't exist in workspace"),
            @ApiResponse(code = 403, message = "Access to requested project is forbidden"),
            @ApiResponse(code = 500, message = "Server error")})
    @GET
    @Path("/estimate/{path:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, List<String>> estimateProject(@ApiParam(value = "ID of workspace to estimate projects", required = true)
                                                     @PathParam("ws-id") String workspace,
                                                     @ApiParam(value = "Path to requested project", required = true)
                                                     @PathParam("path") String path,
                                                     @ApiParam(value = "Project Type ID to estimate against", required = true)
                                                     @QueryParam("type") String projectType)
            throws NotFoundException, ForbiddenException, ServerException, ConflictException {

        final HashMap<String, List<String>> attributes = new HashMap<>();

        for (Map.Entry<String, AttributeValue> attr : projectManager.estimateProject(workspace, path, projectType).entrySet()) {
            attributes.put(attr.getKey(), attr.getValue().getList());
        }

        return attributes;
    }

    @GET
    @Path("/resolve/{path:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SourceEstimation> resolveSources(@ApiParam(value = "ID of workspace to estimate projects", required = true)
                                                 @PathParam("ws-id") String workspace,
                                                 @ApiParam(value = "Path to requested project", required = true)
                                                 @PathParam("path") String path)
            throws NotFoundException, ForbiddenException, ServerException, ConflictException {

        return projectManager.resolveSources(workspace, path, false);
    }

    @ApiOperation(value = "Create file",
                  notes = "Create a new file in a project. If file type isn't specified the server will resolve its type")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = ""),
            @ApiResponse(code = 403, message = "User not authorized to call this operation"),
            @ApiResponse(code = 404, message = "Not found"),
            @ApiResponse(code = 409, message = "File already exists"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @POST
    @Consumes({MediaType.MEDIA_TYPE_WILDCARD})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/file/{parent:.*}")
    public Response createFile(@ApiParam(value = "Workspace ID", required = true)
                               @PathParam("ws-id") String workspace,
                               @ApiParam(value = "Path to a target directory", required = true)
                               @PathParam("parent") String parentPath,
                               @ApiParam(value = "New file name", required = true)
                               @QueryParam("name") String fileName,
                               @ApiParam(value = "New file content type")
                               @HeaderParam("content-type") MediaType contentType,
                               InputStream content) throws NotFoundException, ConflictException, ForbiddenException, ServerException {
        final FolderEntry parent = asFolder(workspace, parentPath);
        // Have issue with client side. Always have Content-type header is set even if client doesn't set it.
        // In this case have Content-type is set with "text/plain; charset=UTF-8" which isn't acceptable.
        // Have agreement with client to send Content-type header with "application/unknown" value if client doesn't want to specify media
        // type of new file. In this case server takes care about resolving media type of file.
        final FileEntry newFile;
        if (contentType == null || ("application".equals(contentType.getType()) && "unknown".equals(contentType.getSubtype()))) {
            newFile = parent.createFile(fileName, content, null);
        } else {
            newFile = parent.createFile(fileName, content, (contentType.getType() + '/' + contentType.getSubtype()));
        }
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        final ItemReference fileReference = DtoConverter.toItemReferenceDto(newFile, uriBuilder.clone());
        final URI location = uriBuilder.clone().path(getClass(), "getFile").build(workspace, newFile.getPath().substring(1));

        eventService.publish(new ProjectItemModifiedEvent(ProjectItemModifiedEvent.EventType.CREATED,
                                                          workspace, projectPath(newFile.getPath()), newFile.getPath(), false));

        return Response.created(location).entity(fileReference).build();
    }

    @ApiOperation(value = "Create a folder",
                  notes = "Create a folder is a specified project")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = ""),
            @ApiResponse(code = 403, message = "User not authorized to call this operation"),
            @ApiResponse(code = 404, message = "Not found"),
            @ApiResponse(code = 409, message = "File already exists"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @POST
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/folder/{path:.*}")
    public Response createFolder(@ApiParam(value = "Workspace ID", required = true)
                                 @PathParam("ws-id") String workspace,
                                 @ApiParam(value = "Path to a new folder destination", required = true)
                                 @PathParam("path") String path)
            throws ConflictException, ForbiddenException, ServerException, NotFoundException {
        final FolderEntry newFolder = projectManager.getProjectsRoot(workspace).createFolder(path);
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        final ItemReference folderReference = DtoConverter.toItemReferenceDto(newFolder, uriBuilder.clone());
        final URI location = uriBuilder.clone().path(getClass(), "getChildren").build(workspace, newFolder.getPath().substring(1));

        eventService.publish(new ProjectItemModifiedEvent(ProjectItemModifiedEvent.EventType.CREATED,
                                                          workspace, projectPath(newFolder.getPath()), newFolder.getPath(), true));

        return Response.created(location).entity(folderReference).build();
    }

    @ApiOperation(value = "Upload a file",
                  notes = "Upload a new file")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = ""),
            @ApiResponse(code = 403, message = "User not authorized to call this operation"),
            @ApiResponse(code = 404, message = "Not found"),
            @ApiResponse(code = 409, message = "File already exists"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @POST
    @Consumes({MediaType.MULTIPART_FORM_DATA})
    @Produces({MediaType.TEXT_HTML})
    @Path("/uploadfile/{parent:.*}")
    public Response uploadFile(@ApiParam(value = "Workspace ID", required = true)
                               @PathParam("ws-id") String workspace,
                               @ApiParam(value = "Destination path", required = true)
                               @PathParam("parent") String parentPath,
                               Iterator<FileItem> formData)
            throws NotFoundException, ConflictException, ForbiddenException, ServerException {
        final FolderEntry parent = asFolder(workspace, parentPath);

        return VirtualFileSystemImpl.uploadFile(parent.getVirtualFile(), formData);
    }

    @ApiOperation(value = "Upload zip folder",
                  notes = "Upload folder from local zip",
                  response = Response.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = ""),
            @ApiResponse(code = 401, message = "User not authorized to call this operation"),
            @ApiResponse(code = 403, message = "Forbidden operation"),
            @ApiResponse(code = 404, message = "Not found"),
            @ApiResponse(code = 409, message = "Resource already exists"),
            @ApiResponse(code = 500, message = "Internal Server Error")})

    @POST
    @Consumes({MediaType.MULTIPART_FORM_DATA})
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/upload/zipfolder/{path:.*}")
    public Response uploadFolderFromZip(@ApiParam(value = "Workspace ID", required = true)
                                        @PathParam("ws-id") String workspace,
                                        @ApiParam(value = "Path in the project", required = true)
                                        @PathParam("path") String path,
                                        Iterator<FileItem> formData)
            throws ServerException, ConflictException, ForbiddenException, NotFoundException {

        final FolderEntry parent = asFolder(workspace, path);
        return VirtualFileSystemImpl.uploadZip(parent.getVirtualFile(), formData);
    }

    @ApiOperation(value = "Get file content",
                  notes = "Get file content by its name")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 403, message = "User not authorized to call this operation"),
            @ApiResponse(code = 404, message = "Not found"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/file/{path:.*}")
    public Response getFile(@ApiParam(value = "Workspace ID", required = true)
                            @PathParam("ws-id") String workspace,
                            @ApiParam(value = "Path to a file", required = true)
                            @PathParam("path") String path)
            throws IOException, NotFoundException, ForbiddenException, ServerException {
        final FileEntry file = asFile(workspace, path);
        return Response.ok().entity(file.getInputStream()).type(file.getMediaType()).build();
    }

    @ApiOperation(value = "Update file",
                  notes = "Update an existing file with new content")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = ""),
            @ApiResponse(code = 403, message = "User not authorized to call this operation"),
            @ApiResponse(code = 404, message = "Not found"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @PUT
    @Consumes({MediaType.MEDIA_TYPE_WILDCARD})
    @Path("/file/{path:.*}")
    public Response updateFile(@ApiParam(value = "Workspace ID", required = true)
                               @PathParam("ws-id") String workspace,
                               @ApiParam(value = "Full path to a file", required = true)
                               @PathParam("path") String path,
                               @ApiParam(value = "Media Type")
                               @HeaderParam("content-type") MediaType contentType,
                               InputStream content) throws NotFoundException, ForbiddenException, ServerException {
        final FileEntry file = asFile(workspace, path);
        // Have issue with client side. Always have Content-type header is set even if client doesn't set it.
        // In this case have Content-type is set with "text/plain; charset=UTF-8" which isn't acceptable.
        // Have agreement with client to send Content-type header with "application/unknown" value if client doesn't want to specify media
        // type of new file. In this case server takes care about resolving media type of file.
        if (contentType == null || ("application".equals(contentType.getType()) && "unknown".equals(contentType.getSubtype()))) {
            file.updateContent(content);
        } else {
            file.updateContent(content, contentType.getType() + '/' + contentType.getSubtype());
        }

        eventService.publish(new ProjectItemModifiedEvent(ProjectItemModifiedEvent.EventType.UPDATED,
                                                          workspace, projectPath(file.getPath()), file.getPath(), false));
        return Response.ok().build();
    }

    @ApiOperation(value = "Delete a resource",
                  notes = "Delete resources. If you want to delete a single project, specify project name. If a folder or file needs to " +
                          "be deleted a path to the requested resource needs to be specified")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = ""),
            @ApiResponse(code = 403, message = "User not authorized to call this operation"),
            @ApiResponse(code = 404, message = "Not found"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @DELETE
    @Path("/{path:.*}")
    public void delete(@ApiParam(value = "Workspace ID", required = true)
                       @PathParam("ws-id") String workspace,
                       @ApiParam(value = "Path to a resource to be deleted", required = true)
                       @PathParam("path") String path,
                       @QueryParam("module") String modulePath)
            throws NotFoundException, ForbiddenException, ConflictException, ServerException {
        if (!projectManager.delete(workspace, path, modulePath)) {
            throw new NotFoundException(String.format("Path '%s' doesn't exist.", path));
        }
    }

    @ApiOperation(value = "Copy resource",
                  notes = "Copy resource to a new location which is specified in a query parameter. " +
                          "It is possible to specify new files name in a JSON. " +
                          "All paths start with /{project-name}")
    @ApiResponses({@ApiResponse(code = 201, message = ""),
                   @ApiResponse(code = 403, message = "User not authorized to call this operation"),
                   @ApiResponse(code = 404, message = "Not found"),
                   @ApiResponse(code = 409, message = "Resource already exists"),
                   @ApiResponse(code = 500, message = "Internal Server Error")})
    @POST
    @Path("/copy/{path:.*}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response copy(@ApiParam("Workspace ID") @PathParam("ws-id") String workspace,
                         @ApiParam("Path to a resource") @PathParam("path") String path,
                         @ApiParam(value = "Path to a new location", required = true) @QueryParam("to") String newParent,
                         CopyOptions copyOptions) throws NotFoundException,
                                                         ForbiddenException,
                                                         ConflictException,
                                                         ServerException {
        final VirtualFileEntry entry = getVirtualFileEntry(workspace, path);
        // used to indicate over write of destination
        boolean isOverWrite = false;
        // used to hold new name set in request body
        String newName = entry.getName();
        if (copyOptions != null) {
            if (copyOptions.getOverWrite() != null) {
                isOverWrite = copyOptions.getOverWrite();
            }
            if (copyOptions.getName() != null) {
                newName = copyOptions.getName();
            }
        }
        final VirtualFileEntry copy = entry.copyTo(newParent, newName, isOverWrite);
        final URI location = getServiceContext().getServiceUriBuilder()
                                                .path(getClass(), copy.isFile() ? "getFile" : "getChildren")
                                                .build(workspace, copy.getPath().substring(1));
        if (copy.isFolder() && ((FolderEntry)copy).isProjectFolder()) {
            Project project = new Project((FolderEntry)copy, projectManager);
            final String name = project.getName();
            final String projectType = project.getConfig().getTypeId();

            logProjectCreatedEvent(name, projectType);
        }
        return Response.created(location).build();
    }

    @ApiOperation(value = "Move resource",
                  notes = "Move resource to a new location which is specified in a query parameter. " +
                          "It is possible to change new file name in a new location. File name is passed in a JSON. " +
                          "All paths start with /{project-name}")
    @ApiResponses({@ApiResponse(code = 201, message = ""),
                   @ApiResponse(code = 403, message = "User not authorized to call this operation"),
                   @ApiResponse(code = 404, message = "Not found"),
                   @ApiResponse(code = 409, message = "Resource already exists"),
                   @ApiResponse(code = 500, message = "Internal Server Error")})
    @POST
    @Path("/move/{path:.*}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response move(@ApiParam("Workspace ID") @PathParam("ws-id") String workspace,
                         @ApiParam("Path to a resource to be moved") @PathParam("path") String path,
                         @ApiParam("Path to a new location") @QueryParam("to") String newParent,
                         MoveOptions moveOptions)
            throws NotFoundException, ForbiddenException, ConflictException, ServerException {
        final VirtualFileEntry entry = getVirtualFileEntry(workspace, path);

// used to indicate over write of destination
        boolean isOverWrite = false;
        // used to hold new name set in request body
        String newName = entry.getName();
        if (moveOptions != null) {
            if (moveOptions.getOverWrite() != null) {
                isOverWrite = moveOptions.getOverWrite();
            }
            if (moveOptions.getName() != null) {
                newName = moveOptions.getName();
            }
        }

        entry.moveTo(newParent, newName, isOverWrite);
        final URI location = getServiceContext().getServiceUriBuilder()
                                                .path(getClass(), entry.isFile() ? "getFile" : "getChildren")
                                                .build(workspace, entry.getPath().substring(1));
        if (entry.isFolder() && ((FolderEntry)entry).isProjectFolder()) {
            Project project = new Project((FolderEntry)entry, projectManager);
            final String name = project.getName();
            final String projectType = project.getConfig().getTypeId();
            LOG.info("EVENT#project-destroyed# PROJECT#{}# TYPE#{}# WS#{}# USER#{}#", name, projectType,
                    EnvironmentContext.getCurrent().getWorkspaceName(), EnvironmentContext.getCurrent().getUser().getName());

            logProjectCreatedEvent(name, projectType);
        }
        return Response.created(location).build();
    }

    @ApiOperation(value = "Rename resource",
                  notes = "Rename resources. It can be project, module, folder or file")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = ""),
            @ApiResponse(code = 403, message = "User not authorized to call this operation"),
            @ApiResponse(code = 404, message = "Not found"),
            @ApiResponse(code = 409, message = "Resource already exists"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @POST
    @Path("/rename/{path:.*}")
    public Response rename(@ApiParam(value = "Workspace ID", required = true)
                           @PathParam("ws-id") String workspace,
                           @ApiParam(value = "Path to resource to be renamed", required = true)
                           @PathParam("path") String path,
                           @ApiParam(value = "New name", required = true)
                           @QueryParam("name") String newName,
                           @ApiParam(value = "New media type")
                           @QueryParam("mediaType") String newMediaType)
            throws NotFoundException, ConflictException, ForbiddenException, ServerException, IOException {
        final VirtualFileEntry entry = projectManager.rename(workspace, path, newName, newMediaType);
        if (entry == null) {
            throw new NotFoundException(String.format("Path '%s' doesn't exist.", path));
        }
        final URI location = getServiceContext().getServiceUriBuilder()
                                                .path(getClass(), entry.isFile() ? "getFile" : "getChildren")
                                                .build(workspace, entry.getPath().substring(1));
        return Response.created(location).build();
    }

    @ApiOperation(value = "Import resource",
                  notes = "Import resource. JSON with a designated importer and project location is sent. It is possible to import from " +
                          "VCS or ZIP",
                  response = ImportResponse.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = ""),
            @ApiResponse(code = 401, message = "User not authorized to call this operation"),
            @ApiResponse(code = 403, message = "Forbidden operation"),
            @ApiResponse(code = 409, message = "Resource already exists"),
            @ApiResponse(code = 500, message = "Unsupported source type")})
    @POST
    @Path("/import/{path:.*}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ImportResponse importProject(@ApiParam(value = "Workspace ID", required = true)
                                        @PathParam("ws-id") String workspace,
                                        @ApiParam(value = "Path in the project", required = true)
                                        @PathParam("path") String path,
                                        @ApiParam(value = "Force rewrite existing project", allowableValues = "true,false")
                                        @QueryParam("force") boolean force,
                                        ImportProject importProject)
            throws ConflictException, ForbiddenException, UnauthorizedException, IOException, ServerException, NotFoundException,
                   BadRequestException {
        requiredNotNull(workspace, "Workspace id");
        requiredNotNull(path, "Project path");
        requiredNotNull(importProject, "Project descriptor");

        final ImportSourceDescriptor projectSource = importProject.getSource().getProject();
        final ProjectImporter importer = importers.getImporter(projectSource.getType());
        if (importer == null) {
            throw new ServerException(String.format("Unable import sources project from '%s'. Sources type '%s' is not supported.",
                                                    projectSource.getLocation(), projectSource.getType()));
        }
        // Preparing websocket output publisher to broadcast output of import process to the ide clients while importing
        final LineConsumerFactory outputOutputConsumerFactory = () -> new ProjectImportOutputWSLineConsumer(path, workspace, 300);

        // Not all importers uses virtual file system API. In this case virtual file system API doesn't get events and isn't able to set
        // correct creation time. Need do it manually.
        long creationDate = System.currentTimeMillis();
        VirtualFileEntry virtualFile = getVirtualFile(workspace, path, force);

        final FolderEntry baseProjectFolder = (FolderEntry)virtualFile;
        importer.importSources(baseProjectFolder, projectSource.getLocation(), projectSource.getParameters(), outputOutputConsumerFactory);

        //project source already imported going to configure project
        return configureProject(importProject, baseProjectFolder, workspace, creationDate);
    }

    private VirtualFileEntry getVirtualFile(String workspace, String path, boolean force)
            throws ServerException, ForbiddenException, ConflictException, NotFoundException {
        VirtualFileEntry virtualFile = projectManager.getProjectsRoot(workspace).getChild(path);
        if (virtualFile != null && virtualFile.isFile()) {
            // File with same name exist already exists.
            throw new ConflictException(String.format("File with the name '%s' already exists.", path));
        } else {
            if (virtualFile == null) {
                return projectManager.getProjectsRoot(workspace).createFolder(path);
            } else if (!force) {
                // Project already exists.
                throw new ConflictException(String.format("Project with the name '%s' already exists.", path));
            }
        }
        return virtualFile;
    }

    private ImportResponse configureProject(ImportProject importProject, FolderEntry baseProjectFolder, String workspace, long creationDate)
            throws IOException, ForbiddenException, ConflictException, NotFoundException, ServerException, BadRequestException {
        NewProject newProject = importProject.getProject();
        requiredNotNull(newProject, "Project descriptor");
        checkProjectRunners(newProject.getRunners());
        ImportResponse importResponse = DtoFactory.getInstance().createDto(ImportResponse.class);
        ProjectTypeRegistry projectTypeRegistry = projectManager.getProjectTypeRegistry();
        Project project;
        ProjectDescriptor projectDescriptor;
        ProjectConfig projectConfig;
        String visibility = null;

        //try convert folder to project with giving config
        try {
            visibility = newProject.getVisibility();
            projectConfig = DtoConverter.fromDto2(newProject, projectTypeRegistry);
            project = projectManager.convertFolderToProject(workspace,
                                                            baseProjectFolder.getPath(),
                                                            projectConfig,
                                                            visibility);

            for (ProjectModule projectModule : newProject.getModules()) {
                ProjectConfig moduleConfig = DtoConverter.toProjectConfig(projectModule, projectManager.getProjectTypeRegistry());
                projectManager.convertFolderToProject(workspace,
                                                      baseProjectFolder.getPath() +
                                                      projectModule.getPath(),
                                                      moduleConfig,
                                                      visibility);

                projectManager.addModule(workspace,
                                         baseProjectFolder.getPath(),
                                         baseProjectFolder.getPath() +
                                         projectModule.getPath(),
                                         moduleConfig,
                                         null,
                                         visibility);
                RunnersDescriptor projectModuleRunners = projectModule.getRunners();
                if (projectModuleRunners != null && projectModuleRunners.getDefault() != null) {
                    String defaultRunnerName = projectModuleRunners.getDefault()
                                                                   .substring(projectModuleRunners.getDefault().lastIndexOf('/') + 1);
                    importRunnerEnvironment(importProject,
                                            (FolderEntry)baseProjectFolder.getChild(projectModule.getPath()),
                                            defaultRunnerName);
                }
            }

            projectDescriptor = DtoConverter.toDescriptorDto2(project,
                                                              getServiceContext().getServiceUriBuilder(),
                                                              getServiceContext().getBaseUriBuilder(),
                                                              projectManager.getProjectTypeRegistry(),
                                                              workspace);
            PostImportProjectHandler postImportProjectHandler =
                    projectHandlerRegistry.getPostImportProjectHandler(projectDescriptor.getType());
            if (postImportProjectHandler != null) {
                postImportProjectHandler.onProjectImported(project.getBaseFolder());
            }
        } catch (ConflictException | ForbiddenException | ServerException | NotFoundException e) {
            if (newProject.getType() == null
                && !Strings.isNullOrEmpty(newProject.getDescription())
                || newProject.getRunners() != null
                || newProject.getBuilders() != null ) {
                throw new BadRequestException("Impossible to save configurations for the project without type");
            }
            project = new NotValidProject(baseProjectFolder, projectManager);
            project.setVisibility(visibility);

            projectDescriptor = DtoConverter.toDescriptorDto2(project,
                                                              getServiceContext().getServiceUriBuilder(),
                                                              getServiceContext().getBaseUriBuilder(),
                                                              projectManager.getProjectTypeRegistry(),
                                                              workspace);
            ProjectProblem problem = DtoFactory.getInstance().createDto(ProjectProblem.class).withCode(1).withMessage(e.getMessage());
            projectDescriptor.setProblems(Collections.singletonList(problem));
        }
        project.getMisc().setContentRoot(newProject.getContentRoot());
        project.getMisc().save();
        projectDescriptor.setContentRoot(newProject.getContentRoot());
        importResponse.setProjectDescriptor(projectDescriptor);
        //we will add project type estimations any way
        List<SourceEstimation> sourceEstimations = projectManager.resolveSources(workspace, baseProjectFolder.getPath(), false);
        importResponse.setSourceEstimations(sourceEstimations);
        reindexProject(creationDate, baseProjectFolder, project);
        importRunnerEnvironment(importProject, baseProjectFolder);
        eventService.publish(new ProjectCreatedEvent(project.getWorkspace(), project.getPath()));
        logProjectCreatedEvent(projectDescriptor.getName(), projectDescriptor.getType());
        return importResponse;
    }

    @ApiOperation(value = "Upload zip project",
                  notes = "Upload project from local zip",
                  response = ImportResponse.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = ""),
            @ApiResponse(code = 401, message = "User not authorized to call this operation"),
            @ApiResponse(code = 403, message = "Forbidden operation"),
            @ApiResponse(code = 409, message = "Resource already exists"),
            @ApiResponse(code = 500, message = "Unsupported source type")})

    @POST
    @Consumes({MediaType.MULTIPART_FORM_DATA})
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/upload/zipproject/{path:.*}")
    public ImportResponse uploadProjectFromZip(@ApiParam(value = "Workspace ID", required = true)
                                               @PathParam("ws-id") String workspace,
                                               @ApiParam(value = "Path in the project", required = true)
                                               @PathParam("path") String path,
                                               @ApiParam(value = "Force rewrite existing project", allowableValues = "true,false")
                                               @QueryParam("force") boolean force,
                                               Iterator<FileItem> formData)
            throws ServerException, IOException, ConflictException, ForbiddenException, NotFoundException, BadRequestException {

        // Not all importers uses virtual file system API. In this case virtual file system API doesn't get events and isn't able to set
        // correct creation time. Need do it manually.
        long creationDate = System.currentTimeMillis();
        final FolderEntry baseProjectFolder = (FolderEntry)getVirtualFile(workspace, path, force);

        int stripNumber = 0;
        String projectName = "";
        String projectDescription = "";
        String privacy = "";
        FileItem contentItem = null;

        while (formData.hasNext()) {
            FileItem item = formData.next();
            if (!item.isFormField()) {
                if (contentItem == null) {
                    contentItem = item;
                } else {
                    throw new ServerException("More then one upload file is found but only one should be. ");
                }
            } else {
                switch (item.getFieldName()) {
                    case ("name"):
                        projectName = item.getString().trim();
                        break;
                    case ("description"):
                        projectDescription = item.getString().trim();
                        break;
                    case ("privacy"):
                        privacy = Boolean.parseBoolean(item.getString().trim()) ? "public" : "private";
                        break;
                    case ("skipFirstLevel"):
                        stripNumber = Boolean.parseBoolean(item.getString().trim()) ? 1 : 0;
                        break;
                }
            }
        }

        if (contentItem == null) {
            throw new ServerException("Cannot find zip file for upload.");
        }
        try (InputStream zip = contentItem.getInputStream()) {
            baseProjectFolder.getVirtualFile().unzip(zip, true, stripNumber);
        }

        final DtoFactory dtoFactory = DtoFactory.getInstance();
        NewProject newProject = dtoFactory.createDto(NewProject.class)
                                          .withName(projectName)
                                          .withDescription(projectDescription)
                                          .withVisibility(privacy);
        Source source = dtoFactory.createDto(Source.class)
                                  .withRunners(new HashMap<String, RunnerSource>());
        ImportProject importProject = dtoFactory.createDto(ImportProject.class)
                                                .withProject(newProject)
                                                .withSource(source);

        //project source already imported going to configure project
        return configureProject(importProject, baseProjectFolder, workspace, creationDate);
    }

    /**
     * Import runner environment tha configure in ImportProject
     *
     * @param importProject
     * @param baseProjectFolder
     * @throws ForbiddenException
     * @throws ServerException
     * @throws ConflictException
     * @throws IOException
     */
    private void importRunnerEnvironment(ImportProject importProject, FolderEntry baseProjectFolder)
            throws ForbiddenException, ServerException, ConflictException, IOException {
        importRunnerEnvironment(importProject, baseProjectFolder, null);
    }

    /**
     * Imports runners into environments.
     */
    private void importRunnerEnvironment(ImportProject importProject, FolderEntry baseProjectFolder, String defaultRunnerName)
            throws ForbiddenException, ServerException, ConflictException, IOException {
        VirtualFileEntry environmentsFolder = baseProjectFolder.getChild(Constants.CODENVY_RUNNER_ENVIRONMENTS_DIR);
        if (environmentsFolder != null && environmentsFolder.isFile()) {
            throw new ConflictException(String.format("Unable import runner environments. File with the name '%s' already exists.",
                                                      Constants.CODENVY_RUNNER_ENVIRONMENTS_DIR));
        } else if (environmentsFolder == null) {
            environmentsFolder = baseProjectFolder.createFolder(Constants.CODENVY_RUNNER_ENVIRONMENTS_DIR);
        }

        Map<String, RunnerSource> filteredRunners = importProject.getSource().getRunners()
                                                                 .entrySet()
                                                                 .stream()
                                                                 .filter(this::isValidRunnerEntry)
                                                                 .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

        for (Map.Entry<String, RunnerSource> entry : filteredRunners.entrySet()) {
            String name = entry.getKey().substring(entry.getKey().lastIndexOf('/') + 1);
            if (defaultRunnerName == null || name.equals(defaultRunnerName)) {
                try (InputStream in = new java.net.URL(entry.getValue().getLocation()).openStream()) {
                    // Add file without mediatype to avoid creation useless metadata files on virtual file system level.
                    // Dockerfile add in list of known files, see ContentTypeGuesser
                    // and content-types.attributes file.
                    ((FolderEntry)environmentsFolder).createFolder(name).createFile("Dockerfile", in, null);
                }
            }
        }
    }

    /**
     * Checks valid of runner location sources and runner type.
     *
     * @param entry
     *         runner entry
     * @return return false if runner location or runner type is not valid
     */
    private boolean isValidRunnerEntry(Map.Entry<String, RunnerSource> entry) {
        final RunnerSource runnerSourceValue = entry.getValue();
        if (runnerSourceValue == null) {
            return false;
        }

        if (!(runnerSourceValue.getLocation().startsWith("https") || runnerSourceValue.getLocation().startsWith("http"))) {
            LOG.warn("ProjectService.importProject :: not valid runner source location available only http or https scheme but we get :" +
                     runnerSourceValue);
            return false;
        }
        return true;
    }

    /**
     * Some importers don't use virtual file system API and changes are not indexed.
     * Force searcher to reindex project to fix such issues.
     *
     * @param creationDate
     * @param baseProjectFolder
     * @param project
     * @throws ServerException
     */
    private void reindexProject(long creationDate, FolderEntry baseProjectFolder, final Project project) throws ServerException {
        final VirtualFile file = baseProjectFolder.getVirtualFile();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    searcherProvider.getSearcher(file.getMountPoint(), true).add(file);
                } catch (Exception e) {
                    LOG.warn(String.format("Workspace: %s, project: %s", project.getWorkspace(), project.getPath()), e.getMessage());
                }
            }
        });
        if (creationDate > 0) {
            final ProjectMisc misc = project.getMisc();
            misc.setCreationDate(creationDate);
        }
    }

    @ApiOperation(value = "Import zip",
                  notes = "Import resources as zip")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = ""),
            @ApiResponse(code = 403, message = "User not authorized to call this operation"),
            @ApiResponse(code = 404, message = "Not found"),
            @ApiResponse(code = 409, message = "Resource already exists"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @POST
    @Path("/import/{path:.*}")
    @Consumes(ExtMediaType.APPLICATION_ZIP)
    public Response importZip(@ApiParam(value = "Workspace ID", required = true)
                              @PathParam("ws-id") String workspace,
                              @ApiParam(value = "Path to a location (where import to?)")
                              @PathParam("path") String path,
                              InputStream zip,
                              @DefaultValue("false") @QueryParam("skipFirstLevel") Boolean skipFirstLevel)
            throws NotFoundException, ConflictException, ForbiddenException, ServerException {
        final FolderEntry parent = asFolder(workspace, path);
        VirtualFileSystemImpl.importZip(parent.getVirtualFile(), zip, true, skipFirstLevel);
        if (parent.isProjectFolder()) {
            Project project = new Project(parent, projectManager);
            eventService.publish(new ProjectCreatedEvent(project.getWorkspace(), project.getPath()));
            final String projectType = project.getConfig().getTypeId();
            logProjectCreatedEvent(path, projectType);
        }
        return Response.created(getServiceContext().getServiceUriBuilder()
                                                   .path(getClass(), "getChildren")
                                                   .build(workspace, parent.getPath().substring(1))).build();
    }

    @ApiOperation(value = "Download ZIP",
                  notes = "Export resource as zip. It can be an entire project or folder")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = ""),
            @ApiResponse(code = 403, message = "User not authorized to call this operation"),
            @ApiResponse(code = 404, message = "Not found"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/export/{path:.*}")
    @Produces(ExtMediaType.APPLICATION_ZIP)
    public ContentStream exportZip(@ApiParam(value = "Workspace ID", required = true)
                                   @PathParam("ws-id") String workspace,
                                   @ApiParam(value = "Path to resource to be imported")
                                   @PathParam("path") String path)
            throws NotFoundException, ForbiddenException, ServerException {
        final FolderEntry folder = asFolder(workspace, path);
        return VirtualFileSystemImpl.exportZip(folder.getVirtualFile());
    }

    @POST
    @Path("/export/{path:.*}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(ExtMediaType.APPLICATION_ZIP)
    public Response exportDiffZip(@PathParam("ws-id") String workspace, @PathParam("path") String path, InputStream in)
            throws NotFoundException, ForbiddenException, ServerException {
        final FolderEntry folder = asFolder(workspace, path);
        return VirtualFileSystemImpl.exportZip(folder.getVirtualFile(), in);
    }

    @POST
    @Path("/export/{path:.*}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.MULTIPART_FORM_DATA)
    public Response exportDiffZipMultipart(@PathParam("ws-id") String workspace, @PathParam("path") String path, InputStream in)
            throws NotFoundException, ForbiddenException, ServerException {
        final FolderEntry folder = asFolder(workspace, path);
        return VirtualFileSystemImpl.exportZipMultipart(folder.getVirtualFile(), in);
    }

    @GET
    @Path("/export/file/{path:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response exportFile(@ApiParam(value = "Workspace ID", required = true)
                               @PathParam("ws-id") String workspace,
                               @ApiParam(value = "Path to resource to be imported")
                               @PathParam("path") String path)
            throws NotFoundException, ForbiddenException, ServerException {
        final FileEntry file = asFile(workspace, path);
        ContentStream content = file.getVirtualFile().getContent();
        return VirtualFileSystemImpl.downloadFile(content);
    }

    @ApiOperation(value = "Get project children items",
                  notes = "Request all children items for a project, such as files and folders",
                  response = ItemReference.class,
                  responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 403, message = "User not authorized to call this operation"),
            @ApiResponse(code = 404, message = "Not found"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/children/{parent:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ItemReference> getChildren(@ApiParam(value = "Workspace ID", required = true)
                                           @PathParam("ws-id") String workspace,
                                           @ApiParam(value = "Path to a project", required = true)
                                           @PathParam("parent") String path)
            throws NotFoundException, ForbiddenException, ServerException {
        final FolderEntry folder = asFolder(workspace, path);
        final List<VirtualFileEntry> children = folder.getChildren();
        final ArrayList<ItemReference> result = new ArrayList<>(children.size());
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        for (VirtualFileEntry child : children) {
            if (child.isFile()) {
                result.add(DtoConverter.toItemReferenceDto((FileEntry)child, uriBuilder.clone()));
            } else {
                result.add(DtoConverter.toItemReferenceDto((FolderEntry)child, uriBuilder.clone()));
            }
        }

        return result;
    }

    @ApiOperation(value = "Get project tree",
                  notes = "Get project tree. Depth is specified in a query parameter",
                  response = TreeElement.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 403, message = "User not authorized to call this operation"),
            @ApiResponse(code = 404, message = "Not found"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/tree/{parent:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public TreeElement getTree(@ApiParam(value = "Workspace ID", required = true)
                               @PathParam("ws-id") String workspace,
                               @ApiParam(value = "Path to resource. Can be project or its folders", required = true)
                               @PathParam("parent") String path,
                               @ApiParam(value = "Tree depth. This parameter can be dropped. If not specified ?depth=1 is used by default")
                               @DefaultValue("1") @QueryParam("depth") int depth,
                               @ApiParam(value = "include children files (in addition to children folders). This parameter can be dropped. If not specified ?includeFiles=false is used by default")
    						   @DefaultValue("false") @QueryParam("includeFiles") boolean includeFiles)
            throws NotFoundException, ForbiddenException, ServerException {
        final FolderEntry folder = asFolder(workspace, path);
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        final DtoFactory dtoFactory = DtoFactory.getInstance();
        return dtoFactory.createDto(TreeElement.class)
                         .withNode(DtoConverter.toItemReferenceDto(folder, uriBuilder.clone()))
                         .withChildren(getTree(folder, depth, includeFiles, uriBuilder, dtoFactory));
    }

    @ApiOperation(value = "Get file or folder",
                  response = ItemReference.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 403, message = "User not authorized to call this operation"),
            @ApiResponse(code = 404, message = "Not found"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/item/{path:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public ItemReference getItem(@ApiParam(value = "Workspace ID", required = true)
                                 @PathParam("ws-id") String workspace,
                                 @ApiParam(value = "Path to resource. Can be project or its folders", required = true)
                                 @PathParam("path") String path)
            throws NotFoundException, ForbiddenException, ServerException, ValueStorageException,
                   ProjectTypeConstraintException {

        Project project = projectManager.getProject(workspace, projectPath(path));
        final VirtualFileEntry entry = project.getItem(path);

        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();

        ItemReference item;
        if (entry.isFile()) {
            item = DtoConverter.toItemReferenceDto((FileEntry)entry, uriBuilder.clone());
        } else {
            item = DtoConverter.toItemReferenceDto((FolderEntry)entry, uriBuilder.clone());
        }

        return item;
    }

    private List<TreeElement> getTree(FolderEntry folder, int depth, boolean includeFiles, UriBuilder uriBuilder, DtoFactory dtoFactory) throws ServerException {
        if (depth == 0) {
            return null;
        }
        final List<? extends VirtualFileEntry> children;

        if (includeFiles) {
        	children = folder.getChildFoldersFiles();
        }else {
        	children = folder.getChildFolders();
        }

        final List<TreeElement> nodes = new ArrayList<>(children.size());
        for (VirtualFileEntry child : children) {
        	if (child.isFolder()) {
        		nodes.add(dtoFactory.createDto(TreeElement.class).withNode(DtoConverter.toItemReferenceDto((FolderEntry)child, uriBuilder.clone())).withChildren(getTree((FolderEntry)child, depth - 1, includeFiles, uriBuilder, dtoFactory)));
        	} else { // child.isFile()
        		nodes.add(dtoFactory.createDto(TreeElement.class).withNode(DtoConverter.toItemReferenceDto((FileEntry)child, uriBuilder.clone())));
        	}
        }
        return nodes;
    }

    @ApiOperation(value = "Search for resources",
                  notes = "Search for resources applying a number of search filters as query parameters",
                  response = ItemReference.class,
                  responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 403, message = "User not authorized to call this operation"),
            @ApiResponse(code = 404, message = "Not found"),
            @ApiResponse(code = 409, message = "Conflict error"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/search/{path:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ItemReference> search(@ApiParam(value = "Workspace ID", required = true)
                                      @PathParam("ws-id") String workspace,
                                      @ApiParam(value = "Path to resource, i.e. where to search?", required = true)
                                      @PathParam("path") String path,
                                      @ApiParam(value = "Resource name")
                                      @QueryParam("name") String name,
                                      @ApiParam(value = "Media type")
                                      @QueryParam("mediatype") String mediatype,
                                      @ApiParam(value = "Search keywords")
                                      @QueryParam("text") String text,
                                      @ApiParam(value = "Maximum items to display. If this parameter is dropped, there are no limits")
                                      @QueryParam("maxItems") @DefaultValue("-1") int maxItems,
                                      @ApiParam(value = "Skip count")
                                      @QueryParam("skipCount") int skipCount)
            throws NotFoundException, ForbiddenException, ConflictException, ServerException {

        // to search from workspace root path should end with "/" i.e /{ws}/search/?<query>
        final FolderEntry folder = path.isEmpty() ? projectManager.getProjectsRoot(workspace) : asFolder(workspace, path);

        if (searcherProvider != null) {
            if (skipCount < 0) {
                throw new ConflictException(String.format("Invalid 'skipCount' parameter: %d.", skipCount));
            }
            final QueryExpression expr = new QueryExpression()
                    .setPath(path.startsWith("/") ? path : ('/' + path))
                    .setName(name)
                    .setMediaType(mediatype)
                    .setText(text);

            final String[] result = searcherProvider.getSearcher(folder.getVirtualFile().getMountPoint(), true).search(expr);
            if (skipCount > 0) {
                if (skipCount > result.length) {
                    throw new ConflictException(
                            String.format("'skipCount' parameter: %d is greater then total number of items in result: %d.",
                                          skipCount, result.length));
                }
            }
            final int length = maxItems > 0 ? Math.min(result.length, maxItems) : result.length;
            final List<ItemReference> items = new ArrayList<>(length);
            final FolderEntry root = projectManager.getProjectsRoot(workspace);
            final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
            for (int i = skipCount; i < length; i++) {
                VirtualFileEntry child = null;
                try {
                    child = root.getChild(result[i]);
                } catch (ForbiddenException ignored) {
                    // Ignore item that user can't access
                }
                if (child != null && child.isFile()) {
                    items.add(DtoConverter.toItemReferenceDto((FileEntry)child, uriBuilder.clone()));
                }
            }
            return items;
        }
        return Collections.emptyList();
    }

    @ApiOperation(value = "Get user permissions in a project",
                  notes = "Get permissions for a user in a specified project, such as read, write, build, " +
                          "run etc. ID of a user is set in a query parameter of a request URL. Roles allowed: workspace/admin",
                  response = AccessControlEntry.class,
                  responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 403, message = "User not authorized to call this operation"),
            @ApiResponse(code = 404, message = "Not found"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/permissions/{path:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("workspace/admin")
    public List<AccessControlEntry> getPermissions(@ApiParam(value = "Workspace ID", required = true)
                                                   @PathParam("ws-id") String wsId,
                                                   @ApiParam(value = "Path to a project", required = true)
                                                   @PathParam("path") String path,
                                                   @ApiParam(value = "User ID", required = true)
                                                   @QueryParam("userid") Set<String> users)
            throws NotFoundException, ForbiddenException, ServerException {
        final Project project = projectManager.getProject(wsId, path);
        if (project == null) {
            throw new NotFoundException(String.format("Project '%s' doesn't exist in workspace '%s'.", path, wsId));
        }
        final List<AccessControlEntry> acl = project.getPermissions();
        if (!(users == null || users.isEmpty())) {
            for (Iterator<AccessControlEntry> itr = acl.iterator(); itr.hasNext(); ) {
                final AccessControlEntry ace = itr.next();
                final Principal principal = ace.getPrincipal();
                if (principal.getType() != Principal.Type.USER || !users.contains(principal.getName())) {
                    itr.remove();
                }
            }
        }
        return acl;
    }

    @ApiOperation(value = "Set project visibility",
                  notes = "Set project visibility. Projects can be private or public")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "OK"),
            @ApiResponse(code = 403, message = "User not authorized to call this operation"),
            @ApiResponse(code = 404, message = "Not found"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @POST
    @Path("/switch_visibility/{path:.*}")
    public void switchVisibility(@ApiParam(value = "Workspace ID", required = true)
                                 @PathParam("ws-id") String wsId,
                                 @ApiParam(value = "Path to a project", required = true)
                                 @PathParam("path") String path,
                                 @ApiParam(value = "Visibility type", required = true, allowableValues = "public,private")
                                 @QueryParam("visibility") String visibility)
            throws NotFoundException, ForbiddenException, ServerException {
        if (visibility == null || visibility.isEmpty()) {
            throw new ServerException(String.format("Invalid visibility '%s'", visibility));
        }
        final Project project = projectManager.getProject(wsId, path);
        if (project == null) {
            throw new NotFoundException(String.format("Project '%s' doesn't exist in workspace '%s'.", path, wsId));
        }
        project.setVisibility(visibility);
    }

    @ApiOperation(value = "Set permissions for a user in a project",
                  notes = "Set permissions for a user in a specified project, such as read, write, build, " +
                          "run etc. ID of a user is set in a query parameter of a request URL.",
                  response = AccessControlEntry.class,
                  responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "OK"),
            @ApiResponse(code = 403, message = "User not authorized to call this operation"),
            @ApiResponse(code = 404, message = "Not found"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @POST
    @Path("/permissions/{path:.*}")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("workspace/admin")
    public List<AccessControlEntry> setPermissions(@ApiParam(value = "Workspace ID", required = true)
                                                   @PathParam("ws-id") String wsId,
                                                   @ApiParam(value = "Path to a project", required = true)
                                                   @PathParam("path") String path,
                                                   @ApiParam(value = "Permissions", required = true)
                                                   List<AccessControlEntry> acl)
            throws ForbiddenException, ServerException, NotFoundException {
        final Project project = projectManager.getProject(wsId, path);
        if (project == null) {
            throw new ServerException(String.format("Project '%s' doesn't exist in workspace '%s'. ", path, wsId));
        }
        project.setPermissions(acl);
        return project.getPermissions();
    }

    @ApiOperation(value = "Get available project-scoped runner environments",
                  notes = "Get available project-scoped runner environments.",
                  response = RunnerEnvironmentTree.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 403, message = "User not authorized to call this operation"),
            @ApiResponse(code = 404, message = "Not found"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/runner_environments/{path:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public RunnerEnvironmentTree getRunnerEnvironments(@ApiParam(value = "Workspace ID", required = true)
                                                       @PathParam("ws-id") String workspace,
                                                       @ApiParam(value = "Path to a project", required = true)
                                                       @PathParam("path") String path)
            throws NotFoundException, ForbiddenException, ServerException {
        final Project project = projectManager.getProject(workspace, path);
        final DtoFactory dtoFactory = DtoFactory.getInstance();
        final RunnerEnvironmentTree root = dtoFactory.createDto(RunnerEnvironmentTree.class).withDisplayName("project");
        if (project != null) {
            final List<RunnerEnvironmentLeaf> environments = new LinkedList<>();
            final VirtualFileEntry environmentsFolder = project.getBaseFolder().getChild(Constants.CODENVY_RUNNER_ENVIRONMENTS_DIR);
            if (environmentsFolder != null && environmentsFolder.isFolder()) {
                for (FolderEntry childFolder : ((FolderEntry)environmentsFolder).getChildFolders()) {
                    final String id = new EnvironmentId(EnvironmentId.Scope.project, childFolder.getName()).toString();
                    environments.add(dtoFactory.createDto(RunnerEnvironmentLeaf.class)
                                               .withEnvironment(dtoFactory.createDto(RunnerEnvironment.class).withId(id))
                                               .withDisplayName(childFolder.getName()));
                }
            }
            return root.withLeaves(environments);
        } else {
            return root.withLeaves(Collections.<RunnerEnvironmentLeaf>emptyList());
        }
    }

    private FileEntry asFile(String workspace, String path) throws ForbiddenException, NotFoundException, ServerException {
        final VirtualFileEntry entry = getVirtualFileEntry(workspace, path);
        if (!entry.isFile()) {
            throw new ForbiddenException(String.format("Item '%s' isn't a file. ", path));
        }
        return (FileEntry)entry;
    }

    private FolderEntry asFolder(String workspace, String path) throws ForbiddenException, NotFoundException, ServerException {
        final VirtualFileEntry entry = getVirtualFileEntry(workspace, path);
        if (!entry.isFolder()) {
            throw new ForbiddenException(String.format("Item '%s' isn't a folder. ", path));
        }
        return (FolderEntry)entry;
    }

    private VirtualFileEntry getVirtualFileEntry(String workspace, String path)
            throws NotFoundException, ForbiddenException, ServerException {
        final FolderEntry root = projectManager.getProjectsRoot(workspace);
        final VirtualFileEntry entry = root.getChild(path);
        if (entry == null) {
            throw new NotFoundException(String.format("Path '%s' doesn't exist.", path));
        }
        return entry;
    }

    private void logProjectCreatedEvent(@NotNull String projectName, @NotNull String projectType) {
        LOG.info("EVENT#project-created# PROJECT#{}# TYPE#{}# WS#{}# USER#{}# PAAS#default#",
                 projectName,
                 projectType,
                 EnvironmentContext.getCurrent().getWorkspaceId(),
                 EnvironmentContext.getCurrent().getUser().getId());
    }

    private String projectPath(String path) {
        int end = path.indexOf("/");
        if (end == -1) {
            return path;
        }
        return path.substring(0, end);
    }

    /**
     * Checks object reference is not {@code null}
     *
     * @param object
     *         object reference to check
     * @param subject
     *         used as subject of exception message "{subject} required"
     * @throws BadRequestException
     *         when object reference is {@code null}
     */
    void requiredNotNull(Object object, String subject) throws BadRequestException {
        if (object == null) {
            throw new BadRequestException(subject + " required");
        }
    }

    /**
     * Checks the validity of project name.
     *
     * @param name
     *         project name
     * @throws BadRequestException
     *         when if the project name is null or empty of is invalid
     */
    void checkProjectName(String name) throws BadRequestException {
        if (Strings.isNullOrEmpty(name)) {
            throw new BadRequestException("Project name required");
        }
        if (!Pattern.compile("[a-zA-Z0-9]+[\\w-]*").matcher(name).matches()) {
            throw new BadRequestException("Project name " + name + " is invalid");
        }
    }

    /**
     * Checks the validity of project runners.
     *
     * @param runnersDescriptor
     *         project runners
     * @throws BadRequestException
     *         when runner name empty or is invalid
     */
    void checkProjectRunners(RunnersDescriptor runnersDescriptor) throws BadRequestException {
        if (runnersDescriptor != null && runnersDescriptor.getConfigs() != null && !runnersDescriptor.getConfigs().isEmpty()) {
            for (String runnerName : runnersDescriptor.getConfigs().keySet()) {
                if (!RUNNER_NAME_VALIDATOR.matcher(runnerName).matches()) {
                    throw new BadRequestException("Runner name " + runnerName + " is invalid");
                }
            }
        }
    }
}
