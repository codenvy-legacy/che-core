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
package org.eclipse.che.api.machine.gwt.client;

import org.eclipse.che.api.machine.shared.dto.MachineDto;
import org.eclipse.che.api.machine.shared.dto.MachineProcessDto;
import org.eclipse.che.api.machine.shared.dto.MachineStateDto;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.commons.annotation.Nullable;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Client for Machine API.
 *
 * @author Artem Zatsarynnyy
 * @author Dmitry Shnurenko
 */
public interface MachineServiceClient {
    /**
     * Get machine information by it's id.
     *
     * @param machineId
     *         ID of the machine
     */
    Promise<MachineDto> getMachine(@NotNull String machineId);

    /**
     * Get machine state information by it's id.
     *
     * @param machineId
     *         ID of the machine
     */
    Promise<MachineStateDto> getMachineState(@NotNull String machineId);

    /**
     * Returns list of machines which are bounded to the specified workspace.
     *
     * @param workspaceId
     *         workspace id
     * @return list of machines
     */
    Promise<List<MachineDto>> getWorkspaceMachines(String workspaceId);

    /**
     * Find machines states bound to the workspace.
     *
     * @param workspaceId workspace id
     */
    Promise<List<MachineStateDto>> getMachinesStates(@NotNull String workspaceId);

    /**
     * Destroy machine with the specified ID.
     *
     * @param machineId
     *         ID of machine that should be destroyed
     */
    Promise<Void> destroyMachine(@NotNull String machineId);

    /**
     * Execute a command in machine.
     *
     * @param machineId
     *         ID of the machine where command should be executed
     * @param commandLine
     *         command line that should be executed in the machine
     * @param outputChannel
     *         websocket chanel for execution logs
     */
    Promise<MachineProcessDto> executeCommand(@NotNull String machineId, @NotNull String commandLine, @Nullable String outputChannel);

    /**
     * Get processes from the specified machine.
     *
     * @param machineId
     *         ID of machine to get processes information from
     */
    Promise<List<MachineProcessDto>> getProcesses(@NotNull String machineId);

    /**
     * Stop process in machine.
     *
     * @param machineId
     *         ID of the machine where process should be stopped
     * @param processId
     *         ID of the process to stop
     */
    Promise<Void> stopProcess(@NotNull String machineId, int processId);

    /**
     * Bind project to machine.
     *
     * @param machineId
     *         machine where project should be bound
     * @param projectPath
     *         project that should be bound
     */
    Promise<Void> bindProject(@NotNull String machineId, @NotNull String projectPath);

    /**
     * Unbind project from machine.
     *
     * @param machineId
     *         machine where project should be unbound
     * @param projectPath
     *         project that should be unbound
     */
    Promise<Void> unbindProject(@NotNull String machineId, @NotNull String projectPath);

    /**
     * Get file content.
     *
     * @param machineId
     *         ID of the machine
     * @param path
     *         path to file on machine instance
     * @param startFrom
     *         line number to start reading from
     * @param limit
     *         limitation on line
     */
    Promise<String> getFileContent(@NotNull String machineId, @NotNull String path, int startFrom, int limit);
}
