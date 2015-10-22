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
package org.eclipse.che.api.machine.shared;

/**
 * Describe process running in a machine
 *
 * @author andrew00x
 */
public interface MachineProcess {
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
     * Checks is process is running or not.
     *
     * @return {@code true} if process running and {@code false} otherwise
     */
    boolean isAlive();
}
