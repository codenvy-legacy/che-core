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
package org.eclipse.che.api.machine.server;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import com.google.common.io.CharStreams;

import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.rest.Service;
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
@Api(value = "/machine", description = "Machine REST API")
@Path("/machine")
public class MachineService extends Service {
    private MachineManager machineManager;

    @Inject
    public MachineService(MachineManager machineManager) {
        this.machineManager = machineManager;
    }

    @GET
    @Path("/{machineId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
//    @GenerateLink(rel = LINK_REL_GET_MACHINE) TODO
    @ApiOperation(value = "Get machine by ID")
//                  notes = "This operation can be performed only by the machine owner"
    @ApiResponses({@ApiResponse(code = 200, message = "The response contains requested machine entity"),
                   @ApiResponse(code = 404, message = "Machine with specified id does not exist"),
//                   @ApiResponse(code = 403, message = "User is not machine owner"),
                   @ApiResponse(code = 500, message = "Internal server error occurred")})
    public MachineDto getMachineById(@ApiParam(value = "Machine ID")
                                     @PathParam("machineId")
                                     String machineId)
            throws ServerException,
                   ForbiddenException,
                   NotFoundException {

        final Instance machine = machineManager.getMachine(machineId);

        checkCurrentUserPermissions(machine);

        return DtoConverter.asDto(machine);
    }

    @GET
    @Path("/{machineId}/state")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
//    @GenerateLink(rel = LINK_REL_GET_MACHINE_STATE) TODO
    @ApiOperation(value = "Get machine state by ID")
//                  notes = "This operation can be performed only by the machine owner",
    @ApiResponses({@ApiResponse(code = 200, message = "The response contains requested machine state entity"),
                   @ApiResponse(code = 404, message = "Machine with specified id does not exist"),
//                   @ApiResponse(code = 403, message = "User is not machine owner"),
                   @ApiResponse(code = 500, message = "Internal server error occurred")})
    public MachineStateDto getMachineStateById(@ApiParam(value = "Machine ID")
                                               @PathParam("machineId")
                                               String machineId)
            throws ServerException,
                   ForbiddenException,
                   NotFoundException {

        final MachineStateImpl machineState = machineManager.getMachineState(machineId);

        checkCurrentUserPermissions(machineState);

        return DtoConverter.asDto(machineState);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
//    @GenerateLink(rel = LINK_REL_GET_MACHINES) TODO
    @ApiOperation(value = "Get all machines of workspace with specified ID",
                  response = MachineDto.class,
                  responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "The response contains requested list of machine entities"),
                   @ApiResponse(code = 403, message = "Workspace ID is not specified"), // TODO change to 400
                   @ApiResponse(code = 500, message = "Internal server error occurred")})
    public List<MachineDto> getMachines(@ApiParam(value = "Workspace ID", required = true)
                                        @QueryParam("workspace")
                                        String workspaceId)
            throws ServerException,
                   ForbiddenException {

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
//    @GenerateLink(rel = LINK_REL_GET_MACHINES_STATES) TODO
    @ApiOperation(value = "Get all states of all machines of workspace with specified ID",
                  response = MachineStateDto.class,
                  responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "The response contains requested list of machine state entities"),
                   @ApiResponse(code = 403, message = "Workspace ID is not specified"), // TODO change to 400
                   @ApiResponse(code = 500, message = "Internal server error occurred")})
    public List<MachineStateDto> getMachinesStates(@ApiParam(value = "Workspace ID", required = true)
                                                   @QueryParam("workspace")
                                                   String workspaceId)
            throws ServerException,
                   ForbiddenException {

        requiredNotNull(workspaceId, "Parameter workspace");

        final String userId = EnvironmentContext.getCurrent().getUser().getId();
        final List<MachineStateImpl> machines = machineManager.getMachinesStates(userId, workspaceId);

        return machines.stream()
                       .map(DtoConverter::asDto)
                       .collect(Collectors.toList());
    }

    @DELETE
    @Path("/{machineId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
//    @GenerateLink(rel = LINK_REL_DESTROY_MACHINE) TODO
    @ApiOperation(value = "Destroy machine")
//                  notes = "This operation can be performed only by the machine owner",
    @ApiResponses({@ApiResponse(code = 204, message = "Machine was successfully destroyed"),
                   @ApiResponse(code = 404, message = "Machine with specified id does not exist"),
//                   @ApiResponse(code = 403, message = "User is not machine owner"),
                   @ApiResponse(code = 500, message = "Internal server error occurred")})
    public void destroyMachine(@ApiParam(value = "Machine ID")
                               @PathParam("machineId")
                               String machineId)
            throws NotFoundException,
                   ServerException,
                   ForbiddenException {

        checkCurrentUserPermissions(machineManager.getMachine(machineId));

        machineManager.destroy(machineId, true);
    }

    @GET
    @Path("/snapshot")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
//    @GenerateLink(rel = LINK_REL_GET_SNAPSHOTS) TODO
    @ApiOperation(value = "Get all snapshots of machines in workspace",
                  response = SnapshotDto.class,
                  responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "The response contains requested list of snapshot entities"),
                   @ApiResponse(code = 403, message = "Workspace ID is not specified"), // TODO change to 400
                   @ApiResponse(code = 500, message = "Internal server error occurred")})
    public List<SnapshotDto> getSnapshots(@ApiParam(value = "Workspace ID", required = true)
                                          @QueryParam("workspace")
                                          String workspaceId)
            throws ServerException,
                   ForbiddenException {

        requiredNotNull(workspaceId, "Parameter workspace");

        final List<SnapshotImpl> snapshots = machineManager.getSnapshots(EnvironmentContext.getCurrent().getUser().getId(), workspaceId);

        return snapshots.stream()
                        .map(DtoConverter::asDto)
                        .collect(Collectors.toList());
    }

    @POST
    @Path("/{machineId}/snapshot")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
