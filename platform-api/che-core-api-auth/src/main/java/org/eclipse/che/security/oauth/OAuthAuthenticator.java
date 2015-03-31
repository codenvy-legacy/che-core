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
package org.eclipse.che.security.oauth;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.AuthorizationCodeResponseUrl;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;

import org.eclipse.che.api.auth.shared.dto.OAuthToken;
import org.eclipse.che.commons.json.JsonHelper;
import org.eclipse.che.commons.json.JsonParseException;
import org.eclipse.che.dto.server.DtoFactory;
import org.eclipse.che.security.oauth.shared.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Authentication service which allow get access token from OAuth provider site. */
public abstract class OAuthAuthenticator {
    private static final Logger LOG = LoggerFactory.getLogger(OAuthAuthenticator.class);

    protected final AuthorizationCodeFlow flow;

    private final Map<Pattern, String> redirectUrisMap;

    public OAuthAuthenticator(AuthorizationCodeFlow flow, List<String> redirectUris) {
        this.flow = flow;
        this.redirectUrisMap = new HashMap<>(redirectUris.size());
        for (String uri : redirectUris) {
            // Redirect URI may be in form urn:ietf:wg:oauth:2.0:oob os use java.net.URI instead of java.net.URL
            this.redirectUrisMap.put(Pattern.compile("([a-z0-9\\-]+\\.)?" + URI.create(uri).getHost()), uri);
        }
    }

    public OAuthAuthenticator(String clientId, String clientSecret, String[] redirectUris, String authUri, String tokenUri,
                              MemoryDataStoreFactory dataStoreFactory) throws IOException {

        this(new AuthorizationCodeFlow.Builder(
                     BearerToken.authorizationHeaderAccessMethod(),
                     new NetHttpTransport(),
                     new JacksonFactory(),
                     new GenericUrl(tokenUri),
                     new ClientParametersAuthentication(
                             clientId,
                             clientSecret),
                     clientId,
                     authUri
             )
                     .setDataStoreFactory(dataStoreFactory).build(),
             Arrays.asList(redirectUris)
            );


        LOG.debug("clientId={}, clientSecret={}, redirectUris={} , authUri={}, tokenUri={}, dataStoreFactory={}",
                  clientId,
                  clientSecret,
                  redirectUris,
                  authUri,
                  tokenUri,
                  dataStoreFactory);
    }


    /**
     * Create authentication URL.
     *
     * @param requestUrl
     *         URL of current HTTP request. This parameter required to be able determine URL for redirection after
     *         authentication. If URL contains query parameters they will be copy to 'state' parameter and returned to
     *         callback method.
     * @param userId
     *         user identifier. This parameter should be not <code>null</code> if user already authenticated in Codenvy site
     *         but need to get OAuth access token to be able use some third party services. This parameter always
     *         <code>null</code> if third party OAuth provider used for authenticate user in Codenvy.
     * @param scopes
     *         specify exactly what type of access needed
     * @return URL for authentication
     */
    public String getAuthenticateUrl(URL requestUrl, String userId, List<String> scopes) throws OAuthAuthenticationException {
        AuthorizationCodeRequestUrl url = flow.newAuthorizationUrl().setRedirectUri(findRedirectUrl(requestUrl))
                                              .setScopes(scopes);
        StringBuilder state = new StringBuilder();
        addState(state);
        String query = requestUrl.getQuery();
        if (query != null) {
            if (state.length() > 0) {
                state.append('&');
            }
            state.append(query);
        }
        if (userId != null) {
            if (state.length() > 0) {
                state.append('&');
            }
            state.append("userId=");
            state.append(userId);
        }
        url.setState(state.toString());
        return url.build();
    }

    private String findRedirectUrl(URL requestUrl) {
        final String requestHost = requestUrl.getHost();
        for (Map.Entry<Pattern, String> e : redirectUrisMap.entrySet()) {
            if (e.getKey().matcher(requestHost).matches()) {
                return e.getValue();
            }
        }
        return null; // TODO : throw exception instead of return null ???
    }

