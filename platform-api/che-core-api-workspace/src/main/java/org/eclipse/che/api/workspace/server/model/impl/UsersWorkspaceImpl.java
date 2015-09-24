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
import org.eclipse.che.api.core.model.workspace.ProjectConfig;
import org.eclipse.che.api.core.model.workspace.UsersWorkspace;
import org.eclipse.che.api.core.model.workspace.WorkspaceConfig;
import org.eclipse.che.api.core.model.workspace.WorkspaceStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * Data object for {@link UsersWorkspace}.
 *
 * @author Eugene Voevodin
 * @author gazarenkov
 */
public class UsersWorkspaceImpl implements UsersWorkspace {

    public static UsersWorkspaceImplBuilder builder() {
        return new UsersWorkspaceImplBuilder();
    }

    private String                       id;
    private String                       name;
    private String                       owner;
    private String                       defaultEnvName;
    private List<CommandImpl>            commands;
    private List<ProjectConfigImpl>      projects;
    private Map<String, String>          attributes;
    private Map<String, EnvironmentImpl> environments;
    private String                       description;
    private boolean                      isTemporary;
    private WorkspaceStatus              status;

    public UsersWorkspaceImpl(String id,
                              String name,
                              String owner,
                              Map<String, String> attributes,
                              List<? extends Command> commands,
                              List<? extends ProjectConfig> projects,
                              Map<String, ? extends Environment> environments,
                              String defaultEnvironment,
                              String description) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.description = description;
        if (environments != null) {
            this.environments = environments.values()
                                            .stream()
                                            .collect(toMap(Environment::getName, EnvironmentImpl::new));
        }
        if (commands != null) {
            this.commands = commands.stream()
                                    .map(CommandImpl::new)
                                    .collect(toList());
        }
        if (projects != null) {
            this.projects = projects.stream()
                                    .map(ProjectConfigImpl::new)
                                    .collect(toList());
        }
        this.attributes = attributes;
        setDefaultEnvName(defaultEnvironment);
    }

    public UsersWorkspaceImpl(WorkspaceConfig workspaceConfig, String id, String owner) {
        this(id,
             workspaceConfig.getName(),
             owner,
             workspaceConfig.getAttributes(),
             workspaceConfig.getCommands(),
             workspaceConfig.getProjects(),
             workspaceConfig.getEnvironments(),
             workspaceConfig.getDefaultEnvName(),
             workspaceConfig.getDescription());
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getOwner() {
        return owner;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean isTemporary() {
        return isTemporary;
    }

    public void setTemporary(boolean isTemporary) {
        this.isTemporary = isTemporary;
    }

    public WorkspaceStatus getStatus() {
        return status;
    }

    public void setStatus(WorkspaceStatus status) {
        this.status = status;
    }

    @Override
    public String getDefaultEnvName() {
        return defaultEnvName;
    }

    /**
     * Sets particular environment configured for this workspace  as default
     * Throws NullPointerException if no Env with incoming name configured
     */
    public void setDefaultEnvName(String name) {
        if (environments.get(name) == null) {
            throw new NullPointerException("No Environment named '" + name + "' found");
        }
        defaultEnvName = name;
    }

    @Override
    public Map<String, String> getAttributes() {
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    @Override
    public List<CommandImpl> getCommands() {
        if (commands == null) {
            commands = new ArrayList<>();
        }
        return commands;
    }

    public void setCommands(List<CommandImpl> commands) {
        this.commands = commands;
    }

    @Override
    public List<ProjectConfigImpl> getProjects() {
        if (projects == null) {
            projects = new ArrayList<>();
        }
        return projects;
    }

    public void setProjects(List<ProjectConfigImpl> projects) {
        this.projects = projects;
    }

    @Override
    public Map<String, EnvironmentImpl> getEnvironments() {
        if (environments == null) {
            environments = new HashMap<>();
        }
        return environments;
    }

    public void setEnvironments(Map<String, EnvironmentImpl> environments) {
        this.environments = environments;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof UsersWorkspaceImpl)) return false;
        final UsersWorkspaceImpl other = (UsersWorkspaceImpl)obj;
        return Objects.equals(owner, other.owner) &&
               Objects.equals(id, other.id) &&
               Objects.equals(name, other.name) &&
               Objects.equals(defaultEnvName, other.defaultEnvName) &&
               Objects.equals(status, other.status) &&
               isTemporary == other.isTemporary &&
               commands.equals(other.commands) &&
               environments.equals(other.environments) &&
               projects.equals(other.projects) &&
               attributes.equals(other.attributes);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + Objects.hashCode(owner);
        hash = 31 * hash + Objects.hashCode(id);
        hash = 31 * hash + Objects.hashCode(name);
        hash = 31 * hash + Objects.hashCode(defaultEnvName);
        hash = 31 * hash + Objects.hashCode(status);
        hash = 31 * hash + Boolean.hashCode(isTemporary);
        hash = 31 * hash + getCommands().hashCode();
        hash = 31 * hash + getEnvironments().hashCode();
        hash = 31 * hash + getProjects().hashCode();
        hash = 31 * hash + getAttributes().hashCode();
        return hash;
    }

    /**
     * Helps to build complex {@link UsersWorkspaceImpl users workspace instance}.
     *
     * @see UsersWorkspaceImpl#builder()
     */
    public static class UsersWorkspaceImplBuilder {

        protected String                             id;
        protected String                             name;
        protected String                             owner;
        protected String                             defaultEnvName;
        protected List<? extends Command>            commands;
        protected List<? extends ProjectConfig>      projects;
        protected Map<String, String>                attributes;
        protected Map<String, ? extends Environment> environments;
        protected String                             description;
        protected boolean                            isTemporary;
        protected WorkspaceStatus                    status;

        UsersWorkspaceImplBuilder() {
        }

        public UsersWorkspaceImpl build() {
            final UsersWorkspaceImpl workspace = new UsersWorkspaceImpl(id,
                                                                        name,
                                                                        owner,
                                                                        attributes,
                                                                        commands,
                                                                        projects,
                                                                        environments,
                                                                        defaultEnvName,
                                                                        description);
            workspace.setStatus(status);
            workspace.setTemporary(isTemporary);
            return workspace;
        }

        public UsersWorkspaceImplBuilder setId(String id) {
            this.id = id;
            return this;
        }

        public UsersWorkspaceImplBuilder setName(String name) {
            this.name = name;
            return this;
        }

        public UsersWorkspaceImplBuilder setOwner(String owner) {
            this.owner = owner;
            return this;
        }

        public UsersWorkspaceImplBuilder setDefaultEnvName(String defaultEnvName) {
            this.defaultEnvName = defaultEnvName;
            return this;
        }

        public UsersWorkspaceImplBuilder setCommands(List<? extends Command> commands) {
            this.commands = commands;
            return this;
        }

        public UsersWorkspaceImplBuilder setProjects(List<? extends ProjectConfig> projects) {
            this.projects = projects;
            return this;
        }

        public UsersWorkspaceImplBuilder setAttributes(Map<String, String> attributes) {
            this.attributes = attributes;
            return this;
        }

        public UsersWorkspaceImplBuilder setEnvironments(Map<String, ? extends Environment> environments) {
            this.environments = environments;
            return this;
        }

        public UsersWorkspaceImplBuilder setDescription(String description) {
            this.description = description;
            return this;
        }

        public UsersWorkspaceImplBuilder setTemporary(boolean isTemporary) {
            this.isTemporary = isTemporary;
            return this;
        }

        public UsersWorkspaceImplBuilder setStatus(WorkspaceStatus status) {
            this.status = status;
            return this;
        }
    }
}
