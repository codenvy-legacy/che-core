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
package org.eclipse.che.api.auth.pac4j;

import org.pac4j.core.client.BaseClient;
import org.pac4j.core.client.ClientType;
import org.pac4j.http.client.direct.DirectHttpClient;
import org.pac4j.http.credentials.UsernamePasswordCredentials;
import org.pac4j.http.credentials.authenticator.Authenticator;
import org.pac4j.http.credentials.extractor.Extractor;
import org.pac4j.http.profile.HttpProfile;
import org.pac4j.http.profile.creator.ProfileCreator;

import javax.inject.Inject;

/**
 * @author Sergii Kabashniuk
 */
public class AuthenticationClient extends DirectHttpClient<UsernamePasswordCredentials> {


    @Inject
    public AuthenticationClient(Extractor<UsernamePasswordCredentials> extractor,
                                Authenticator<UsernamePasswordCredentials> authenticator,
                                ProfileCreator<UsernamePasswordCredentials, HttpProfile> profileCreator) {

        this.extractor = extractor;
        setAuthenticator(authenticator);
        setProfileCreator(profileCreator);

    }


    @Override
    protected BaseClient newClient() {
        return new AuthenticationClient(extractor, getAuthenticator(), getProfileCreator());
    }

    @Override
    public ClientType getClientType() {
        return ClientType.BASICAUTH_BASED;
    }


}
