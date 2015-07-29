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

/**
 * Implementation of {@link UsersWorkspace} which
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
                                      workspaceConfig.getDefaultEnvironment());
    }

    private String                   id;
    private String                   name;
    private String                   owner;
    private String                   defaultEnvironment;
    private List<Command>            commands;
    private List<ProjectConfig>      projects;
    private Map<String, String>      attributes;
    private Map<String, Environment> environments;

    public UsersWorkspaceImpl(String id,
                              String name,
                              String owner,
                              Map<String, String> attributes,
                              List<? extends Command> commands,
                              List<? extends ProjectConfig> projects,
                              Map<String, ? extends Environment> environments,
                              String defaultEnvironment) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        setEnvironments(environments);
        setCommands(commands);
        setProjects(projects);
        setAttributes(attributes);
        setDefaultEnvironment(defaultEnvironment);
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
        if (environments.get(name) == null) {
            throw new NullPointerException("No Environment named '" + name + "' found");
        }
        defaultEnvironment = name;
    }

    @Override
    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        if (attributes == null) {
            this.attributes = new HashMap<>();
        } else {
            this.attributes = new HashMap<>(attributes);
        }
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

    public void setOwner(String owner) {
        this.owner = owner;
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
