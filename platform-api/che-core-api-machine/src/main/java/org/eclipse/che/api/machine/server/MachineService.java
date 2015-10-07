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

import com.google.common.io.CharStreams;

import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.machine.server.exception.MachineException;
import org.eclipse.che.api.machine.server.impl.SnapshotImpl;
import org.eclipse.che.api.machine.server.model.impl.MachineStateImpl;
import org.eclipse.che.api.machine.server.spi.Instance;
import org.eclipse.che.api.machine.shared.dto.CommandDto;
import org.eclipse.che.api.machine.shared.dto.MachineDto;
import org.eclipse.che.api.machine.shared.dto.MachineProcessDto;
import org.eclipse.che.api.machine.shared.dto.MachineStateDto;
import org.eclipse.che.api.machine.shared.dto.NewSnapshotDescriptor;
import org.eclipse.che.api.machine.shared.dto.SnapshotDto;
import org.eclipse.che.commons.env.EnvironmentContext;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
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
import java.util.List;
import java.util.stream.Collectors;

/**
 * Machine API
 *
 * @author Alexander Garagatyi
 * @author Anton Korneta
 */
@Path("/machine")
public class MachineService {
    private MachineManager machineManager;

    @Inject
    public MachineService(MachineManager machineManager) {
        this.machineManager = machineManager;
    }

