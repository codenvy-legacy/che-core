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
package org.eclipse.che.api.workspace.server;

import com.google.common.collect.ImmutableMap;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.machine.MachineSource;
import org.eclipse.che.api.core.model.machine.Server;
import org.eclipse.che.api.core.model.workspace.Machine;
import org.eclipse.che.api.core.model.workspace.MachineMetadata;
import org.eclipse.che.api.core.model.workspace.RuntimeWorkspace;
import org.eclipse.che.api.core.model.workspace.UsersWorkspace;
import org.eclipse.che.api.core.rest.Service;
import org.eclipse.che.api.core.rest.annotations.GenerateLink;
import org.eclipse.che.api.core.rest.permission.PermissionManager;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.workspace.server.model.impl.CommandImpl;
import org.eclipse.che.api.workspace.server.model.impl.EnvironmentImpl;
import org.eclipse.che.api.workspace.server.model.impl.ProjectConfigImpl;
import org.eclipse.che.api.workspace.server.model.impl.UsersWorkspaceImpl;
import org.eclipse.che.api.workspace.shared.dto.CommandDto;
import org.eclipse.che.api.workspace.shared.dto.EnvironmentDto;
import org.eclipse.che.api.workspace.shared.dto.MachineDto;
import org.eclipse.che.api.workspace.shared.dto.MachineMetadataDto;
import org.eclipse.che.api.workspace.shared.dto.MachineSourceDto;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.api.workspace.shared.dto.RuntimeWorkspaceDto;
import org.eclipse.che.api.workspace.shared.dto.ServerDto;
import org.eclipse.che.api.workspace.shared.dto.UsersWorkspaceDto;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceConfigDto;
import org.eclipse.che.commons.env.EnvironmentContext;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.che.api.core.model.workspace.WorkspaceStatus.RUNNING;
import static org.eclipse.che.api.core.util.LinksHelper.createLink;
import static org.eclipse.che.api.workspace.server.Constants.GET_ALL_USER_WORKSPACES;
import static org.eclipse.che.api.workspace.server.Constants.LINK_REL_GET_RUNTIMEWORKSPACE;
import static org.eclipse.che.api.workspace.server.Constants.GET_USERS_WORKSPACE;
import static org.eclipse.che.api.workspace.server.Constants.LINK_REL_CREATE_WORKSPACE;
import static org.eclipse.che.api.workspace.server.Constants.LINK_REL_REMOVE_WORKSPACE;
import static org.eclipse.che.api.workspace.server.Constants.LINK_REL_START_WORKSPACE;
import static org.eclipse.che.api.workspace.server.Constants.START_WORKSPACE;
import static org.eclipse.che.api.workspace.server.Constants.STOP_WORKSPACE;
import static org.eclipse.che.dto.server.DtoFactory.newDto;

/**
 * Workspace API
 *
 * @author Eugene Voevodin
 */
@Api(value = "/workspace", description = "Workspace service")
@Path("/workspace")
public class WorkspaceService extends Service {

    private final WorkspaceManager  workspaceManager;
    private final PermissionManager permissionManager;

    @Context
    private SecurityContext securityContext;

    @Inject
    public WorkspaceService(WorkspaceManager workspaceManager, @Named("workspace") PermissionManager permissionManager) {
        this.workspaceManager = workspaceManager;
        this.permissionManager = permissionManager;
    }

