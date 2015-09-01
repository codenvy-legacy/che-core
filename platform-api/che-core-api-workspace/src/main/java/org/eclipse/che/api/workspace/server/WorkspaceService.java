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
import org.eclipse.che.api.core.model.machine.Command;
import org.eclipse.che.api.core.model.machine.MachineConfig;
import org.eclipse.che.api.core.model.machine.MachineSource;
import org.eclipse.che.api.core.model.machine.Server;
import org.eclipse.che.api.core.model.workspace.Environment;
import org.eclipse.che.api.core.model.workspace.Machine;
import org.eclipse.che.api.core.model.workspace.ProjectConfig;
import org.eclipse.che.api.core.model.workspace.RuntimeWorkspace;
import org.eclipse.che.api.core.model.workspace.UsersWorkspace;
import org.eclipse.che.api.core.model.workspace.WorkspaceConfig;
import org.eclipse.che.api.core.rest.Service;
import org.eclipse.che.api.core.rest.annotations.GenerateLink;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.workspace.server.model.impl.CommandImpl;
import org.eclipse.che.api.workspace.server.model.impl.EnvironmentImpl;
import org.eclipse.che.api.workspace.server.model.impl.ProjectConfigImpl;
import org.eclipse.che.api.workspace.server.model.impl.UsersWorkspaceImpl;
import org.eclipse.che.api.workspace.shared.dto.CommandDto;
import org.eclipse.che.api.workspace.shared.dto.EnvironmentDto;
import org.eclipse.che.api.workspace.shared.dto.MachineConfigDto;
import org.eclipse.che.api.workspace.shared.dto.MachineDto;
import org.eclipse.che.api.workspace.shared.dto.MachineSourceDto;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.api.workspace.shared.dto.RuntimeWorkspaceDto;
import org.eclipse.che.api.workspace.shared.dto.ServerDto;
import org.eclipse.che.api.workspace.shared.dto.SourceStorageDto;
import org.eclipse.che.api.workspace.shared.dto.UsersWorkspaceDto;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceConfigDto;

import javax.inject.Inject;
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

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.che.api.core.model.workspace.WorkspaceStatus.RUNNING;
import static org.eclipse.che.api.core.util.LinksHelper.createLink;
import static org.eclipse.che.api.workspace.server.Constants.GET_ALL_USER_WORKSPACES;
import static org.eclipse.che.api.workspace.server.Constants.GET_RUNTIME_WORKSPACE;
import static org.eclipse.che.api.workspace.server.Constants.GET_USERS_WORKSPACE;
import static org.eclipse.che.api.workspace.server.Constants.LINK_REL_CREATE_WORKSPACE;
import static org.eclipse.che.api.workspace.server.Constants.LINK_REL_REMOVE_WORKSPACE;
import static org.eclipse.che.api.workspace.server.Constants.START_WORKSPACE;
import static org.eclipse.che.api.workspace.server.Constants.STOP_WORKSPACE;
import static org.eclipse.che.dto.server.DtoFactory.newDto;

//TODO add permissions check

/**
 * Workspace API
 *
 * @author Eugene Voevodin
 */
@Api(value = "/workspace", description = "Workspace service")
@Path("/workspace")
public class WorkspaceService extends Service {

    private final WorkspaceManager workspaceManager;

    @Context
    private SecurityContext securityContext;

