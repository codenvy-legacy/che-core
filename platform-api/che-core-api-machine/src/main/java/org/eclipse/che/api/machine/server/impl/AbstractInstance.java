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

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.model.machine.Recipe;
import org.eclipse.che.api.machine.server.exception.MachineException;
import org.eclipse.che.api.machine.server.spi.Instance;
import org.eclipse.che.api.machine.shared.MachineStatus;
import org.eclipse.che.api.machine.shared.ProjectBinding;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Alexander Garagatyi
 */
public abstract class AbstractInstance implements Instance {
    private final String               id;
    private final String               type;
    private final String               owner;
    private final Recipe               recipe;
    private final List<ProjectBinding> projectBindings;
    private final String               workspaceId;
    private final boolean              isWorkspaceBound;
    private final String               displayName;

    private MachineStatus status;

    public AbstractInstance(String id,
                            String type,
                            String workspaceId,
                            String owner,
                            Recipe recipe,
                            boolean isWorkspaceBound,
                            String displayName) {
        this.id = id;
        this.type = type;
        this.owner = owner;
        this.workspaceId = workspaceId;
        this.isWorkspaceBound = isWorkspaceBound;
        this.recipe = recipe;
        this.displayName = displayName;
        projectBindings = new CopyOnWriteArrayList<>();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Recipe getRecipe() {
        return recipe;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getOwner() {
        return owner;
    }

    @Override
    public List<ProjectBinding> getProjects() {
        return projectBindings;
    }

    @Override
    public String getWorkspaceId() {
        return workspaceId;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public boolean isDev() {
        return isWorkspaceBound;
    }

    @Override
    public synchronized MachineStatus getStatus() {
        return status;
    }

    @Override
    public synchronized void setStatus(MachineStatus status) {
        this.status = status;
    }

    @Override
    public void bindProject(ProjectBinding project) throws MachineException {
        if (!isWorkspaceBound) {
            if (status != MachineStatus.RUNNING) {
                throw new MachineException(String.format("Machine %s is not ready to bind the project", id));
            }

            doBindProject(project);

            this.projectBindings.add(project);
        }
    }

    protected abstract void doBindProject(ProjectBinding project) throws MachineException;

    public void unbindProject(ProjectBinding project) throws MachineException, NotFoundException {
        if (!isWorkspaceBound) {
            if (status != MachineStatus.RUNNING) {
                throw new MachineException(String.format("Machine %s is not ready to unbind the project", id));
            }

            for (ProjectBinding projectBinding : projectBindings) {
                if (projectBinding.getPath().equals(project.getPath())) {
                    doUnbindProject(project);
                    this.projectBindings.remove(project);
                    return;
                }
            }
            throw new NotFoundException(String.format("Binding of project %s in machine %s not found", project.getPath(), id));
        }
    }

    protected abstract void doUnbindProject(ProjectBinding project) throws MachineException;
}