    @ApiOperation(value = "Create a new workspace",
                  notes = "For 'system/admin' it is required to set new workspace owner, " +
                          "when for any other kind of 'user' it is not(users identifier will be used for this purpose)")
    @ApiResponses({@ApiResponse(code = 201, message = "Workspace was created successfully"),
                   @ApiResponse(code = 400, message = "Missed required parameters"),
                   @ApiResponse(code = 403, message = "User does not have access to create new workspace"),
                   @ApiResponse(code = 409, message = "Conflict error was occurred during workspace creation"),
                   @ApiResponse(code = 500, message = "Internal server error was occurred during workspace creation")})
    @POST
    @Path("/config")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed("user")
    @GenerateLink(rel = LINK_REL_CREATE_WORKSPACE)
    public UsersWorkspaceDto create(@ApiParam(value = "new workspace", required = true) UsersWorkspaceDto newWorkspace,
                                    @ApiParam("account id") @QueryParam("account") String accountId)
            throws ConflictException, ServerException, BadRequestException, ForbiddenException, NotFoundException {
        if (securityContext.isUserInRole("user")) {
            newWorkspace.withOwner(getCurrentUserId());
        }
        if (newWorkspace.getOwner() == null) {
            throw new BadRequestException("New workspace owner required");
        }
        return injectLinks(DtoConverter.asDto(workspaceManager.createWorkspace(newWorkspace, newWorkspace.getOwner(), accountId)));
    }

    @DELETE
    @Path("/{id}/config")
    @RolesAllowed("user")
    public void delete(@PathParam("id") String id)
            throws BadRequestException, ServerException, NotFoundException, ConflictException, ForbiddenException {
        ensureUserIsWorkspaceOwner(id);
        workspaceManager.removeWorkspace(id);
    }

    @PUT
    @Path("/{id}/config")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed("user")
    public UsersWorkspaceDto update(@PathParam("id") String id, WorkspaceConfigDto workspaceCfg)
            throws BadRequestException, ServerException, ForbiddenException, NotFoundException, ConflictException {
        ensureUserIsWorkspaceOwner(id);
        return injectLinks(DtoConverter.asDto(workspaceManager.updateWorkspace(id, workspaceCfg)));
    }

    @ApiOperation("Get workspace by id")
    @ApiResponses({@ApiResponse(code = 200, message = "Response contains requested workspace entity"),
                   @ApiResponse(code = 404, message = "Workspace with specified id does not exist"),
                   @ApiResponse(code = 403, message = "User does not have access to requested workspace"),
                   @ApiResponse(code = 500, message = "Internal server error was occurred during workspace getting")})
    @GET
    @Path("/{id}")
    @Produces(APPLICATION_JSON)
    @RolesAllowed("user")
    public UsersWorkspaceDto getById(@ApiParam("Workspace ID") @PathParam("id") String id)
            throws NotFoundException, ServerException, ForbiddenException, BadRequestException {
        ensureUserIsWorkspaceOwner(id);
        return injectLinks(DtoConverter.asDto(workspaceManager.getWorkspace(id)));
    }

    @GET
    @Path("/name/{name}")
    @RolesAllowed("user")
    @Produces(APPLICATION_JSON)
    public UsersWorkspaceDto getByName(@PathParam("name") String name)
            throws ServerException, BadRequestException, NotFoundException, ForbiddenException {
        final UsersWorkspace workspace = workspaceManager.getWorkspace(name, getCurrentUserId());
        ensureUserIsWorkspaceOwner(workspace.getId());
        return injectLinks(DtoConverter.asDto(workspace));
    }

    @GET
    @Produces(APPLICATION_JSON)
    @RolesAllowed("user")
    public List<UsersWorkspaceDto> getWorkspaces(@DefaultValue("0") @QueryParam("skipCount") Integer skipCount,
                                                 @DefaultValue("30") @QueryParam("maxItems") Integer maxItems)
            throws ServerException, BadRequestException {
        //TODO add maxItems & skipCount to manager
        return workspaceManager.getWorkspaces(getCurrentUserId())
                               .stream()
                               .map(workspace -> injectLinks(DtoConverter.asDto(workspace)))
                               .collect(toList());
    }

    @GET
    @Path("/runtime")
    @Produces(APPLICATION_JSON)
    @RolesAllowed("user")
    public List<RuntimeWorkspaceDto> getRuntimeWorkspaces(@DefaultValue("0") @QueryParam("skipCount") Integer skipCount,
                                                          @DefaultValue("30") @QueryParam("maxItems") Integer maxItems)
            throws BadRequestException {
        //TODO add maxItems & skipCount to manager
        return workspaceManager.getRuntimeWorkspaces(getCurrentUserId())
                               .stream()
                               .map(this::asDto)
                               .collect(toList());
    }

