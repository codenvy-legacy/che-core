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

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.machine.shared.ProjectBinding;

/**
 * @author Alexander Garagatyi
 *
 * @deprecated
 */
public interface MachineNode {
    void copyProjectToMachine(String machineId, ProjectBinding project) throws ServerException, NotFoundException;

    void removeProjectFromMachine(String machineId, ProjectBinding project) throws ServerException, NotFoundException;

    void startSynchronization(String machineId, ProjectBinding project) throws ServerException, NotFoundException;

    void stopSynchronization(String machineId, ProjectBinding project) throws ServerException, NotFoundException;
}
