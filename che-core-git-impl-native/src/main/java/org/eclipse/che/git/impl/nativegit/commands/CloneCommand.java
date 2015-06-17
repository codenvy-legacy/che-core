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
 * This command used for cloning repositories.
 *
 * @author Eugene Voevodin
 */
public class CloneCommand extends RemoteUperationCommand<Void> {

    private String remoteName;

    public CloneCommand(File repository) {
        super(repository);
    }

    /** @see GitCommand#execute() */
    @Override
    public Void execute() throws GitException {
        reset();
        commandLine.add("clone");
        if (remoteName != null) {
            commandLine.add("--origin", remoteName);
        } //else default origin name
        commandLine.add(getRemoteUrl(), getRepository().getAbsolutePath());
        // Progress not shown if not a terminal. Activating progress output. See git clone man page.
        commandLine.add("--progress");
        start();
        return null;
    }

    /**
     * @param remoteName
     *         name of remote, if it is null than default "origin" name will be used
     * @return CloneCommand with established remoteName
     */
    public CloneCommand setRemoteName(String remoteName) {
        this.remoteName = remoteName;
        return this;
    }
}
