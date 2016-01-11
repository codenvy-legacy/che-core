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
package org.eclipse.che.api.machine.server.model.impl;

import java.util.Objects;

import org.eclipse.che.api.core.model.machine.Command;

/**
 * Data object for {@link Command}.
 *
 * @author Eugene Voevodin
 */
public class CommandImpl implements Command {

    private String name;
    private String commandLine;
    private String type;
    private String previewUrl;

    public CommandImpl(String name, String commandLine, String type) {
        this.name = name;
        this.commandLine = commandLine;
        this.type = type;
    }

    public CommandImpl(Command command) {
        this.name = command.getName();
        this.commandLine = command.getCommandLine();
        this.type = command.getType();
        this.previewUrl = command.getPreviewUrl();
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getCommandLine() {
        return commandLine;
    }

    public void setCommandLine(String commandLine) {
        this.commandLine = commandLine;
    }

    @Override
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String getPreviewUrl() {
        return previewUrl;
    }

    public void setPreviewUrl(String previewUrl) {
        this.previewUrl = previewUrl;
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
               Objects.equals(type, command.type) &&
               Objects.equals(previewUrl, command.previewUrl);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + Objects.hashCode(name);
        hash = 31 * hash + Objects.hashCode(commandLine);
        hash = 31 * hash + Objects.hashCode(type);
        hash = 31 * hash + Objects.hashCode(previewUrl);
        return hash;
    }

    @Override
    public String toString() {
        return "CommandImpl{" +
               "name='" + name + '\'' +
               ", commandLine='" + commandLine + '\'' +
               ", type='" + type + '\'' +
               ", previewUrl='" + previewUrl + '\'' +
               '}';
    }
}
