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
package org.eclipse.che.git.impl.nativegit.commands;

import org.eclipse.che.api.git.GitException;

import java.io.File;

/**
 * Used for getting content of the file from specified revision or branch.
 *
 * @author Igor Vinokur
 */
public class ShowCommand extends GitCommand<String> {
    
    private String filePattern;
    private String version;

    public ShowCommand(File repositoryPlace) {
        super(repositoryPlace);
    }

    /**
     * @see GitCommand#execute()
     */
    @Override
    public String execute() throws GitException {
        if (filePattern == null) {
            throw new GitException("No file pattern was set.");
        }
        reset();
        commandLine.add("show");
        commandLine.add(version + ":" + filePattern);
        start();
        
        String content = "";
        
        for (String line : lines) {
            content = content.concat(line + "\n");
        }
        
        return content;
    }

    /**
     * Set up file pattern for show command.
     *
     * @param filePattern file pattern for show command.
     * @return ShowCommand with established file pattern
     */
    public ShowCommand withFilePattern(String filePattern) {
        this.filePattern = filePattern;
        return this;
    }

    /**
     * Set up revision or branch for show command.
     *
     * @param version revision or branch for show command.
     * @return ShowCommand with established revision or branch
     */
    public ShowCommand withVersion(String version) {
        this.version = version;
        return this;
    }

}
