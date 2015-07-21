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

import java.util.List;

/**
 * Describes machine
 *
 * @author gazarenkov
 */
public interface Machine {

    /**
     * Unique ID of this machine
     */
    String getId();

    /**
     * Machine type (i.e. "docker")
     */
    String getType();

    /**
     * Identifier of user who launched this machine.
     */
    String getOwner();

    MachineStatus getStatus();

    /**
     * List of projects bound to this machine
     */
    List<? extends ProjectBinding> getProjects();

    /**
     * Id of workspace this machine belongs to
     */
    String getWorkspaceId();

    /**
     * Is workspace bound to machine or not
     */
    boolean isWorkspaceBound();

    String getDisplayName();

    /** Get memory size (in megabytes) that is allocated for starting machine. */
    int getMemorySize();
}