    @GET
    @Path("/{id}/runtime")
    @Produces(APPLICATION_JSON)
    @RolesAllowed("user")
    public RuntimeWorkspaceDto getRuntimeWorkspaceById(@PathParam("id") String id)
            throws ServerException, BadRequestException, NotFoundException, ForbiddenException {
        ensureUserIsWorkspaceOwner(id);
        return asDto(workspaceManager.getRuntimeWorkspace(id));
    }

    @GET
    @Path("/name/{name}/runtime")
    @Produces(APPLICATION_JSON)
    @RolesAllowed("user")
    public RuntimeWorkspace getRuntimeWorkspaceByName(@PathParam("name") String name)
            throws ServerException, BadRequestException, ForbiddenException {
        final RuntimeWorkspace workspace = workspaceManager.getRuntimeWorkspace(name, getCurrentUserId());
        ensureUserIsWorkspaceOwner(workspace.getId());
        return asDto(workspace);
    }

    //TODO who can perform this method?

    @POST
    @Path("/runtime")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public UsersWorkspaceDto startTemporary(WorkspaceConfigDto cfg, @QueryParam("account") String accountId)
            throws BadRequestException, ForbiddenException, NotFoundException, ServerException {
        return injectLinks(DtoConverter.asDto(workspaceManager.startTemporaryWorkspace(cfg, accountId)));
    }

    @POST
    @Path("/{id}/runtime")
    @Produces(APPLICATION_JSON)
    @RolesAllowed("user")
    public UsersWorkspaceDto startById(@PathParam("id") String workspaceId,
                                       @QueryParam("environment") String envName,
                                       @QueryParam("accountId") String accountId)
            throws ServerException, BadRequestException, NotFoundException, ForbiddenException {
        ensureUserIsWorkspaceOwner(workspaceId);

        final Map<String, String> params = ImmutableMap.of("accountId", accountId, "workspaceId", workspaceId);
        permissionManager.checkPermission(START_WORKSPACE, getCurrentUserId(), params);

        return injectLinks(DtoConverter.asDto(workspaceManager.startWorkspaceById(workspaceId, envName, accountId)));
    }

    @POST
    @Path("/name/{name}/runtime")
    @Produces(APPLICATION_JSON)
    @RolesAllowed("user")
    public UsersWorkspaceDto startByName(@QueryParam("name") String name,
                                         @QueryParam("environment") String envName,
                                         @QueryParam("accountId") String accountId)
            throws ServerException, BadRequestException, NotFoundException, ForbiddenException {
        final UsersWorkspace workspace = workspaceManager.getWorkspace(name, getCurrentUserId());
        ensureUserIsWorkspaceOwner(workspace.getId());

        final ImmutableMap<String, String> params = ImmutableMap.of("accountId", accountId, "workspaceId", workspace.getId());
        permissionManager.checkPermission(START_WORKSPACE, getCurrentUserId(), params);

        return injectLinks(DtoConverter.asDto(workspaceManager.startWorkspaceByName(name, envName, getCurrentUserId(), accountId)));
    }

    @DELETE
    @Path("/{id}/runtime")
    @RolesAllowed("user")
    public void stop(@PathParam("id") String id)
            throws BadRequestException, ForbiddenException, NotFoundException, ServerException {
        ensureUserIsWorkspaceOwner(id);
        workspaceManager.stopWorkspace(id);
    }

    @POST
    @Path("/{id}/command")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed("user")
    public UsersWorkspaceDto addCommand(@PathParam("id") String id, CommandDto newCommand)
            throws ServerException, BadRequestException, NotFoundException, ConflictException, ForbiddenException {
        ensureUserIsWorkspaceOwner(id);
        if (newCommand == null) {
            throw new BadRequestException("Command required");
        }
        final UsersWorkspaceImpl workspace = workspaceManager.getWorkspace(id);
        workspace.getCommands().add(new CommandImpl(newCommand));
        return injectLinks(DtoConverter.asDto(workspaceManager.updateWorkspace(workspace.getId(), workspace)));
    }

