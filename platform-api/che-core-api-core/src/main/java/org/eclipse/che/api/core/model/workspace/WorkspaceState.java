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
package org.eclipse.che.api.core.model.workspace;

/**
 * Defines workspace state.
 *
 * @author Alexander Garagatyi
 */
public interface WorkspaceState {
    enum WorkspaceStatus {
        STARTING, RUNNING, STOPPED
    }

    /**
     * Returns true if this workspace is temporary otherwise returns false.
     */
    boolean isTemporary();

    /**
     * Returns workspace identifier.
     */
    String getId();

    /**
     * Returns workspace owner(users identifier).
     */
    String getOwner();

    /**
     * Returns workspace name.
     */
    String getName();

    /**
     * Returns status of current workspace.
     */
    WorkspaceStatus getStatus();
}