//    @GenerateLink(rel = LINK_REL_SAVE_SNAPSHOT) TODO
    @ApiOperation(value = "Save snapshot of machine")
    @ApiResponses({@ApiResponse(code = 200, message = "The response contains requested snapshot entity"),
                   @ApiResponse(code = 403, message = "Snapshot description is not specified"), // TODO change to 400
//                   @ApiResponse(code = 403, message = "User is not owner of machine"),
                   @ApiResponse(code = 404, message = "Snapshot with specified ID does not exist"),
                   @ApiResponse(code = 500, message = "Internal server error occurred")})
    public SnapshotDto saveSnapshot(@ApiParam(value = "Machine ID")
                                    @PathParam("machineId")
                                    String machineId,
                                    @ApiParam(value = "Snapshot description", required = true)
                                    NewSnapshotDescriptor newSnapshotDescriptor)
            throws NotFoundException,
                   ServerException,
                   ForbiddenException {

        requiredNotNull(newSnapshotDescriptor, "Snapshot description");
        checkCurrentUserPermissions(machineManager.getMachine(machineId));

        return DtoConverter.asDto(machineManager.save(machineId,
                                                      EnvironmentContext.getCurrent().getUser().getId(),
                                                      newSnapshotDescriptor.getDescription()));
    }

    @DELETE
    @Path("/snapshot/{snapshotId}")
    @RolesAllowed("user")
//    @GenerateLink(rel = LINK_REL_REMOVE_SNAPSHOT) TODO
    @ApiOperation(value = "Remove snapshot of machine")
    @ApiResponses({@ApiResponse(code = 204, message = "Snapshot was successfully removed"),
//                   @ApiResponse(code = 403, message = "User is not owner of machine"),
                   @ApiResponse(code = 404, message = "Snapshot with specified ID does not exist"),
                   @ApiResponse(code = 500, message = "Internal server error occurred")})
    public void removeSnapshot(@ApiParam(value = "Snapshot ID")
                               @PathParam("snapshotId")
                               String snapshotId)
            throws ForbiddenException,
                   NotFoundException,
                   ServerException {

        checkCurrentUserPermissions(machineManager.getSnapshot(snapshotId));

        machineManager.removeSnapshot(snapshotId);
    }

    @POST
    @Path("/{machineId}/command")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
