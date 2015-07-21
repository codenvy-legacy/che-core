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
import org.eclipse.che.api.core.rest.Service;
import org.eclipse.che.api.core.rest.annotations.Description;
import org.eclipse.che.api.core.rest.annotations.GenerateLink;
import org.eclipse.che.api.core.rest.annotations.Required;
import org.eclipse.che.api.core.rest.permission.PermissionManager;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.core.util.LinksHelper;
import org.eclipse.che.api.user.server.UserService;
import org.eclipse.che.api.user.server.dao.*;
import org.eclipse.che.api.user.shared.dto.MembershipDto;
import org.eclipse.che.api.user.shared.model.Membership;
import org.eclipse.che.api.workspace.server.dao.Member;
import org.eclipse.che.api.workspace.server.dao.MemberDao;


import org.eclipse.che.api.workspace.server.spi.WorkspaceDao;
import org.eclipse.che.api.workspace.server.spi.WorkspaceDo;
import org.eclipse.che.api.workspace.shared.dto.*;
import org.eclipse.che.api.workspace.shared.dto2.WorkspaceDto;
import org.eclipse.che.api.workspace.shared.model.Workspace;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.dto.server.DtoFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import java.util.*;

import static java.lang.Boolean.parseBoolean;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.status;
import static org.eclipse.che.api.user.server.Constants.LINK_REL_GET_USER_BY_ID;
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
    public WorkspaceDto create(@ApiParam(value = "new workspace", required = true)
                           @Required
                           @Description("new workspace")
                           WorkspaceDto newWorkspace,
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

        workspaceManager.createWorkspace(newWorkspace, options);
        return newWorkspace;

    }

    /**
     * Creates new temporary workspace and adds current user
     * as member to created workspace with roles <i>"workspace/admin"</i> and <i>"workspace/developer"</i>.
     * If user does not exist, it will be created with role <i>"tmp_user"</i>.
     * Returns status code <strong>201 CREATED</strong> and {@link org.eclipse.che.api.workspace.shared.dto.WorkspaceDescriptor} if workspace
     * has been created successfully. Each new workspace should contain
     * at least workspace name and account identifier.
     *
     * @param newWorkspace
     *         new workspace
     * @return descriptor of created workspace
     * @throws org.eclipse.che.api.core.ConflictException
     *         when current user account identifier and given account identifier are different
     * @throws org.eclipse.che.api.core.BadRequestException
     *         when either new workspace or workspace name or account identifier is {@code null}
     * @throws org.eclipse.che.api.core.NotFoundException
     *         when account with given identifier does not exist
     * @throws org.eclipse.che.api.core.ServerException
     *         when some error occurred while retrieving/persisting account, workspace, member or profile
     * @see org.eclipse.che.api.workspace.shared.dto.WorkspaceDescriptor
     * @see #getById(String, javax.ws.rs.core.SecurityContext)
     * @see #getByName(String, javax.ws.rs.core.SecurityContext)
     */
    @ApiOperation(value = "Create a temporary workspace",
                  notes = "Create a temporary workspace created by a Factory",
                  response = WorkspaceDescriptor.class,
                  position = 1)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "CREATED"),
            @ApiResponse(code = 403, message = "You have no access to create more workspaces"),
            @ApiResponse(code = 404, message = "NOT FOUND"),
            @ApiResponse(code = 409, message = "You can create workspace associated only to your own account"),
            @ApiResponse(code = 500, message = "INTERNAL SERVER ERROR")
    })
    @POST
    @Path("/temp")
    @GenerateLink(rel = Constants.LINK_REL_CREATE_TEMP_WORKSPACE)
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response createTemporary(@ApiParam(value = "New Temporary workspace", required = true)
                                    @Required
                                    @Description("New temporary workspace")
                                    NewWorkspace newWorkspace,
                                    @Context SecurityContext context) throws ConflictException,
                                                                             NotFoundException,
                                                                             BadRequestException,
                                                                             ServerException {
        requiredNotNull(newWorkspace, "New workspace");
        if (newWorkspace.getAttributes() != null) {
            validateAttributes(newWorkspace.getAttributes());
        }
        final Workspace workspace = new Workspace().withId(generate(Workspace.class.getSimpleName().toLowerCase(), Constants.ID_LENGTH))
                                                   .withName(newWorkspace.getName())
                                                   .withTemporary(true)
                                                   .withAccountId(newWorkspace.getAccountId())
                                                   .withAttributes(newWorkspace.getAttributes());

        //temporary user should be created if real user does not exist
        final User user;
        boolean isTemporary = false;
        if (context.getUserPrincipal() == null) {
            user = createTemporaryUser();
            isTemporary = true;
        } else {
            user = userDao.getById(currentUser().getId());
        }

        if (!isTemporary && !context.isUserInRole("system/admin")) {
            final Account account = accountDao.getById(newWorkspace.getAccountId());
            ensureCurrentUserOwnerOf(account);
        }

        createTemporaryWorkspace(workspace);
        final Member newMember = new Member().withUserId(user.getId())
                                             .withWorkspaceId(workspace.getId())
                                             .withRoles(asList("workspace/developer", "workspace/admin"));
        memberDao.create(newMember);

        LOG.info("EVENT#workspace-created# WS#{}# WS-ID#{}# USER#{}#", workspace.getName(), workspace.getId(), user.getId());
        return status(CREATED).entity(toDescriptor(workspace, context)).build();
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
    public WorkspaceDto getById(@ApiParam(value = "Workspace ID")
                                       @Description("Workspace ID")
                                       @PathParam("id")
                                       String id,
                                       @Context SecurityContext context) throws NotFoundException,
                                                                                ServerException,
                                                                                ForbiddenException {

        Map<String, String> options = new HashMap<>(1);
        options.put("id", id);
        permissionManager.checkPermission("get workspace", options, context);
        final WorkspaceDo workspace = workspaceManager.getWorkspaceById(id);

//        final WorkspaceDo workspace = workspaceDao.getById(id);


//        if (!context.isUserInRole("account/owner") &&
//            !context.isUserInRole("workspace/developer") &&
//            !context.isUserInRole("workspace/admin")) {
//            // tmp_workspace_cloned_from_private_repo - gives information
//            // whether workspace was clone from private repository or not. It can be use
//            // by temporary workspace sharing filter for user that are not workspace/admin
//            // so we need that property here.
//            // PLZ DO NOT REMOVE!!!!
//            final Map<String, String> attributes = workspace.getAttributes();
//            if (attributes.containsKey("allowAnyoneAddMember")) {
//                workspace.setAttributes(singletonMap("allowAnyoneAddMember", attributes.get("allowAnyoneAddMember")));
//            } else {
//                attributes.clear();
//            }
//        }


        return toDescriptor(workspace, context);
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
    public WorkspaceDto getByName(@ApiParam(value = "Name of workspace", required = true)
                                         @Required
                                         @Description("Name of workspace")
                                         @QueryParam("name")
                                         String name,
                                         @Context SecurityContext context) throws NotFoundException,
                                                                                  ServerException,
                                                                                  BadRequestException {




        requiredNotNull(name, "Workspace name");


        final WorkspaceDo workspace = workspaceDao.getByName(name);
        if (!context.isUserInRole("account/owner") &&
            !context.isUserInRole("workspace/developer") &&
            !context.isUserInRole("workspace/admin")) {
            // tmp_workspace_cloned_from_private_repo - gives information
            // whether workspace was clone from private repository or not. It can be use
            // by temporary workspace sharing filter for user that are not workspace/admin
            // so we need that property here.
            // PLZ DO NOT REMOVE!!!!
            final Map<String, String> attributes = workspace.getAttributes();
            if (attributes.containsKey("allowAnyoneAddMember")) {
                workspace.setAttributes(singletonMap("allowAnyoneAddMember", attributes.get("allowAnyoneAddMember")));
            } else {
                attributes.clear();
            }
        }
        return toDescriptor(workspace, context);
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
    @RolesAllowed({"account/owner", "workspace/admin", "system/admin"})
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public WorkspaceDto update(@ApiParam("Workspace ID") @PathParam("id") String id,
                                      @ApiParam("Workspace update") WorkspaceUpdate update,
                                      @Context SecurityContext context) throws NotFoundException,
                                                                               ConflictException,
                                                                               BadRequestException,
                                                                               ServerException {
        requiredNotNull(update, "Workspace update");
        final WorkspaceDo workspace = workspaceDao.getById(id);
        final Map<String, String> attributes = update.getAttributes();
        if (attributes != null) {
            validateAttributes(attributes);
            workspace.getAttributes().putAll(attributes);
        }
        final String newName = update.getName();
        if (newName != null) {
            workspace.setName(newName);
        }
        workspaceDao.update(workspace);

        LOG.info("EVENT#workspace-updated# WS#{}# WS-ID#{}#", workspace.getName(), workspace.getId());
        return toDescriptor(workspace, context);
    }

//    /**
//     * Returns workspace descriptors for certain workspaces with given account identifier.
//     *
//     * @param accountId
//     *         account identifier
//     * @return workspaces descriptors
//     * @throws org.eclipse.che.api.core.BadRequestException
//     *         when account identifier is {@code null}
//     * @throws org.eclipse.che.api.core.ServerException
//     *         when some error occurred while retrieving workspace
//     * @see org.eclipse.che.api.workspace.shared.dto.WorkspaceDescriptor
//     */
//    @ApiOperation(value = "Get workspace by Account ID",
//                  notes = "Search for a workspace by its Account ID which is added as query parameter",
//                  response = WorkspaceDescriptor.class,
//                  responseContainer = "List",
//                  position = 6)
//    @ApiResponses(value = {
//            @ApiResponse(code = 403, message = "User is not authorized to call this operation"),
//            @ApiResponse(code = 500, message = "Internal Server Error")})
//    @GET
//    @Path("/find/account")
//    @GenerateLink(rel = Constants.LINK_REL_GET_WORKSPACES_BY_ACCOUNT)
//    @RolesAllowed({"user", "system/admin", "system/manager"})
//    @Produces(APPLICATION_JSON)
//    public List<WorkspaceDto> getWorkspacesByAccount(@ApiParam(value = "Account ID", required = true)
//                                                            @Required
//                                                            @QueryParam("id")
//                                                            String accountId,
//                                                            @Context SecurityContext context) throws ServerException,
//                                                                                                     BadRequestException {
//        requiredNotNull(accountId, "Account ID");
//        final List<WorkspaceDto> workspaces = workspaceDao.getByAccount(accountId);
//        final List<WorkspaceDto> descriptors = new ArrayList<>(workspaces.size());
//        for (Workspace workspace : workspaces) {
//            descriptors.add(toDescriptor(workspace, context));
//        }
//        return descriptors;
//    }



//    /**
//     * Returns all memberships of current user.
//     *
//     * @return current user memberships
//     * @throws org.eclipse.che.api.core.ServerException
//     *         when some error occurred while retrieving user or members
//     * @see org.eclipse.che.api.workspace.shared.dto.MemberDescriptor
//     */
//    @ApiOperation(value = "Get membership of a current user",
//                  notes = "Get membership and workspace roles of a current user",
//                  response = MemberDescriptor.class,
//                  responseContainer = "List",
//                  position = 9)
//    @ApiResponses(value = {
//            @ApiResponse(code = 200, message = "OK"),
//            @ApiResponse(code = 404, message = "Not Found"),
//            @ApiResponse(code = 500, message = "Internal Server Error")})
//    @GET
//    @Path("/all")
//    @GenerateLink(rel = Constants.LINK_REL_GET_CURRENT_USER_WORKSPACES)
//    @RolesAllowed({"user", "temp_user"})
//    @Produces(APPLICATION_JSON)
//    public List<MembershipDto> getMembershipsOfCurrentUser(@Context SecurityContext context) throws NotFoundException,
//                                                                                                       ServerException {
//        final List<Membership> members = membershipDao.getUserRelationships(currentUser().getId());
//        final List<MemberDescriptor> memberships = new ArrayList<>(members.size());
//        for (Member member : members) {
//            try {
//                final Workspace workspace = workspaceDao.getById(member.getWorkspaceId());
//                memberships.add(toDescriptor(member, workspace, context));
//            } catch (NotFoundException nfEx) {
//                LOG.error("Workspace {} doesn't exist but user {} refers to it. ", member.getWorkspaceId(), currentUser().getId());
//            }
//        }
//        return memberships;
//    }

//    /**
//     * Returns all memberships of certain user.
//     *
//     * @param userId
//     *         user identifier to search memberships
//     * @return certain user memberships
//     * @throws org.eclipse.che.api.core.NotFoundException
//     *         when user with given identifier doesn't exist
//     * @throws org.eclipse.che.api.core.BadRequestException
//     *         when user identifier is {@code null}
//     * @throws org.eclipse.che.api.core.ServerException
//     *         when some error occurred while retrieving user or members
//     * @see org.eclipse.che.api.workspace.shared.dto.MemberDescriptor
//     */
//    @ApiOperation(value = "Get memberships by user ID",
//                  notes = "Search for a workspace by User ID which is added to URL as query parameter. JSON with workspace details and user roles is returned",
//                  response = MemberDescriptor.class,
//                  responseContainer = "List",
//                  position = 7)
//    @ApiResponses(value = {
//            @ApiResponse(code = 200, message = "OK"),
//            @ApiResponse(code = 403, message = "User not authorized to call this action"),
//            @ApiResponse(code = 404, message = "Not Foound"),
//            @ApiResponse(code = 500, message = "Internal Server Error")})
//    @GET
//    @Path("/find")
//    @GenerateLink(rel = Constants.LINK_REL_GET_CONCRETE_USER_WORKSPACES)
//    @RolesAllowed({"system/admin", "system/manager"})
//    @Produces(APPLICATION_JSON)
//    public List<MemberDescriptor> getMembershipsOfSpecificUser(@ApiParam(value = "User ID", required = true)
//                                                               @Required
//                                                               @QueryParam("userid")
//                                                               String userId,
//                                                               @Context SecurityContext context) throws NotFoundException,
//                                                                                                        BadRequestException,
//                                                                                                        ServerException {
//        requiredNotNull(userId, "User ID");
//        final List<Member> members = memberDao.getUserRelationships(userId);
//        final List<MemberDescriptor> memberships = new ArrayList<>(members.size());
//        for (Member member : members) {
//            try {
//                final Workspace workspace = workspaceDao.getById(member.getWorkspaceId());
//                memberships.add(toDescriptor(member, workspace, context));
//            } catch (NotFoundException nfEx) {
//                LOG.error("Workspace {} doesn't exist but user {} refers to it. ", member.getWorkspaceId(), userId);
//            }
//        }
//        return memberships;
//    }
//
//    /**
//     * Returns all workspace members.
//     *
//     * @param wsId
//     *         workspace identifier
//     * @return workspace members
//     * @throws org.eclipse.che.api.core.NotFoundException
//     *         when workspace with given identifier doesn't exist
//     * @throws org.eclipse.che.api.core.ServerException
//     *         when some error occurred while retrieving workspace or members
//     * @see org.eclipse.che.api.workspace.shared.dto.MemberDescriptor
//     * @see #addMember(String, org.eclipse.che.api.workspace.shared.dto.NewMembership, javax.ws.rs.core.SecurityContext)
//     * @see #removeMember(String, String, javax.ws.rs.core.SecurityContext)
//     */
//    @ApiOperation(value = "Get workspace members by workspace ID",
//                  notes = "Get all workspace members of a specified workspace. A JSOn with members and their roles is returned",
//                  response = MemberDescriptor.class,
//                  responseContainer = "List",
//                  position = 8)
//    @ApiResponses(value = {
//            @ApiResponse(code = 200, message = "OK"),
//            @ApiResponse(code = 404, message = "Not Found"),
//            @ApiResponse(code = 500, message = "Internal Server Error")})
//    @GET
//    @Path("/{id}/members")
//    @RolesAllowed({"workspace/admin", "workspace/developer", "account/owner", "system/admin", "system/manager"})
//    @Produces(APPLICATION_JSON)
//    public List<MemberDescriptor> getMembers(@ApiParam(value = "Workspace ID")
//                                             @PathParam("id")
//                                             String wsId,
//                                             @Context SecurityContext context) throws NotFoundException,
//                                                                                      ServerException,
//                                                                                      ForbiddenException {
//        final Workspace workspace = workspaceDao.getById(wsId);
//        final List<Member> members = memberDao.getWorkspaceMembers(wsId);
//        final List<MemberDescriptor> descriptors = new ArrayList<>(members.size());
//        for (Member member : members) {
//            descriptors.add(toDescriptor(member, workspace, context));
//        }
//        return descriptors;
//    }
//
//    /**
//     * Returns membership for current user in the given workspace.
//     *
//     * @param wsId
//     *         workspace identifier
//     * @return workspace member
//     * @throws org.eclipse.che.api.core.NotFoundException
//     *         when workspace with given identifier doesn't exist
//     * @throws org.eclipse.che.api.core.ServerException
//     *         when some error occurred while retrieving workspace or members
//     * @see org.eclipse.che.api.workspace.shared.dto.MemberDescriptor
//     * @see #addMember(String, org.eclipse.che.api.workspace.shared.dto.NewMembership, javax.ws.rs.core.SecurityContext)
//     * @see #removeMember(String, String, javax.ws.rs.core.SecurityContext)
//     */
//    @ApiOperation(value = "Get user membership in a specified workspace",
//                  notes = "Returns membership of a user with roles",
//                  response = MemberDescriptor.class,
//                  position = 10)
//    @ApiResponses(value = {
//            @ApiResponse(code = 200, message = "OK"),
//            @ApiResponse(code = 404, message = "Not Found"),
//            @ApiResponse(code = 500, message = "Internal Server Error")})
//    @GET
//    @Path("/{id}/membership")
//    @RolesAllowed({"workspace/stakeholder", "workspace/developer", "workspace/admin"})
//    @Produces(APPLICATION_JSON)
//    public MemberDescriptor getMembershipOfCurrentUser(@ApiParam(value = "Workspace ID")
//                                                       @PathParam("id")
//                                                       String wsId,
//                                                       @Context SecurityContext context) throws NotFoundException,
//                                                                                                ServerException {
//        final Workspace workspace = workspaceDao.getById(wsId);
//        final Member member = memberDao.getWorkspaceMember(wsId, currentUser().getId());
//        return toDescriptor(member, workspace, context);
//    }
//
//    /**
//     * Removes attribute from certain workspace.
//     *
//     * @param wsId
//     *         workspace identifier
//     * @param attributeName
//     *         attribute name to remove
//     * @throws org.eclipse.che.api.core.NotFoundException
//     *         when workspace with given identifier doesn't exist
//     * @throws org.eclipse.che.api.core.ServerException
//     *         when some error occurred while getting or updating workspace
//     * @throws org.eclipse.che.api.core.ConflictException
//     *         when given attribute name is not valid
//     */
//    @ApiOperation(value = "Delete workspace attribute",
//                  notes = "Deletes attributes of a specified workspace",
//                  position = 11)
//    @ApiResponses(value = {
//            @ApiResponse(code = 204, message = "No Content"),
//            @ApiResponse(code = 404, message = "Not Found"),
//            @ApiResponse(code = 409, message = "Invalid attribute name"),
//            @ApiResponse(code = 500, message = "Internal Server Error")})
//    @DELETE
//    @Path("/{id}/attribute")
//    @RolesAllowed({"account/owner", "workspace/admin", "system/admin"})
//    public void removeAttribute(@ApiParam(value = "Workspace ID")
//                                @PathParam("id")
//                                String wsId,
//                                @ApiParam(value = "Attribute2 name", required = true)
//                                @Required
//                                @QueryParam("name")
//                                String attributeName,
//                                @Context SecurityContext context) throws NotFoundException,
//                                                                         ServerException,
//                                                                         ConflictException {
//        validateAttributeName(attributeName);
//        final Workspace workspace = workspaceDao.getById(wsId);
//        if (null != workspace.getAttributes().remove(attributeName)) {
//            workspaceDao.update(workspace);
//        }
//    }
//
//    /**
//     * Creates new workspace member.
//     *
//     * @param wsId
//     *         workspace identifier
//     * @param newMembership
//     *         new membership
//     * @return descriptor of created member
//     * @throws org.eclipse.che.api.core.NotFoundException
//     *         when workspace with given identifier doesn't exist
//     * @throws org.eclipse.che.api.core.ServerException
//     *         when some error occurred while retrieving {@link org.eclipse.che.api.workspace.server.dao.Workspace}, {@link org.eclipse.che.api.user.shared.dto.UserDescriptor}
//     *         or persisting new {@link org.eclipse.che.api.workspace.server.dao.Member}
//     * @throws org.eclipse.che.api.core.ConflictException
//     *         when new membership is {@code null}
//     *         or if new membership user id is {@code null} or
//     *         of new membership roles is {@code null} or empty
//     * @throws org.eclipse.che.api.core.ForbiddenException
//     *         when current user hasn't access to workspace with given identifier
//     * @see org.eclipse.che.api.workspace.shared.dto.MemberDescriptor
//     * @see #removeMember(String, String, javax.ws.rs.core.SecurityContext)
//     * @see #getMembers(String, javax.ws.rs.core.SecurityContext)
//     */
//    @ApiOperation(value = "Create new workspace member",
//                  notes = "Add a new member into a workspace",
//                  response = MemberDescriptor.class,
//                  position = 12)
//    @ApiResponses(value = {
//            @ApiResponse(code = 201, message = "OK"),
//            @ApiResponse(code = 403, message = "User not authorized to perform this operation"),
//            @ApiResponse(code = 404, message = "Not Found"),
//            @ApiResponse(code = 409, message = "No user ID and/or role specified")})
//    @POST
//    @Path("/{id}/members")
//    @RolesAllowed({"user", "temp_user"})
//    @Consumes(APPLICATION_JSON)
//    @Produces(APPLICATION_JSON)
//    public Response addMember(@ApiParam("Workspace ID") @PathParam("id") String wsId,
//                              @ApiParam(value = "New membership", required = true) NewMembership newMembership,
//                              @Context SecurityContext context) throws NotFoundException,
//                                                                       ServerException,
//                                                                       ConflictException,
//                                                                       ForbiddenException,
//                                                                       BadRequestException {
//        requiredNotNull(newMembership, "New membership");
//        requiredNotNull(newMembership.getUserId(), "User ID");
//        final Workspace workspace = workspaceDao.getById(wsId);
//        if (memberDao.getWorkspaceMembers(wsId).isEmpty()) {
//            //if workspace doesn't contain members then member that is been added
//            //should be added with roles 'workspace/admin' and 'workspace/developer'
//            newMembership.setRoles(asList("workspace/admin", "workspace/developer"));
//        } else {
//            requiredNotNull(newMembership.getRoles(), "Roles");
//            if (newMembership.getRoles().isEmpty()) {
//                throw new ConflictException("Roles should not be empty");
//            }
//            if (!context.isUserInRole("workspace/admin") &&
//                !parseBoolean(workspace.getAttributes().get("allowAnyoneAddMember")) &&
//                !isCurrentUserAccountOwnerOf(wsId)) {
//                throw new ForbiddenException("Access denied");
//            }
//        }
//        final User user = userDao.getById(newMembership.getUserId());
//        final Member newMember = new Member().withWorkspaceId(wsId)
//                                             .withUserId(user.getId())
//                                             .withRoles(newMembership.getRoles());
//        memberDao.create(newMember);
//        return status(CREATED).entity(toDescriptor(newMember, workspace, context)).build();
//    }
//
//    /**
//     * Removes user with given identifier as member from certain workspace.
//     *
//     * @param wsId
//     *         workspace identifier
//     * @param userId
//     *         user identifier to remove member
//     * @throws org.eclipse.che.api.core.NotFoundException
//     *         when workspace with given identifier doesn't exist
//     * @throws org.eclipse.che.api.core.ServerException
//     *         when some error occurred while retrieving workspace or removing member
//     * @throws org.eclipse.che.api.core.ConflictException
//     *         when removal member is last <i>"workspace/admin"</i> in given workspace
//     * @see #addMember(String, org.eclipse.che.api.workspace.shared.dto.NewMembership, javax.ws.rs.core.SecurityContext)
//     * @see #getMembers(String, javax.ws.rs.core.SecurityContext)
//     */
//    @ApiOperation(value = "Remove user from workspace",
//                  notes = "Remove a user from a workspace by User ID",
//                  position = 13)
//    @ApiResponses(value = {
//            @ApiResponse(code = 204, message = "No Content"),
//            @ApiResponse(code = 404, message = "Not Found"),
//            @ApiResponse(code = 409, message = "Cannot remove workspace/admin"),
//            @ApiResponse(code = 500, message = "Internal Server Error")})
//    @DELETE
//    @Path("/{id}/members/{userid}")
//    @RolesAllowed({"account/owner", "workspace/admin"})
//    public void removeMember(@ApiParam(value = "Workspace ID")
//                             @PathParam("id")
//                             String wsId,
//                             @ApiParam(value = "User ID")
//                             @PathParam("userid")
//                             String userId,
//                             @Context SecurityContext context) throws NotFoundException, ServerException, ConflictException {
//        memberDao.remove(new Member().withUserId(userId).withWorkspaceId(wsId));
//    }
//
//    /**
//     * Removes certain workspace.
//     *
//     * @param wsId
//     *         workspace identifier to remove workspace
//     * @throws org.eclipse.che.api.core.NotFoundException
//     *         when workspace with given identifier doesn't exist
//     * @throws org.eclipse.che.api.core.ServerException
//     *         when some error occurred while retrieving/removing workspace or member
//     * @throws org.eclipse.che.api.core.ConflictException
//     *         if some error occurred while removing member
//     */
//    @ApiOperation(value = "Delete a workspace",
//                  notes = "Delete a workspace by its ID",
//                  position = 14)
//    @ApiResponses(value = {
//            @ApiResponse(code = 204, message = "No Content"),
//            @ApiResponse(code = 404, message = "Not Found"),
//            @ApiResponse(code = 409, message = "Failed to remove workspace member"),
//            @ApiResponse(code = 500, message = "Internal Server Error")})
//    @DELETE
//    @Path("/{id}")
//    @RolesAllowed({"account/owner", "workspace/admin", "system/admin"})
//    public void remove(@ApiParam(value = "Workspace ID")
//                       @PathParam("id")
//                       String wsId) throws NotFoundException, ServerException, ConflictException {
//        workspaceDao.remove(wsId);
//    }

    private void createTemporaryWorkspace(Workspace workspace) throws ConflictException, ServerException {
        try {
            //let vfs create temporary workspace in correct place
            EnvironmentContext.getCurrent().setWorkspaceTemporary(true);
            workspaceDao.create(workspace);
        } finally {
            EnvironmentContext.getCurrent().setWorkspaceTemporary(false);
        }
    }

    private User createTemporaryUser() throws ConflictException, ServerException, NotFoundException {
        final String id = generate("tmp_user", org.eclipse.che.api.user.server.Constants.ID_LENGTH);

        //creating user
        final User user = new User().withId(id);
        userDao.create(user);

        //creating profile for it
        profileDao.create(new Profile().withId(id).withUserId(id));

        //storing preferences
        final Map<String, String> preferences = new HashMap<>(4);
        preferences.put("temporary", String.valueOf(true));
        preferences.put("codenvy:created", Long.toString(System.currentTimeMillis()));
        preferenceDao.setPreferences(id, preferences);

        return user;
    }

    /**
     * Converts {@link org.eclipse.che.api.workspace.server.dao.Member} to {@link org.eclipse.che.api.workspace.shared.dto.MemberDescriptor}
     */
    /* used in tests */MemberDescriptor toDescriptor(Member member, Workspace workspace, SecurityContext context) {
        final UriBuilder serviceUriBuilder = getServiceContext().getServiceUriBuilder();
        final UriBuilder baseUriBuilder = getServiceContext().getBaseUriBuilder();
        final List<Link> links = new LinkedList<>();

        if (context.isUserInRole("account/owner") ||
            context.isUserInRole("workspace/admin") ||
            context.isUserInRole("workspace/developer")) {
            links.add(LinksHelper.createLink(HttpMethod.GET,
                                             serviceUriBuilder.clone()
                                                              .path(getClass(), "getMembers")
                                                              .build(workspace.getId())
                                                              .toString(),
                                             null,
                                             APPLICATION_JSON,
                                             Constants.LINK_REL_GET_WORKSPACE_MEMBERS));
        }
        if (context.isUserInRole("account/owner") || context.isUserInRole("workspace/admin")) {
            links.add(LinksHelper.createLink(HttpMethod.DELETE,
                                             serviceUriBuilder.clone()
                                                              .path(getClass(), "removeMember")
                                                              .build(workspace.getId(), member.getUserId())
                                                              .toString(),
                                             null,
                                             null,
                                             Constants.LINK_REL_REMOVE_WORKSPACE_MEMBER));
        }
        links.add(LinksHelper.createLink(HttpMethod.GET,
                                         baseUriBuilder.clone()
                                                       .path(UserService.class)
                                                       .path(UserService.class, "getById")
                                                       .build(member.getUserId())
                                                       .toString(),
                                         null,
                                         APPLICATION_JSON,
                                         LINK_REL_GET_USER_BY_ID));
        final Link wsLink = LinksHelper.createLink(HttpMethod.GET,
                                                   serviceUriBuilder.clone()
                                                                    .path(getClass(), "getById")
                                                                    .build(workspace.getId())
                                                                    .toString(),
                                                   null,
                                                   APPLICATION_JSON,
                                                   Constants.LINK_REL_GET_WORKSPACE_BY_ID);
        //TODO replace hardcoded path with UriBuilder + ProjectService
        final Link projectsLink = LinksHelper.createLink(HttpMethod.GET,
                                                         getServiceContext().getBaseUriBuilder().clone()
                                                                            .path("/project/{ws-id}")
                                                                            .build(member.getWorkspaceId())
                                                                            .toString(),
                                                         null,
                                                         APPLICATION_JSON,
                                                         "get projects");
        final WorkspaceReference wsRef = DtoFactory.getInstance().createDto(WorkspaceReference.class)
                                                   .withId(workspace.getId())
                                                   .withName(workspace.getName())
                                                   .withTemporary(workspace.isTemporary())
                                                   .withLinks(asList(wsLink, projectsLink));
        return DtoFactory.getInstance().createDto(MemberDescriptor.class)
                         .withUserId(member.getUserId())
                         .withWorkspaceReference(wsRef)
                         .withRoles(member.getRoles())
                         .withLinks(links);
    }

    /**
     * Converts {@link org.eclipse.che.api.workspace.server.dao.Workspace} to {@link org.eclipse.che.api.workspace.shared.dto.WorkspaceDescriptor}
     */
    /* used in tests */
    WorkspaceDto toDescriptor(Workspace workspace, SecurityContext context) {
        final WorkspaceDto workspaceDescriptor = DtoFactory.getInstance().createDto(WorkspaceDto.class)
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

    /**
     * Checks object reference is not {@code null}
     *
     * @param object
     *         object reference to check
     * @param subject
     *         used as subject of exception message "{subject} required"
     * @throws org.eclipse.che.api.core.BadRequestException
     *         when object reference is {@code null}
     */
    private void requiredNotNull(Object object, String subject) throws BadRequestException {
        if (object == null) {
            throw new BadRequestException(subject + " required");
        }
    }

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