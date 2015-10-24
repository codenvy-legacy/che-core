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
package org.eclipse.che.api.auth; /*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * <p>
 * Contributors:
 * Codenvy, S.A. - initial API and implementation
 *******************************************************************************/

import static com.jayway.restassured.RestAssured.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.eclipse.che.api.auth.AuthenticationService;
import org.eclipse.che.api.auth.TokenInvalidationHandler;
import org.eclipse.che.api.auth.TokenManager;

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.internal.mapper.ObjectMapperType;
import com.jayway.restassured.mapper.ObjectMapper;
import com.jayway.restassured.response.Cookie;

import org.eclipse.che.api.auth.server.dto.DtoServerImpls;
import org.eclipse.che.api.auth.shared.dto.Credentials;
import org.eclipse.che.api.auth.shared.dto.Token;
import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.user.server.dao.User;
import org.eclipse.che.api.user.server.dao.UserDao;
import org.eclipse.che.dto.server.DtoFactory;
import org.everrest.assured.EverrestJetty;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.http.credentials.UsernamePasswordCredentials;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import java.lang.reflect.Field;

@Listeners(value = {EverrestJetty.class, MockitoTestNGListener.class})
public class AuthenticationServiceTest {

    @Mock
    UserDao                  userDao;
    @Mock
    User                     user;
    @Mock
    TokenManager             tokenManager;
    @Mock
    TokenInvalidationHandler tokenInvalidationHandler;

    Clients clients;
    @Mock
    Client      client;
    @Mock
    UserProfile profile;



    @InjectMocks
    AuthenticationService service;


    @BeforeMethod
    public void setUp() throws Exception {
        clients = new Clients(client);

        Field field = AuthenticationService.class.getDeclaredField("clients");
        field.setAccessible(true);
        field.set(service, clients);

    }

    @Test
    public void shouldFailIfNoBodyOnLogin() {
        given()
                .contentType(ContentType.JSON)
                .then()
                .expect().statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .when()
                .post("/auth/login");
    }

    @Test
    public void shouldFailIfUserNameIsNull() {
        given()
                .contentType(ContentType.JSON)
                .body(
                        DtoFactory.getInstance().createDto(Credentials.class)
                                  .withUsername(null)
                                  .withPassword("secret")
                     )
                .then()
                .expect().statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .when()
                .post("/auth/login");
    }

    @Test
    public void shouldFailIfUserNameIsEmpty() {
        given()
                .contentType(ContentType.JSON)
                .body(
                        DtoFactory.getInstance().createDto(Credentials.class)
                                  .withUsername("")
                                  .withPassword("secret")
                     )
                .then()
                .expect().statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .when()
                .post("/auth/login");
    }

    @Test
    public void shouldFailIfPasswordIsEmpty() {
        given()
                .contentType(ContentType.JSON)
                .body(
                        DtoFactory.getInstance().createDto(Credentials.class)
                                  .withUsername("User")
                                  .withPassword("")
                     )
                .then()
                .expect().statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .when()
                .post("/auth/login");
    }

    @Test
    public void shouldFailIfPasswordIsNull() {
        given()
                .contentType(ContentType.JSON)
                .body(
                        DtoFactory.getInstance().createDto(Credentials.class)
                                  .withUsername("User")
                                  .withPassword(null)
                     )
                .then()
                .expect().statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .when()
                .post("/auth/login");
    }


    @Test
    public void shouldReturnToken() throws ApiException {
        //given
        Token expected = DtoFactory.getInstance().createDto(Token.class).withValue("t-12342345");
//        when(userDao.authenticate(eq("User"), eq("password"))).thenReturn(true);
//        when(userDao.getByAlias(eq("User"))).thenReturn(user);
        when(profile.getId()).thenReturn("u-1");
        when(tokenManager.createToken(eq("u-1"))).thenReturn("t-12342345");
        when(client.getUserProfile(any(org.pac4j.core.credentials.Credentials.class), any(WebContext.class))).thenReturn(profile);
        //when

        Token actual = given()
                .contentType(ContentType.JSON)
                .body(
                        DtoFactory.getInstance().createDto(Credentials.class)
                                  .withUsername("User")
                                  .withPassword("password")
                     )
                .then()
                .expect().statusCode(200)
                .when()
                .cookie("session-access-key", "t-12342345")
                .post("/auth/login").as(DtoServerImpls.TokenImpl.class, ObjectMapperType.GSON);
        //then
        assertEquals(actual, expected);
        ArgumentCaptor<UsernamePasswordCredentials> credetials = ArgumentCaptor.forClass(UsernamePasswordCredentials.class);
        ArgumentCaptor<WebContext> context = ArgumentCaptor.forClass(WebContext.class);
        verify(client).getUserProfile(credetials.capture(), context.capture());

        assertEquals(credetials.getValue().getUsername(), "User");
        assertEquals(credetials.getValue().getPassword(), "password");

    }

    @Test
    public void shouldFailToLogoutIfTokenIsNull() {
        given()
                .then()
                .expect().statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .when()
                .post("/auth/logout");
    }

    @Test
    public void shouldLogOut() {
        //given
        //when
        Cookie cookie = given()
                .then()
                .expect().statusCode(204)
                .when()
                .post("/auth/logout?token=er00349").getDetailedCookie("session-access-key");
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        //then
        assertTrue(cookie.isHttpOnly());
        assertEquals(cookie.getMaxAge(), 0);
        verify(tokenManager).invalidateToken(argument.capture());
//        verify(tokenInvalidationHandler).onTokenInvalidated(eq("er00349"));
        assertEquals(argument.getValue(), "er00349");

    }


}