    @Inject
    public WorkspaceService(WorkspaceManager workspaceManager) {
        this.workspaceManager = workspaceManager;
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
    @GenerateLink(rel = LINK_REL_CREATE_WORKSPACE)
    public UsersWorkspaceDto create(@ApiParam(value = "new workspace", required = true) UsersWorkspaceDto newWorkspace,
                                    @ApiParam("account id") @QueryParam("account") String accountId)
            throws ConflictException, ServerException, BadRequestException, ForbiddenException, NotFoundException {
        if (securityContext.isUserInRole("user")) {
            newWorkspace.withOwner(securityContext.getUserPrincipal().getName());
        }
        if (newWorkspace.getOwner() == null) {
            throw new BadRequestException("New workspace owner required");
        }
        return asUsersWorkspaceDto(workspaceManager.createWorkspace(newWorkspace, accountId));
    }

    @DELETE
    @Path("/{id}/config")
    public void delete(@PathParam("id") String id) throws BadRequestException, ServerException, NotFoundException, ConflictException {
        workspaceManager.removeWorkspace(id);
    }

    @PUT
    @Path("/{id}/config")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public UsersWorkspaceDto update(@PathParam("id") String id, WorkspaceConfig workspaceCfg)
            throws BadRequestException, ServerException, ForbiddenException, NotFoundException, ConflictException {
        return asUsersWorkspaceDto(workspaceManager.updateWorkspace(id, workspaceCfg));
    }

    @ApiOperation("Get workspace by id")
    @ApiResponses({@ApiResponse(code = 200, message = "Response contains requested workspace entity"),
                   @ApiResponse(code = 404, message = "Workspace with specified id does not exist"),
                   @ApiResponse(code = 403, message = "User does not have access to requested workspace"),
                   @ApiResponse(code = 500, message = "Internal server error was occurred during workspace getting")})
    @GET
    @Path("/{id}")
    @Produces(APPLICATION_JSON)
    public UsersWorkspaceDto getById(@ApiParam("Workspace ID") @PathParam("id") String id)
            throws NotFoundException, ServerException, ForbiddenException, BadRequestException {
        return asUsersWorkspaceDto(workspaceManager.getWorkspace(id));
    }

    @GET
    @Path("/name/{name}")
    @Produces(APPLICATION_JSON)
    public UsersWorkspaceDto getByName(@PathParam("name") String name) throws ServerException, BadRequestException, NotFoundException {
        return asUsersWorkspaceDto(workspaceManager.getWorkspace(name, securityContext.getUserPrincipal().getName()));
    }

    @GET
    @Produces(APPLICATION_JSON)
    public List<UsersWorkspaceDto> getWorkspaces(@DefaultValue("0") @QueryParam("skipCount") Integer skipCount,
                                                 @DefaultValue("30") @QueryParam("maxItems") Integer maxItems)
            throws ServerException, BadRequestException {
        //TODO add maxItems & skipCount to manager
        return workspaceManager.getWorkspaces(securityContext.getUserPrincipal().getName())
                               .stream()
                               .map(this::asUsersWorkspaceDto)
                               .collect(toList());
    }

    //TODO
    @GET
    @Path("/runtime")
    @Produces(APPLICATION_JSON)
    public List<RuntimeWorkspaceDto> getRuntimeWorkspaces(@DefaultValue("0") @QueryParam("skipCount") Integer skipCount,
                                                          @DefaultValue("30") @QueryParam("maxItems") Integer maxItems) {
        return emptyList();
    }

    @GET
    @Path("/{id}/runtime")
    @Produces(APPLICATION_JSON)
    public RuntimeWorkspaceDto getRuntimeWorkspaceById(@PathParam("id") String id)
            throws ServerException, BadRequestException, NotFoundException {
        return asDto(workspaceManager.getRuntimeWorkspace(id));
    }

    @GET
    @Path("/name/{name}/runtime")
    @Produces(APPLICATION_JSON)
    public RuntimeWorkspace getRuntimeWorkspaceByName(@PathParam("name") String name) {
        return asDto(workspaceManager.getRuntimeWorkspace(name, securityContext.getUserPrincipal().getName()));
    }

    @POST
    @Path("/runtime")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public UsersWorkspaceDto startTemporary(WorkspaceConfigDto cfg, @QueryParam("account") String accountId)
            throws BadRequestException, ForbiddenException, NotFoundException, ServerException {
        return asUsersWorkspaceDto(workspaceManager.startTemporaryWorkspace(cfg, accountId));
    }

    @POST
    @Path("/{id}/runtime")
    @Produces(APPLICATION_JSON)
    public UsersWorkspaceDto startById(@PathParam("id") String workspaceId, @QueryParam("environment") String envName)
            throws ServerException, BadRequestException, NotFoundException {
        return asUsersWorkspaceDto(workspaceManager.startWorkspaceById(workspaceId, envName));
    }

    @POST
    @Path("/name/{name}/runtime")
    @Produces(APPLICATION_JSON)
    public UsersWorkspaceDto startByName(@QueryParam("name") String name, @QueryParam("environment") String envName)
            throws ServerException, BadRequestException, NotFoundException {
        return asUsersWorkspaceDto(workspaceManager.startWorkspaceByName(name, envName, securityContext.getUserPrincipal().getName()));
    }

    @DELETE
    @Path("/{id}/runtime")
    public void stop(@PathParam("id") String id)
            throws BadRequestException, ForbiddenException, NotFoundException, ServerException {
        workspaceManager.stopWorkspace(id);
    }

    @POST
    @Path("/{id}/command")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public UsersWorkspaceDto addCommand(@PathParam("id") String id, CommandDto newCommand)
            throws ServerException, BadRequestException, NotFoundException, ConflictException, ForbiddenException {
        final UsersWorkspaceImpl workspace = workspaceManager.getWorkspace(id);
        workspace.getCommands().add(new CommandImpl(newCommand));
        return asUsersWorkspaceDto(workspaceManager.updateWorkspace(workspace.getId(), workspace));
    }

    @PUT
    @Path("/{id}/command")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public UsersWorkspaceDto updateCommand(@PathParam("id") String id, CommandDto update)
            throws ServerException, BadRequestException, NotFoundException, ConflictException, ForbiddenException {
        final UsersWorkspaceImpl workspace = workspaceManager.getWorkspace(id);
        if (!workspace.getCommands().removeIf(cmd -> cmd.getName().equals(update.getName()))) {
            throw new NotFoundException("Workspace " + id + " doesn't contain command " + update.getName());
        }
        workspace.getCommands().add(new CommandImpl(update));
        return asUsersWorkspaceDto(workspaceManager.updateWorkspace(workspace.getId(), workspace));
    }

    @DELETE
    @Path("/{id}/command/{name}")
    public void deleteCommand(@PathParam("id") String id, @PathParam("name") String commandName)
            throws ServerException, BadRequestException, NotFoundException, ConflictException, ForbiddenException {
        final UsersWorkspaceImpl workspace = workspaceManager.getWorkspace(id);
        if (workspace.getCommands().removeIf(command -> command.getName().equals(commandName))) {
            workspaceManager.updateWorkspace(id, workspace);
        }
    }

    @POST
    @Path("/{id}/environment")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public UsersWorkspaceDto addEnvironment(@PathParam("id") String id, EnvironmentDto newEnvironment)
            throws ServerException, BadRequestException, NotFoundException, ConflictException, ForbiddenException {
        final UsersWorkspaceImpl workspace = workspaceManager.getWorkspace(id);
        workspace.getEnvironments().put(newEnvironment.getName(), new EnvironmentImpl(newEnvironment));
        return asUsersWorkspaceDto(workspaceManager.updateWorkspace(id, workspace));
    }

    @PUT
    @Path("/{id}/environment")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public UsersWorkspaceDto updateEnvironment(@PathParam("id") String id, EnvironmentDto update)
            throws ServerException, BadRequestException, NotFoundException, ConflictException, ForbiddenException {
        final UsersWorkspaceImpl workspace = workspaceManager.getWorkspace(id);
        if (!workspace.getEnvironments().containsKey(update.getName())) {
            throw new NotFoundException("Workspace " + id + " doesn't contain environment " + update.getName());
        }
        workspace.getEnvironments().put(update.getName(), new EnvironmentImpl(update));
        return asUsersWorkspaceDto(workspaceManager.updateWorkspace(id, workspace));
    }

    @DELETE
    @Path("/{id}/environment/{name}")
    public void deleteEnvironment(@PathParam("id") String id, @PathParam("name") String envName)
            throws ServerException, BadRequestException, NotFoundException, ConflictException, ForbiddenException {
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
    public UsersWorkspaceDto addProject(@PathParam("id") String id, ProjectConfigDto newProject)
            throws ServerException, BadRequestException, NotFoundException, ConflictException, ForbiddenException {
        final UsersWorkspaceImpl workspace = workspaceManager.getWorkspace(id);
        workspace.getProjects().add(new ProjectConfigImpl(newProject));
        return asUsersWorkspaceDto(workspaceManager.updateWorkspace(id, workspace));
    }

    @PUT
    @Path("/{id}/project")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public UsersWorkspaceDto updateProject(@PathParam("id") String id, ProjectConfigDto update)
            throws ServerException, BadRequestException, NotFoundException, ConflictException, ForbiddenException {
        final UsersWorkspaceImpl workspace = workspaceManager.getWorkspace(id);
        if (!workspace.getProjects().removeIf(project -> project.getName().equals(update.getName()))) {
            throw new NotFoundException("Workspace " + id + " doesn't contain project " + update.getName());
        }
        workspace.getProjects().add(new ProjectConfigImpl(update));
        return asUsersWorkspaceDto(workspaceManager.updateWorkspace(id, workspace));
    }

    @DELETE
    @Path("/{id}/project/{name}")
    public void deleteProject(@PathParam("id") String id, @PathParam("name") String projectName)
            throws ServerException, BadRequestException, NotFoundException, ConflictException, ForbiddenException {
        final UsersWorkspaceImpl workspace = workspaceManager.getWorkspace(id);
        if (workspace.getProjects().removeIf(project -> project.getName().equals(projectName))) {
            workspaceManager.updateWorkspace(id, workspace);
        }
    }

    private UsersWorkspaceDto asUsersWorkspaceDto(UsersWorkspace workspace) {
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();

        final List<Link> links = new ArrayList<>();
        links.add(createLink("POST",
                             uriBuilder.clone()
                                       .path(getClass(), "startById")
                                       .build(workspace.getId())
                                       .toString(),
                             APPLICATION_JSON,
                             START_WORKSPACE));
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
                                 GET_RUNTIME_WORKSPACE));
        }