    @PUT
    @Path("/{id}/command")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed("user")
    public UsersWorkspaceDto updateCommand(@PathParam("id") String id, CommandDto update)
            throws ServerException, BadRequestException, NotFoundException, ConflictException, ForbiddenException {
        ensureUserIsWorkspaceOwner(id);
        if (update == null) {
            throw new BadRequestException("Command update required");
        }
        final UsersWorkspaceImpl workspace = workspaceManager.getWorkspace(id);
        if (!workspace.getCommands().removeIf(cmd -> cmd.getName().equals(update.getName()))) {
            throw new NotFoundException("Workspace " + id + " doesn't contain command " + update.getName());
        }
        workspace.getCommands().add(new CommandImpl(update));
        return injectLinks(DtoConverter.asDto(workspaceManager.updateWorkspace(workspace.getId(), workspace)));
    }

    @DELETE
    @Path("/{id}/command/{name}")
    @RolesAllowed("user")
    public void deleteCommand(@PathParam("id") String id, @PathParam("name") String commandName)
            throws ServerException, BadRequestException, NotFoundException, ConflictException, ForbiddenException {
        ensureUserIsWorkspaceOwner(id);
        final UsersWorkspaceImpl workspace = workspaceManager.getWorkspace(id);
        if (workspace.getCommands().removeIf(command -> command.getName().equals(commandName))) {
            workspaceManager.updateWorkspace(id, workspace);
        }
    }

    @POST
    @Path("/{id}/environment")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed("user")
    public UsersWorkspaceDto addEnvironment(@PathParam("id") String id, EnvironmentDto newEnvironment)
            throws ServerException, BadRequestException, NotFoundException, ConflictException, ForbiddenException {
        ensureUserIsWorkspaceOwner(id);
        final UsersWorkspaceImpl workspace = workspaceManager.getWorkspace(id);
        workspace.getEnvironments().put(newEnvironment.getName(), new EnvironmentImpl(newEnvironment));
        return injectLinks(DtoConverter.asDto(workspaceManager.updateWorkspace(id, workspace)));
    }

    @PUT
    @Path("/{id}/environment")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed("user")
    public UsersWorkspaceDto updateEnvironment(@PathParam("id") String id, EnvironmentDto update)
            throws ServerException, BadRequestException, NotFoundException, ConflictException, ForbiddenException {
        ensureUserIsWorkspaceOwner(id);
        final UsersWorkspaceImpl workspace = workspaceManager.getWorkspace(id);
        if (!workspace.getEnvironments().containsKey(update.getName())) {
            throw new NotFoundException("Workspace " + id + " doesn't contain environment " + update.getName());
        }
        workspace.getEnvironments().put(update.getName(), new EnvironmentImpl(update));
        return injectLinks(DtoConverter.asDto(workspaceManager.updateWorkspace(id, workspace)));
    }

    @DELETE
    @Path("/{id}/environment/{name}")
    @RolesAllowed("user")
    public void deleteEnvironment(@PathParam("id") String id, @PathParam("name") String envName)
            throws ServerException, BadRequestException, NotFoundException, ConflictException, ForbiddenException {
        ensureUserIsWorkspaceOwner(id);
        final UsersWorkspaceImpl workspace = workspaceManager.getWorkspace(id);
        if (workspace.getEnvironments().containsKey(envName)) {
            workspace.getEnvironments().remove(envName);
            workspaceManager.updateWorkspace(id, workspace);
        }
    }

