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

import com.jayway.restassured.response.Response;

import org.eclipse.che.api.auth.shared.dto.OAuthToken;
import org.eclipse.che.dto.server.DtoFactory;
import org.everrest.assured.EverrestJetty;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import java.security.Principal;

import static com.jayway.restassured.RestAssured.given;
import static org.everrest.assured.JettyHttpServer.ADMIN_USER_NAME;
import static org.everrest.assured.JettyHttpServer.ADMIN_USER_PASSWORD;
import static org.everrest.assured.JettyHttpServer.SECURE_PATH;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

/**
 * @author Max Shaposhnik
 *
 */

@Listeners(value = {EverrestJetty.class, MockitoTestNGListener.class})
public class OAuthAuthenticationServiceTest {

    @Mock
    protected OAuthAuthenticatorProvider providers;
    @Mock
    protected UriInfo                    uriInfo;
    @Mock
    protected SecurityContext            security;
    @InjectMocks
    OAuthAuthenticationService           service;


    @Test
    public void shouldThrowWebApplicationExceptionIfNoSuchProviderFound() {
        final Response response = given().auth()
                                         .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
                                         .contentType("application/json")
                                         .when()
                                         .queryParam("oauth_provider", "unknown")
                                         .get(SECURE_PATH + "/oauth/token");

        assertEquals(response.getStatusCode(), 400);
        assertEquals(response.getBody().print(), "Unsupported OAuth provider unknown");
    }


    @Test
    public void shouldBeAbleToGetUserToken() throws Exception {
        String provider = "myprovider";
        String token = "token123";
        OAuthAuthenticator authenticator  = mock(OAuthAuthenticator.class);
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("username");
        when(security.getUserPrincipal()).thenReturn(principal);
        when(providers.getAuthenticator(eq(provider))).thenReturn(authenticator);
        when(authenticator.getToken(anyString())).thenReturn(DtoFactory.newDto(OAuthToken.class).withToken(token));

        final Response response = given().auth()
                                         .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
                                         .contentType("application/json")
                                         .when()
                                         .queryParam("oauth_provider", provider)
                                         .get(SECURE_PATH + "/oauth/token");

        assertEquals(response.getStatusCode(), 200);
        assertEquals(DtoFactory.getInstance()
                               .createDtoFromJson(response.getBody().asInputStream(), OAuthToken.class)
                               .getToken(), token);
    }
}