        final List<CommandDto> commands = workspace.getCommands()
                                                   .stream()
                                                   .map(this::asDto)
                                                   .collect(toList());
        final List<ProjectConfigDto> projects = workspace.getProjects()
                                                         .stream()
                                                         .map(this::asDto)
                                                         .collect(toList());
        final Map<String, EnvironmentDto> environments = workspace.getEnvironments()
                                                                  .values()
                                                                  .stream()
                                                                  .collect(toMap(Environment::getName, this::asDto));

        return newDto(UsersWorkspaceDto.class).withId(workspace.getId())
                                              .withStatus(workspace.getStatus())
                                              .withName(workspace.getName())
                                              .withOwner(workspace.getOwner())
                                              .withDefaultEnvName(workspace.getDefaultEnvName())
                                              .withCommands(commands)
                                              .withProjects(projects)
                                              .withEnvironments(environments)
                                              .withAttributes(workspace.getAttributes())
                                              .withLinks(links);
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
        final UsersWorkspaceDto usersWorkspace = asUsersWorkspaceDto(workspace);
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

    private CommandDto asDto(Command command) {
        return newDto(CommandDto.class).withName(command.getName())
                                       .withCommandLine(command.getCommandLine())
                                       .withType(command.getType())
                                       .withVisibility(command.getVisibility())
                                       .withWorkingDir(command.getWorkingDir());
    }

