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
package org.eclipse.che.api.workspace.server.spi;

import org.eclipse.che.api.user.shared.model.Membership;
import org.eclipse.che.api.user.server.dao.MembershipDo;
import org.eclipse.che.api.workspace.shared.model.*;

import java.util.*;

/**
 * Workspace Data Object
 * @author gazarenkov
 */
public class WorkspaceDo implements Workspace {


    private final String id;
    private String name;
    private final boolean temporary;
    private Map<String, String> attributes;
    private final Set<Membership> members;
    private final List<Command> commands;
    private final List<ProjectConfig> projects;
    private final Map<String, Environment> environments;
    private String defaultEnvironment;

    public WorkspaceDo(String id, String name, boolean temporary, Map<String, String> attributes,
                       Set<Membership> members, List<Command> commands, List<ProjectConfig> projects,
                       Map<String, Environment> environments, String defaultEnvironment) {
        this.id = id;
        this.name = name;
        this.temporary = temporary;
        this.attributes = attributes;
        this.members = members;
        this.commands = commands;
        this.projects = projects;
        if(environments == null)
            this.environments = new HashMap<String, Environment>();
        else
            this.environments = environments;
        this.defaultEnvironment = defaultEnvironment;
    }

    public WorkspaceDo(String id, String name, boolean temporary, Map<String, String> attributes,
                       Membership owner) {

        this(id, name, temporary, attributes, new HashSet<Membership>(), new ArrayList<Command>(),
                new ArrayList<ProjectConfig>(), new HashMap<String, Environment>(), null);

        members.add(owner);
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
    public boolean isTemporary() {
        return temporary;
    }

    @Override
    public Set<Membership> getMembers() {
        return members;
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
    public Environment getDefaultEnvironment() {
        return environments.get(defaultEnvironment);
    }

    /**
     * Sets particular environment configured for this workspace  as default
     * Throws NullPointerException if no Env with incoming name configured
     * @param name - name
     */
    public void setDefaultEnvironmentName(String name) {
        if(this.environments.get(name) == null)
            throw new NullPointerException("No Environment named '"+name+"' found");
        this.defaultEnvironment = name;
    }

    @Override
    public Map<String, Environment> getEnvironments() {
        return environments;
    }


    @Override
    public void setName(String name) {
        this.name = name;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }
}
