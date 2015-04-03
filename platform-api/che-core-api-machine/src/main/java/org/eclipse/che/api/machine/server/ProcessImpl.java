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

import org.eclipse.che.api.machine.server.spi.InstanceProcess;
import org.eclipse.che.api.machine.shared.Process;

/**
 * @author andrew00x
 */
public class ProcessImpl implements Process {
    private final InstanceProcess instanceProcess;

    ProcessImpl(InstanceProcess instanceProcess) {
        this.instanceProcess = instanceProcess;
    }

    @Override
    public int getPid() {
        return instanceProcess.getPid();
    }

    @Override
    public String getCommandLine() {
        return instanceProcess.getCommandLine();
    }

    @Override
    public boolean isAlive() {
        return instanceProcess.isAlive();
    }

    public void kill() throws MachineException {
        instanceProcess.kill();
    }
}