    private ProjectConfigDto asDto(ProjectConfig projectCfg) {
        return newDto(ProjectConfigDto.class)
                .withName(projectCfg.getName())
                .withDescription(projectCfg.getDescription())
                .withPath(projectCfg.getPath())
                .withType(projectCfg.getType())
                .withAttributes(projectCfg.getAttributes())
                .withMixinTypes(projectCfg.getMixinTypes())
                .withSourceStorage(newDto(SourceStorageDto.class)
                                           .withLocation(projectCfg.getSourceStorage().getLocation())
                                           .withType(projectCfg.getSourceStorage().getType())
                                           .withParameters(projectCfg.getSourceStorage().getParameters()));
    }

    //TODO add recipe
    private EnvironmentDto asDto(Environment environment) {
        final List<MachineConfigDto> machineConfigs = environment.getMachineConfigs()
                                                                 .stream()
                                                                 .map(this::asDto)
                                                                 .collect(toList());
        return newDto(EnvironmentDto.class).withName(environment.getName()).withMachineConfigs(machineConfigs);
    }

    private MachineConfigDto asDto(MachineConfig config) {
        return newDto(MachineConfigDto.class).withName(config.getName())
                                             .withType(config.getType())
                                             .withDev(config.isDev())
                                             .withOutputChannel(config.getOutputChannel())
                                             .withStatusChannel(config.getStatusChannel())
                                             .withSource(asDto(config.getSource()));
    }
}
