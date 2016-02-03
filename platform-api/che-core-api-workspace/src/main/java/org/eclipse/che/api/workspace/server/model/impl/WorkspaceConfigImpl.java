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
import org.eclipse.che.api.core.model.workspace.WorkspaceConfig;
import org.eclipse.che.api.machine.server.model.impl.CommandImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * Implementation of {@link WorkspaceConfig}, contains information about workspace creation
 *
 * @author Eugene Voevodin
 * @author gazarenkov
 * @author Alexander Andrienko
 */
public class WorkspaceConfigImpl implements WorkspaceConfig {

    public static WorkspaceConfigBuilder builder() {
        return new WorkspaceConfigBuilder();
    }

    protected   String                            name;
    protected   String                            description;
    protected   String                            defaultEnvName;
    protected   List<CommandImpl>                 commands;
    protected   List<ProjectConfigImpl>           projects;
    protected   Map<String, EnvironmentStateImpl> environments;
    protected   Map<String, String>               attributes;

    public WorkspaceConfigImpl(String name,
                               String description,
                               String defaultEnvName,
                               List<? extends Command> commands,
                               List<? extends ProjectConfig> projects,
                               Map<String, ? extends Environment> environments,
                               Map<String, String> attributes) {
        this.name = name;
        this.description = description;

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
        if (environments != null) {
            this.environments = environments.values()
                                            .stream()
                                            .collect(toMap(Environment::getName, EnvironmentStateImpl::new));
        }
        if (attributes != null) {
            this.attributes = new HashMap<>(attributes);
        }
        setDefaultEnvName(defaultEnvName);
    }

    public WorkspaceConfigImpl(WorkspaceConfig workspaceConfig) {
        this(workspaceConfig.getName(),
             workspaceConfig.getDescription(),
             workspaceConfig.getDefaultEnvName(),
             workspaceConfig.getCommands(),
             workspaceConfig.getProjects(),
             workspaceConfig.getEnvironments(),
             workspaceConfig.getAttributes());
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
    public Map<String, EnvironmentStateImpl> getEnvironments() {
        if (environments == null) {
            environments = new HashMap<>();
        }
        return environments;
    }

    public void setEnvironments(Map<String, EnvironmentStateImpl> environments) {
        this.environments = environments;
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
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof WorkspaceConfigImpl)) return false;
        final WorkspaceConfigImpl other = (WorkspaceConfigImpl)obj;
        return Objects.equals(name, other.name) &&
               Objects.equals(defaultEnvName, other.defaultEnvName) &&
               getCommands().equals(other.getCommands()) &&
               getEnvironments().equals(other.getEnvironments()) &&
               getProjects().equals(other.getProjects()) &&
               getAttributes().equals(other.getAttributes()) &&
               Objects.equals(description, other.description);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + Objects.hashCode(name);
        hash = 31 * hash + Objects.hashCode(defaultEnvName);
        hash = 31 * hash + getCommands().hashCode();
        hash = 31 * hash + getEnvironments().hashCode();
        hash = 31 * hash + getProjects().hashCode();
        hash = 31 * hash + getAttributes().hashCode();
        hash = 31 * hash + Objects.hashCode(description);
        return hash;
    }

    @Override
    public String toString() {
        return "WorkspaceConfigImpl{" +
               " name='" + name + '\'' +
               ", description='" + description + '\'' +
               ", defaultEnvName='" + defaultEnvName + '\'' +
               ", commands=" + commands +
               ", projects=" + projects +
               ", environments=" + environments +
               ", attributes=" + attributes +
               '}';
    }

    public static class WorkspaceConfigBuilder {
        private String name;
        private String description;
        private String defaultEnvName;
        private List<? extends Command> commands;
        private List<? extends ProjectConfig> projects;
        private Map<String, ? extends Environment> environments;
        private Map<String, String> attributes;

        public WorkspaceConfigBuilder() {
        }

        public WorkspaceConfigBuilder setName(String name) {
            this.name = name;
            return this;
        }

        public WorkspaceConfigBuilder setDescription(String description) {
            this.description = description;
            return this;
        }

        public WorkspaceConfigBuilder setDefaultEnvName(String defaultEnvName) {
            this.defaultEnvName = defaultEnvName;
            return this;
        }

        public WorkspaceConfigBuilder setCommands(List<? extends Command> commands) {
            this.commands = commands;
            return this;
        }

        public WorkspaceConfigBuilder setProjects(List<? extends ProjectConfig> projects) {
            this.projects = projects;
            return this;
        }

        public WorkspaceConfigBuilder setEnvironments(Map<String, ? extends Environment> environments) {
            this.environments = environments;
            return this;
        }

        public WorkspaceConfigBuilder setAttributes(Map<String, String> attributes) {
            this.attributes = attributes;
            return this;
        }

        public WorkspaceConfigImpl build() {
            return new WorkspaceConfigImpl(name, description, defaultEnvName, commands, projects, environments, attributes);
        }
    }
}
