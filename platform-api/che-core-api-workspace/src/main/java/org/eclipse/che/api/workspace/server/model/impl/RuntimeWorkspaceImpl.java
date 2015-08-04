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

import org.eclipse.che.api.core.model.machine.Command;
import org.eclipse.che.api.core.model.workspace.Environment;
import org.eclipse.che.api.core.model.workspace.Machine;
import org.eclipse.che.api.core.model.workspace.ProjectConfig;
import org.eclipse.che.api.core.model.workspace.RuntimeWorkspace;
import org.eclipse.che.api.core.model.workspace.UsersWorkspace;
import org.eclipse.che.api.core.model.workspace.WorkspaceState.WorkspaceStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Alexander Garagatyi
 */
public class RuntimeWorkspaceImpl extends UsersWorkspaceImpl implements RuntimeWorkspace {
    private final Machine                 devMachine;
    private final List<? extends Machine> machines;
    private final boolean                 isTemporary;
    private final String                  rootFolder;
    private final WorkspaceStatus         workspaceStatus;
    private final String                  currentEnvironment;

    public RuntimeWorkspaceImpl(String id,
                                String name,
                                String owner,
                                Map<String, String> attributes,
                                List<? extends Command> commands,
                                List<? extends ProjectConfig> projects,
                                Map<String, ? extends Environment> environments,
                                String defaultEnvironment,
                                String description,
                                Machine devMachine,
                                List<? extends Machine> machines,
                                boolean isTemporary,
                                String rootFolder,
                                WorkspaceStatus workspaceStatus,
                                String currentEnvironment) {
        super(id, name, owner, attributes, commands, projects, environments, defaultEnvironment, description);
        this.devMachine = devMachine;
        this.currentEnvironment = currentEnvironment;
        this.machines = machines != null ? machines : new ArrayList<Machine>();
        this.isTemporary = isTemporary;
        this.rootFolder = rootFolder;
        this.workspaceStatus = workspaceStatus;
    }

    public RuntimeWorkspaceImpl(UsersWorkspace usersWorkspace,
                                Machine devMachine,
                                List<? extends Machine> machines,
                                boolean isTemporary,
                                String rootFolder,
                                WorkspaceStatus workspaceStatus,
                                String currentEnvironment) {
        this(usersWorkspace.getId(),
             usersWorkspace.getName(),
             usersWorkspace.getOwner(),
             usersWorkspace.getAttributes(),
             usersWorkspace.getCommands(),
             usersWorkspace.getProjects(),
             usersWorkspace.getEnvironments(),
             usersWorkspace.getDefaultEnvironment(),
             usersWorkspace.getDescription(),
             devMachine,
             machines,
             isTemporary,
             rootFolder,
             workspaceStatus,
             currentEnvironment);
    }

    @Override
    public String getDescription() {
        return null;// TODO
    }

    @Override
    public Machine getDevMachine() {
        return devMachine;
    }

    @Override
    public List<? extends Machine> getMachines() {
        return machines;
    }

    @Override
    public boolean isTemporary() {
        return isTemporary;
    }

    @Override
    public String getRootFolder() {
        return rootFolder;
    }

    @Override
    public WorkspaceStatus getStatus() {
        return workspaceStatus;
    }

    @Override
    public String getCurrentEnvironment() {
        return currentEnvironment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RuntimeWorkspaceImpl)) return false;
        if (!super.equals(o)) return false;
        RuntimeWorkspaceImpl that = (RuntimeWorkspaceImpl)o;
        return Objects.equals(isTemporary, that.isTemporary) &&
               Objects.equals(devMachine, that.devMachine) &&
               Objects.equals(machines, that.machines) &&
               Objects.equals(rootFolder, that.rootFolder) &&
               Objects.equals(workspaceStatus, that.workspaceStatus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), devMachine, machines, isTemporary, rootFolder, workspaceStatus);
    }
}
