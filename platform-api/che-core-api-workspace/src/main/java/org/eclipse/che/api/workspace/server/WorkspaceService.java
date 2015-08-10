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

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
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
import org.eclipse.che.api.workspace.server.model.impl.CommandImpl;
import org.eclipse.che.api.workspace.server.model.impl.EnvironmentImpl;
import org.eclipse.che.api.workspace.server.model.impl.MachineConfigImpl;
import org.eclipse.che.api.workspace.server.model.impl.MachineSourceImpl;
import org.eclipse.che.api.workspace.server.model.impl.ProjectConfigImpl;
import org.eclipse.che.api.workspace.server.model.impl.SourceStorageImpl;
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMapWithExpectedSize;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.che.api.workspace.server.Constants.LINK_REL_CREATE_WORKSPACE;
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
        return asDto(workspaceManager.createWorkspace(newWorkspace, accountId));
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
        return asDto(workspaceManager.getWorkspace(id));
    }

    @GET
    @Produces(APPLICATION_JSON)
    public UsersWorkspaceDto getByName(@QueryParam("name") String name) throws ServerException, BadRequestException, NotFoundException {
        return asDto(workspaceManager.getWorkspace(name, securityContext.getUserPrincipal().getName()));
    }

    @GET
    @Path("/runtime/{id}")
    @Produces(APPLICATION_JSON)
    public RuntimeWorkspaceDto getRuntimeWorkspaceById(@PathParam("id") String id)
            throws ServerException, BadRequestException, NotFoundException {
        return asDto(workspaceManager.getRuntimeWorkspace(id));
    }

    @GET
    @Path("/runtime")
    @Produces(APPLICATION_JSON)
    public RuntimeWorkspace getRuntimeWorkspaceByName(@QueryParam("name") String name) {
        return asDto(workspaceManager.getRuntimeWorkspace(name, securityContext.getUserPrincipal().getName()));
    }

    @GET
    @Path("/list")
    @Produces(APPLICATION_JSON)
    public List<UsersWorkspaceDto> getList(@DefaultValue("0") @QueryParam("skipCount") Integer skipCount,
                                           @DefaultValue("30") @QueryParam("maxItems") Integer maxItems)
            throws ServerException, BadRequestException {
        //TODO add maxItems & skipCount to manager
        return FluentIterable.from(workspaceManager.getWorkspaces(securityContext.getUserPrincipal().getName()))
                             .transform(new Function<UsersWorkspace, UsersWorkspaceDto>() {
                                 @Override
                                 public UsersWorkspaceDto apply(UsersWorkspace src) {
                                     return asDto(src);
                                 }
                             })
                             .toList();
    }

    @PUT
    @Path("/{id}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public UsersWorkspaceDto update(@PathParam("id") String id, WorkspaceConfig workspaceCfg)
            throws BadRequestException, ServerException, ForbiddenException, NotFoundException, ConflictException {
        return asDto(workspaceManager.updateWorkspace(id, workspaceCfg));
    }

    @DELETE
    @Path("/{id}")
    public void delete(@PathParam("id") String id) throws BadRequestException, ServerException, NotFoundException, ConflictException {
        workspaceManager.removeWorkspace(id);
    }

    @POST
    @Path("/start/{id}")
    @Produces(APPLICATION_JSON)
    public UsersWorkspaceDto startById(@PathParam("id") String workspaceId, @QueryParam("environment") String envName)
            throws ServerException, BadRequestException, NotFoundException {
        return asDto(workspaceManager.startWorkspaceById(workspaceId, envName));
    }

    @POST
    @Path("/start")
    @Produces(APPLICATION_JSON)
    public UsersWorkspaceDto startByName(@QueryParam("name") String name, @QueryParam("environment") String envName)
            throws ServerException, BadRequestException, NotFoundException {
        return asDto(workspaceManager.startWorkspaceByName(name, envName, securityContext.getUserPrincipal().getName()));
    }

    @POST
    @Path("/start-temp")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public UsersWorkspaceDto startTemporary(WorkspaceConfigDto cfg, @QueryParam("account") String accountId)
            throws BadRequestException, ForbiddenException, NotFoundException, ServerException {
        return asDto(workspaceManager.startTemporaryWorkspace(cfg, accountId));
    }

    @POST
    @Path("/stop/{id}")
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
        workspace.getCommands().add(newCommand);
        return asDto(workspaceManager.updateWorkspace(workspace.getId(), workspace));
    }

    @PUT
    @Path("/{id}/command")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public UsersWorkspaceDto updateCommand(@PathParam("id") String id, CommandDto update)
            throws ServerException, BadRequestException, NotFoundException, ConflictException, ForbiddenException {
        final UsersWorkspaceImpl workspace = workspaceManager.getWorkspace(id);
        boolean found = false;
        for (Iterator<Command> cmdIt = workspace.getCommands().iterator(); cmdIt.hasNext() && !found; ) {
            if (cmdIt.next().getName().equals(update.getName())) {
                cmdIt.remove();
                found = true;
            }
        }
        if (!found) {
            throw new NotFoundException("Workspace " + id + " doesn't contain command " + update.getName());
        }
        workspace.getCommands().add(asImpl(update));
        return asDto(workspaceManager.updateWorkspace(workspace.getId(), workspace));
    }

    @DELETE
    @Path("/{id}/command/{name}")
    public void deleteCommand(@PathParam("id") String id, @PathParam("name") String commandName)
            throws ServerException, BadRequestException, NotFoundException, ConflictException, ForbiddenException {
        final UsersWorkspaceImpl workspace = workspaceManager.getWorkspace(id);
        for (Iterator<Command> cmdIt = workspace.getCommands().iterator(); cmdIt.hasNext(); ) {
            if (cmdIt.next().getName().equals(commandName)) {
                cmdIt.remove();
                workspaceManager.updateWorkspace(id, workspace);
                break;
            }
        }
    }

    @POST
    @Path("/{id}/environment")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public UsersWorkspaceDto addEnvironment(@PathParam("id") String id, EnvironmentDto newEnvironment)
            throws ServerException, BadRequestException, NotFoundException, ConflictException, ForbiddenException {
        final UsersWorkspaceImpl workspace = workspaceManager.getWorkspace(id);
        workspace.getEnvironments().put(newEnvironment.getName(), newEnvironment);
        return asDto(workspaceManager.updateWorkspace(id, workspace));
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
        workspace.getEnvironments().put(update.getName(), update);
        return asDto(workspaceManager.updateWorkspace(id, workspace));
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
        workspace.getProjects().add(newProject);
        return asDto(workspaceManager.updateWorkspace(id, workspace));
    }

    @PUT
    @Path("/{id}/project")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public UsersWorkspaceDto updateEnvironment(@PathParam("id") String id, ProjectConfigDto update)
            throws ServerException, BadRequestException, NotFoundException, ConflictException, ForbiddenException {
        final UsersWorkspaceImpl workspace = workspaceManager.getWorkspace(id);
        boolean found = false;
        for (Iterator<ProjectConfig> projIt = workspace.getProjects().iterator(); projIt.hasNext() && !found; ) {
            if (projIt.next().getName().equals(update.getName())) {
                projIt.remove();
                found = true;
            }
        }
        if (!found) {
            throw new NotFoundException("Workspace " + id + " doesn't contain project " + update.getName());
        }
        workspace.getProjects().add(update);
        return asDto(workspaceManager.updateWorkspace(id, workspace));
    }

    @DELETE
    @Path("/{id}/project/{name}")
    public void deleteProject(@PathParam("id") String id, @PathParam("name") String projectName)
            throws ServerException, BadRequestException, NotFoundException, ConflictException, ForbiddenException {
        final UsersWorkspaceImpl workspace = workspaceManager.getWorkspace(id);
        for (Iterator<ProjectConfig> projIt = workspace.getProjects().iterator(); projIt.hasNext(); ) {
            if (projIt.next().getName().equals(projectName)) {
                projIt.remove();
                workspaceManager.updateWorkspace(id, workspace);
                break;
            }
        }
    }

    //TODO add links
    private UsersWorkspaceDto asDto(UsersWorkspace workspace) {
        final List<CommandDto> commands = new ArrayList<>(workspace.getCommands().size());
        for (Command command : workspace.getCommands()) {
            commands.add(asDto(command));
        }
        final List<ProjectConfigDto> projects = new ArrayList<>(workspace.getProjects().size());
        for (ProjectConfig project : workspace.getProjects()) {
            projects.add(asDto(project));
        }
        final Map<String, EnvironmentDto> environments = newHashMapWithExpectedSize(workspace.getEnvironments().size());
        for (Map.Entry<String, ? extends Environment> entry : workspace.getEnvironments().entrySet()) {
            environments.put(entry.getKey(), asDto(entry.getValue()));
        }
        return newDto(UsersWorkspaceDto.class).withId(workspace.getId())
                                              .withName(workspace.getName())
                                              .withOwner(workspace.getOwner())
                                              .withDefaultEnvName(workspace.getDefaultEnvName())
                                              .withCommands(commands)
                                              .withProjects(projects)
                                              .withEnvironments(environments)
                                              .withAttributes(workspace.getAttributes());
    }

    private RuntimeWorkspaceDto asDto(RuntimeWorkspace workspace) {
        final List<MachineDto> machines = new ArrayList<>(workspace.getMachines().size());
        for (Machine machine : workspace.getMachines()) {
            machines.add(asDto(machine));
        }
        final UsersWorkspaceDto usersWorkspace = asDto(workspace);
        return newDto(RuntimeWorkspaceDto.class).withId(workspace.getId())
                                                .withName(workspace.getName())
                                                .withOwner(workspace.getOwner())
                                                .withDefaultEnvName(workspace.getDefaultEnvName())
                                                .withCommands(usersWorkspace.getCommands())
                                                .withProjects(usersWorkspace.getProjects())
                                                .withEnvironments(usersWorkspace.getEnvironments())
                                                .withAttributes(workspace.getAttributes())
                                                .withActiveEnvName(workspace.getActiveEnvName())
                                                .withDevMachine(asDto(workspace.getDevMachine()))
                                                .withRootFolder(workspace.getRootFolder())
                                                .withMachines(machines);
    }

    private MachineDto asDto(Machine machine) {
        final Map<String, ServerDto> servers = Maps.newHashMapWithExpectedSize(machine.getServers().size());
        for (Map.Entry<String, ? extends Server> entry : machine.getServers().entrySet()) {
            servers.put(entry.getKey(), asDto(entry.getValue()));
        }
        return newDto(MachineDto.class).withId(machine.getId())
                                       .withName(machine.getName())
                                       .withDev(machine.isDev())
                                       .withType(machine.getType())
                                       .withOutputChannel(machine.getOutputChannel())
                                       .withProperties(machine.getProperties())
                                       .withSource(asDto(machine.getSource()))
                                       .withServers(servers);
    }

    private MachineSourceDto asDto(MachineSource source) {
        return newDto(MachineSourceDto.class).withType(source.getType()).withLocation(source.getLocation());
    }

    private ServerDto asDto(Server machine) {
        return newDto(ServerDto.class).withUrl(machine.getUrl())
                                      .withRef(machine.getRef())
                                      .withAddress(machine.getAddress());
    }

    private UsersWorkspaceImpl asImpl(UsersWorkspaceDto workspaceDto) {
        final List<CommandImpl> commands = new ArrayList<>(workspaceDto.getCommands().size());
        for (CommandDto commandDto : workspaceDto.getCommands()) {
            commands.add(asImpl(commandDto));
        }
        final List<ProjectConfigImpl> projects = new ArrayList<>(workspaceDto.getProjects().size());
        for (ProjectConfigDto projectCfgDto : workspaceDto.getProjects()) {
            projects.add(asImpl(projectCfgDto));
        }
        final Map<String, EnvironmentImpl> environments = newHashMapWithExpectedSize(workspaceDto.getEnvironments().size());
        for (Map.Entry<String, EnvironmentDto> entry : workspaceDto.getEnvironments().entrySet()) {
            environments.put(entry.getKey(), asImpl(entry.getValue()));
        }
        return new UsersWorkspaceImpl(workspaceDto.getId(),
                                      workspaceDto.getName(),
                                      workspaceDto.getOwner(),
                                      workspaceDto.getAttributes(),
                                      commands,
                                      projects,
                                      environments,
                                      workspaceDto.getDefaultEnvName(),
                                      workspaceDto.getDescription());
    }

    private CommandDto asDto(Command command) {
        return newDto(CommandDto.class).withName(command.getName())
                                       .withCommandLine(command.getCommandLine())
                                       .withType(command.getType())
                                       .withVisibility(command.getVisibility())
                                       .withWorkingDir(command.getWorkingDir());
    }

    private CommandImpl asImpl(CommandDto commandDto) {
        return new CommandImpl(commandDto.getName(),
                               commandDto.getCommandLine(),
                               commandDto.getVisibility(),
                               commandDto.getType(),
                               commandDto.getWorkingDir());
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

    private ProjectConfigImpl asImpl(ProjectConfigDto projectCfgDto) {
        return new ProjectConfigImpl().setName(projectCfgDto.getName())
                                      .setDescription(projectCfgDto.getDescription())
                                      .setPath(projectCfgDto.getPath())
                                      .setType(projectCfgDto.getType())
                                      .setAttributes(projectCfgDto.getAttributes())
                                      .setMixinTypes(projectCfgDto.getMixinTypes())
                                      .setSourceStorage(new SourceStorageImpl(projectCfgDto.getSourceStorage().getType(),
                                                                              projectCfgDto.getSourceStorage().getLocation(),
                                                                              projectCfgDto.getSourceStorage().getParameters()));
    }

    //TODO add recipe
    private EnvironmentDto asDto(Environment environment) {
        final List<MachineConfigDto> machineConfigs = new ArrayList<>(environment.getMachineConfigs().size());
        for (MachineConfig machineCfg : environment.getMachineConfigs()) {
            machineConfigs.add(asDto(machineCfg));
        }
        return newDto(EnvironmentDto.class).withName(environment.getName()).withMachineConfigs(machineConfigs);
    }

    //TODO add recipe
    private EnvironmentImpl asImpl(EnvironmentDto envDto) {
        final List<MachineConfigImpl> machineConfigs = new ArrayList<>(envDto.getMachineConfigs().size());
        for (MachineConfigDto machineCfgDto : envDto.getMachineConfigs()) {
            machineConfigs.add(asImpl(machineCfgDto));
        }
        return new EnvironmentImpl(envDto.getName(), envDto.getRecipe(), machineConfigs);
    }

    private MachineConfigDto asDto(MachineConfig config) {
        return newDto(MachineConfigDto.class).withName(config.getName())
                                             .withType(config.getType())
                                             .withDev(config.isDev())
                                             .withSource(asDto(config.getSource()));
    }

    private MachineConfigImpl asImpl(MachineConfigDto machineCfgDto) {
        return new MachineConfigImpl().setName(machineCfgDto.getName())
                                      .setType(machineCfgDto.getType())
                                      .setIsDev(machineCfgDto.isDev())
                                      .setSource(new MachineSourceImpl(machineCfgDto.getSource().getType(),
                                                                       machineCfgDto.getSource().getLocation()));
    }
}