    @Path("/{machineId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public MachineDto getMachineById(@PathParam("machineId") String machineId)
            throws ServerException, ForbiddenException, NotFoundException {
        final Instance machine = machineManager.getMachine(machineId);

        checkCurrentUserPermissions(machine);

        return DtoConverter.asDto(machine);
    }

    @Path("/{machineId}/state")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public MachineStateDto getMachineStateById(@PathParam("machineId") String machineId)
            throws ServerException, ForbiddenException, NotFoundException {
        final MachineStateImpl machineState = machineManager.getMachineState(machineId);

        checkCurrentUserPermissions(machineState);

        return DtoConverter.asDto(machineState);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public List<MachineDto> getMachines(@QueryParam("workspace") String workspaceId,
                                        @QueryParam("project") String path)
            throws ServerException, ForbiddenException {
        requiredNotNull(workspaceId, "Parameter workspace");

        final String userId = EnvironmentContext.getCurrent().getUser().getId();

        return machineManager.getMachines(userId, workspaceId)
                             .stream()
                             .map(DtoConverter::asDto)
                             .collect(Collectors.toList());
    }

    @GET
    @Path("/state")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public List<MachineStateDto> getMachinesStates(@QueryParam("workspace") String workspaceId,
                                                   @QueryParam("project") String path)
            throws ServerException, ForbiddenException {
        requiredNotNull(workspaceId, "Parameter workspace");

        final String userId = EnvironmentContext.getCurrent().getUser().getId();
        final List<MachineStateImpl> machines = machineManager.getMachinesStates(userId, workspaceId);

        return machines.stream()
                       .map(DtoConverter::asDto)
                       .collect(Collectors.toList());
    }

    @Path("/{machineId}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public void destroyMachine(@PathParam("machineId") String machineId)
            throws NotFoundException, ServerException, ForbiddenException {
        checkCurrentUserPermissions(machineManager.getMachine(machineId));

        machineManager.destroy(machineId, true);
    }

    @Path("/snapshot")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public List<SnapshotDto> getSnapshots(@QueryParam("workspace") String workspaceId,
                                          @QueryParam("project") String path) throws ServerException, ForbiddenException {

        requiredNotNull(workspaceId, "Parameter workspace");

        final List<SnapshotImpl> snapshots = machineManager.getSnapshots(EnvironmentContext.getCurrent().getUser().getId(), workspaceId);

        return snapshots.stream()
                        .map(DtoConverter::asDto)
                        .collect(Collectors.toList());
    }

    @Path("/{machineId}/snapshot")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public SnapshotDto saveSnapshot(@PathParam("machineId") String machineId, NewSnapshotDescriptor newSnapshotDescriptor)
            throws NotFoundException, ServerException, ForbiddenException {

        requiredNotNull(newSnapshotDescriptor, "Snapshot description");
        checkCurrentUserPermissions(machineManager.getMachine(machineId));

        return DtoConverter.asDto(machineManager.save(machineId,
                                                      EnvironmentContext.getCurrent().getUser().getId(),
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
    public MachineProcessDto executeCommandInMachine(@PathParam("machineId") String machineId,
                                                     final CommandDto command,
                                                     @QueryParam("outputChannel") String outputChannel)
            throws NotFoundException, ServerException, ForbiddenException {

        requiredNotNull(command, "Command description");
        requiredNotNull(command.getCommandLine(), "Commandline");
        checkCurrentUserPermissions(machineManager.getMachine(machineId));

        return DtoConverter.asDto(machineManager.exec(machineId, command, outputChannel));
    }

    @Path("/{machineId}/process")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
    public List<MachineProcessDto> getProcesses(@PathParam("machineId") String machineId)
            throws NotFoundException, ServerException, ForbiddenException {

        checkCurrentUserPermissions(machineManager.getMachine(machineId));

        return machineManager.getProcesses(machineId)
                             .stream()
                             .map(DtoConverter::asDto)
                             .collect(Collectors.toList());
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

    /**
     * Reads file content by specified file path.
     *
     * @param path
     *         path to file on machine instance
     * @param startFrom
     *         line number to start reading from
     * @param limit
     *         limitation on line if not specified will used 2000 lines
     * @return file content.
     * @throws MachineException
     *         if any error occurs with file reading
     */
    @GET
    @Path("/{machineId}/filepath/{path:.*}")
    @Produces(MediaType.TEXT_PLAIN)
    @RolesAllowed("user")
    public String getFileContent(@PathParam("machineId") String machineId,
                                 @PathParam("path") String path,
                                 @DefaultValue("1") @QueryParam("startFrom") Integer startFrom,
                                 @DefaultValue("2000") @QueryParam("limit") Integer limit)
            throws NotFoundException, ForbiddenException, ServerException {
        checkCurrentUserPermissions(machineManager.getMachine(machineId));

        return machineManager.getMachine(machineId).readFileContent(path, startFrom, limit);
    }

    /**
     * Copies files from specified machine into current machine.
     *
     * @param sourceMachineId
     *         source machine id
     * @param targetMachineId
     *         target machine id
     * @param sourcePath
     *         path to file or directory inside specified machine
     * @param targetPath
     *         path to destination file or directory inside machine
     * @param overwrite
     *         If "false" then it will be an error if unpacking the given content would cause
     *         an existing directory to be replaced with a non-directory and vice versa.
     * @throws MachineException
     *         if any error occurs when files are being copied
     * @throws NotFoundException
     *         if any error occurs with getting source machine
     */
    @POST
    @Path("/copy")
    @RolesAllowed("user")
    public void copyFilesBetweenMachines(@QueryParam("sourceMachineId") String sourceMachineId,
                                         @QueryParam("targetMachineId") String targetMachineId,
                                         @QueryParam("sourcePath") String sourcePath,
                                         @QueryParam("targetPath") String targetPath,
                                         @DefaultValue("false") @QueryParam("overwrite") Boolean overwrite)
            throws NotFoundException, ServerException, ForbiddenException {
        requiredNotNull(sourceMachineId, "Source machine id");
        requiredNotNull(targetMachineId, "Target machine id");
        requiredNotNull(sourcePath, "Source path");
        requiredNotNull(targetPath, "Target path");

        checkCurrentUserPermissions(machineManager.getMachine(sourceMachineId));
        checkCurrentUserPermissions(machineManager.getMachine(targetMachineId));

        machineManager.getMachine(targetMachineId).copy(machineManager.getMachine(sourceMachineId), sourcePath, targetPath, overwrite);
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

    private void checkCurrentUserPermissions(MachineStateImpl machineState) throws ForbiddenException, ServerException {
        checkCurrentUserPermissions(machineState.getWorkspaceId());
    }

    private void checkCurrentUserPermissions(String workspaceId) throws ForbiddenException, ServerException {
        // TODO
//        try {
//            final Member member = memberDao.getWorkspaceMember(workspaceId, EnvironmentContext.getCurrent().getUser().getId());
//            if (member.getRoles().contains("workspace/admin") || member.getRoles().contains("workspace/developer")) {
//                return;
//            }
//        } catch (NotFoundException ignored) {
//        }
//        throw new ForbiddenException("You are not a member of workspace " + workspaceId);
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
}