//    @GenerateLink(rel = LINK_REL_EXECUTE_COMMAND) TODO
    @ApiOperation(value = "Start specified command in machine")
    @ApiResponses({@ApiResponse(code = 200, message = "The response contains entity of created machine process"),
                   @ApiResponse(code = 400, message = "Command entity is invalid"),
                   @ApiResponse(code = 403, message = "Command entity is invalid"), // TODO change to 400
//                   @ApiResponse(code = 403, message = "User is not owner of machine"),
                   @ApiResponse(code = 404, message = "Machine with specified ID does not exist"),
                   @ApiResponse(code = 500, message = "Internal server error occurred")})
    public MachineProcessDto executeCommandInMachine(@ApiParam(value = "Machine ID")
                                                     @PathParam("machineId")
                                                     String machineId,
                                                     @ApiParam(value = "Command to execute", required = true)
                                                     final CommandDto command,
                                                     @ApiParam(value = "Channel for command output", required = false)
                                                     @QueryParam("outputChannel")
                                                     String outputChannel)
            throws NotFoundException,
                   ServerException,
                   ForbiddenException,
                   BadRequestException {

        requiredNotNull(command, "Command description");
        requiredNotNull(command.getCommandLine(), "Commandline");
        checkCurrentUserPermissions(machineManager.getMachine(machineId));

        return DtoConverter.asDto(machineManager.exec(machineId, command, outputChannel));
    }

    @GET
    @Path("/{machineId}/process")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("user")
//    @GenerateLink(rel = LINK_REL_GET_PROCESSES) TODO
    @ApiOperation(value = "Get processes of machine")
    @ApiResponses({@ApiResponse(code = 200, message = "The response contains machine process entities"),
//                   @ApiResponse(code = 403, message = "User is not owner of machine"),
                   @ApiResponse(code = 404, message = "Machine with specified ID does not exist"),
                   @ApiResponse(code = 500, message = "Internal server error occurred")})
    public List<MachineProcessDto> getProcesses(@ApiParam(value = "Machine ID")
                                                @PathParam("machineId")
                                                String machineId)
            throws NotFoundException,
                   ServerException,
                   ForbiddenException {

        checkCurrentUserPermissions(machineManager.getMachine(machineId));

        return machineManager.getProcesses(machineId)
                             .stream()
                             .map(DtoConverter::asDto)
                             .collect(Collectors.toList());
    }

    @DELETE
    @Path("/{machineId}/process/{processId}")
    @RolesAllowed("user")
//    @GenerateLink(rel = LINK_REL_STOP_PROCESS) TODO
    @ApiOperation(value = "Stop process in machine")
    @ApiResponses({@ApiResponse(code = 204, message = "Process was successfully stopped"),
//                   @ApiResponse(code = 403, message = "User is not owner of machine"),
                   @ApiResponse(code = 404, message = "Machine with specified ID does not exist"),
                   @ApiResponse(code = 500, message = "Internal server error occurred")})
    public void stopProcess(@ApiParam(value = "Machine ID")
                            @PathParam("machineId")
                            String machineId,
                            @ApiParam(value = "Process ID")
                            @PathParam("processId")
                            int processId)
            throws NotFoundException,
                   ForbiddenException,
                   ServerException {

        checkCurrentUserPermissions(machineManager.getMachine(machineId));

        machineManager.stopProcess(machineId, processId);
    }

    @GET
    @Path("/{machineId}/logs")
    @Produces(MediaType.TEXT_PLAIN)
    @RolesAllowed("user")
//    @GenerateLink(rel = LINK_REL_GET_MACHINE_LOGS) TODO
    @ApiOperation(value = "Get logs of machine")
    @ApiResponses({@ApiResponse(code = 200, message = "The response contains logs"),
//                   @ApiResponse(code = 403, message = "User is not owner of machine"),
                   @ApiResponse(code = 404, message = "Machine with specified ID does not exist"),
                   @ApiResponse(code = 500, message = "Internal server error occurred")})
    public void getMachineLogs(@ApiParam(value = "Machine ID")
                               @PathParam("machineId")
                               String machineId,
                               @Context
                               HttpServletResponse httpServletResponse)
            throws NotFoundException,
                   ForbiddenException,
                   ServerException,
                   IOException {

        checkCurrentUserPermissions(machineManager.getMachine(machineId));

        addLogsToResponse(machineManager.getMachineLogReader(machineId), httpServletResponse);
    }

    @GET
    @Path("/{machineId}/process/{pid}/logs")
    @Produces(MediaType.TEXT_PLAIN)
    @RolesAllowed("user")
