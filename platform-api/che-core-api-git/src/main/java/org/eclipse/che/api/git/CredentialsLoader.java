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
package org.eclipse.che.api.git;


import org.eclipse.che.api.git.exception.GitException;
import org.eclipse.che.api.git.shared.GitUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * Load credentials
 *
 * @author Eugene Voevodin
 */
@Singleton
public class CredentialsLoader {

    private static final Logger LOG = LoggerFactory.getLogger(CredentialsLoader.class);

    private Map<String, CredentialsProvider> credentialsProviders;

    @Inject
    public CredentialsLoader(Set<CredentialsProvider> credentialsProviders) {

        this.credentialsProviders = new HashMap<>(credentialsProviders.size());
        for (CredentialsProvider credentialsProvider : credentialsProviders) {
            this.credentialsProviders.put(credentialsProvider.getId(), credentialsProvider);
        }
    }

    /**
     * Searches for CredentialsProvider instances and if needed instance exists, it return
     * given credentials, else null;
     *
     * @param url
     *         given URL
     * @return credentials from provider
     * @throws GitException
     *         when it is not possible to store credentials
     */
    public UserCredential getUserCredential(String url) throws GitException {
        for (CredentialsProvider cp : credentialsProviders.values()) {
            if (url != null && cp.canProvideCredentials(url)) {
                UserCredential commandCredentials = cp.getUserCredential();
                if (commandCredentials != null && !commandCredentials.getProviderId().equals(cp.getId())) {
                    throw new GitException(
                            "Provider " + cp.getId() + " returned credential with wrong id " + commandCredentials.getProviderId());
                }
                LOG.debug("Url {} user {}", url, commandCredentials);
                return commandCredentials;
            }
        }

        return null;
    }

    /**
     * @param providerId
     * @return user by provider id.
     * @throws GitException
     */
    public GitUser getUser(String providerId) throws GitException {
        CredentialsProvider provider = credentialsProviders.get(providerId);
        if (provider == null) {
            throw new GitException("Provider " + providerId + " are not found");
        }
        GitUser user = provider.getUser();
        LOG.debug("Provider {} user {}", providerId, user);
        return user;
    }

}