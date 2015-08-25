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

/**
 * Data object for {@link UsersWorkspace}.
 *
 * @author Eugene Voevodin
 * @author gazarenkov
 */
public class UsersWorkspaceImpl implements UsersWorkspace {

    private String                   id;
    private String                   name;
    private String                   owner;
    private String                   defaultEnvName;
    private List<Command>            commands;
    private List<ProjectConfig>      projects;
    private Map<String, String>      attributes;
    private Map<String, Environment> environments;
    private String                   description;
    private boolean                  isTemporary;
    private WorkspaceStatus          status;

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
        setEnvironments(environments);
        setCommands(commands);
        setProjects(projects);
        setAttributes(attributes);
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
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes == null ? new HashMap<String, String>() : new HashMap<>(attributes);
    }

    @Override
    public List<Command> getCommands() {
        return commands;
    }

    public void setCommands(List<? extends Command> commands) {
        this.commands = commands == null ? new ArrayList<Command>() : new ArrayList<>(commands);
    }

    @Override
    public List<ProjectConfig> getProjects() {
        return projects;
    }

    public void setProjects(List<? extends ProjectConfig> projects) {
        this.projects = projects == null ? new ArrayList<ProjectConfig>() : new ArrayList<>(projects);
    }

    @Override
    public Map<String, Environment> getEnvironments() {
        return environments;
    }

    public void setEnvironments(Map<String, ? extends Environment> environments) {
        this.environments = environments == null ? new HashMap<String, Environment>() : new HashMap<>(environments);
    }

    @Override
    public String getOwner() {
        return this.owner;
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
        hash = 31 * hash + (isTemporary ? 123 : 321);
        hash = 31 * hash + commands.hashCode();
        hash = 31 * hash + environments.hashCode();
        hash = 31 * hash + projects.hashCode();
        hash = 31 * hash + attributes.hashCode();
        return hash;
    }
}
