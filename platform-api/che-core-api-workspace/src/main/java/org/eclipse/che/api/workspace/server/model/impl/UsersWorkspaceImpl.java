/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
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

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Data object for {@link UsersWorkspace}.
 *
 * @author Eugene Voevodin
 * @author gazarenkov
 */
public class UsersWorkspaceImpl extends WorkspaceConfigImpl implements UsersWorkspace {

    public static UsersWorkspaceImplBuilder builder() {
        return new UsersWorkspaceImplBuilder();
    }

    private String                            id;
    private String                            owner;
    private boolean                           isTemporary;
    private WorkspaceStatus                   status;

    public UsersWorkspaceImpl(String id,
                              String name,
                              String owner,
                              Map<String, String> attributes,
                              List<? extends Command> commands,
                              List<? extends ProjectConfig> projects,
                              Map<String, ? extends Environment> environments,
                              String defaultEnvironment,
                              String description) {
        super(name, description, defaultEnvironment, commands, projects, environments, attributes);
        this.id = id;
        this.name = name;
        this.owner = owner;
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
    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
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
               getCommands().equals(other.getCommands()) &&
               getEnvironments().equals(other.getEnvironments()) &&
               getProjects().equals(other.getProjects()) &&
               getAttributes().equals(other.getAttributes()) &&
               Objects.equals(description, other.description);
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
        hash = 31 * hash + Objects.hashCode(description);
        return hash;
    }

    @Override
    public String toString() {
        return "UsersWorkspaceImpl{" +
               "id='" + id + '\'' +
               ", name='" + name + '\'' +
               ", owner='" + owner + '\'' +
               ", defaultEnvName='" + defaultEnvName + '\'' +
               ", commands=" + commands +
               ", projects=" + projects +
               ", attributes=" + attributes +
               ", environments=" + environments +
               ", description='" + description + '\'' +
               ", isTemporary=" + isTemporary +
               ", status=" + status +
               '}';
    }

    /**
     * Helps to build complex {@link UsersWorkspaceImpl users workspace instance}.
     *
     * @see UsersWorkspaceImpl#builder()
     */
    public static class UsersWorkspaceImplBuilder extends WorkspaceConfigBuilder {

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

        public UsersWorkspaceImplBuilder fromConfig(WorkspaceConfig workspaceConfig) {
            this.name = workspaceConfig.getName();
            this.description = workspaceConfig.getDescription();
            this.defaultEnvName = workspaceConfig.getDefaultEnvName();
            this.projects = workspaceConfig.getProjects();
            this.commands = workspaceConfig.getCommands();
            this.environments = workspaceConfig.getEnvironments();
            this.attributes = workspaceConfig.getAttributes();
            return this;
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
