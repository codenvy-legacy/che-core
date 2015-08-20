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
import org.eclipse.che.git.impl.nativegit.GitUrl;
import org.eclipse.che.git.impl.nativegit.ssh.GitSshScript;
import org.eclipse.che.git.impl.nativegit.ssh.GitSshScriptProvider;

import java.io.File;

/**
 * @author Sergii Kabashniuk
 */
public abstract class RemoteOperationCommand<T> extends GitCommand<T> {

    private final GitSshScriptProvider gitSshScriptProvider;
    private       String               remoteUri;

    /**
     * @param repository
     *         directory where command will be executed
     */
    public RemoteOperationCommand(File repository, GitSshScriptProvider gitSshScriptProvider) {
        super(repository);
        this.gitSshScriptProvider = gitSshScriptProvider;

    }

    public String getRemoteUri() {
        return remoteUri;
    }

    /**
     * @param remoteUri
     *         remote repository uri
     */
    public RemoteOperationCommand setRemoteUri(String remoteUri) {
        this.remoteUri = remoteUri;
        return this;
    }

    @Override
    protected void start() throws GitException {
        if (!GitUrl.isSSH(remoteUri)) {
            super.start();
        } else {
            GitSshScript sshScript = gitSshScriptProvider.gitSshScript(remoteUri);
            setCommandEnvironment("GIT_SSH", sshScript.getSshScriptFile().getAbsolutePath());
            try {
                super.start();
            } finally {
                sshScript.delete();
            }
        }
    }
}
