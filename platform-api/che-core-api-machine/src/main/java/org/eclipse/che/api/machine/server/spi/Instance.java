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
package org.eclipse.che.api.machine.server.spi;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.util.LineConsumer;
import org.eclipse.che.api.machine.server.exception.MachineException;
import org.eclipse.che.api.machine.shared.Machine;
import org.eclipse.che.api.machine.shared.MachineStatus;
import org.eclipse.che.api.machine.shared.ProjectBinding;
import org.eclipse.che.api.machine.shared.Server;

import java.util.List;
import java.util.Map;

/**
 * Representation of machine instance in implementation specific way.
 *
 * @author gazarenkov
 * @author Alexander Garagatyi
 */
public interface Instance extends Machine {

    void setStatus(MachineStatus status);

    LineConsumer getLogger();

    /**
     * Get metadata of the instance
     *
     * @throws MachineException
     *         if error occurs on retrieving metadata
     */
    InstanceMetadata getMetadata() throws MachineException;

    /**
     * Returns mapping of exposed ports to {link Server}
     */
    Map<String, Server> getServers() throws MachineException;

    /**
     * Get {@link InstanceProcess} by its id
     *
     * @param pid
     *         id of the process
     * @throws NotFoundException
     *         if process with specified id is not found. Process can be finished already or doesn't exist.
     * @throws MachineException
     *         if any other error occurs
     */
    InstanceProcess getProcess(int pid) throws NotFoundException, MachineException;

    /**
     * Get list of all running processes in the instance
     *
     * @return list of running processes or empty list if no process is running
     * @throws MachineException
     *         if any error occurs on the processes list retrieving
     */
    List<InstanceProcess> getProcesses() throws MachineException;

    /**
     * Create process from command line.
     * Returned {@link InstanceProcess#getPid()} should return unique pid on this stage.
     * This pid allow to control process from clients and save process logs if needed.
     *
     * @param commandLine
     *         command line from which process should be created
     * @return {@link InstanceProcess} with unique pid, that can't be used in future for other process of instance
     * @throws MachineException
     *         if error occurs on creating process
     */
    InstanceProcess createProcess(String commandLine) throws MachineException;

    /**
     * Save state of the instance
     *
     * @param owner
     *         id of the user that is owner of the snapshot
     * @param label
     *         description of the snapshot
     * @return {@code InstanceSnapshotKey} that describe implementation specific keys of snapshot
     * @throws MachineException
     *         if error occurs on storing state of the instance
     */
    InstanceKey saveToSnapshot(String owner, String label) throws MachineException;

    /**
     * Destroy instance
     *
     * @throws MachineException
     *         if error occurs on instance destroying
     */
    void destroy() throws MachineException;

    /**
     * Binds project to machine instance
     *
     * @param project
     *         project that should be bound to machine instance
     */
    void bindProject(ProjectBinding project) throws MachineException;

    /**
     * Unbinds project from machine instance
     *
     * @param project
     *         project that should be unbound from machine instance
     */
    void unbindProject(ProjectBinding project) throws MachineException, NotFoundException;

    /**
     * Returns {@link InstanceNode} that represents server where machine is launched
     */
    InstanceNode getNode();
}
