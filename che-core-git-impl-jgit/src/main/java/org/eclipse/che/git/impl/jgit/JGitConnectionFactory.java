/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - API 
 *   SAP           - initial implementation
 *******************************************************************************/
package org.eclipse.che.git.impl.jgit;

import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.inject.Inject;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.util.LineConsumerFactory;
import org.eclipse.che.api.git.CredentialsLoader;
import org.eclipse.che.api.git.GitConnectionFactory;
import org.eclipse.che.api.git.GitException;
import org.eclipse.che.api.git.server.dto.DtoServerImpls.GitUserImpl;
import org.eclipse.che.api.git.shared.GitUser;
import org.eclipse.che.api.user.server.dao.User;
import org.eclipse.che.api.user.server.dao.UserDao;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;

/**
 * JGit implementation for GitConnectionFactory
 * 
 * @author Tareq Sharafy (tareq.sha@gmail.com)
 */
public class JGitConnectionFactory extends GitConnectionFactory {

    private final CredentialsLoader credentialsLoader;
    private final UserDao userDao;

    // Create a trust manager that does not validate certificate chains
    TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
        }

        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
        }
    } };

    @Inject
    public JGitConnectionFactory(CredentialsLoader credentialsLoader, UserDao userDao) throws GitException {
        this.credentialsLoader = credentialsLoader;
        this.userDao = userDao;

        // Install the all-trusting trust manager
       try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (NoSuchAlgorithmException e) {
            throw new GitException(e);
        } catch (KeyManagementException e) {
            throw new GitException(e);
        }
    }

    @Override
    public JGitConnection getConnection(File workDir, LineConsumerFactory outputPublisherFactory) throws GitException {
        return getConnection(workDir, getGitUserFromCurrentUserInfo(), outputPublisherFactory);
    }

    private GitUser getGitUserFromCurrentUserInfo() throws GitException {
        try {
            User user = userDao.getById(EnvironmentContext.getCurrent().getUser().getId());
            final GitUser gitUser = new GitUserImpl().withName(user.getId()).withEmail(user.getEmail());
            return gitUser;
        } catch (NotFoundException | ServerException e) {
            throw new GitException(Messages.getString("ERROR_RETRIEVING_USER_NAME_OR_EMAIL"), e); //$NON-NLS-1$
        }
    }

    public JGitConnection getConnection(File workDir, GitUser user, LineConsumerFactory outputPublisherFactory)
            throws GitException {
        Repository gitRepo = createRepository(workDir);
        JGitConnection conn = new JGitConnection(gitRepo, user, credentialsLoader);
        conn.setOutputLineConsumerFactory(outputPublisherFactory);
        return conn;
    }

    private static Repository createRepository(File workDir) throws GitException {
        try {
            return new FileRepository(new File(workDir, Constants.DOT_GIT));
        } catch (IOException e) {
            throw new GitException(e.getMessage(), e);
        }
    }

    @Override
    public CredentialsLoader getCredentialsLoader() {
        return credentialsLoader;
    }
}
