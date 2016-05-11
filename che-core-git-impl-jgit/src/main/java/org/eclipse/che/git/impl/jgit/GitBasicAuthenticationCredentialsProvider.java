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

import org.eclipse.che.api.git.CredentialsProvider;
import org.eclipse.che.api.git.GitException;
import org.eclipse.che.api.git.UserCredential;
import org.eclipse.che.api.git.server.dto.DtoServerImpls;
import org.eclipse.che.api.git.shared.GitUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;

@Singleton
public class GitBasicAuthenticationCredentialsProvider implements CredentialsProvider {

    private static final Logger LOG = LoggerFactory.getLogger(GitBasicAuthenticationCredentialsProvider.class);

    public static final String PROVIDER_ID = "git-basic";

    private static ThreadLocal<UserCredential> currRequestCredentials = new ThreadLocal<>();

    public static void setCurrentCredentials(String user, String password) {
        UserCredential creds = new UserCredential(user, password, PROVIDER_ID);
        currRequestCredentials.set(creds);
    }

    public static void clearCredentials() {
        currRequestCredentials.set(null);
    }

    @Override
    public UserCredential getUserCredential() {
        UserCredential credentials = currRequestCredentials.get();
        if (credentials == null) {
            return null;
        }
        LOG.info("Successfully fetched credentials for remote repository");
        return credentials;
    }

    @Override
    public GitUser getUser() throws GitException {
        DtoServerImpls.GitUserImpl gitUser = new DtoServerImpls.GitUserImpl();
        gitUser.setName(currRequestCredentials.get().getUserName());
        return gitUser;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public boolean canProvideCredentials(String spec) {
        return getUserCredential() != null;
    }

}
