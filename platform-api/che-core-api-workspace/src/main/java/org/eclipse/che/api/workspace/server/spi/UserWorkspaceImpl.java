package org.eclipse.che.api.workspace.server.spi;


import org.eclipse.che.api.core.model.machine.Command;
import org.eclipse.che.api.core.model.workspace.Environment;
import org.eclipse.che.api.core.model.workspace.ProjectConfig;
import org.eclipse.che.api.core.model.workspace.UsersWorkspace;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author gazarenkov
 */
public class UserWorkspaceImpl implements UsersWorkspace {

    private final String                   id;
    private       String                   name;
    private final String                   owner;
    private       Map<String, String>      attributes;
    //private final Set<Membership> members;
    private final List<Command>            commands;
    private final List<ProjectConfig>      projects;
    private final Map<String, Environment> environments;
    private       String                   defaultEnvironment;

    public UserWorkspaceImpl(String id, String name, String owner, Map<String, String> attributes,
                             List<Command> commands, List<ProjectConfig> projects,
                             Map<String, Environment> environments, String defaultEnvironment) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.attributes = attributes;
        //this.members = members;
        this.commands = commands;
        this.projects = projects;
        if (environments == null)
            this.environments = new HashMap<>();
        else
            this.environments = environments;
        this.defaultEnvironment = defaultEnvironment;
    }

//    public WorkspaceDo(String id, String name, String owner, Map<String, String> attributes) {
//
//        this(id, name, owner, attributes, new ArrayList<Command>(),
//                new ArrayList<ProjectConfig>(), new HashMap<String, Environment>(), null);
//
//    }

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
    public void setDefaultEnvironmentName(String name) {
        if (this.environments.get(name) == null)
            throw new NullPointerException("No Environment named '" + name + "' found");
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
