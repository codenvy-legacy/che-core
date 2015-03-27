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

import org.eclipse.che.security.oauth1.shared.User;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

/**
 * OAuth authentication for bitbucket account.
 *
 * @author Kevin Pollet
 */
@Singleton
public class BitbucketOAuthAuthenticator extends OAuthAuthenticator {
    @Inject
    public BitbucketOAuthAuthenticator(@Nonnull @Named("oauth.bitbucket.clientid") String clientId,
                                       @Nonnull @Named("oauth.bitbucket.clientsecret") String clientSecret,
                                       @Nonnull @Named("oauth.bitbucket.authuri") String authUri,
                                       @Nonnull @Named("oauth.bitbucket.requesttokenuri") String requestTokenUri,
                                       @Nonnull @Named("oauth.bitbucket.requestaccesstokenuri") String requestAccessTokenUri,
                                       @Nonnull @Named("oauth.bitbucket.verifyaccesstokenuri") String verifyAccessTokenUri,
                                       @Nonnull @Named("oauth.bitbucket.redirecturis") String redirectUri) {
        super(clientId,
              clientSecret,
              authUri,
              requestTokenUri,
              requestAccessTokenUri,
              verifyAccessTokenUri,
              redirectUri);
    }

    @Override
    public User getUser(final String token, final String tokenSecret) throws OAuthAuthenticationException {
        final BitbucketUser user = getJson("https://api.bitbucket.org/2.0/user", token, tokenSecret, BitbucketUser.class);
        final BitbucketEmail[] emails = getJson("https://api.bitbucket.org/1.0/emails", token, tokenSecret, BitbucketEmail[].class);

        BitbucketEmail primaryEmail = null;
        for (final BitbucketEmail oneEmail : emails) {
            if (oneEmail.isPrimary()) {
                primaryEmail = oneEmail;
                break;
            }
        }

        if (primaryEmail == null || primaryEmail.getEmail() == null || primaryEmail.getEmail().isEmpty()) {
            throw new OAuthAuthenticationException("Sorry, we failed to find any primary emails associated with your Bitbucket account.");
        }

        user.setEmail(primaryEmail.getEmail());

        try {

            new InternetAddress(user.getEmail()).validate();

        } catch (final AddressException e) {
            throw new OAuthAuthenticationException(e);
        }

        return user;
    }

    @Override
    public String getOAuthProvider() {
        return "bitbucket";
    }

    /**
     * Information for each email address indicating if the address.
     */
    public static class BitbucketEmail {
        private boolean primary;
        private String  email;

        public boolean isPrimary() {
            return primary;
        }

        @SuppressWarnings("UnusedDeclaration")
        public void setPrimary(boolean primary) {
            this.primary = primary;
        }

        public String getEmail() {
            return email;
        }

        @SuppressWarnings("UnusedDeclaration")
        public void setEmail(String email) {
            this.email = email;
        }
    }
}
