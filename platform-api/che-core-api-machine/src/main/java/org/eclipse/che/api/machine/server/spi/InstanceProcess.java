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

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.util.LineConsumer;
import org.eclipse.che.api.machine.server.MachineException;

/**
 * Represents process in the machine created by command.
 *
 * @author andrew00x
 * @author Alexander Garagatyi
 */
public interface InstanceProcess {
    /**
     * Returns pid of the process.
     * To be able to control from the clients pid should be valid even if process isn't started yet.
     *
     * @return pid of the process
     */
    int getPid();

    /**
     * Returns command with all its arguments
     *
     * @return command
     */
    String getCommandLine();

    /**
     * Starts process in the background.
     *
     * @throws org.eclipse.che.api.core.ConflictException
     *         if process is started already
     * @throws MachineException
     *         if internal error occurs
     * @see #start()
     * @see #isAlive()
     */
    void start() throws ConflictException, MachineException;

    /**
     * Starts process.
     *
     * @param output
     *         consumer for process' output. If this parameter is {@code null} process started in the background. If this parameter is
     *         specified then this method is blocked until process is running.
     * @throws org.eclipse.che.api.core.ConflictException
     *         if process is started already
     * @throws MachineException
     *         if internal error occurs
     */
    void start(LineConsumer output) throws ConflictException, MachineException;

    /**
     * Checks is process is running or not.
     *
     * @return {@code true} if process running and {@code false} otherwise
     */
    boolean isAlive();

    /**
     * Kills this process.
     *
     * @throws MachineException
     *         if internal error occurs
     */
    void kill() throws MachineException;
}
