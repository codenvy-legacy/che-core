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
package org.eclipse.che.api.machine.server.command;

import org.eclipse.che.api.machine.shared.ManagedCommand;

import java.util.Objects;

/**
 * Implementation of {@link ManagedCommand}
 *
 * @author Eugene Voevodin
 */
public class CommandImpl implements ManagedCommand {

    private String id;
    private String name;
    private String commandLine;
    private String creator;
    private String workspaceId;
    private String visibility;
    private String type;
    private String workingDir;

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public CommandImpl withId(String id) {
        this.id = id;
        return this;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CommandImpl withName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public String getCommandLine() {
        return commandLine;
    }

    public void setCommandLine(String commandLine) {
        this.commandLine = commandLine;
    }

    public CommandImpl withCommandLine(String commandLine) {
        this.commandLine = commandLine;
        return this;
    }

    @Override
    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public CommandImpl withCreator(String creator) {
        this.creator = creator;
        return this;
    }

    @Override
    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public CommandImpl withWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
        return this;
    }

    @Override
    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public CommandImpl withVisibility(String visibility) {
        this.visibility = visibility;
        return this;
    }

    @Override
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public CommandImpl withType(String type) {
        this.type = type;
        return this;
    }

    @Override
    public String getWorkingDir() {
        return workingDir;
    }

    public void setWorkingDir(String workingDir) {
        this.workingDir = workingDir;
    }

    public CommandImpl withWorkingDir(String workingDir) {
        this.workingDir = workingDir;
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CommandImpl)) {
            return false;
        }
        final CommandImpl command = (CommandImpl)obj;
        return Objects.equals(id, command.id) &&
               Objects.equals(name, command.name) &&
               Objects.equals(commandLine, command.commandLine) &&
               Objects.equals(creator, command.creator) &&
               Objects.equals(workspaceId, command.workspaceId) &&
               Objects.equals(visibility, command.visibility) &&
               Objects.equals(type, command.type) &&
               Objects.equals(workingDir, command.workingDir);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + Objects.hashCode(id);
        hash = 31 * hash + Objects.hashCode(name);
        hash = 31 * hash + Objects.hashCode(commandLine);
        hash = 31 * hash + Objects.hashCode(creator);
        hash = 31 * hash + Objects.hashCode(workspaceId);
        hash = 31 * hash + Objects.hashCode(visibility);
        hash = 31 * hash + Objects.hashCode(type);
        hash = 31 * hash + Objects.hashCode(workingDir);
        return hash;
    }

    @Override
    public String toString() {
        return "CommandImpl{" +
               "id='" + id + '\'' +
               ", name='" + name + '\'' +
               ", commandLine='" + commandLine + '\'' +
               ", creator='" + creator + '\'' +
               ", workspaceId='" + workspaceId + '\'' +
               ", visibility='" + visibility + '\'' +
               ", type='" + type + '\'' +
               ", workingDir='" + workingDir + '\'' +
               '}';
    }
}
