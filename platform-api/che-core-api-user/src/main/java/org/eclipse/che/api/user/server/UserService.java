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
package org.eclipse.che.api.user.server;


import com.google.common.annotations.Beta;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.UnauthorizedException;
import org.eclipse.che.api.core.rest.Service;
import org.eclipse.che.api.core.rest.annotations.Description;
import org.eclipse.che.api.core.rest.annotations.GenerateLink;
import org.eclipse.che.api.core.rest.annotations.Required;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.core.util.LinksHelper;
import org.eclipse.che.api.user.server.dao.PreferenceDao;
import org.eclipse.che.api.user.server.dao.Profile;
import org.eclipse.che.api.user.server.dao.User;
import org.eclipse.che.api.user.server.dao.UserDao;
import org.eclipse.che.api.user.server.dao.UserProfileDao;
import org.eclipse.che.api.user.shared.dto.UserDescriptor;
import org.eclipse.che.api.user.shared.dto.UserInRoleDescriptor;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.dto.server.DtoFactory;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.status;
import static org.eclipse.che.api.user.server.Constants.LINK_REL_CREATE_USER;
import static org.eclipse.che.api.user.server.Constants.LINK_REL_GET_CURRENT_USER;
import static org.eclipse.che.api.user.server.Constants.LINK_REL_GET_CURRENT_USER_PROFILE;
import static org.eclipse.che.api.user.server.Constants.LINK_REL_GET_USER_BY_EMAIL;
import static org.eclipse.che.api.user.server.Constants.LINK_REL_GET_USER_BY_ID;
import static org.eclipse.che.api.user.server.Constants.LINK_REL_GET_USER_PROFILE_BY_ID;
import static org.eclipse.che.api.user.server.Constants.LINK_REL_INROLE;
import static org.eclipse.che.api.user.server.Constants.LINK_REL_REMOVE_USER_BY_ID;
import static org.eclipse.che.api.user.server.Constants.LINK_REL_UPDATE_PASSWORD;
import static org.eclipse.che.api.user.server.Constants.PASSWORD_LENGTH;
import static org.eclipse.che.commons.lang.NameGenerator.generate;

/**
 * Provides REST API for user management
 *
 * @author Eugene Voevodin
 */
@Api(value = "/user", description = "User manager")
@Path("/user")
public class UserService extends Service {

    private final UserDao        userDao;
    private final UserProfileDao profileDao;
    private final PreferenceDao  preferenceDao;
    private final TokenValidator tokenValidator;

    @Inject
    public UserService(UserDao userDao,
                       UserProfileDao profileDao,
                       PreferenceDao preferenceDao,
                       TokenValidator tokenValidator) {
        this.userDao = userDao;
        this.profileDao = profileDao;
        this.preferenceDao = preferenceDao;
        this.tokenValidator = tokenValidator;
    }

    /**
     * Creates new user and profile.
     *
     * @param token
     *         authentication token
     * @param isTemporary
     *         if it is {@code true} creates temporary user
     * @return entity of created user
     * @throws UnauthorizedException
     *         when token is {@code null}
     * @throws ConflictException
     *         when token is not valid
     * @throws ServerException
     *         when some error occurred while persisting user or user profile
     * @see UserDescriptor
     * @see #getCurrent(SecurityContext)
     * @see #updatePassword(String)
     * @see #getById(String, SecurityContext)
     * @see #getByEmail(String, SecurityContext)
     * @see #remove(String)
     */
    @ApiOperation(value = "Create a new user",
                  notes = "Create a new user in the system",
                  response = UserDescriptor.class,
                  position = 1)
    @ApiResponses({@ApiResponse(code = 201, message = "Created"),
                   @ApiResponse(code = 401, message = "Missed token parameter"),
                   @ApiResponse(code = 409, message = "Invalid token"),
                   @ApiResponse(code = 500, message = "Internal Server Error")})
    @POST
    @Path("/create")
    @GenerateLink(rel = LINK_REL_CREATE_USER)
    @Produces(APPLICATION_JSON)
    public Response create(@ApiParam(value = "Authentication token", required = true) @QueryParam("token") @Required String token,
                           @ApiParam(value = "User type") @QueryParam("temporary") boolean isTemporary,
                           @Context SecurityContext context) throws UnauthorizedException,
                                                                    ConflictException,
                                                                    ServerException,
                                                                    NotFoundException {
        if (token == null) {
            throw new UnauthorizedException("Missed token parameter");
        }
        final String email = tokenValidator.validateToken(token);
        final String id = generate("user", Constants.ID_LENGTH);

        //creating user
        final User user = new User().withId(id)
                                    .withEmail(email)
                                    .withPassword(generate("pass", PASSWORD_LENGTH));
        userDao.create(user);

        //creating profile
        profileDao.create(new Profile().withId(id).withUserId(id));

        //storing preferences
        final Map<String, String> preferences = new HashMap<>(4);
        preferences.put("temporary", String.valueOf(isTemporary));
        preferences.put("codenvy:created", Long.toString(System.currentTimeMillis()));
        preferenceDao.setPreferences(id, preferences);

        return status(CREATED).entity(toDescriptor(user, context)).build();
    }