    @POST
    @Path("/{id}/project")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed("user")
    public UsersWorkspaceDto addProject(@PathParam("id") String id, ProjectConfigDto newProject)
            throws ServerException, BadRequestException, NotFoundException, ConflictException, ForbiddenException {
        ensureUserIsWorkspaceOwner(id);
        final UsersWorkspaceImpl workspace = workspaceManager.getWorkspace(id);
        workspace.getProjects().add(new ProjectConfigImpl(newProject));
        return injectLinks(DtoConverter.asDto(workspaceManager.updateWorkspace(id, workspace)));
    }

    @PUT
    @Path("/{id}/project")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed("user")
    public UsersWorkspaceDto updateProject(@PathParam("id") String id, ProjectConfigDto update)
            throws ServerException, BadRequestException, NotFoundException, ConflictException, ForbiddenException {
        ensureUserIsWorkspaceOwner(id);
        final UsersWorkspaceImpl workspace = workspaceManager.getWorkspace(id);
        if (!workspace.getProjects().removeIf(project -> project.getName().equals(update.getName()))) {
            throw new NotFoundException("Workspace " + id + " doesn't contain project " + update.getName());
        }
        workspace.getProjects().add(new ProjectConfigImpl(update));
        return injectLinks(DtoConverter.asDto(workspaceManager.updateWorkspace(id, workspace)));
    }

    @DELETE
    @Path("/{id}/project/{name}")
    @RolesAllowed("user")
    public void deleteProject(@PathParam("id") String id, @PathParam("name") String projectName)
            throws ServerException, BadRequestException, NotFoundException, ConflictException, ForbiddenException {
        ensureUserIsWorkspaceOwner(id);
        final UsersWorkspaceImpl workspace = workspaceManager.getWorkspace(id);
        if (workspace.getProjects().removeIf(project -> project.getName().equals(projectName))) {
            workspaceManager.updateWorkspace(id, workspace);
        }
    }

    /**
     * Checks that principal from current {@link EnvironmentContext#getUser() context} is in 'workspace/owner' role
     * if he is not throws {@link ForbiddenException}.
     *
     * <p>{@link SecurityContext#isUserInRole(String)} is not the case,
     * as it works only for 'user', 'tmp-user', 'system/admin', 'system/manager.
     */
    private void ensureUserIsWorkspaceOwner(String workspaceId) throws ServerException, BadRequestException, ForbiddenException {
        final String userId = getCurrentUserId();
        final List<UsersWorkspaceImpl> workspaces = workspaceManager.getWorkspaces(userId);
        if (workspaces.stream().noneMatch(workspace -> workspace.getId().equals(workspaceId))) {
            throw new ForbiddenException("User '" + userId + "' doesn't have access to '" + workspaceId + "' workspace");
        }
    }

    private UsersWorkspaceDto injectLinks(UsersWorkspaceDto workspace) {
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        final List<Link> links = new ArrayList<>(5);
        links.add(createLink("POST",
                             uriBuilder.clone()
                                       .path(getClass(), "startById")
                                       .build(workspace.getId())
                                       .toString(),
                             APPLICATION_JSON,
                             LINK_REL_START_WORKSPACE));
        links.add(createLink("DELETE",
                             uriBuilder.clone()
                                       .path(getClass(), "delete")
                                       .build(workspace.getId())
                                       .toString(),
                             APPLICATION_JSON,
                             LINK_REL_REMOVE_WORKSPACE));
        links.add(createLink("GET",
                             uriBuilder.clone()
                                       .path(getClass(), "getWorkspaces")
                                       .build()
                                       .toString(),
                             APPLICATION_JSON,
                             GET_ALL_USER_WORKSPACES));
        links.add(createLink("GET",
                             uriBuilder.clone()
                                       .path(getClass(), "getById")
                                       .build(workspace.getId())
                                       .toString(),
                             APPLICATION_JSON,
                             "self link"));
        if (workspace.getStatus() == RUNNING) {
            links.add(createLink("GET",
                                 uriBuilder.clone()
                                           .path(getClass(), "getRuntimeWorkspaceById")
                                           .build(workspace.getId())
                                           .toString(),
                                 APPLICATION_JSON,
                                 LINK_REL_GET_RUNTIMEWORKSPACE));
        }
        return workspace.withLinks(links);
    }

