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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

//TODO use impls

/**
 * Data object for {@link UsersWorkspace}.
 *
 * @author Eugene Voevodin
 * @author gazarenkov
 */
public class UsersWorkspaceImpl implements UsersWorkspace {

    public static UsersWorkspaceImpl from(WorkspaceConfig workspaceConfig) {
        return new UsersWorkspaceImpl(null,
                                      workspaceConfig.getName(),
                                      null,
                                      workspaceConfig.getAttributes(),
                                      workspaceConfig.getCommands(),
                                      workspaceConfig.getProjects(),
                                      workspaceConfig.getEnvironments(),
                                      workspaceConfig.getDefaultEnvName(),
                                      workspaceConfig.getDescription());
    }

    private String                   id;
    private String                   name;
    private String                   owner;
    private String                   defaultEnvironment;
    private List<Command>            commands;
    private List<ProjectConfig>      projects;
    private Map<String, String>      attributes;
    private Map<String, Environment> environments;
    private String                   description;

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
        setDefaultEnvironment(defaultEnvironment);
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

    public UsersWorkspaceImpl setId(String id) {
        this.id = id;
        return this;
    }

    @Override
    public String getName() {
        return name;
    }

    public UsersWorkspaceImpl setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getDefaultEnvName() {
        return defaultEnvironment;
    }

    /**
     * Sets particular environment configured for this workspace  as default
     * Throws NullPointerException if no Env with incoming name configured
     */
    public UsersWorkspaceImpl setDefaultEnvironment(String name) {
        if (environments.get(name) == null) {
            throw new NullPointerException("No Environment named '" + name + "' found");
        }
        defaultEnvironment = name;
        return this;
    }

    @Override
    public Map<String, String> getAttributes() {
        return attributes;
    }

    public UsersWorkspaceImpl setAttributes(Map<String, String> attributes) {
        this.attributes = attributes == null ? new HashMap<String, String>() : new HashMap<>(attributes);
        return this;
    }

    @Override
    public List<Command> getCommands() {
        return commands;
    }

    public UsersWorkspaceImpl setCommands(List<? extends Command> commands) {
        this.commands = commands == null ? new ArrayList<Command>() : new ArrayList<>(commands);
        return this;
    }

    @Override
    public List<ProjectConfig> getProjects() {
        return projects;
    }

    public UsersWorkspaceImpl setProjects(List<? extends ProjectConfig> projects) {
        this.projects = projects == null ? new ArrayList<ProjectConfig>() : new ArrayList<>(projects);
        return this;
    }

    @Override
    public Map<String, Environment> getEnvironments() {
        return environments;
    }

    public UsersWorkspaceImpl setEnvironments(Map<String, ? extends Environment> environments) {
        this.environments = environments == null ? new HashMap<String, Environment>() : new HashMap<>(environments);
        return this;
    }

    @Override
    public Environment getEnvironment(String envName) {
        return getEnvironments().get(envName);
    }

    @Override
    public String getOwner() {
        return this.owner;
    }

    public UsersWorkspaceImpl setOwner(String owner) {
        this.owner = owner;
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof UsersWorkspaceImpl)) return false;
        final UsersWorkspaceImpl other = (UsersWorkspaceImpl)obj;
        return Objects.equals(owner, other.owner) &&
               Objects.equals(id, other.id) &&
               Objects.equals(name, other.name) &&
               Objects.equals(defaultEnvironment, other.defaultEnvironment) &&
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
        hash = 31 * hash + Objects.hashCode(defaultEnvironment);
        hash = 31 * hash + commands.hashCode();
        hash = 31 * hash + environments.hashCode();
        hash = 31 * hash + projects.hashCode();
        hash = 31 * hash + attributes.hashCode();
        return hash;
    }
}
