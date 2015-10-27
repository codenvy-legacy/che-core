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

import com.google.common.collect.Maps;
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
import org.eclipse.che.api.core.model.workspace.RuntimeWorkspace;
import org.eclipse.che.api.core.model.workspace.UsersWorkspace;
import org.eclipse.che.api.core.rest.Service;
import org.eclipse.che.api.core.rest.annotations.GenerateLink;
import org.eclipse.che.api.core.rest.permission.PermissionManager;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.machine.server.MachineManager;
import org.eclipse.che.api.machine.server.model.impl.CommandImpl;
import org.eclipse.che.api.machine.server.model.impl.MachineStateImpl;
import org.eclipse.che.api.machine.shared.dto.CommandDto;
import org.eclipse.che.api.machine.shared.dto.MachineConfigDto;
import org.eclipse.che.api.machine.shared.dto.MachineStateDto;
import org.eclipse.che.api.workspace.server.model.impl.EnvironmentStateImpl;
import org.eclipse.che.api.workspace.server.model.impl.ProjectConfigImpl;
import org.eclipse.che.api.workspace.server.model.impl.RuntimeWorkspaceImpl;
import org.eclipse.che.api.workspace.server.model.impl.UsersWorkspaceImpl;
import org.eclipse.che.api.workspace.shared.dto.EnvironmentDto;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.api.workspace.shared.dto.RuntimeWorkspaceDto;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.che.api.core.model.workspace.WorkspaceStatus.RUNNING;
import static org.eclipse.che.api.core.util.LinksHelper.createLink;
import static org.eclipse.che.api.workspace.server.Constants.GET_ALL_USER_WORKSPACES;
import static org.eclipse.che.api.workspace.server.Constants.LINK_REL_CREATE_WORKSPACE;
import static org.eclipse.che.api.workspace.server.Constants.LINK_REL_GET_RUNTIMEWORKSPACE;
import static org.eclipse.che.api.workspace.server.Constants.LINK_REL_REMOVE_WORKSPACE;
import static org.eclipse.che.api.workspace.server.Constants.LINK_REL_START_WORKSPACE;
import static org.eclipse.che.api.workspace.server.Constants.START_WORKSPACE;

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
    private final MachineManager    machineManager;

    @Context
    private SecurityContext securityContext;

    @Inject
    public WorkspaceService(WorkspaceManager workspaceManager,
                            MachineManager machineManager,
                            @Named("service.workspace.permission_manager") PermissionManager permissionManager) {
        this.workspaceManager = workspaceManager;
        this.machineManager = machineManager;
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
    public UsersWorkspaceDto create(@ApiParam(value = "new workspace", required = true) WorkspaceConfigDto newWorkspace,
                                    @ApiParam("owner id") @QueryParam("owner") String owner,
                                    @ApiParam("account id") @QueryParam("account") String accountId)
            throws ConflictException, ServerException, BadRequestException, ForbiddenException, NotFoundException {
        requiredNotNull(newWorkspace, "New workspace description");
        if (securityContext.isUserInRole("user")) {
            owner = getCurrentUserId();
        }
        requiredNotNull(owner, "New workspace owner");
        return injectLinks(DtoConverter.asDto(workspaceManager.createWorkspace(newWorkspace, owner, accountId)));
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
        requiredNotNull(workspaceCfg, "Workspace configuration");
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
        final UsersWorkspaceImpl workspace = workspaceManager.getWorkspace(id);
        ensureUserIsWorkspaceOwner(workspace);
        return injectLinks(DtoConverter.asDto(workspace));
    }

    @GET
    @Path("/name/{name}")
    @RolesAllowed("user")
    @Produces(APPLICATION_JSON)
    public UsersWorkspaceDto getByName(@PathParam("name") String name)
            throws ServerException, BadRequestException, NotFoundException, ForbiddenException {
        final UsersWorkspace workspace = workspaceManager.getWorkspace(name, getCurrentUserId());
        ensureUserIsWorkspaceOwner(workspace);
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
                               .map(workspace -> injectLinks(DtoConverter.asDto(workspace)))
                               .collect(toList());
    }

    @GET
    @Path("/{id}/runtime")
    @Produces(APPLICATION_JSON)
    @RolesAllowed("user")
    public RuntimeWorkspaceDto getRuntimeWorkspaceById(@PathParam("id") String id)
            throws ServerException, BadRequestException, NotFoundException, ForbiddenException {
        final RuntimeWorkspaceImpl runtimeWorkspace = workspaceManager.getRuntimeWorkspace(id);
        ensureUserIsWorkspaceOwner(runtimeWorkspace);
        return injectLinks(DtoConverter.asDto(runtimeWorkspace));
    }

    @GET
    @Path("/name/{name}/runtime")
    @Produces(APPLICATION_JSON)
    @RolesAllowed("user")
    public RuntimeWorkspace getRuntimeWorkspaceByName(@PathParam("name") String name) throws ServerException, BadRequestException, ForbiddenException {
        final RuntimeWorkspace workspace = workspaceManager.getRuntimeWorkspace(name, getCurrentUserId());
        ensureUserIsWorkspaceOwner(workspace);
        return injectLinks(DtoConverter.asDto(workspace));
    }

    @POST
    @Path("/runtime")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed({"user", "temp-user"})
    public UsersWorkspaceDto startTemporary(WorkspaceConfigDto cfg, @QueryParam("account") String accountId)
            throws BadRequestException, ForbiddenException, NotFoundException, ServerException, ConflictException {
        requiredNotNull(cfg, "Workspace configuration");
        permissionManager.checkPermission(START_WORKSPACE, getCurrentUserId(), "accountId", accountId);
        return injectLinks(DtoConverter.asDto(workspaceManager.startTemporaryWorkspace(cfg, accountId)));
    }

    @POST
    @Path("/{id}/runtime")
    @Produces(APPLICATION_JSON)
    @RolesAllowed("user")
    public UsersWorkspaceDto startById(@PathParam("id") String workspaceId,
                                       @QueryParam("environment") String envName,
                                       @QueryParam("accountId") String accountId)
            throws ServerException, BadRequestException, NotFoundException, ForbiddenException, ConflictException {
        ensureUserIsWorkspaceOwner(workspaceId);

        final Map<String, String> params = Maps.newHashMapWithExpectedSize(2);
        params.put("accountId", accountId);
        params.put("workspaceId", workspaceId);
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
            throws ServerException, BadRequestException, NotFoundException, ForbiddenException, ConflictException {
        final UsersWorkspace workspace = workspaceManager.getWorkspace(name, getCurrentUserId());
        ensureUserIsWorkspaceOwner(workspace);

        final Map<String, String> params = Maps.newHashMapWithExpectedSize(2);
        params.put("accountId", accountId);
        params.put("workspaceId", workspace.getId());
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
        requiredNotNull(newCommand, "Command");
        final UsersWorkspaceImpl workspace = workspaceManager.getWorkspace(id);
        ensureUserIsWorkspaceOwner(workspace);
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
        requiredNotNull(update, "Command update");
        final UsersWorkspaceImpl workspace = workspaceManager.getWorkspace(id);
        ensureUserIsWorkspaceOwner(workspace);
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
        final UsersWorkspaceImpl workspace = workspaceManager.getWorkspace(id);
        ensureUserIsWorkspaceOwner(workspace);
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
        requiredNotNull(newEnvironment, "New environment");
        final UsersWorkspaceImpl workspace = workspaceManager.getWorkspace(id);
        ensureUserIsWorkspaceOwner(workspace);
        workspace.getEnvironments().put(newEnvironment.getName(), new EnvironmentStateImpl(newEnvironment));
        return injectLinks(DtoConverter.asDto(workspaceManager.updateWorkspace(id, workspace)));
    }

    @PUT
    @Path("/{id}/environment")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed("user")
    public UsersWorkspaceDto updateEnvironment(@PathParam("id") String id, EnvironmentDto update)
            throws ServerException, BadRequestException, NotFoundException, ConflictException, ForbiddenException {
        requiredNotNull(update, "Environment description");
        final UsersWorkspaceImpl workspace = workspaceManager.getWorkspace(id);
        ensureUserIsWorkspaceOwner(workspace);
        if (!workspace.getEnvironments().containsKey(update.getName())) {
            throw new NotFoundException("Workspace " + id + " doesn't contain environment " + update.getName());
        }
        workspace.getEnvironments().put(update.getName(), new EnvironmentStateImpl(update));
        return injectLinks(DtoConverter.asDto(workspaceManager.updateWorkspace(id, workspace)));
    }

    @DELETE
    @Path("/{id}/environment/{name}")
    @RolesAllowed("user")
    public void deleteEnvironment(@PathParam("id") String id, @PathParam("name") String envName)
            throws ServerException, BadRequestException, NotFoundException, ConflictException, ForbiddenException {
        final UsersWorkspaceImpl workspace = workspaceManager.getWorkspace(id);
        ensureUserIsWorkspaceOwner(workspace);
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
        requiredNotNull(newProject, "New project config");
        final UsersWorkspaceImpl workspace = workspaceManager.getWorkspace(id);
        ensureUserIsWorkspaceOwner(workspace);
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
        requiredNotNull(update, "Project config");
        final UsersWorkspaceImpl workspace = workspaceManager.getWorkspace(id);
        ensureUserIsWorkspaceOwner(workspace);
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
        final UsersWorkspaceImpl workspace = workspaceManager.getWorkspace(id);
        ensureUserIsWorkspaceOwner(workspace);
        if (workspace.getProjects().removeIf(project -> project.getName().equals(projectName))) {
            workspaceManager.updateWorkspace(id, workspace);
        }
    }

    @POST
    @Path("/{id}/machine")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public MachineStateDto createMachine(@PathParam("id") String workspaceId, MachineConfigDto machineConfig)
            throws ForbiddenException, NotFoundException, ServerException, ConflictException, BadRequestException {
        requiredNotNull(machineConfig, "Machine configuration");
        requiredNotNull(machineConfig.getType(), "Machine type");
        requiredNotNull(machineConfig.getSource(), "Machine source");
        requiredNotNull(machineConfig.getSource().getType(), "Machine source type");
        requiredNotNull(machineConfig.getSource().getLocation(), "Machine source location");

        RuntimeWorkspaceImpl runtimeWorkspace = workspaceManager.getRuntimeWorkspace(workspaceId);

        ensureUserIsWorkspaceOwner(runtimeWorkspace);

        final MachineStateImpl machine = machineManager.createMachineAsync(machineConfig, workspaceId, runtimeWorkspace.getActiveEnvName());

        return org.eclipse.che.api.machine.server.DtoConverter.asDto(machine);
    }

    /**
     * Checks that principal from current {@link EnvironmentContext#getUser() context} is in 'workspace/owner' role
     * if he is not throws {@link ForbiddenException}.
     *
     * <p>{@link SecurityContext#isUserInRole(String)} is not the case,
     * as it works only for 'user', 'tmp-user', 'system/admin', 'system/manager.
     */
    private void ensureUserIsWorkspaceOwner(String workspaceId)
            throws ServerException, BadRequestException, ForbiddenException, NotFoundException {
        final UsersWorkspaceImpl workspace = workspaceManager.getWorkspace(workspaceId);
        ensureUserIsWorkspaceOwner(workspace);
    }

    /**
     * Checks that principal from current {@link EnvironmentContext#getUser() context} is in 'workspace/owner' role
     * if he is not throws {@link ForbiddenException}.
     *
     * <p>{@link SecurityContext#isUserInRole(String)} is not the case,
     * as it works only for 'user', 'tmp-user', 'system/admin', 'system/manager.
     */
    private void ensureUserIsWorkspaceOwner(UsersWorkspace usersWorkspace) throws ServerException, BadRequestException, ForbiddenException {
        final String userId = getCurrentUserId();
        if (!usersWorkspace.getOwner().equals(userId)) {
            throw new ForbiddenException("User '" + userId + "' doesn't have access to '" + usersWorkspace.getId() + "' workspace");
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends UsersWorkspaceDto> T injectLinks(T workspace) {
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
        if (RuntimeWorkspaceDto.class.isAssignableFrom(workspace.getClass())) {
            links.add(createLink("GET",
                                 uriBuilder.clone()
                                           .path(getClass(), "getRuntimeWorkspaceById")
                                           .build(workspace.getId())
                                           .toString(),
                                 APPLICATION_JSON,
                                 "self link"));
            RuntimeWorkspaceDto runtimeWorkspace = (RuntimeWorkspaceDto)workspace;
            runtimeWorkspace.getMachines()
                            .forEach(machineDto -> machineDto.withLinks(
                                    singletonList(createLink("GET",
                                                             getServiceContext().getBaseUriBuilder()
                                                                                .path("/machine/{id}")
                                                                                .build(machineDto.getId())
                                                                                .toString(),
                                                             APPLICATION_JSON,
                                                             "get machine"))));
        } else {
            links.add(createLink("GET",
                                 uriBuilder.clone()
                                           .path(getClass(), "getById")
                                           .build(workspace.getId())
                                           .toString(),
                                 APPLICATION_JSON,
                                 "self link"));
        }
        if (workspace.getStatus() == RUNNING) {
            links.add(createLink("GET",
                                 uriBuilder.clone()
                                           .path(getClass(), "getRuntimeWorkspaceById")
                                           .build(workspace.getId())
                                           .toString(),
                                 APPLICATION_JSON,
                                 LINK_REL_GET_RUNTIMEWORKSPACE));
            links.add(createLink("DELETE",
                                 uriBuilder.clone()
                                           .path(getClass(), "stop")
                                           .build(workspace.getId())
                                           .toString(),
                                 Constants.STOP_WORKSPACE));
        }
        return (T)workspace.withLinks(links);
    }

    private static String getCurrentUserId() {
        return EnvironmentContext.getCurrent().getUser().getId();
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
    private void requiredNotNull(Object object, String subject) throws BadRequestException {
        if (object == null) {
            throw new BadRequestException(subject + " required");
        }
    }
}
