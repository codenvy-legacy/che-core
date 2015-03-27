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
package org.eclipse.che.security.oauth1;


import org.eclipse.che.api.auth.oauth.OAuthAuthorizationHeaderProvider;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Map;

/**
 * Compute the Authorization header used to sign the OAuth1 request with the help of an {@link OAuthAuthenticatorProvider}.
 *
 * @author Kevin Pollet
 */
@Singleton
public class OAuthAuthenticatorAuthorizationHeaderProvider implements OAuthAuthorizationHeaderProvider {
    private final OAuthAuthenticatorProvider oAuthAuthenticatorProvider;

    @Inject
    public OAuthAuthenticatorAuthorizationHeaderProvider(@Nonnull final OAuthAuthenticatorProvider oAuthAuthenticatorProvider) {
        this.oAuthAuthenticatorProvider = oAuthAuthenticatorProvider;
    }

    @Override
    public String getAuthorizationHeader(@Nonnull final String oauthProviderName,
                                         @Nonnull final String userId,
                                         @Nonnull final String requestMethod,
                                         @Nonnull final String requestUrl,
                                         @Nonnull final Map<String, String> requestParameters) throws IOException {

        final OAuthAuthenticator oAuthAuthenticator = oAuthAuthenticatorProvider.getAuthenticator(oauthProviderName);
        if (oAuthAuthenticator != null) {
            return oAuthAuthenticator.computeAuthorizationHeader(userId, requestMethod, requestUrl, requestParameters);
        }
        return null;
    }
}
