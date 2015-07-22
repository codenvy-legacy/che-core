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
package org.eclipse.che.api.machine.server;

import com.google.common.collect.Maps;
import com.google.common.io.CharStreams;

import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.machine.server.impl.MachineImpl;
import org.eclipse.che.api.machine.server.impl.ProjectBindingImpl;
import org.eclipse.che.api.machine.server.impl.SnapshotImpl;
import org.eclipse.che.api.machine.server.spi.Instance;
import org.eclipse.che.api.machine.server.spi.InstanceProcess;
import org.eclipse.che.api.machine.shared.ProjectBinding;
import org.eclipse.che.api.machine.shared.Server;
import org.eclipse.che.api.machine.shared.dto.CommandDescriptor;
import org.eclipse.che.api.machine.shared.dto.MachineDescriptor;
import org.eclipse.che.api.machine.shared.dto.MachineStateDescriptor;
import org.eclipse.che.api.machine.shared.dto.NewSnapshotDescriptor;
import org.eclipse.che.api.machine.shared.dto.ProcessDescriptor;
import org.eclipse.che.api.machine.shared.dto.ProjectBindingDescriptor;
import org.eclipse.che.api.machine.shared.dto.RecipeMachineCreationMetadata;
import org.eclipse.che.api.machine.shared.dto.ServerDescriptor;
import org.eclipse.che.api.machine.shared.dto.SnapshotDescriptor;
import org.eclipse.che.api.machine.shared.dto.SnapshotMachineCreationMetadata;
import org.eclipse.che.api.machine.shared.dto.recipe.MachineRecipe;
import org.eclipse.che.api.workspace.server.dao.Member;
import org.eclipse.che.api.workspace.server.dao.MemberDao;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.lang.Strings;
import org.eclipse.che.dto.server.DtoFactory;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Machine API
 *
 * @author Alexander Garagatyi
 */
@Path("/machine")
public class MachineService {
    private MachineManager machineManager;
    private DtoFactory     dtoFactory;
    private MemberDao      memberDao;

    @Inject
    public MachineService(MachineManager machineManager, MemberDao memberDao) {
        this.machineManager = machineManager;
        this.memberDao = memberDao;
        this.dtoFactory = DtoFactory.getInstance();
    }

    @Path("/recipe")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public MachineStateDescriptor createMachineFromRecipe(final RecipeMachineCreationMetadata machineFromRecipeMetadata)
            throws ServerException, ForbiddenException, NotFoundException {
        requiredNotNull(machineFromRecipeMetadata, "Machine description");
        requiredNotNull(machineFromRecipeMetadata.getRecipeDescriptor(), "Machine type");
        requiredNotNull(machineFromRecipeMetadata.getWorkspaceId(), "Workspace id");
        requiredNotNull(machineFromRecipeMetadata.getRecipeDescriptor(), "Recipe descriptor");
        requiredNotNull(machineFromRecipeMetadata.getRecipeDescriptor().getScript(), "Recipe script");
        requiredNotNull(machineFromRecipeMetadata.getRecipeDescriptor().getType(), "Recipe type");

        checkCurrentUserPermissions(machineFromRecipeMetadata.getWorkspaceId());

        final MachineImpl machine = machineManager.create(machineFromRecipeMetadata);

        return toDescriptor(machine);
    }

    @Path("/snapshot")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public MachineStateDescriptor createMachineFromSnapshot(SnapshotMachineCreationMetadata machineFromSnapshotMetadata)
            throws ForbiddenException, NotFoundException, ServerException {
        requiredNotNull(machineFromSnapshotMetadata, "Snapshot description");
        requiredNotNull(machineFromSnapshotMetadata.getSnapshotId(), "Snapshot id");

        final SnapshotImpl snapshot = machineManager.getSnapshot(machineFromSnapshotMetadata.getSnapshotId());
        checkCurrentUserPermissions(snapshot);
        checkCurrentUserPermissions(snapshot.getWorkspaceId());

        final MachineImpl machine = machineManager.create(machineFromSnapshotMetadata);

        return toDescriptor(machine);
    }