    private RuntimeWorkspaceDto asDto(RuntimeWorkspace workspace) {
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();

        final List<Link> links = new ArrayList<>();
        if (workspace.getStatus() == RUNNING) {
            links.add(createLink("DELETE",
                                 uriBuilder.clone()
                                           .path(getClass(), "stop")
                                           .build(workspace.getId())
                                           .toString(),
                                 STOP_WORKSPACE));
        }
        links.add(createLink("GET",
                             uriBuilder.clone()
                                       .path(getClass(), "getById")
                                       .build(workspace.getId())
                                       .toString(),
                             null,
                             APPLICATION_JSON,
                             GET_USERS_WORKSPACE));
        links.add(createLink("GET",
                             uriBuilder.clone()
                                       .path(getClass(), "getWorkspaces")
                                       .build()
                                       .toString(),
                             APPLICATION_JSON,
                             GET_ALL_USER_WORKSPACES));
        links.add(createLink("GET",
                             uriBuilder.clone()
                                       .path(getClass(), "getRuntimeWorkspaceById")
                                       .build(workspace.getId())
                                       .toString(),
                             APPLICATION_JSON,
                             "self link"));
        final List<MachineDto> machines = workspace.getMachines()
                                                   .stream()
                                                   .map(this::asDto)
                                                   .collect(toList());
        final UsersWorkspaceDto usersWorkspace = DtoConverter.asDto(workspace);
        return newDto(RuntimeWorkspaceDto.class).withId(workspace.getId())
                                                .withName(workspace.getName())
                                                .withStatus(workspace.getStatus())
                                                .withOwner(workspace.getOwner())
                                                .withActiveEnvName(workspace.getActiveEnvName())
                                                .withDefaultEnvName(workspace.getDefaultEnvName())
                                                .withCommands(usersWorkspace.getCommands())
                                                .withProjects(usersWorkspace.getProjects())
                                                .withEnvironments(usersWorkspace.getEnvironments())
                                                .withAttributes(workspace.getAttributes())
                                                .withActiveEnvName(workspace.getActiveEnvName())
                                                .withDevMachine(asDto(workspace.getDevMachine()))
                                                .withRootFolder(workspace.getRootFolder())
                                                .withMachines(machines)
                                                .withLinks(links);
    }

    private MachineDto asDto(Machine machine) {
        if (machine == null) {
            return null;
        }
        final Link machineLink = createLink("GET",
                                            getServiceContext().getBaseUriBuilder()
                                                               .path("/machine/{id}")
                                                               .build(machine.getId())
                                                               .toString(),
                                            APPLICATION_JSON,
                                            "get machine");

        Map<String, ServerDto> servers = machine.getServers()
                                                .values()
                                                .stream()
                                                .collect(toMap(Server::getUrl, this::asDto));
        return newDto(MachineDto.class).withId(machine.getId())
                                       .withName(machine.getName())
                                       .withDev(machine.isDev())
                                       .withType(machine.getType())
                                       .withOutputChannel(machine.getOutputChannel())
                                       .withProperties(machine.getProperties())
                                       .withSource(asDto(machine.getSource()))
                                       .withServers(servers)
                                       .withMetadata(asDto(machine.getMetadata()))
                                       .withLinks(singletonList(machineLink));
    }

    private MachineSourceDto asDto(MachineSource source) {
        return newDto(MachineSourceDto.class).withType(source.getType()).withLocation(source.getLocation());
    }

    private ServerDto asDto(Server machine) {
        return newDto(ServerDto.class).withUrl(machine.getUrl())
                                      .withRef(machine.getRef())
                                      .withAddress(machine.getAddress());
    }

    private MachineMetadataDto asDto(MachineMetadata metadata) {
        return newDto(MachineMetadataDto.class).withEnvVariables(metadata.getEnvVariables());
    }

    private static String getCurrentUserId() {
        return EnvironmentContext.getCurrent().getUser().getId();
    }
}
