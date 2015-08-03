/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.workspace.server.model.impl;

import org.eclipse.che.api.core.model.machine.Command;

import java.util.Objects;

//TODO move?

/**
 * Data object for {@link Command}.
 *
 * @author Eugene Voevodin
 */
public class CommandImpl implements Command {

    private String name;
    private String commandLine;
    private String visibility;
    private String type;
    private String workingDir;

    @Override
    public String getName() {
        return name;
    }

    public CommandImpl setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public String getCommandLine() {
        return commandLine;
    }

    public CommandImpl setCommandLine(String commandLine) {
        this.commandLine = commandLine;
        return this;
    }

    @Override
    public String getVisibility() {
        return visibility;
    }

    public CommandImpl setVisibility(String visibility) {
        this.visibility = visibility;
        return this;
    }

    @Override
    public String getType() {
        return type;
    }

    public CommandImpl setType(String type) {
        this.type = type;
        return this;
    }

    @Override
    public String getWorkingDir() {
        return workingDir;
    }

    public CommandImpl setWorkingDir(String workingDir) {
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
        return Objects.equals(name, command.name) &&
               Objects.equals(commandLine, command.commandLine) &&
               Objects.equals(visibility, command.visibility) &&
               Objects.equals(type, command.type) &&
               Objects.equals(workingDir, command.workingDir);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + Objects.hashCode(name);
        hash = 31 * hash + Objects.hashCode(commandLine);
        hash = 31 * hash + Objects.hashCode(visibility);
        hash = 31 * hash + Objects.hashCode(type);
        hash = 31 * hash + Objects.hashCode(workingDir);
        return hash;
    }

    @Override
    public String toString() {
        return "CommandImpl{" +
               "name='" + name + '\'' +
               ", commandLine='" + commandLine + '\'' +
               ", visibility='" + visibility + '\'' +
               ", type='" + type + '\'' +
               ", workingDir='" + workingDir + '\'' +
               '}';
    }
}
