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

import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.machine.MachineConfig;
import org.eclipse.che.api.workspace.server.model.impl.MachineImpl;

/**
 * @author Alexander Garagatyi
 */
public interface MachineClient {
    MachineImpl start(MachineConfig machineConfig, String workspaceId, String envName)
            throws ServerException, BadRequestException, NotFoundException, ConflictException;

    void destroy(String machineId) throws NotFoundException, ServerException;
}
