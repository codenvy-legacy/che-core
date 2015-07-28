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
package org.eclipse.che.api.workspace.server;


import com.wordnik.swagger.annotations.*;
import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.core.*;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.model.UsersWorkspace;
import org.eclipse.che.api.core.model.Workspace;
import org.eclipse.che.api.core.rest.Service;
import org.eclipse.che.api.core.rest.annotations.Description;
import org.eclipse.che.api.core.rest.annotations.GenerateLink;
import org.eclipse.che.api.core.rest.annotations.Required;
import org.eclipse.che.api.core.rest.permission.PermissionManager;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.core.util.LinksHelper;
import org.eclipse.che.api.user.server.dao.*;


import org.eclipse.che.api.workspace.server.spi.WorkspaceDao;
import org.eclipse.che.api.workspace.server.spi.WorkspaceDo;
import org.eclipse.che.api.workspace.shared.WorkspaceDo;
import org.eclipse.che.api.workspace.shared.dto.*;
import org.eclipse.che.api.workspace.shared.dto2.UsersWorkspaceDto;
import org.eclipse.che.api.workspace.shared.dto2.WorkspaceConfigDto;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.dto.server.DtoFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import java.util.*;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.status;
import static org.eclipse.che.commons.lang.NameGenerator.generate;

/**
 * Workspace API
 *
 * @author Eugene Voevodin
 * @author Max Shaposhnik
 */
@Api(value = "/workspace",
     description = "Workspace service")
@Path("/workspace")
public class NewWorkspaceService extends Service {
    private static final Logger LOG = LoggerFactory.getLogger(NewWorkspaceService.class);

    private final WorkspaceManager workspaceManager;

    private final WorkspaceDao workspaceDao;
    private final UserDao        userDao;
//    private final MemberDao      memberDao;
    private final UserProfileDao profileDao;
    private final PreferenceDao  preferenceDao;
//    private final AccountDao     accountDao;
    private final PermissionManager permissionManager;

    @Inject
    public NewWorkspaceService(WorkspaceManager workspaceManager,
                               WorkspaceDao workspaceDao,
                               UserDao userDao,
//                               MemberDao memberDao,
//                               AccountDao accountDao,
                               UserProfileDao profileDao,
                               PreferenceDao preferenceDao,
                               PermissionManager permissionManager) {

        this.workspaceManager = workspaceManager;


        this.workspaceDao = workspaceDao;
        this.userDao = userDao;
//        this.memberDao = memberDao;
//        this.accountDao = accountDao;
        this.profileDao = profileDao;
        this.preferenceDao = preferenceDao;
        this.permissionManager = permissionManager;
    }

