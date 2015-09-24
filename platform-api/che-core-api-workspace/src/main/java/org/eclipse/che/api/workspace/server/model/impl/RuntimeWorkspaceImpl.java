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
import org.eclipse.che.api.core.model.workspace.WorkspaceStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

/**
 * Data object for {@link RuntimeWorkspace}.
 *
 * @author Eugene Voevodin
 * @author Alexander Garagatyi
 */
public class RuntimeWorkspaceImpl extends UsersWorkspaceImpl implements RuntimeWorkspace {

    public static RuntimeWorkspaceBuilder builder() {
        return new RuntimeWorkspaceBuilder();
    }

    private final String rootFolder;

    private MachineImpl       devMachine;
    private List<MachineImpl> machines;
    private String            activeEnvName;

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
                                String rootFolder,
                                String currentEnvironment,
                                WorkspaceStatus status) {
        super(id, name, owner, attributes, commands, projects, environments, defaultEnvironment, description);
        if (devMachine != null) {
            this.devMachine = new MachineImpl(devMachine);
        }
        this.activeEnvName = currentEnvironment;
        this.rootFolder = rootFolder;
        setStatus(status);
        if (machines != null) {
            this.machines = machines.stream()
                                    .map(MachineImpl::new)
                                    .collect(toList());
        }
    }

    public RuntimeWorkspaceImpl(UsersWorkspace usersWorkspace, String rootFolder, String activeEnvName) {
        this(usersWorkspace.getId(),
             usersWorkspace.getName(),
             usersWorkspace.getOwner(),
             usersWorkspace.getAttributes(),
             usersWorkspace.getCommands(),
             usersWorkspace.getProjects(),
             usersWorkspace.getEnvironments(),
             usersWorkspace.getDefaultEnvName(),
             usersWorkspace.getDescription(),
             null,
             null,
             rootFolder,
             activeEnvName,
             usersWorkspace.getStatus());
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
    public List<MachineImpl> getMachines() {
        if (machines == null) {
            machines = new ArrayList<>();
        }
        return machines;
    }

    @Override
    public String getRootFolder() {
        return rootFolder;
    }

    @Override
    public String getActiveEnvName() {
        return activeEnvName;
    }

    public void setDevMachine(MachineImpl devMachine) {
        this.devMachine = devMachine;
    }

    public void setMachines(List<MachineImpl> machines) {
        this.machines = machines;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RuntimeWorkspaceImpl)) return false;
        if (!super.equals(o)) return false;
        RuntimeWorkspaceImpl that = (RuntimeWorkspaceImpl)o;
        return Objects.equals(devMachine, that.devMachine) &&
               Objects.equals(machines, that.machines) &&
               Objects.equals(activeEnvName, that.activeEnvName) &&
               Objects.equals(rootFolder, that.rootFolder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), devMachine, machines, rootFolder);
    }

    /**
     * Helps to build complex {@link RuntimeWorkspaceImpl runtime workspace}.
     *
     * @see RuntimeWorkspaceImpl#builder()
     */
    public static class RuntimeWorkspaceBuilder extends UsersWorkspaceImplBuilder {

        private String                             rootFolder;
        private String                             activeEnvName;
        private Machine                            devMachine;
        private List<? extends Machine>            machines;

        public RuntimeWorkspaceImpl build() {
            return new RuntimeWorkspaceImpl(id,
                                            name,
                                            owner,
                                            attributes,
                                            commands,
                                            projects,
                                            environments,
                                            defaultEnvName,
                                            description,
                                            devMachine,
                                            machines,
                                            rootFolder,
                                            activeEnvName,
                                            status);
        }

        public RuntimeWorkspaceBuilder fromWorkspace(UsersWorkspace workspace) {
            this.id = workspace.getId();
            this.name = workspace.getName();
            this.owner = workspace.getOwner();
            this.description = workspace.getDescription();
            this.defaultEnvName = workspace.getDefaultEnvName();
            this.commands = workspace.getCommands();
            this.projects = workspace.getProjects();
            this.environments = workspace.getEnvironments();
            this.attributes = workspace.getAttributes();
            return this;
        }

        public RuntimeWorkspaceBuilder setId(String id) {
            this.id = id;
            return this;
        }

        public RuntimeWorkspaceBuilder setName(String name) {
            this.name = name;
            return this;
        }

        public RuntimeWorkspaceBuilder setOwner(String owner) {
            this.owner = owner;
            return this;
        }

        public RuntimeWorkspaceBuilder setDescription(String description) {
            this.description = description;
            return this;
        }

        public RuntimeWorkspaceBuilder setDefaultEnvName(String defaultEnvName) {
            this.defaultEnvName = defaultEnvName;
            return this;
        }

        public RuntimeWorkspaceBuilder setRootFolder(String rootFolder) {
            this.rootFolder = rootFolder;
            return this;
        }

        public RuntimeWorkspaceBuilder setActiveEnvName(String activeEnvName) {
            this.activeEnvName = activeEnvName;
            return this;
        }

        public RuntimeWorkspaceBuilder setDevMachine(Machine devMachine) {
            this.devMachine = devMachine;
            return this;
        }

        public RuntimeWorkspaceBuilder setStatus(WorkspaceStatus status) {
            this.status = status;
            return this;
        }

        public RuntimeWorkspaceBuilder setAttributes(Map<String, String> attributes) {
            this.attributes = attributes;
            return this;
        }

        public RuntimeWorkspaceBuilder setCommands(List<? extends Command> commands) {
            this.commands = commands;
            return this;
        }

        public RuntimeWorkspaceBuilder setProjects(List<? extends ProjectConfig> projects) {
            this.projects = projects;
            return this;
        }

        public RuntimeWorkspaceBuilder setEnvironments(Map<String, ? extends Environment> environments) {
            this.environments = environments;
            return this;
        }

        public RuntimeWorkspaceBuilder setMachines(List<? extends Machine> machines) {
            this.machines = machines;
            return this;
        }
    }
}
