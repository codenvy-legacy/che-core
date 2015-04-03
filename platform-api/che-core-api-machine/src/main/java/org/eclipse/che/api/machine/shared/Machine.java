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

import org.eclipse.che.api.machine.server.MachineException;
import org.eclipse.che.api.machine.server.spi.InstanceMetadata;

import java.util.Set;

/**
 * @author gazarenkov
 */
public interface Machine {

    /**
     *
     * @return unique ID of this machine
     */
    String getId();

    /**
     *
     * @return machine type (i.e. "docker")
     */
    String getType();

    /**
     * Gets identifier of user who launched this machine.
     *
     * @return identifier of user who launched this machine
     */
    String getOwner();

    /**
     *
     * @return list of projects bound to this machine
     */
    Set<? extends ProjectBinding> getProjects();

    /**
     *
     * @return id of workspace this machine belongs to
     */
    String getWorkspaceId();

    /**
     *
     * @return machine specific metadata
     */
    InstanceMetadata getMetadata() throws MachineException;
}
