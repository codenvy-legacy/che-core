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

import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.util.LineConsumer;
import org.eclipse.che.api.core.util.WebsocketLineConsumer;
import org.eclipse.che.api.machine.shared.ProjectBinding;
import org.eclipse.che.api.machine.shared.dto.CreateMachineFromRecipe;
import org.eclipse.che.api.machine.shared.dto.CreateMachineFromSnapshot;
import org.eclipse.che.api.machine.shared.dto.MachineDescriptor;
import org.eclipse.che.api.machine.shared.dto.CommandDescriptor;
import org.eclipse.che.api.machine.shared.dto.ProcessDescriptor;
import org.eclipse.che.api.machine.shared.dto.NewSnapshotDescriptor;
import org.eclipse.che.api.machine.shared.dto.ProjectBindingDescriptor;
import org.eclipse.che.api.machine.shared.dto.SnapshotDescriptor;
import org.eclipse.che.api.workspace.server.dao.Member;
import org.eclipse.che.api.workspace.server.dao.MemberDao;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.dto.server.DtoFactory;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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
    public MachineDescriptor createMachineFromRecipe(final CreateMachineFromRecipe createMachineRequest)
            throws ServerException, ForbiddenException, NotFoundException {
        requiredNotNull(createMachineRequest, "Machine description");
        requiredNotNull(createMachineRequest.getRecipeDescriptor(), "Machine type");
        requiredNotNull(createMachineRequest.getWorkspaceId(), "Workspace id");
        requiredNotNull(createMachineRequest.getRecipeDescriptor(), "Recipe descriptor");
        requiredNotNull(createMachineRequest.getRecipeDescriptor().getScript(), "Recipe script");
        requiredNotNull(createMachineRequest.getRecipeDescriptor().getType(), "Recipe type");

        checkCurrentUserPermissionsForWorkspace(createMachineRequest.getWorkspaceId());

        final LineConsumer lineConsumer = getLineConsumer(createMachineRequest.getOutputChannel());

        final MachineImpl machine = machineManager.create(createMachineRequest.getType(),
                                                          RecipeImpl.fromDescriptor(createMachineRequest.getRecipeDescriptor()),
                                                          createMachineRequest.getWorkspaceId(),
                                                          EnvironmentContext.getCurrent().getUser().getId(),
                                                          lineConsumer);

        // TODO displayName? machine description?
        return toDescriptor(machine);
    }

    @Path("/snapshot")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public MachineDescriptor createMachineFromSnapshot(CreateMachineFromSnapshot createMachineRequest)
            throws ForbiddenException, NotFoundException, ServerException {
        requiredNotNull(createMachineRequest, "Snapshot description");
        requiredNotNull(createMachineRequest.getSnapshotId(), "Snapshot id");
        final Snapshot snapshot = machineManager.getSnapshot(createMachineRequest.getSnapshotId());
        checkCurrentUserPermissionsForSnapshot(snapshot);
        checkCurrentUserPermissionsForWorkspace(snapshot.getWorkspaceId());

        final LineConsumer lineConsumer = getLineConsumer(createMachineRequest.getOutputChannel());

        final MachineImpl machine = machineManager.create(createMachineRequest.getSnapshotId(),
                                                          EnvironmentContext.getCurrent().getUser().getId(),
                                                          lineConsumer);

        return toDescriptor(machine);
    }

    @Path("/{machineId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public MachineDescriptor getMachineById(@PathParam("machineId") String machineId)
            throws ServerException, ForbiddenException, NotFoundException {
        final MachineImpl machine = machineManager.getMachine(machineId);

        checkCurrentUserPermissionsForMachine(machine.getOwner());

        return toDescriptor(machine);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public List<MachineDescriptor> getMachines(@QueryParam("workspace") String workspaceId,
                                               @QueryParam("project") String path)
            throws ServerException, ForbiddenException {
        requiredNotNull(workspaceId, "Parameter workspace");

        final String userId = EnvironmentContext.getCurrent().getUser().getId();
        final List<MachineImpl> machines = machineManager.getMachines(userId,
                                                                      workspaceId,
                                                                      path != null && !path.isEmpty() ? new ProjectBindingImpl().withPath(
                                                                              path) : null);

        final List<MachineDescriptor> machinesDescriptors = new LinkedList<>();
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
        checkCurrentUserPermissionsForMachine(machineManager.getMachine(machineId).getOwner());

        machineManager.destroy(machineId);
    }

    @Path("/snapshot")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public List<SnapshotDescriptor> getSnapshots(@QueryParam("workspace") String workspaceId,
                                                 @QueryParam("project") String path)
            throws ServerException, ForbiddenException {
        requiredNotNull(workspaceId, "Parameter workspace");

        final List<Snapshot> snapshots = machineManager.getSnapshots(EnvironmentContext.getCurrent().getUser().getId(),
                                                                     workspaceId,
                                                                     path != null && !path.isEmpty() ? new ProjectBindingImpl()
                                                                             .withPath(path) : null);

        final List<SnapshotDescriptor> snapshotDescriptors = new LinkedList<>();
        for (Snapshot snapshot : snapshots) {
            snapshotDescriptors.add(toDescriptor(snapshot));
        }

        return snapshotDescriptors;
    }

    @Path("/{machineId}/snapshot")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public void saveSnapshot(@PathParam("machineId") String machineId, NewSnapshotDescriptor newSnapshotDescriptor)
            throws NotFoundException, ServerException, ForbiddenException {
        checkCurrentUserPermissionsForMachine(machineManager.getMachine(machineId).getOwner());

        machineManager.save(machineId, EnvironmentContext.getCurrent().getUser().getId(), newSnapshotDescriptor.getDescription());
    }

    @Path("/snapshot/{snapshotId}")
    @DELETE
    @RolesAllowed("user")
    public void removeSnapshot(@PathParam("snapshotId") String snapshotId)
            throws ForbiddenException, NotFoundException, ServerException {
        checkCurrentUserPermissionsForSnapshot(machineManager.getSnapshot(snapshotId));

        machineManager.removeSnapshot(snapshotId);
    }

    @Path("/{machineId}/command")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public ProcessDescriptor executeCommandInMachine(@PathParam("machineId") String machineId, final CommandDescriptor command)
            throws NotFoundException, ServerException, ForbiddenException {
        requiredNotNull(command, "Command description");
        checkCurrentUserPermissionsForMachine(machineManager.getMachine(machineId).getOwner());

        final LineConsumer lineConsumer = getLineConsumer(command.getOutputChannel());

        return toDescriptor(machineManager.exec(machineId, command, lineConsumer));
    }

    @Path("/{machineId}/process")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public List<ProcessDescriptor> getProcesses(@PathParam("machineId") String machineId)
            throws NotFoundException, ServerException, ForbiddenException {
        checkCurrentUserPermissionsForMachine(machineManager.getMachine(machineId).getOwner());

        final List<ProcessDescriptor> processesDescriptors = new LinkedList<>();
        for (ProcessImpl process : machineManager.getProcesses(machineId)) {
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
        checkCurrentUserPermissionsForMachine(machineManager.getMachine(machineId).getOwner());

        machineManager.stopProcess(machineId, processId);
    }

    @Path("/{machineId}/binding/{path:.*}")
    @POST
    @RolesAllowed("user")
    public void bindProject(@PathParam("machineId") String machineId,
                            @PathParam("path") String path)
            throws NotFoundException, ServerException, ForbiddenException {
        checkCurrentUserPermissionsForMachine(machineManager.getMachine(machineId).getOwner());

        machineManager.bindProject(machineId, new ProjectBindingImpl().withPath(path));
    }

    @Path("/{machineId}/binding/{path:.*}")
    @DELETE
    @RolesAllowed("user")
    public void unbindProject(@PathParam("machineId") String machineId,
                              @PathParam("path") String path)
            throws NotFoundException, ServerException, ForbiddenException {
        checkCurrentUserPermissionsForMachine(machineManager.getMachine(machineId).getOwner());

        machineManager.unbindProject(machineId, new ProjectBindingImpl().withPath(path));
    }

    private void checkCurrentUserPermissionsForSnapshot(Snapshot snapshot) throws ForbiddenException, NotFoundException, ServerException {
        if (!EnvironmentContext.getCurrent().getUser().getId().equals(snapshot.getOwner())) {
            throw new ForbiddenException("You are not the owner of this snapshot");
        }
    }

    private void checkCurrentUserPermissionsForMachine(String machineOwner) throws ForbiddenException {
        final String userId = EnvironmentContext.getCurrent().getUser().getId();
        if (!userId.equals(machineOwner)) {
            throw new ForbiddenException("You are not the owner of this machine");
        }
    }

    private void checkCurrentUserPermissionsForWorkspace(String workspaceId) throws ForbiddenException, ServerException {
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

    // make it package private for testing purposes
    LineConsumer getLineConsumer(String outputChannel) {
        final LineConsumer lineConsumer;
        if (outputChannel != null) {
            lineConsumer = new WebsocketLineConsumer(outputChannel);
        } else {
            lineConsumer = LineConsumer.DEV_NULL;
        }
        return lineConsumer;
    }

    private MachineDescriptor toDescriptor(MachineImpl machine) throws ServerException {
        final List<ProjectBindingDescriptor> projectDescriptors = new ArrayList<>();
        for (ProjectBinding project : machine.getProjects()) {
            projectDescriptors.add(dtoFactory.createDto(ProjectBindingDescriptor.class)
                                             .withPath(project.getPath())
                                             .withLinks(null)); // TODO
        }

        return dtoFactory.createDto(MachineDescriptor.class)
                         .withId(machine.getId())
                         .withType(machine.getType())
                         .withState(machine.getState())
                         .withOwner(machine.getOwner())
                         .withWorkspaceId(machine.getWorkspaceId())
                         .withProjects(projectDescriptors)
                         .withMetadata(machine.getInstance() != null ? machine.getInstance().getMetadata().getProperties() : null)
                         .withLinks(null); // TODO
    }

    private ProcessDescriptor toDescriptor(ProcessImpl process) throws ServerException {
        return dtoFactory.createDto(ProcessDescriptor.class)
                         .withPid(process.getPid())
                         .withCommandLine(process.getCommandLine())
                         .withIsAlive(process.isAlive())
                         .withLinks(null); // TODO
    }

    private SnapshotDescriptor toDescriptor(Snapshot snapshot) {
        final List<ProjectBindingDescriptor> projectDescriptors = new ArrayList<>(snapshot.getProjects().size());
        for (ProjectBinding projectBinding : snapshot.getProjects()) {
            projectDescriptors.add(dtoFactory.createDto(ProjectBindingDescriptor.class)
                                             .withPath(projectBinding.getPath())
                                             .withLinks(null));
        }

        return dtoFactory.createDto(SnapshotDescriptor.class)
                         .withId(snapshot.getId())
                         .withOwner(snapshot.getOwner())
                         .withImageType(snapshot.getImageType())
                         .withDescription(snapshot.getDescription())
                         .withCreationDate(snapshot.getCreationDate())
                         .withWorkspaceId(snapshot.getWorkspaceId())
                         .withProjects(projectDescriptors)
                         .withLinks(null);// TODO
    }
}
