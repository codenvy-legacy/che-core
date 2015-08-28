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
package org.eclipse.che.git.impl.nativegit;

import org.eclipse.che.api.core.util.LineConsumerFactory;
import org.eclipse.che.api.git.GitConnection;
import org.eclipse.che.api.git.GitConnectionFactory;
import org.eclipse.che.api.git.GitException;
import org.eclipse.che.api.git.shared.GitUser;
import org.eclipse.che.dto.server.DtoFactory;
import org.eclipse.che.git.impl.nativegit.ssh.GitSshScriptProvider;
import org.eclipse.che.ide.api.preferences.PreferencesManager;

import javax.inject.Inject;
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

    private final CredentialsLoader    credentialsLoader;
    private final GitSshScriptProvider gitSshScriptProvider;
    private final PreferencesManager   preferencesManager;

    @Inject
    public NativeGitConnectionFactory(CredentialsLoader credentialsLoader,
                                      GitSshScriptProvider gitSshScriptProvider,
                                      PreferencesManager   preferencesManager) {
        this.credentialsLoader = credentialsLoader;
        this.preferencesManager = preferencesManager;
        this.gitSshScriptProvider = gitSshScriptProvider;
    }

    @Override
    public GitConnection getConnection(File workDir, GitUser user, LineConsumerFactory outputPublisherFactory) throws GitException {
        final GitConnection gitConnection = new NativeGitConnection(workDir, user, gitSshScriptProvider, credentialsLoader);
        gitConnection.setOutputLineConsumerFactory(outputPublisherFactory);
        return gitConnection;
    }

    @Override
    public GitConnection getConnection(File workDir, LineConsumerFactory outputPublisherFactory) throws GitException {
        final GitConnection gitConnection = new NativeGitConnection(workDir, getGitUser(), gitSshScriptProvider, credentialsLoader);
        gitConnection.setOutputLineConsumerFactory(outputPublisherFactory);
        return gitConnection;
    }


    private GitUser getGitUser() {
        final GitUser gitUser = DtoFactory.getInstance().createDto(GitUser.class);

        String name = preferencesManager.getValue("git.committer.name");
        String email = preferencesManager.getValue("git.committer.email");

        gitUser.setName(name != null && !name.isEmpty() ? name : "Anonymous");
        gitUser.setEmail(email != null ? email : "anonymous@noemail.com");

        return gitUser;
    }
}