    /**
     * Process callback request.
     *
     * @param requestUrl
     *         request URI. URI should contain authorization code generated by authorization server
     * @param scopes
     *         specify exactly what type of access needed. This list must be exactly the same as list passed to the method
     *         {@link #getAuthenticateUrl(URL, String, java.util.List)}
     * @return id of authenticated user
     * @throws OAuthAuthenticationException
     *         if authentication failed or <code>requestUrl</code> does not contain required parameters, e.g. 'code'
     */
    public String callback(URL requestUrl, List<String> scopes) throws OAuthAuthenticationException {
        AuthorizationCodeResponseUrl authorizationCodeResponseUrl = new AuthorizationCodeResponseUrl(requestUrl
                                                                                                             .toString());
        final String error = authorizationCodeResponseUrl.getError();
        if (error != null) {
            throw new OAuthAuthenticationException("Authentication failed: " + error);
        }
        final String code = authorizationCodeResponseUrl.getCode();
        if (code == null) {
            throw new OAuthAuthenticationException("Missing authorization code. ");
        }

        try {
            TokenResponse tokenResponse = flow.newTokenRequest(code).setRequestInitializer(new HttpRequestInitializer() {
                @Override
                public void initialize(HttpRequest request) throws IOException {
                    if (request.getParser() == null) {
                        request.setParser(flow.getJsonFactory().createJsonObjectParser());
                    }
                    request.getHeaders().setAccept(MediaType.APPLICATION_JSON);
                }
            }).setRedirectUri(findRedirectUrl(requestUrl)).setScopes(scopes).execute();
            String userId = getUserFromUrl(authorizationCodeResponseUrl);
            if (userId == null) {
                userId = getUser(
                        DtoFactory.getInstance().createDto(OAuthToken.class).withToken(tokenResponse.getAccessToken())).getId();
            }
            flow.createAndStoreCredential(tokenResponse, userId);
            return userId;
        } catch (IOException ioe) {
            throw new OAuthAuthenticationException(ioe.getMessage());
        }
    }

    /**
     * Get user info.
     *
     * @param accessToken
     *         oauth access token
     * @return user info
     * @throws OAuthAuthenticationException
     *         if fail to get user info
     */
    public abstract User getUser(OAuthToken accessToken) throws OAuthAuthenticationException;

    /**
     * Get the name of OAuth provider supported by current implementation.
     *
     * @return oauth provider name
     */
    public abstract String getOAuthProvider();

    protected void addState(StringBuilder state) {
    }

    private String getUserFromUrl(AuthorizationCodeResponseUrl authorizationCodeResponseUrl) throws IOException {
        String state = authorizationCodeResponseUrl.getState();
        if (!(state == null || state.isEmpty())) {
            String decoded = URLDecoder.decode(state, "UTF-8");
            String[] items = decoded.split("&");
            for (String str : items) {
                if (str.startsWith("userId=")) {
                    return str.substring(7, str.length());
                }
            }
        }
        return null;
    }

    protected <O> O getJson(String getUserUrl, Class<O> userClass) throws OAuthAuthenticationException {
        HttpURLConnection urlConnection = null;
        InputStream urlInputStream = null;

        try {
            urlConnection = (HttpURLConnection)new URL(getUserUrl).openConnection();
            urlInputStream = urlConnection.getInputStream();
            return JsonHelper.fromJson(urlInputStream, userClass, null);
        } catch (JsonParseException e) {
            throw new OAuthAuthenticationException(e.getMessage(), e);
        } catch (IOException e) {
            throw new OAuthAuthenticationException(e.getMessage(), e);
        } finally {
            if (urlInputStream != null) {
                try {
                    urlInputStream.close();
                } catch (IOException ignored) {
                }
            }

            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    /**
     * Return authorization token by userId.
     * <p/>
     * WARN!!!. DO not use it directly.
     *
     * @param userId
     * @return
     * @throws IOException
     * @see org.eclipse.che.api.auth.oauth.OAuthTokenProvider#getToken(String, String)
     */
    public OAuthToken getToken(String userId) throws IOException {
        Credential credential = flow.loadCredential(userId);
        if (credential != null) {
            Long expirationTime = credential.getExpiresInSeconds();
            if (expirationTime != null && expirationTime < 0) {
                credential.refreshToken();
            }

            return DtoFactory.getInstance().createDto(OAuthToken.class).withToken(credential.getAccessToken());
        }
        return null;
    }

    /**
     * Invalidate OAuth token for specified user.
     *
     * @param userId
     *         user
     * @return <code>true</code> if OAuth token invalidated and <code>false</code> otherwise, e.g. if user does not have
     * token yet
     */
    public boolean invalidateToken(String userId) throws IOException {
        Credential credential = flow.loadCredential(userId);
        if (credential != null) {
            flow.getCredentialStore().delete(userId, credential);
            return true;
        }
        return false;
    }

}