    /**
     * Creates new workspace and adds current user as member to created workspace
     * with roles <i>"workspace/admin"</i> and <i>"workspace/developer"</i>. Returns status code <strong>201 CREATED</strong>
     * and {@link org.eclipse.che.api.workspace.shared.dto.WorkspaceDescriptor} if workspace has been created successfully.
     * Each new workspace should contain at least name and account identifier.
     *
     * @param newWorkspace
     *         new workspace
     * @return descriptor of created workspace
     * @throws org.eclipse.che.api.core.ConflictException
     *         when current user account identifier and given account identifier are different
     * @throws org.eclipse.che.api.core.NotFoundException
     *         when account with given identifier does not exist
     * @throws org.eclipse.che.api.core.ServerException
     *         when some error occurred while retrieving/persisting account, workspace or member
     * @throws org.eclipse.che.api.core.BadRequestException
     *         when either new workspace or workspace name or account id is {@code null}
     * @see org.eclipse.che.api.workspace.shared.dto.NewWorkspace
     * @see org.eclipse.che.api.workspace.shared.dto.WorkspaceDescriptor
     * //@see #getById(String, javax.ws.rs.core.SecurityContext)
     * //@see #getByName(String, javax.ws.rs.core.SecurityContext)
     */
    @ApiOperation(value = "Create a new workspace",
                  response = WorkspaceDescriptor.class,
                  position = 2)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "CREATED"),
            @ApiResponse(code = 403, message = "You have no access to create more workspaces"),
            @ApiResponse(code = 404, message = "NOT FOUND"),
            @ApiResponse(code = 409, message = "You can create workspace associated only to your own account"),
            @ApiResponse(code = 500, message = "INTERNAL SERVER ERROR")})
    @POST
    @GenerateLink(rel = Constants.LINK_REL_CREATE_WORKSPACE)
    //@RolesAllowed({"user", "system/admin"})
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public UsersWorkspaceDto create(@ApiParam(value = "new workspace", required = true)
                           @Required
                           @Description("new workspace")
                                    UsersWorkspaceDto newWorkspace,
                           @QueryParam("account")
                           String accountId,
                           @Context SecurityContext context) throws ConflictException,
                                                                    NotFoundException,
                                                                    ServerException,
                                                                    BadRequestException,
                                                                    ForbiddenException {

        Map<String, String> options = new HashMap<>(1);
        if(accountId != null)
            options.put("accountId", accountId);



        permissionManager.checkPermission("new workspace", options, context);

        if(context.isUserInRole("user"))
            newWorkspace.setOwner(context.getUserPrincipal().getName());
        else if(context.isUserInRole("system/admin")) {
            if (newWorkspace.getOwner() == null)
                throw new BadRequestException("Owner field is mandatory");
        }

        workspaceManager.createWorkspace(newWorkspace, accountId);
        return newWorkspace;

    }


    /**
     * Searches for workspace with given identifier and returns {@link org.eclipse.che.api.workspace.shared.dto.WorkspaceDescriptor} if found.
     * If user that has called this method is not <i>"workspace/admin"</i> or <i>"workspace/developer"</i>
     * workspace attributes will not be added to response.
     *
     * @param id
     *         workspace identifier
     * @return descriptor of found workspace
     * @throws org.eclipse.che.api.core.NotFoundException
     *         when workspace with given identifier doesn't exist
     * @throws org.eclipse.che.api.core.ServerException
     *         when some error occurred while retrieving workspace
     * @see org.eclipse.che.api.workspace.shared.dto.WorkspaceDescriptor
     * @see #getByName(String, javax.ws.rs.core.SecurityContext)
     */
    @ApiOperation(value = "Get workspace by ID",
                  response = WorkspaceDescriptor.class,
                  position = 5)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Workspace with specified ID does not exist"),
            @ApiResponse(code = 403, message = "Access to requested workspace is forbidden"),
            @ApiResponse(code = 500, message = "Server error")})
    @GET
    @Path("/{id}")
    @Produces(APPLICATION_JSON)
    public UsersWorkspaceDto getById(@ApiParam(value = "Workspace ID")
                                       @Description("Workspace ID")
                                       @PathParam("id")
                                       String id,
                                       @Context SecurityContext context) throws NotFoundException,
                                                                                ServerException,
                                                                                ForbiddenException {



        final WorkspaceDo workspace = workspaceManager.getWorkspace(id);

        Map<String, String> options = new HashMap<>(1);
        options.put("owner", workspace.getOwner());

        permissionManager.checkPermission("get workspace", options, context);

        return toWorkspaceDto(workspace, context);
    }

    /**
     * Searches for workspace with given name and return {@link org.eclipse.che.api.workspace.shared.dto.WorkspaceDescriptor} for it.
     * If user that has called this method is not <i>"workspace/admin"</i> or <i>"workspace/developer"</i>
     * workspace attributes will not be added to response.
     *
     * @param name
     *         workspace name
     * @return descriptor of found workspace
     * @throws org.eclipse.che.api.core.NotFoundException
     *         when workspace with given identifier doesn't exist
     * @throws org.eclipse.che.api.core.ServerException
     *         when some error occurred while retrieving workspace
     * @see org.eclipse.che.api.workspace.shared.dto.WorkspaceDescriptor
     * @see #getById(String, javax.ws.rs.core.SecurityContext)
     */
    @ApiOperation(value = "Gets workspace by name",
                  response = WorkspaceDescriptor.class,
                  position = 4)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Workspace with specified name doesn't exist"),
            @ApiResponse(code = 403, message = "Access to requested workspace is forbidden"),
            @ApiResponse(code = 500, message = "Server error")})
    @GET
    @GenerateLink(rel = Constants.LINK_REL_GET_WORKSPACE_BY_NAME)
    @Produces(APPLICATION_JSON)
    public UsersWorkspaceDto getByName(@ApiParam(value = "Name of workspace", required = true)
                                         @Required
                                         @Description("Name of workspace")
                                         @QueryParam("name")
                                         String name,
                                         @Description("Owner of workspace")
                                         @QueryParam("owner")
                                         String owner,
                                         @Context SecurityContext context) throws NotFoundException,
                                                                                  ServerException,
                                                                                  BadRequestException {

        final WorkspaceDo workspace = workspaceManager.getWorkspace(name, owner);

        Map<String, String> options = new HashMap<>(1);
        options.put("owner", owner);

        permissionManager.checkPermission("get workspace", options, context);

//        requiredNotNull(name, "Workspace name");

        return toWorkspaceDto(workspace, context);
    }

    /**
     * <p>Updates workspace.</p>
     * <strong>Note:</strong> existed workspace attributes with same name as
     * update attributes will be replaced with update attributes.
     *
     * @param id
     *         workspace identifier
     * @param update
     *         workspace update
     * @return descriptor of updated workspace
     * @throws org.eclipse.che.api.core.NotFoundException
     *         when workspace with given name doesn't exist
     * @throws org.eclipse.che.api.core.ConflictException
     *         when attribute with not valid name
     * @throws org.eclipse.che.api.core.BadRequestException
     *         when update is {@code null} or updated attributes contains not valid attribute
     * @throws org.eclipse.che.api.core.ServerException
     *         when some error occurred while retrieving/updating workspace
     * @see org.eclipse.che.api.workspace.shared.dto.WorkspaceUpdate
     * @see org.eclipse.che.api.workspace.shared.dto.WorkspaceDescriptor
     * //@see #removeAttribute(String, String, javax.ws.rs.core.SecurityContext)
     */
    @ApiOperation(value = "Update workspace",
                  response = WorkspaceDescriptor.class,
                  notes = "Update an existing workspace. A JSON with updated properties is sent.",
                  position = 3)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Workspace updated"),
            @ApiResponse(code = 404, message = "Not found"),
            @ApiResponse(code = 403, message = "Access to required workspace is forbidden"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @POST
    @Path("/{id}")
    //@RolesAllowed({"account/owner", "workspace/admin", "system/admin"})
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public UsersWorkspaceDto update(@ApiParam("Workspace ID") @PathParam("id") String id,
                                      @ApiParam("Workspace update") WorkspaceConfigDto update,
                                      @Context SecurityContext context) throws NotFoundException,
                                                                               ConflictException,
                                                                               BadRequestException,
                                                                               ServerException {

        final WorkspaceDo old = workspaceManager.getWorkspace(id);

        Map<String, String> options = new HashMap<>(1);
        options.put("owner", old.getOwner());

        permissionManager.checkPermission("get workspace", options, context);


        WorkspaceDo workspace = workspaceManager.updateWorkspace(id, update);

        LOG.info("EVENT#workspace-updated# WS#{}# WS-ID#{}#", workspace.getName(), workspace.getId());
        return toWorkspaceDto(workspace, context);
    }




    /**
     * Removes certain workspace.
     *
     * @param wsId
     *         workspace identifier to remove workspace
     * @throws org.eclipse.che.api.core.NotFoundException
     *         when workspace with given identifier doesn't exist
     * @throws org.eclipse.che.api.core.ServerException
     *         when some error occurred while retrieving/removing workspace or member
     * @throws org.eclipse.che.api.core.ConflictException
     *         if some error occurred while removing member
     */
    @ApiOperation(value = "Delete a workspace",
                  notes = "Delete a workspace by its ID",
                  position = 14)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "No Content"),
            @ApiResponse(code = 404, message = "Not Found"),
            @ApiResponse(code = 409, message = "Failed to remove workspace member"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @DELETE
    @Path("/{id}")
    //@RolesAllowed({"account/owner", "workspace/admin", "system/admin"})
    public void remove(@ApiParam(value = "Workspace ID")
                       @PathParam("id")
                       String id,
                       @Context SecurityContext context) throws NotFoundException, ServerException, ConflictException {

        final WorkspaceDo ws = workspaceManager.getWorkspace(id);

        Map<String, String> options = new HashMap<>(1);
        options.put("owner", ws.getOwner());

        permissionManager.checkPermission("get workspace", options, context);


        workspaceManager.removeWorkspace(id);
    }



    public Workspace startWorkspace(String id) {

    }

    public void stopWorkspace(String id) {

    }



    public void shareWorkspace(String wsId, List<String> users) {

    }


    public Workspace getWorkspace(String id) {

    }


    // owned|shared|both
    public Workspace getWorkspaces(String user, String ownership) {

    }





//
//    private void createTemporaryWorkspace(Workspace workspace) throws ConflictException, ServerException {
//        try {
//            //let vfs create temporary workspace in correct place
//            EnvironmentContext.getCurrent().setWorkspaceTemporary(true);
//            workspaceDao.create(workspace);
//        } finally {
//            EnvironmentContext.getCurrent().setWorkspaceTemporary(false);
//        }
//    }
//
//    private User createTemporaryUser() throws ConflictException, ServerException, NotFoundException {
//        final String id = generate("tmp_user", org.eclipse.che.api.user.server.Constants.ID_LENGTH);
//
//        //creating user
//        final User user = new User().withId(id);
//        userDao.create(user);
//
//        //creating profile for it
//        profileDao.create(new Profile().withId(id).withUserId(id));
//
//        //storing preferences
//        final Map<String, String> preferences = new HashMap<>(4);
//        preferences.put("temporary", String.valueOf(true));
//        preferences.put("codenvy:created", Long.toString(System.currentTimeMillis()));
//        preferenceDao.setPreferences(id, preferences);
//
//        return user;
//    }

//    /**
//     * Converts {@link org.eclipse.che.api.workspace.server.dao.Member} to {@link org.eclipse.che.api.workspace.shared.dto.MemberDescriptor}
//     */
//    /* used in tests */MemberDescriptor toWorkspaceDto(Member member, Workspace workspace, SecurityContext context) {
//        final UriBuilder serviceUriBuilder = getServiceContext().getServiceUriBuilder();
//        final UriBuilder baseUriBuilder = getServiceContext().getBaseUriBuilder();
//        final List<Link> links = new LinkedList<>();
//
//        if (context.isUserInRole("account/owner") ||
//            context.isUserInRole("workspace/admin") ||
//            context.isUserInRole("workspace/developer")) {
//            links.add(LinksHelper.createLink(HttpMethod.GET,
//                                             serviceUriBuilder.clone()
//                                                              .path(getClass(), "getMembers")
//                                                              .build(workspace.getId())
//                                                              .toString(),
//                                             null,
//                                             APPLICATION_JSON,
//                                             Constants.LINK_REL_GET_WORKSPACE_MEMBERS));
//        }
//        if (context.isUserInRole("account/owner") || context.isUserInRole("workspace/admin")) {
//            links.add(LinksHelper.createLink(HttpMethod.DELETE,
//                                             serviceUriBuilder.clone()
//                                                              .path(getClass(), "removeMember")
//                                                              .build(workspace.getId(), member.getUserId())
//                                                              .toString(),
//                                             null,
//                                             null,
//                                             Constants.LINK_REL_REMOVE_WORKSPACE_MEMBER));
//        }
//        links.add(LinksHelper.createLink(HttpMethod.GET,
//                                         baseUriBuilder.clone()
//                                                       .path(UserService.class)
//                                                       .path(UserService.class, "getById")
//                                                       .build(member.getUserId())
//                                                       .toString(),
//                                         null,
//                                         APPLICATION_JSON,
//                                         LINK_REL_GET_USER_BY_ID));
//        final Link wsLink = LinksHelper.createLink(HttpMethod.GET,
//                                                   serviceUriBuilder.clone()
//                                                                    .path(getClass(), "getById")
//                                                                    .build(workspace.getId())
//                                                                    .toString(),
//                                                   null,
//                                                   APPLICATION_JSON,
//                                                   Constants.LINK_REL_GET_WORKSPACE_BY_ID);
//        //TODO replace hardcoded path with UriBuilder + ProjectService
//        final Link projectsLink = LinksHelper.createLink(HttpMethod.GET,
//                                                         getServiceContext().getBaseUriBuilder().clone()
//                                                                            .path("/project/{ws-id}")
//                                                                            .build(member.getWorkspaceId())
//                                                                            .toString(),
//                                                         null,
//                                                         APPLICATION_JSON,
//                                                         "get projects");
//        final WorkspaceReference wsRef = DtoFactory.getInstance().createDto(WorkspaceReference.class)
//                                                   .withId(workspace.getId())
//                                                   .withName(workspace.getName())
//                                                   .withTemporary(workspace.isTemporary())
//                                                   .withLinks(asList(wsLink, projectsLink));
//        return DtoFactory.getInstance().createDto(MemberDescriptor.class)
//                         .withUserId(member.getUserId())
//                         .withWorkspaceReference(wsRef)
//                         .withRoles(member.getRoles())
//                         .withLinks(links);
//    }

    /**
     * Converts {@link org.eclipse.che.api.workspace.server.dao.Workspace} to {@link org.eclipse.che.api.workspace.shared.dto.WorkspaceDescriptor}
     */
    /* used in tests */
    UsersWorkspaceDto toWorkspaceDto(UsersWorkspace workspace, SecurityContext context) {
        final UsersWorkspaceDto workspaceDescriptor = DtoFactory.getInstance().createDto(UsersWorkspaceDto.class)
                                                                  .withId(workspace.getId())
                                                                  .withName(workspace.getName())
                                                                  .withTemporary(workspace.isTemporary())
//                                                                  .withAccountId(workspace.getAccountId())
                                                                  .withAttributes(workspace.getAttributes());
        final List<Link> links = new LinkedList<>();
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        if (context.isUserInRole("user")) {
            //TODO replace hardcoded path with UriBuilder + ProjectService
            links.add(LinksHelper.createLink(HttpMethod.GET,
                                             getServiceContext().getBaseUriBuilder().clone()
                                                                .path("/project/{ws-id}")
                                                                .build(workspaceDescriptor.getId())
                                                                .toString(),
                                             null,
                                             APPLICATION_JSON,
                                             "get projects"));
            links.add(LinksHelper.createLink(HttpMethod.GET,
                                             uriBuilder.clone()
                                                       .path(getClass(), "getMembershipsOfCurrentUser")
                                                       .build()
                                                       .toString(),
                                             null,
                                             APPLICATION_JSON,
                                             Constants.LINK_REL_GET_CURRENT_USER_WORKSPACES));
            links.add(LinksHelper.createLink(HttpMethod.GET,
                                             uriBuilder.clone()
                                                       .path(getClass(), "getMembershipOfCurrentUser")
                                                       .build(workspaceDescriptor.getId())
                                                       .toString(),
                                             null,
                                             APPLICATION_JSON,
                                             Constants.LINK_REL_GET_CURRENT_USER_MEMBERSHIP));
        }
        if (context.isUserInRole("workspace/admin") || context.isUserInRole("workspace/developer") ||
            context.isUserInRole("system/admin") || context.isUserInRole("system/manager") || context.isUserInRole("account/owner")) {
            links.add(LinksHelper.createLink(HttpMethod.GET,
                                             uriBuilder.clone().
                                                     path(getClass(), "getByName")
                                                       .queryParam("name", workspaceDescriptor.getName())
                                                       .build()
                                                       .toString(),
                                             null,
                                             APPLICATION_JSON,
                                             Constants.LINK_REL_GET_WORKSPACE_BY_NAME));
            links.add(LinksHelper.createLink(HttpMethod.GET,
                                             uriBuilder.clone()
                                                       .path(getClass(), "getById")
                                                       .build(workspaceDescriptor.getId())
                                                       .toString(),
                                             null,
                                             APPLICATION_JSON,
                                             Constants.LINK_REL_GET_WORKSPACE_BY_ID));
            links.add(LinksHelper.createLink(HttpMethod.GET,
                                             uriBuilder.clone()
                                                       .path(getClass(), "getMembers")
                                                       .build(workspaceDescriptor.getId())
                                                       .toString(),
                                             null,
                                             APPLICATION_JSON,
                                             Constants.LINK_REL_GET_WORKSPACE_MEMBERS));
        }
        if (context.isUserInRole("account/owner") || context.isUserInRole("workspace/admin") || context.isUserInRole("system/admin")) {
            links.add(LinksHelper.createLink(HttpMethod.DELETE,
                                             uriBuilder.clone()
                                                       .path(getClass(), "remove")
                                                       .build(workspaceDescriptor.getId())
                                                       .toString(),
                                             null,
                                             null,
                                             Constants.LINK_REL_REMOVE_WORKSPACE));
        }
        return workspaceDescriptor.withLinks(links);
    }

//    /**
//     * Checks object reference is not {@code null}
//     *
//     * @param object
//     *         object reference to check
//     * @param subject
//     *         used as subject of exception message "{subject} required"
//     * @throws org.eclipse.che.api.core.BadRequestException
//     *         when object reference is {@code null}
//     */
//    private void requiredNotNull(Object object, String subject) throws BadRequestException {
//        if (object == null) {
//            throw new BadRequestException(subject + " required");
//        }
//    }

    /**
     * Validates attribute name.
     *
     * @param attributeName
     *         attribute name to check
     * @throws org.eclipse.che.api.core.ConflictException
     *         when attribute name is {@code null}, empty or it starts with "codenvy"
     */
    private void validateAttributeName(String attributeName) throws ConflictException {
        if (attributeName == null || attributeName.isEmpty() || attributeName.toLowerCase().startsWith("codenvy")) {
            throw new ConflictException(String.format("Attribute2 name '%s' is not valid", attributeName));
        }
    }

    private void validateAttributes(Map<String, String> attributes) throws ConflictException {
        for (String attributeName : attributes.keySet()) {
            validateAttributeName(attributeName);
        }
    }

//    private void ensureCurrentUserOwnerOf(Account target) throws ServerException, NotFoundException, ConflictException {
//        final List<Account> accounts = accountDao.getByOwner(currentUser().getId());
//        for (Account account : accounts) {
//            if (account.getId().equals(target.getId())) {
//                return;
//            }
//        }
//        throw new ConflictException("You can create workspace associated only with your own account");
//    }

//    private boolean isCurrentUserAccountOwnerOf(String wsId) throws ServerException, NotFoundException {
//        final List<Account> accounts = accountDao.getByOwner(currentUser().getId());
//        final List<Workspace> workspaces = new LinkedList<>();
//        //fetch all workspaces related to accounts
//        for (Account account : accounts) {
//            workspaces.addAll(workspaceDao.getByAccount(account.getId()));
//        }
//        for (Workspace workspace : workspaces) {
//            if (workspace.getId().equals(wsId)) {
//                return true;
//            }
//        }
//        return false;
//    }

    /**
     * Generates workspace name based on current user email.
     * Generating process is simple, assuming we have user with email user@codenvy.com,
     * then first time we will check for workspace with name equal to "user" and if it is free
     * it will be returned, but if it is reserved then  number suffix will be added to the end of "user" name
     * and it will be checked again until free workspace name is not found.
     */
    private String generateWorkspaceName() throws ServerException {
        //should be email
        String userName = currentUser().getName();
        int atIdx = userName.indexOf('@');
        //if username contains email then fetch part before '@'
        if (atIdx != -1) {
            userName = userName.substring(0, atIdx);
        }
        //search first workspace name which is free
        int suffix = 2;
        String workspaceName = userName;
        while (workspaceExists(workspaceName)) {
            workspaceName = userName + suffix++;
        }
        return workspaceName;
    }

    private boolean workspaceExists(String name) throws ServerException {
        try {
            workspaceDao.getByName(name);
        } catch (NotFoundException nfEx) {
            return false;
        }
        return true;
    }

    private org.eclipse.che.commons.user.User currentUser() {
        return EnvironmentContext.getCurrent().getUser();
    }
}