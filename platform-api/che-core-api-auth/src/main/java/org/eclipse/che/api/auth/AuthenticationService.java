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
package org.eclipse.che.api.auth;


import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

import org.eclipse.che.api.auth.shared.dto.Credentials;
import org.eclipse.che.api.auth.shared.dto.Token;
import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.UnauthorizedException;
import org.eclipse.che.api.user.server.dao.User;
import org.eclipse.che.api.user.server.dao.UserDao;
import org.eclipse.che.dto.server.DtoFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

/**
 * Authenticate user by username and password.
 * <p/>
 * In response user receive "token". This token user can use
 * to identify him in all other request to API, to do that he should pass it as query parameter.
 *
 * @author Sergii Kabashniuk
 * @author Alexander Garagatyi
 */

@Api(value = "/auth",
        description = "Authentication manager")
@Path("/auth")
public class AuthenticationService {


    private final UserDao      userDao;
    private final TokenManager tokenManager;

    @Inject
    public AuthenticationService(UserDao userDao,
                                 TokenManager tokenManager) {

        this.userDao = userDao;
        this.tokenManager = tokenManager;
    }

    /**
     * Get token to be able to call secure api methods.
     *
     * @param credentials
     *         - username and password
     * @return - auth token in JSON, session-based and persistent cookies
     * @throws ApiException
     */
    @ApiOperation(value = "Login",
            notes = "Login to a Codenvy account. Either auth token or cookie are used",
            response = Token.class,
            position = 2)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 400, message = "Authentication error")})
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/login")
    public Response login(Credentials credentials, @Context SecurityContext context) throws ApiException {

        if (credentials == null
            || credentials.getPassword() == null
            || credentials.getPassword().isEmpty()
            || credentials.getUsername() == null
            || credentials.getUsername().isEmpty()) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        try {
            if (!userDao.authenticate(credentials.getUsername(), credentials.getPassword()/*, realm*/)) {
                throw new UnauthorizedException("Authentication failed. Please check username and password.");
            }
        } catch (NotFoundException e) {
            throw new UnauthorizedException("Authentication failed. Please check username and password.");
        }

        User user = userDao.getByAlias(credentials.getUsername());
        if (user == null) {
            throw new UnauthorizedException("Authentication failed. Please check username and password.");
        }

        String token = tokenManager.createToken(user.getId());
        return Response.ok()
                       .entity(DtoFactory.getInstance().createDto(Token.class).withValue(token))
                       .header("Set-Cookie",
                               new NewCookie("session-access-key", token, "/", null, null, -1, context.isSecure()) + ";HttpOnly")
                       .build();
    }

    /**
     * Perform logout for the given token.
     *
     * @param token
     *         - authentication token
     */
    @ApiOperation(value = "Logout",
            notes = "Logout from a Codenvy account",
            position = 1)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 400, message = "Authentication error")})
    @POST
    @Path("/logout")
    public Response logout(@ApiParam(value = "Auth token", required = true) @QueryParam("token") String token,
                           @Context SecurityContext context) {
        if (token == null) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        tokenManager.invalidateToken(token);
        return Response.noContent()
                       .header("Set-Cookie",
                               new NewCookie("session-access-key", token, "/", null, null, 0, context.isSecure()) + ";HttpOnly")
                       .build();
    }
}