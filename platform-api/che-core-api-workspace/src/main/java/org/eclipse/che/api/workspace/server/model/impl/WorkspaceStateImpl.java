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
package org.eclipse.che.api.workspace.server.model.impl;

import org.eclipse.che.api.core.model.workspace.UsersWorkspace;
import org.eclipse.che.api.core.model.workspace.WorkspaceState;

/**
 * @author Alexander Garagatyi
 */
public class WorkspaceStateImpl implements WorkspaceState {
    private final String          id;
    private final String          name;
    private final String          owner;
    private final boolean         isTemporary;
    private final WorkspaceStatus status;

    public WorkspaceStateImpl(String id,
                              String name,
                              String owner,
                              boolean isTemporary,
                              WorkspaceStatus status) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.isTemporary = isTemporary;
        this.status = status;
    }

    public WorkspaceStateImpl(UsersWorkspace usersWorkspace, boolean isTemporary, WorkspaceStatus status) {
        this(usersWorkspace.getId(), usersWorkspace.getName(), usersWorkspace.getOwner(), isTemporary, status);
    }

    @Override
    public boolean isTemporary() {
        return isTemporary;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getOwner() {
        return owner;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public WorkspaceStatus getStatus() {
        return status;
    }
}