    /**
     * Returns {@link UserDescriptor} of current user
     *
     * @return entity of current user.
     * @throws ServerException
     *         when some error occurred while retrieving current user
     */
    @ApiOperation(value = "Get current user",
                  notes = "Get user currently logged in the system",
                  response = UserDescriptor.class,
                  position = 2)
    @ApiResponses({@ApiResponse(code = 200, message = "OK"),
                   @ApiResponse(code = 404, message = "Not Found"),
                   @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @GenerateLink(rel = LINK_REL_GET_CURRENT_USER)
    @RolesAllowed({"user", "temp_user"})
    @Produces(APPLICATION_JSON)
    public UserDescriptor getCurrent(@Context SecurityContext context) throws NotFoundException, ServerException {
        final User user = userDao.getById(currentUser().getId());
        return toDescriptor(user, context);
    }

    /**
     * Updates current user password.
     *
     * @param password
     *         new user password
     * @throws ConflictException
     *         when given password is {@code null}
     * @throws ServerException
     *         when some error occurred while updating profile
     * @see UserDescriptor
     */
    @ApiOperation(value = "Update password",
                  notes = "Update current password",
                  position = 3)
    @ApiResponses({@ApiResponse(code = 204, message = "OK"),
                   @ApiResponse(code = 404, message = "Not Found"),
                   @ApiResponse(code = 409, message = "Invalid password"),
                   @ApiResponse(code = 500, message = "Internal Server Error")})
    @POST
    @Path("/password")
    @GenerateLink(rel = LINK_REL_UPDATE_PASSWORD)
    @RolesAllowed("user")
    @Consumes(APPLICATION_FORM_URLENCODED)
    public void updatePassword(@ApiParam(value = "New password", required = true)
                               @FormParam("password")
                               String password) throws NotFoundException, ServerException, ConflictException {
        checkPassword(password);

        final User user = userDao.getById(currentUser().getId());
        user.setPassword(password);

        userDao.update(user);
    }

    /**
     * Returns status <b>200</b> and {@link UserDescriptor} built from user with given {@code id}
     * or status <b>404</b> when user with given {@code id} was not found
     *
     * @param id
     *         identifier to search user
     * @return entity of found user
     * @throws NotFoundException
     *         when user with given identifier doesn't exist
     * @throws ServerException
     *         when some error occurred while retrieving user
     * @see UserDescriptor
     * @see #getByEmail(String, SecurityContext)
     */
    @ApiOperation(value = "Get user by ID",
                  notes = "Get user by its ID in the system. Roles allowed: system/admin, system/manager.",
                  response = UserDescriptor.class,
                  position = 4)
    @ApiResponses({@ApiResponse(code = 200, message = "OK"),
                   @ApiResponse(code = 404, message = "Not Found"),
                   @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/{id}")
    @GenerateLink(rel = LINK_REL_GET_USER_BY_ID)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    @Produces(APPLICATION_JSON)
    public UserDescriptor getById(@ApiParam(value = "User ID") @PathParam("id") String id,
                                  @Context SecurityContext context) throws NotFoundException, ServerException {
        final User user = userDao.getById(id);
        return toDescriptor(user, context);
    }

    /**
     * Returns status <b>200</b> and {@link UserDescriptor} built from user with given {@code email}
     * or status <b>404</b> when user with given {@code email} was not found
     *
     * @param email
     *         email to search user
     * @return entity of found user
     * @throws NotFoundException
     *         when user with given email doesn't exist
     * @throws ServerException
     *         when some error occurred while retrieving user
     * @see UserDescriptor
     * @see #getById(String, SecurityContext)
     * @see #remove(String)
     */
    @ApiOperation(value = "Get user by email",
                  notes = "Get user by registration email. Roles allowed: system/admin, system/manager.",
                  response = UserDescriptor.class,
                  position = 5)
    @ApiResponses({@ApiResponse(code = 200, message = "OK"),
                   @ApiResponse(code = 403, message = "Missed parameter email"),
                   @ApiResponse(code = 404, message = "Not Found"),
                   @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/find")
    @GenerateLink(rel = LINK_REL_GET_USER_BY_EMAIL)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    @Produces(APPLICATION_JSON)
    public UserDescriptor getByEmail(@ApiParam(value = "User email", required = true) @QueryParam("email") @Required String email,
                                     @Context SecurityContext context) throws NotFoundException, ServerException, ConflictException {
        if (email == null) {
            throw new ConflictException("Missed parameter email");
        }
        final User user = userDao.getByAlias(email);
        return toDescriptor(user, context);
    }

    /**
     * Removes user with given identifier.
     *
     * @param id
     *         identifier to remove user
     * @throws NotFoundException
     *         when user with given identifier doesn't exist
     * @throws ServerException
     *         when some error occurred while removing user
     * @throws ConflictException
     *         when some error occurred while removing user
     */
    @ApiOperation(value = "Delete user",
                  notes = "Delete a user from the system. Roles allowed: system/admin.",
                  position = 6)
    @ApiResponses({@ApiResponse(code = 204, message = "Deleted"),
                   @ApiResponse(code = 404, message = "Not Found"),
                   @ApiResponse(code = 409, message = "Impossible to remove user"),
                   @ApiResponse(code = 500, message = "Internal Server Error")})
    @DELETE
    @Path("/{id}")
    @GenerateLink(rel = LINK_REL_REMOVE_USER_BY_ID)
    @RolesAllowed("system/admin")
    public void remove(@ApiParam(value = "User ID")
                       @PathParam("id") String id) throws NotFoundException, ServerException, ConflictException {
        userDao.remove(id);
    }


    /**
     * Allow to check if current user has a given role or not. status <b>200</b> and {@link UserInRoleDescriptor} is returned by indicating if role is granted or not
     *
     * @param role
     *         role to search (like admin or manager)
     * @param scope
     *         the optional scope like system, workspace, account.(default scope is system)
     * @param scopeId
     *         an optional scopeID used by the scope like the workspace ID if scope is workspace.
     * @return {UserInRoleDescriptor} which indicates if role is granted or not
     * @throws org.eclipse.che.api.core.ForbiddenException
     *         with an uknown scope
     * @throws ServerException
     *         when unable to perform the check
     */
    @ApiOperation(value = "Check role for the authenticated user",
            notes = "Check if user has a role in given scope (default is system) and with an optional scope id. Roles allowed: user, system/admin, system/manager.",
            response = UserInRoleDescriptor.class,
            position = 7)
    @ApiResponses({@ApiResponse(code = 200, message = "OK"),
                   @ApiResponse(code = 403, message = "Unable to check for the given scope"),
                   @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/inrole")
    @GenerateLink(rel = LINK_REL_INROLE)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    @Produces(APPLICATION_JSON)
    @Beta
    public UserInRoleDescriptor inRole(@Required @Description("role inside a scope") @QueryParam("role") String role,
                                       @DefaultValue("system") @Description("scope of the role (like system, workspace)") @QueryParam("scope") String scope,
                                       @DefaultValue("") @Description("id used by the scope, like workspaceId for workspace scope") @QueryParam("scopeId") String scopeId,
                                       @Context SecurityContext context) throws NotFoundException, ServerException, ForbiddenException {

        // handle scope
        boolean isInRole;
        if ("system".equals(scope)) {
            String roleToCheck;
            if ("user".equals(role) || "temp_user".equals(role)) {
                roleToCheck = role;
            } else {
                roleToCheck = "system/" + role;
            }

            // check role
            isInRole = context.isUserInRole(roleToCheck);
        } else {
            throw new ForbiddenException(String.format("Only system scope is handled for now. Provided scope is %s", scope));
        }

        return DtoFactory.getInstance().createDto(UserInRoleDescriptor.class).withIsInRole(isInRole).withRoleName(role).withScope(scope).withScopeId(scopeId);

    }

    private void checkPassword(String password) throws ConflictException {
        if (password == null) {
            throw new ConflictException("Password required");
        }
        if (password.length() < 8) {
            throw new ConflictException("Password should contain at least 8 characters");
        }
        int numOfLetters = 0;
        int numOfDigits = 0;
        for (char passwordChar : password.toCharArray()) {
            if (Character.isDigit(passwordChar)) {
                numOfDigits++;
            }
            if (Character.isLetter(passwordChar)) {
                numOfLetters++;
            }
        }
        if (numOfDigits == 0 || numOfLetters == 0) {
            throw new ConflictException("Password should contain letters and digits");
        }
    }

    private UserDescriptor toDescriptor(User user, SecurityContext context) {
        final List<Link> links = new LinkedList<>();
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        if (context.isUserInRole("user")) {
            links.add(LinksHelper.createLink("GET",
                                             getServiceContext().getBaseUriBuilder().path(UserProfileService.class)
                                                                .path(UserProfileService.class, "getCurrent")
                                                                .build()
                                                                .toString(),
                                             null,
                                             APPLICATION_JSON,
                                             LINK_REL_GET_CURRENT_USER_PROFILE));
            links.add(LinksHelper.createLink("GET",
                                             uriBuilder.clone()
                                                       .path(getClass(), "getCurrent")
                                                       .build()
                                                       .toString(),
                                             null,
                                             APPLICATION_JSON,
                                             LINK_REL_GET_CURRENT_USER));
            links.add(LinksHelper.createLink("POST",
                                             uriBuilder.clone()
                                                       .path(getClass(), "updatePassword")
                                                       .build()
                                                       .toString(),
                                             APPLICATION_FORM_URLENCODED,
                                             null,
                                             LINK_REL_UPDATE_PASSWORD));
        }
        if (context.isUserInRole("system/admin") || context.isUserInRole("system/manager")) {
            links.add(LinksHelper.createLink("GET",
                                             uriBuilder.clone()
                                                       .path(getClass(), "getById")
                                                       .build(user.getId())
                                                       .toString(),
                                             null,
                                             APPLICATION_JSON,
                                             LINK_REL_GET_USER_BY_ID));
            links.add(LinksHelper.createLink("GET",
                                             getServiceContext().getBaseUriBuilder()
                                                                .path(UserProfileService.class).path(UserProfileService.class, "getById")
                                                                .build(user.getId())
                                                                .toString(),
                                             null,
                                             APPLICATION_JSON,
                                             LINK_REL_GET_USER_PROFILE_BY_ID));
            links.add(LinksHelper.createLink("GET",
                                             uriBuilder.clone()
                                                       .path(getClass(), "getByEmail")
                                                       .queryParam("email", user.getEmail())
                                                       .build()
                                                       .toString(),
                                             null,
                                             APPLICATION_JSON,
                                             LINK_REL_GET_USER_BY_EMAIL));
        }
        if (context.isUserInRole("system/admin")) {
            links.add(LinksHelper.createLink("DELETE",
                                             uriBuilder.clone()
                                                       .path(getClass(), "remove")
                                                       .build(user.getId())
                                                       .toString(),
                                             null,
                                             null,
                                             LINK_REL_REMOVE_USER_BY_ID));
        }
        return DtoFactory.getInstance().createDto(UserDescriptor.class)
                         .withId(user.getId())
                         .withEmail(user.getEmail())
                         .withAliases(user.getAliases())
                         .withPassword("<none>")
                         .withLinks(links);
    }

    private org.eclipse.che.commons.user.User currentUser() {
        return EnvironmentContext.getCurrent().getUser();
    }
}