/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.git.impl.nativegit;

import org.eclipse.che.api.core.util.LineConsumerFactory;
import org.eclipse.che.api.git.CredentialsLoader;
import org.eclipse.che.api.git.CredentialsProvider;
import org.eclipse.che.api.git.GitConnection;
import org.eclipse.che.api.git.GitConnectionFactory;
import org.eclipse.che.api.git.GitException;
import org.eclipse.che.git.impl.nativegit.ssh.GitSshScriptProvider;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;

/**
 * Native implementation for GitConnectionFactory
 *
 * @author Eugene Voevodin
 * @author Valeriy Svydenko
 */
@Singleton
public class NativeGitConnectionFactory extends GitConnectionFactory {

    private File mountRoot;
    private final CredentialsLoader credentialsLoader;
    private final GitSshScriptProvider gitSshScriptProvider;

    @Inject
    public NativeGitConnectionFactory(@Named("vfs.local.fs_root_dir") java.io.File mountRoot, CredentialsLoader credentialsLoader,
                                      GitSshScriptProvider gitSshScriptProvider) {
        this.mountRoot = mountRoot;
        this.credentialsLoader = credentialsLoader;
        this.gitSshScriptProvider = gitSshScriptProvider;
    }

    @Override
    public GitConnection getConnection(File workDir, LineConsumerFactory outputPublisherFactory) throws GitException {
        final GitConnection gitConnection = new NativeGitConnection(mountRoot, workDir, gitSshScriptProvider, credentialsLoader);
        gitConnection.setOutputLineConsumerFactory(outputPublisherFactory);
        return gitConnection;
    }

    @Override
    public CredentialsLoader getCredentialsLoader() {
        return credentialsLoader;
    }

}