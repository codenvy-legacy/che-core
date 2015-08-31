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

import org.eclipse.che.api.machine.shared.Machine;
import org.eclipse.che.api.machine.shared.MachineStatus;
import org.eclipse.che.api.machine.shared.ProjectBinding;
import org.eclipse.che.api.machine.shared.Recipe;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Describe state of machine
 *
 * @author Alexander Garagatyi
 */
public class MachineImpl implements Machine {
    private final String  id;
    private final String  type;
    private final String  owner;
    private final Recipe  recipe;
    private final String  workspaceId;
    private final boolean isDev;
    private final String  displayName;
    private final int     memorySizeMB;

    private MachineStatus status;

    public MachineImpl(String id,
                       String type,
                       Recipe recipe,
                       String workspaceId,
                       String owner,
                       boolean isDev,
                       String displayName,
                       int memorySizeMB,
                       MachineStatus status) {
        this.id = id;
        this.type = type;
        this.owner = owner;
        this.recipe = recipe;
        this.workspaceId = workspaceId;
        this.isDev = isDev;
        this.displayName = displayName;
        this.memorySizeMB = memorySizeMB;
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

    public Recipe getRecipe() {
        return recipe;
    }

    public MachineStatus getStatus() {
        return this.status;
    }

    public List<ProjectBinding> getProjects() {
        return Collections.emptyList();
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isDev() {
        return isDev;
    }

    public int getMemorySize() {
        return memorySizeMB;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MachineImpl)) return false;
        MachineImpl state = (MachineImpl)o;
        return Objects.equals(isDev, state.isDev) &&
               Objects.equals(memorySizeMB, state.memorySizeMB) &&
               Objects.equals(id, state.id) &&
               Objects.equals(type, state.type) &&
               Objects.equals(recipe, state.recipe) &&
               Objects.equals(owner, state.owner) &&
               Objects.equals(workspaceId, state.workspaceId) &&
               Objects.equals(displayName, state.displayName) &&
               Objects.equals(status, state.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, recipe, owner, workspaceId, isDev, displayName, memorySizeMB, status);
    }
}