//    @GenerateLink(rel = LINK_REL_GET_PROCESS_LOGS) TODO
    @ApiOperation(value = "Get logs of machine process")
    @ApiResponses({@ApiResponse(code = 200, message = "The response contains logs"),
//                   @ApiResponse(code = 403, message = "User is not owner of machine"),
                   @ApiResponse(code = 404, message = "Machine with specified ID does not exist"),
                   @ApiResponse(code = 404, message = "Process with specified ID does not exist"),
                   @ApiResponse(code = 500, message = "Internal server error occurred")})
    public void getProcessLogs(@ApiParam(value = "Machine ID")
                               @PathParam("machineId")
                               String machineId,
                               @ApiParam(value = "Process ID")
                               @PathParam("pid")
                               int pid,
                               @Context
                               HttpServletResponse httpServletResponse)
            throws NotFoundException,
                   ForbiddenException,
                   ServerException,
                   IOException {

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
//    @GenerateLink(rel = LINK_REL_GET_FILE_CONTENT) TODO
    @ApiOperation(value = "Get content of file in machine")
    @ApiResponses({@ApiResponse(code = 200, message = "The response contains file content"),
//                   @ApiResponse(code = 403, message = "User is not owner of machine"),
                   @ApiResponse(code = 404, message = "Machine with specified ID does not exist"),
                   @ApiResponse(code = 500, message = "Internal server error occurred")})
    public String getFileContent(@ApiParam(value = "Machine ID")
                                 @PathParam("machineId")
                                 String machineId,
                                 @ApiParam(value = "Path of file")
                                 @PathParam("path")
                                 String path,
                                 @ApiParam(value = "From line")
                                 @QueryParam("startFrom")
                                 @DefaultValue("1")
                                 Integer startFrom,
                                 @ApiParam(value = "Number of lines")
                                 @QueryParam("limit")
                                 @DefaultValue("2000")
                                 Integer limit)
            throws NotFoundException,
                   ForbiddenException,
                   ServerException {

        final Instance machine = machineManager.getMachine(machineId);

        checkCurrentUserPermissions(machine);

        return machine.readFileContent(path, startFrom, limit);
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
//    @GenerateLink(rel = LINK_REL_COPY_FILES) TODO
    @ApiOperation(value = "Copy files from one machine to another")
    @ApiResponses({@ApiResponse(code = 200, message = "Files were copied successfully"),
                   @ApiResponse(code = 403, message = "Source machine ID is not specified"), // TODO change to 400
                   @ApiResponse(code = 403, message = "Target machine ID is not specified"), // TODO change to 400
                   @ApiResponse(code = 403, message = "Source path is not specified"), // TODO change to 400
                   @ApiResponse(code = 403, message = "Target path is not specified"), // TODO change to 400
//                   @ApiResponse(code = 403, message = "User is not owner of source machine"),
//                   @ApiResponse(code = 403, message = "User is not owner of target machine"),
                   @ApiResponse(code = 404, message = "Source machine does not exist"),
                   @ApiResponse(code = 404, message = "Target machine does not exist"),
                   @ApiResponse(code = 500, message = "Internal server error occurred")})
    public void copyFilesBetweenMachines(@ApiParam(value = "Source machine ID", required = true)
                                         @QueryParam("sourceMachineId")
                                         String sourceMachineId,
                                         @ApiParam(value = "Target machine ID", required = true)
                                         @QueryParam("targetMachineId")
                                         String targetMachineId,
                                         @ApiParam(value = "Source path", required = true)
                                         @QueryParam("sourcePath")
                                         String sourcePath,
                                         @ApiParam(value = "Target path", required = true)
                                         @QueryParam("targetPath")
                                         String targetPath,
                                         @ApiParam(value = "Is files overwriting allowed")
                                         @QueryParam("overwrite")
                                         @DefaultValue("false")
                                         Boolean overwrite)
            throws NotFoundException,
                   ServerException,
                   ForbiddenException {

        requiredNotNull(sourceMachineId, "Source machine id");
        requiredNotNull(targetMachineId, "Target machine id");
        requiredNotNull(sourcePath, "Source path");
        requiredNotNull(targetPath, "Target path");

        final Instance sourceMachine = machineManager.getMachine(sourceMachineId);
        final Instance targetMachine = machineManager.getMachine(targetMachineId);

        checkCurrentUserPermissions(sourceMachine);
        checkCurrentUserPermissions(targetMachine);

        targetMachine.copy(sourceMachine, sourcePath, targetPath, overwrite);
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
    private void requiredNotNull(Object object, String subject) throws ForbiddenException { // TODO change to BadRequestException
        if (object == null) {
            throw new ForbiddenException(subject + " required");
        }
    }
}
