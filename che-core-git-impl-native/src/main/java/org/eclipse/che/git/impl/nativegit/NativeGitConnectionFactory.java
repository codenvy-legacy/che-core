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

import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.util.LineConsumerFactory;
import org.eclipse.che.api.git.GitConnection;
import org.eclipse.che.api.git.GitConnectionFactory;
import org.eclipse.che.api.git.GitException;
import org.eclipse.che.api.git.shared.GitUser;
import org.eclipse.che.api.user.server.dao.PreferenceDao;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.dto.server.DtoFactory;
import org.eclipse.che.git.impl.nativegit.ssh.GitSshScriptProvider;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;

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
    private PreferenceDao preferenceDao;

    @Inject
    public NativeGitConnectionFactory(CredentialsLoader credentialsLoader,
                                      GitSshScriptProvider gitSshScriptProvider,
                                      PreferenceDao preferenceDao) {
        this.credentialsLoader = credentialsLoader;
        this.gitSshScriptProvider = gitSshScriptProvider;
        this.preferenceDao = preferenceDao;
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

        try {
            Map<String, String> preferences = preferenceDao.getPreferences(EnvironmentContext.getCurrent().getUser().getId(),
                                                                           "git.committer.\\w+");

            String name = preferences.get("git.committer.name");
            String email = preferences.get("git.committer.email");

            gitUser.setName(isNullOrEmpty(name) ? "Anonymous" : name);
            gitUser.setEmail(isNullOrEmpty(email) ? "anonymous@noemail.com" : email);
        } catch (ServerException e) {
            gitUser.setName("Anonymous");
            gitUser.setEmail("anonymous@noemail.com");
        }

        return gitUser;
    }
}
