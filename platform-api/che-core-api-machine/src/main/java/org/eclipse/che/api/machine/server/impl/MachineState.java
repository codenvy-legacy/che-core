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
package org.eclipse.che.api.machine.server.impl;

import org.eclipse.che.api.machine.shared.MachineStatus;
import org.eclipse.che.api.machine.shared.ProjectBinding;

import java.util.Collections;
import java.util.Set;

/**
 * Describe state of machine
 *
 * @author Alexander Garagatyi
 */
public class MachineState {
    private final String  id;
    private final String  type;
    private final String  owner;
    private final String  workspaceId;
    private final boolean isWorkspaceBound;
    private final String  displayName;

    private MachineStatus status;

    public MachineState(String id,
                        String type,
                        String workspaceId,
                        String owner,
                        boolean isWorkspaceBound,
                        String displayName,
                        MachineStatus status) {
        this.id = id;
        this.type = type;
        this.owner = owner;
        this.workspaceId = workspaceId;
        this.isWorkspaceBound = isWorkspaceBound;
        this.displayName = displayName;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getOwner() {
        return owner;
    }

    public MachineStatus getStatus() {
        return this.status;
    }

    public Set<ProjectBinding> getProjects() {
        return Collections.emptySet();
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isWorkspaceBound() {
        return isWorkspaceBound;
    }
}
