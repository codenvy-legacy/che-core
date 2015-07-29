package org.eclipse.che.api.workspace.server.model.impl;


import org.eclipse.che.api.core.model.machine.Command;
import org.eclipse.che.api.core.model.workspace.Environment;
import org.eclipse.che.api.core.model.workspace.ProjectConfig;
import org.eclipse.che.api.core.model.workspace.UsersWorkspace;
import org.eclipse.che.api.core.model.workspace.WorkspaceConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author gazarenkov
 */
public class UsersWorkspaceImpl implements UsersWorkspace {

    private final String                   id;
    private       String                   name;
    private final String                   owner;
    private       Map<String, String>      attributes;
    private final List<Command>            commands;
    private final List<ProjectConfig>      projects;
    private final Map<String, Environment> environments;
    private       String                   defaultEnvironment;

    public UsersWorkspaceImpl(String id,
                              String name,
                              String owner,
                              Map<String, String> attributes,
                              List<Command> commands,
                              List<ProjectConfig> projects,
                              Map<String, Environment> environments,
                              String defaultEnvironment) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.attributes = attributes;
        this.commands = commands;
        this.projects = projects;
        this.environments = environments != null ? environments : new HashMap<String, Environment>();
        this.defaultEnvironment = defaultEnvironment;
    }

    public static UsersWorkspaceImpl from(WorkspaceConfig workspaceConfig) {
        return new UsersWorkspaceImpl(null,
                                     workspaceConfig.getName(),
                                     null,
                                     workspaceConfig.getAttributes(),
                                     (List<Command>)workspaceConfig.getCommands(),
                                     (List<ProjectConfig>)workspaceConfig.getProjects(),
                                     (Map<String, Environment>)workspaceConfig.getEnvironments(),
                                     workspaceConfig.getDefaultEnvironment());
    }


    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Map<String, String> getAttributes() {
        return attributes;
    }

    @Override
    public List<Command> getCommands() {
        return commands;
    }

    @Override
    public List<ProjectConfig> getProjects() {
        return projects;
    }

    @Override
    public String getDefaultEnvironment() {
        return defaultEnvironment;
    }

    /**
     * Sets particular environment configured for this workspace  as default
     * Throws NullPointerException if no Env with incoming name configured
     */
    public void setDefaultEnvironment(String name) {
        if (this.environments.get(name) == null) {
            throw new NullPointerException("No Environment named '" + name + "' found");
        }
        this.defaultEnvironment = name;
    }

    @Override
    public Map<String, Environment> getEnvironments() {
        return environments;
    }


    public void setName(String name) {
        this.name = name;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getOwner() {
        return this.owner;
    }
}