    @Path("/{machineId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public MachineDescriptor getMachineById(@PathParam("machineId") String machineId)
            throws ServerException, ForbiddenException, NotFoundException {
        final Instance machine = machineManager.getMachine(machineId);

        checkCurrentUserPermissions(machine);

        return toDescriptor(machine);
    }

    @Path("/{machineId}/state")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public MachineStateDescriptor getMachineStateById(@PathParam("machineId") String machineId)
            throws ServerException, ForbiddenException, NotFoundException {
        final MachineImpl machineState = machineManager.getMachineState(machineId);

        checkCurrentUserPermissions(machineState);

        return toDescriptor(machineState);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public List<MachineDescriptor> getMachines(@QueryParam("workspace") String workspaceId,
                                               @QueryParam("project") String path)
            throws ServerException, ForbiddenException {
        requiredNotNull(workspaceId, "Parameter workspace");

        final String userId = EnvironmentContext.getCurrent().getUser().getId();
        final List<Instance> machines = machineManager.getMachines(userId,
                                                                   workspaceId,
                                                                   Strings.isNullOrEmpty(path) ? null : new ProjectBindingImpl(path));

        final List<MachineDescriptor> machinesDescriptors = new LinkedList<>();
        for (Instance machine : machines) {
            machinesDescriptors.add(toDescriptor(machine));
        }

        return machinesDescriptors;
    }

    @GET
    @Path("/state")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public List<MachineStateDescriptor> getMachinesStates(@QueryParam("workspace") String workspaceId,
                                                          @QueryParam("project") String path)
            throws ServerException, ForbiddenException {
        requiredNotNull(workspaceId, "Parameter workspace");

        final String userId = EnvironmentContext.getCurrent().getUser().getId();
        final List<MachineImpl> machines = machineManager.getMachinesStates(userId,
                                                                            workspaceId,
                                                                            Strings.isNullOrEmpty(path) ? null
                                                                                                        : new ProjectBindingImpl(path));

        final List<MachineStateDescriptor> machinesDescriptors = new LinkedList<>();
        for (MachineImpl machine : machines) {
            machinesDescriptors.add(toDescriptor(machine));
        }

        return machinesDescriptors;
    }

    @Path("/{machineId}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public void destroyMachine(@PathParam("machineId") String machineId)
            throws NotFoundException, ServerException, ForbiddenException {
        checkCurrentUserPermissions(machineManager.getMachine(machineId));

        machineManager.destroy(machineId);
    }

    @Path("/snapshot")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public List<SnapshotDescriptor> getSnapshots(@QueryParam("workspace") String workspaceId,
                                                 @QueryParam("project") String path) throws ServerException, ForbiddenException {

        requiredNotNull(workspaceId, "Parameter workspace");

        final List<SnapshotImpl> snapshots = machineManager.getSnapshots(EnvironmentContext.getCurrent().getUser().getId(),
                                                                         workspaceId,
                                                                         Strings.isNullOrEmpty(path) ? null : new ProjectBindingImpl(path));

        final List<SnapshotDescriptor> snapshotDescriptors = new LinkedList<>();
        for (SnapshotImpl snapshot : snapshots) {
            snapshotDescriptors.add(toDescriptor(snapshot));
        }

        return snapshotDescriptors;
    }

    @Path("/{machineId}/snapshot")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public SnapshotDescriptor saveSnapshot(@PathParam("machineId") String machineId, NewSnapshotDescriptor newSnapshotDescriptor)
            throws NotFoundException, ServerException, ForbiddenException {

        requiredNotNull(newSnapshotDescriptor, "Snapshot description");
        checkCurrentUserPermissions(machineManager.getMachine(machineId));

        return toDescriptor(machineManager.save(machineId,
                                                EnvironmentContext.getCurrent().getUser().getId(),
                                                newSnapshotDescriptor.getLabel(),
                                                newSnapshotDescriptor.getDescription()));
    }

    @Path("/snapshot/{snapshotId}")
    @DELETE
    @RolesAllowed("user")
    public void removeSnapshot(@PathParam("snapshotId") String snapshotId)
            throws ForbiddenException, NotFoundException, ServerException {
        checkCurrentUserPermissions(machineManager.getSnapshot(snapshotId));

        machineManager.removeSnapshot(snapshotId);
    }

    @Path("/{machineId}/command")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public ProcessDescriptor executeCommandInMachine(@PathParam("machineId") String machineId, final CommandDescriptor command)
            throws NotFoundException, ServerException, ForbiddenException {

        requiredNotNull(command, "Command description");
        checkCurrentUserPermissions(machineManager.getMachine(machineId));

        return toDescriptor(machineManager.exec(machineId, command, command.getOutputChannel()));
    }

    @Path("/{machineId}/process")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public List<ProcessDescriptor> getProcesses(@PathParam("machineId") String machineId)
            throws NotFoundException, ServerException, ForbiddenException {

        checkCurrentUserPermissions(machineManager.getMachine(machineId));

        final List<ProcessDescriptor> processesDescriptors = new LinkedList<>();
        for (InstanceProcess process : machineManager.getProcesses(machineId)) {
            processesDescriptors.add(toDescriptor(process));
        }

        return processesDescriptors;
    }

    @Path("/{machineId}/process/{processId}")
    @DELETE
    @RolesAllowed("user")
    public void stopProcess(@PathParam("machineId") String machineId,
                            @PathParam("processId") int processId)
            throws NotFoundException, ForbiddenException, ServerException {
        checkCurrentUserPermissions(machineManager.getMachine(machineId));

        machineManager.stopProcess(machineId, processId);
    }

    @Path("/{machineId}/binding/{path:.*}")
    @POST
    @RolesAllowed("user")
    public void bindProject(@PathParam("machineId") String machineId,
                            @PathParam("path") String path)
            throws NotFoundException, ServerException, ForbiddenException {
        checkCurrentUserPermissions(machineManager.getMachine(machineId));

        machineManager.bindProject(machineId, new ProjectBindingImpl().withPath(path));
    }

    @Path("/{machineId}/binding/{path:.*}")
    @DELETE
    @RolesAllowed("user")
    public void unbindProject(@PathParam("machineId") String machineId,
                              @PathParam("path") String path)
            throws NotFoundException, ServerException, ForbiddenException {
        checkCurrentUserPermissions(machineManager.getMachine(machineId));

        machineManager.unbindProject(machineId, new ProjectBindingImpl().withPath(path));
    }

    @GET
    @Path("/{machineId}/logs")
    @Produces(MediaType.TEXT_PLAIN)
    @RolesAllowed("user")
    public void getMachineLogs(@PathParam("machineId") String machineId,
                               @Context HttpServletResponse httpServletResponse)
            throws NotFoundException, ForbiddenException, ServerException, IOException {
        checkCurrentUserPermissions(machineManager.getMachine(machineId));

        addLogsToResponse(machineManager.getMachineLogReader(machineId), httpServletResponse);
    }

    @GET
    @Path("/{machineId}/process/{pid}/logs")
    @Produces(MediaType.TEXT_PLAIN)
    @RolesAllowed("user")
    public void getProcessLogs(@PathParam("machineId") String machineId,
                               @PathParam("pid") int pid,
                               @Context HttpServletResponse httpServletResponse)
            throws NotFoundException, ForbiddenException, ServerException, IOException {
        checkCurrentUserPermissions(machineManager.getMachine(machineId));

        addLogsToResponse(machineManager.getProcessLogReader(machineId, pid), httpServletResponse);
    }

    private void addLogsToResponse(Reader logsReader, HttpServletResponse httpServletResponse) throws IOException {
        // Response is written directly to the servlet request stream
        httpServletResponse.setContentType("text/plain");
        CharStreams.copy(logsReader, httpServletResponse.getWriter());
        httpServletResponse.getWriter().flush();
    }

    private void checkCurrentUserPermissions(SnapshotImpl snapshot) throws ForbiddenException, ServerException {
        checkCurrentUserPermissions(snapshot.getWorkspaceId());
    }

    private void checkCurrentUserPermissions(Instance machine) throws ForbiddenException, ServerException {
        checkCurrentUserPermissions(machine.getWorkspaceId());
    }

    private void checkCurrentUserPermissions(MachineImpl machineState) throws ForbiddenException, ServerException {
        checkCurrentUserPermissions(machineState.getWorkspaceId());
    }

    private void checkCurrentUserPermissions(String workspaceId) throws ForbiddenException, ServerException {
        try {
            final Member member = memberDao.getWorkspaceMember(workspaceId, EnvironmentContext.getCurrent().getUser().getId());
            if (member.getRoles().contains("workspace/admin") || member.getRoles().contains("workspace/developer")) {
                return;
            }
        } catch (NotFoundException ignored) {
        }
        throw new ForbiddenException("You are not a member of workspace " + workspaceId);
    }

    /**
     * Checks object reference is not {@code null}
     *
     * @param object
     *         object reference to check
     * @param subject
     *         used as subject of exception message "{subject} required"
     * @throws ForbiddenException
     *         when object reference is {@code null}
     */
    private void requiredNotNull(Object object, String subject) throws ForbiddenException {
        if (object == null) {
            throw new ForbiddenException(subject + " required");
        }
    }

    private MachineStateDescriptor toDescriptor(MachineImpl machineState) throws ServerException {
        final List<ProjectBindingDescriptor> projectDescriptors = new ArrayList<>();
        for (ProjectBinding project : machineState.getProjects()) {
            projectDescriptors.add(dtoFactory.createDto(ProjectBindingDescriptor.class)
                                             .withPath(project.getPath())
                                             .withLinks(null)); // TODO
        }

        MachineRecipe machineRecipe = DtoFactory.newDto(MachineRecipe.class)
                                                .withType(machineState.getRecipe().getType())
                                                .withScript(machineState.getRecipe().getScript());

        final MachineStateDescriptor machineDescriptor = dtoFactory.createDto(MachineStateDescriptor.class)
                                                                   .withId(machineState.getId())
                                                                   .withType(machineState.getType())
                                                                   .withRecipe(machineRecipe)
                                                                   .withStatus(machineState.getStatus())
                                                                   .withOwner(machineState.getOwner())
                                                                   .withWorkspaceId(machineState.getWorkspaceId())
                                                                   .withWorkspaceBound(machineState.isWorkspaceBound())
                                                                   .withProjects(projectDescriptors)
                                                                   .withDisplayName(machineState.getDisplayName())
                                                                   .withMemorySize(machineState.getMemorySize());

        machineDescriptor.setLinks(null); // TODO

        return machineDescriptor;
    }

    private MachineDescriptor toDescriptor(Instance machine) throws ServerException {
        final List<ProjectBindingDescriptor> projectDescriptors = new ArrayList<>();
        for (ProjectBinding project : machine.getProjects()) {
            projectDescriptors.add(dtoFactory.createDto(ProjectBindingDescriptor.class)
                                             .withPath(project.getPath())
                                             .withLinks(null)); // TODO
        }

        MachineRecipe machineRecipe = DtoFactory.newDto(MachineRecipe.class)
                                                .withType(machine.getRecipe().getType())
                                                .withScript(machine.getRecipe().getScript());

        final MachineDescriptor machineDescriptor = dtoFactory.createDto(MachineDescriptor.class)
                                                              .withId(machine.getId())
                                                              .withType(machine.getType())
                                                              .withRecipe(machineRecipe)
                                                              .withStatus(machine.getStatus())
                                                              .withOwner(machine.getOwner())
                                                              .withWorkspaceId(machine.getWorkspaceId())
                                                              .withWorkspaceBound(machine.isWorkspaceBound())
                                                              .withProjects(projectDescriptors)
                                                              .withDisplayName(machine.getDisplayName())
                                                              .withMemorySize(machine.getMemorySize());

        Map<String, Server> servers = machine.getServers();
        final Map<String, ServerDescriptor> serverDescriptors = Maps.newHashMapWithExpectedSize(servers.size());
        for (Map.Entry<String, Server> serverEntry : servers.entrySet()) {
            serverDescriptors.put(serverEntry.getKey(),
                                  dtoFactory.createDto(ServerDescriptor.class)
                                            .withAddress(serverEntry.getValue().getAddress())
                                            .withRef(serverEntry.getValue().getRef())
                                            .withUrl(serverEntry.getValue().getUrl()));
        }
        machineDescriptor.withMetadata(machine.getMetadata().getProperties())
                         .withServers(serverDescriptors);
        machineDescriptor.setLinks(null); // TODO

        return machineDescriptor;
    }

    private ProcessDescriptor toDescriptor(InstanceProcess process) throws ServerException {
        return dtoFactory.createDto(ProcessDescriptor.class)
                         .withPid(process.getPid())
                         .withCommandLine(process.getCommandLine())
                         .withAlive(process.isAlive())
                         .withLinks(null); // TODO
    }

    private SnapshotDescriptor toDescriptor(SnapshotImpl snapshot) {
        final List<ProjectBindingDescriptor> projectDescriptors = new ArrayList<>(snapshot.getProjects().size());
        for (ProjectBinding projectBinding : snapshot.getProjects()) {
            projectDescriptors.add(dtoFactory.createDto(ProjectBindingDescriptor.class)
                                             .withPath(projectBinding.getPath())
                                             .withLinks(null));
        }

        MachineRecipe machineRecipe = DtoFactory.newDto(MachineRecipe.class)
                                                .withType(snapshot.getRecipe().getType())
                                                .withScript(snapshot.getRecipe().getScript());

        return dtoFactory.createDto(SnapshotDescriptor.class)
                         .withId(snapshot.getId())
                         .withOwner(snapshot.getOwner())
                         .withType(snapshot.getType())
                         .withRecipe(machineRecipe)
                         .withDescription(snapshot.getDescription())
                         .withCreationDate(snapshot.getCreationDate())
                         .withWorkspaceId(snapshot.getWorkspaceId())
                         .withProjects(projectDescriptors)
                         .withWorkspaceBound(snapshot.isWorkspaceBound())
                         .withLinks(null);// TODO
    }
}
