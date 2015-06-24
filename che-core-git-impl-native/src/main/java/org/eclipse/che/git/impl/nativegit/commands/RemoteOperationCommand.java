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

import com.google.common.collect.ImmutableMap;

import org.eclipse.che.api.git.GitException;
import org.eclipse.che.git.impl.nativegit.GitUrl;
import org.eclipse.che.git.impl.nativegit.ssh.GitSshScriptProvider;

import java.io.File;


/**
 * @author Sergii Kabashniuk
 */
public abstract class RemoteOperationCommand<T> extends GitCommand<T> {

    private final GitSshScriptProvider gitSshScriptProvider;
    private       String               remoteUrl;

    /**
     * @param repository
     *         directory where command will be executed
     */
    public RemoteOperationCommand(File repository, GitSshScriptProvider gitSshScriptProvider) {
        super(repository);
        this.gitSshScriptProvider = gitSshScriptProvider;

    }


    public String getRemoteUrl() {
        return remoteUrl;
    }

    public RemoteOperationCommand setRemoteUrl(String remoteUrl) {
        this.remoteUrl = remoteUrl;
        return this;
    }

    @Override
    protected void start() throws GitException {
        if (!GitUrl.isSSH(remoteUrl)) {
            super.start();
        } else {
            File sshScript = gitSshScriptProvider.gitSshScript();
            sshScript.deleteOnExit();
            setCommandEnvironment(ImmutableMap.<String, String>builder()
                                              .putAll(getCommandEnvironment())
                                              .put("GIT_SSH", sshScript.getAbsolutePath())
                                              .build());
            try {
                super.start();
            } finally {
                sshScript.delete();
            }

        }
    }


}
