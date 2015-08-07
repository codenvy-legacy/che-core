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


import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.machine.Command;
import org.eclipse.che.api.core.model.machine.MachineConfig;
import org.eclipse.che.api.core.model.workspace.Environment;
import org.eclipse.che.api.core.model.workspace.ProjectConfig;
import org.eclipse.che.api.core.model.workspace.UsersWorkspace;
import org.eclipse.che.api.core.rest.Service;
import org.eclipse.che.api.core.rest.annotations.GenerateLink;
import org.eclipse.che.api.core.rest.permission.PermissionManager;


import org.eclipse.che.api.user.server.dao.PreferenceDao;
import org.eclipse.che.api.user.server.dao.UserDao;
import org.eclipse.che.api.user.server.dao.UserProfileDao;
import org.eclipse.che.api.workspace.server.model.impl.CommandImpl;
import org.eclipse.che.api.workspace.server.model.impl.EnvironmentImpl;
import org.eclipse.che.api.workspace.server.model.impl.MachineConfigImpl;
import org.eclipse.che.api.workspace.server.model.impl.MachineSourceImpl;
import org.eclipse.che.api.workspace.server.model.impl.ProjectConfigImpl;
import org.eclipse.che.api.workspace.server.model.impl.SourceStorageImpl;
import org.eclipse.che.api.workspace.server.model.impl.UsersWorkspaceImpl;
import org.eclipse.che.api.workspace.server.spi.WorkspaceDao;
import org.eclipse.che.api.workspace.shared.dto.CommandDto;
import org.eclipse.che.api.workspace.shared.dto.EnvironmentDto;
import org.eclipse.che.api.workspace.shared.dto.MachineConfigDto;
import org.eclipse.che.api.workspace.shared.dto.MachineSourceDto;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.api.workspace.shared.dto.SourceStorageDto;
import org.eclipse.che.api.workspace.shared.dto.UsersWorkspaceDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMapWithExpectedSize;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.che.api.workspace.server.Constants.LINK_REL_CREATE_WORKSPACE;
import static org.eclipse.che.dto.server.DtoFactory.newDto;

//TODO add permissions check

/**
 * Workspace API
 *
 * @author Eugene Voevodin
 */
@Api(value = "/workspace", description = "Workspace service")
@Path("/workspace")
public class WorkspaceService extends Service {
    private static final Logger LOG = LoggerFactory.getLogger(WorkspaceService.class);

    private final WorkspaceManager  workspaceManager;
    private final WorkspaceDao      workspaceDao;
    private final UserDao           userDao;
    private final UserProfileDao    profileDao;
    private final PreferenceDao     preferenceDao;
    private final PermissionManager permissionManager;

    @Context
    private SecurityContext securityContext;

    @Inject
    public WorkspaceService(WorkspaceManager workspaceManager,
                            WorkspaceDao workspaceDao,
                            UserDao userDao,
                            UserProfileDao profileDao,
                            PreferenceDao preferenceDao,
                            PermissionManager permissionManager) {
        this.workspaceManager = workspaceManager;
        this.workspaceDao = workspaceDao;
        this.userDao = userDao;
        this.profileDao = profileDao;
        this.preferenceDao = preferenceDao;
        this.permissionManager = permissionManager;
    }

    @ApiOperation(value = "Create a new workspace",
                  notes = "For 'system/admin' it is required to set new workspace owner, " +
                          "when for any other kind of 'user' it is not(users identifier will be used for this purpose)")
    @ApiResponses({@ApiResponse(code = 201, message = "Workspace was created successfully"),
                   @ApiResponse(code = 400, message = "Missed required parameters"),
                   @ApiResponse(code = 403, message = "User does not have access to create new workspace"),
                   @ApiResponse(code = 409, message = "Conflict error was occurred during workspace creation"),
                   @ApiResponse(code = 500, message = "Internal server error was occurred during workspace creation")})
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @GenerateLink(rel = LINK_REL_CREATE_WORKSPACE)
    public UsersWorkspaceDto create(@ApiParam(value = "new workspace", required = true) UsersWorkspaceDto newWorkspace,
                                    @ApiParam("account id") @QueryParam("account") String accountId) throws ConflictException,
                                                                                                            ServerException,
                                                                                                            BadRequestException,
                                                                                                            ForbiddenException,
                                                                                                            NotFoundException {
        if (securityContext.isUserInRole("user")) {
            newWorkspace.setOwner(securityContext.getUserPrincipal().getName());
        }
        if (newWorkspace.getOwner() == null) {
            throw new BadRequestException("New workspace owner required");
        }

        return asDto(workspaceManager.createWorkspace(newWorkspace, accountId));
    }

    @ApiOperation("Get workspace by id")
    @ApiResponses({@ApiResponse(code = 200, message = "Response contains requested workspace entity"),
                   @ApiResponse(code = 404, message = "Workspace with specified id does not exist"),
                   @ApiResponse(code = 403, message = "User does not have access to requested workspace"),
                   @ApiResponse(code = 500, message = "Internal server error was occurred during workspace getting")})
    @GET
    @Path("/{id}")
    @Produces(APPLICATION_JSON)
    public UsersWorkspaceDto getById(@ApiParam("Workspace ID") @PathParam("id") String id) throws NotFoundException,
                                                                                                  ServerException,
                                                                                                  ForbiddenException,
                                                                                                  BadRequestException {
        return asDto(workspaceManager.getWorkspace(id));
    }

    @POST
    @Path("/start/{id}")
    @Produces(APPLICATION_JSON)
    public UsersWorkspaceDto start(@PathParam("id") String workspaceId, @QueryParam("environment") String envName) throws ServerException,
                                                                                                                          BadRequestException,
                                                                                                                          NotFoundException {
        return asDto(workspaceManager.startWorkspaceById(workspaceId, envName));
    }

//    /**
//     * Searches for workspace with given name and return {@link org.eclipse.che.api.workspace.shared.dto.WorkspaceDescriptor} for it.
//     * If user that has called this method is not <i>"workspace/admin"</i> or <i>"workspace/developer"</i>
//     * workspace attributes will not be added to response.
//     *
//     * @param name
//     *         workspace name
//     * @return descriptor of found workspace
//     * @throws org.eclipse.che.api.core.NotFoundException
//     *         when workspace with given identifier doesn't exist
//     * @throws org.eclipse.che.api.core.ServerException
//     *         when some error occurred while retrieving workspace
//     * @see org.eclipse.che.api.workspace.shared.dto.WorkspaceDescriptor
//     * @see #getById(String, javax.ws.rs.core.SecurityContext)
//     */
//    @ApiOperation(value = "Gets workspace by name",
//                  response = WorkspaceDescriptor.class,
//                  position = 4)
//    @ApiResponses(value = {
//            @ApiResponse(code = 200, message = "OK"),
//            @ApiResponse(code = 404, message = "Workspace with specified name doesn't exist"),
//            @ApiResponse(code = 403, message = "Access to requested workspace is forbidden"),
//            @ApiResponse(code = 500, message = "Server error")})
//    @GET
//    @GenerateLink(rel = Constants.LINK_REL_GET_WORKSPACE_BY_NAME)
//    @Produces(APPLICATION_JSON)
//    public UsersWorkspaceDto getByName(@ApiParam(value = "Name of workspace", required = true)
//                                       @Required
//                                       @Description("Name of workspace")
//                                       @QueryParam("name")
//                                       String name,
//                                       @Description("Owner of workspace")
//                                       @QueryParam("owner")
//                                       String owner,
//                                       @Context SecurityContext context) throws NotFoundException,
//                                                                                ServerException,
//                                                                                BadRequestException {
//
//        final UsersWorkspaceImpl workspace = workspaceManager.getWorkspace(name, owner);
//
//        Map<String, String> options = new HashMap<>(1);
//        options.put("owner", owner);
//
//        permissionManager.checkPermission("get workspace", options, context);
//
////        requiredNotNull(name, "Workspace name");
//
//        return toWorkspaceDto(workspace, context);
//    }
//
//    /**
//     * <p>Updates workspace.</p>
//     * <strong>Note:</strong> existed workspace attributes with same name as
//     * update attributes will be replaced with update attributes.
//     *
//     * @param id
//     *         workspace identifier
//     * @param update
//     *         workspace update
//     * @return descriptor of updated workspace
//     * @throws org.eclipse.che.api.core.NotFoundException
//     *         when workspace with given name doesn't exist
//     * @throws org.eclipse.che.api.core.ConflictException
//     *         when attribute with not valid name
//     * @throws org.eclipse.che.api.core.BadRequestException
//     *         when update is {@code null} or updated attributes contains not valid attribute
//     * @throws org.eclipse.che.api.core.ServerException
//     *         when some error occurred while retrieving/updating workspace
//     * @see org.eclipse.che.api.workspace.shared.dto.WorkspaceUpdate
//     * @see org.eclipse.che.api.workspace.shared.dto.WorkspaceDescriptor
//     * //@see #removeAttribute(String, String, javax.ws.rs.core.SecurityContext)
//     */
//    @ApiOperation(value = "Update workspace",
//                  response = WorkspaceDescriptor.class,
//                  notes = "Update an existing workspace. A JSON with updated properties is sent.",
//                  position = 3)
//    @ApiResponses(value = {
//            @ApiResponse(code = 200, message = "Workspace updated"),
//            @ApiResponse(code = 404, message = "Not found"),
//            @ApiResponse(code = 403, message = "Access to required workspace is forbidden"),
//            @ApiResponse(code = 500, message = "Internal server error")})
//    @POST
//    @Path("/{id}")
//    //@RolesAllowed({"account/owner", "workspace/admin", "system/admin"})
//    @Consumes(APPLICATION_JSON)
//    @Produces(APPLICATION_JSON)
//    public UsersWorkspaceDto update(@ApiParam("Workspace ID") @PathParam("id") String id,
//                                    @ApiParam("Workspace update") WorkspaceConfigDto update,
//                                    @Context SecurityContext context) throws NotFoundException,
//                                                                             ConflictException,
//                                                                             BadRequestException,
//                                                                             ServerException {
//
//        final UsersWorkspaceImpl old = workspaceManager.getWorkspace(id);
//
//        Map<String, String> options = new HashMap<>(1);
//        options.put("owner", old.getOwner());
//
//        permissionManager.checkPermission("get workspace", options, context);
//
//
//        UsersWorkspaceImpl workspace = workspaceManager.updateWorkspace(id, update);
//
//        LOG.info("EVENT#workspace-updated# WS#{}# WS-ID#{}#", workspace.getName(), workspace.getId());
//        return toWorkspaceDto(workspace, context);
//    }
//
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
//    //@RolesAllowed({"account/owner", "workspace/admin", "system/admin"})
//    public void remove(@ApiParam(value = "Workspace ID")
//                       @PathParam("id")
//                       String id,
//                       @Context SecurityContext context) throws NotFoundException, ServerException, ConflictException {
//
//        final UsersWorkspaceImpl ws = workspaceManager.getWorkspace(id);
//
//        Map<String, String> options = new HashMap<>(1);
//        options.put("owner", ws.getOwner());
//
//        permissionManager.checkPermission("get workspace", options, context);
//
//
//        workspaceManager.removeWorkspace(id);
//    }
//
//
//    public Workspace startWorkspace(String id) {
//
//    }
//
//    public void stopWorkspace(String id) {
//
//    }
//
//
//    public void shareWorkspace(String wsId, List<String> users) {
//
//    }
//
//
//    public Workspace getWorkspace(String id) {
//
//    }
//
//
//    // owned|shared|both
//    public Workspace getWorkspaces(String user, String ownership) {
//
//    }
//
//
////
////    private void createTemporaryWorkspace(Workspace workspace) throws ConflictException, ServerException {
////        try {
////            //let vfs create temporary workspace in correct place
////            EnvironmentContext.getCurrent().setWorkspaceTemporary(true);
////            workspaceDao.create(workspace);
////        } finally {
////            EnvironmentContext.getCurrent().setWorkspaceTemporary(false);
////        }
////    }
////
////    private User createTemporaryUser() throws ConflictException, ServerException, NotFoundException {
////        final String id = generate("tmp_user", org.eclipse.che.api.user.server.Constants.ID_LENGTH);
////
////        //creating user
////        final User user = new User().withId(id);
////        userDao.create(user);
////
////        //creating profile for it
////        profileDao.create(new Profile().withId(id).withUserId(id));
////
////        //storing preferences
////        final Map<String, String> preferences = new HashMap<>(4);
////        preferences.put("temporary", String.valueOf(true));
////        preferences.put("codenvy:created", Long.toString(System.currentTimeMillis()));
////        preferenceDao.setPreferences(id, preferences);
////
////        return user;
////    }
//
////    /**
////     * Converts {@link org.eclipse.che.api.workspace.server.dao.Member} to {@link org.eclipse.che.api.workspace.shared.dto.MemberDescriptor}
////     */
////    /* used in tests */MemberDescriptor toWorkspaceDto(Member member, Workspace workspace, SecurityContext context) {
////        final UriBuilder serviceUriBuilder = getServiceContext().getServiceUriBuilder();
////        final UriBuilder baseUriBuilder = getServiceContext().getBaseUriBuilder();
////        final List<Link> links = new LinkedList<>();
////
////        if (context.isUserInRole("account/owner") ||
////            context.isUserInRole("workspace/admin") ||
////            context.isUserInRole("workspace/developer")) {
////            links.add(LinksHelper.createLink(HttpMethod.GET,
////                                             serviceUriBuilder.clone()
////                                                              .path(getClass(), "getMembers")
////                                                              .build(workspace.getId())
////                                                              .toString(),
////                                             null,
////                                             APPLICATION_JSON,
////                                             Constants.LINK_REL_GET_WORKSPACE_MEMBERS));
////        }
////        if (context.isUserInRole("account/owner") || context.isUserInRole("workspace/admin")) {
////            links.add(LinksHelper.createLink(HttpMethod.DELETE,
////                                             serviceUriBuilder.clone()
////                                                              .path(getClass(), "removeMember")
////                                                              .build(workspace.getId(), member.getUserId())
////                                                              .toString(),
////                                             null,
////                                             null,
////                                             Constants.LINK_REL_REMOVE_WORKSPACE_MEMBER));
////        }
////        links.add(LinksHelper.createLink(HttpMethod.GET,
////                                         baseUriBuilder.clone()
////                                                       .path(UserService.class)
////                                                       .path(UserService.class, "getById")
////                                                       .build(member.getUserId())
////                                                       .toString(),
////                                         null,
////                                         APPLICATION_JSON,
////                                         LINK_REL_GET_USER_BY_ID));
////        final Link wsLink = LinksHelper.createLink(HttpMethod.GET,
////                                                   serviceUriBuilder.clone()
////                                                                    .path(getClass(), "getById")
////                                                                    .build(workspace.getId())
////                                                                    .toString(),
////                                                   null,
////                                                   APPLICATION_JSON,
////                                                   Constants.LINK_REL_GET_WORKSPACE_BY_ID);
////        //TODO replace hardcoded path with UriBuilder + ProjectService
////        final Link projectsLink = LinksHelper.createLink(HttpMethod.GET,
////                                                         getServiceContext().getBaseUriBuilder().clone()
////                                                                            .path("/project/{ws-id}")
////                                                                            .build(member.getWorkspaceId())
////                                                                            .toString(),
////                                                         null,
////                                                         APPLICATION_JSON,
////                                                         "get projects");
////        final WorkspaceReference wsRef = DtoFactory.getInstance().createDto(WorkspaceReference.class)
////                                                   .withId(workspace.getId())
////                                                   .withName(workspace.getName())
////                                                   .withTemporary(workspace.isTemporary())
////                                                   .withLinks(asList(wsLink, projectsLink));
////        return DtoFactory.getInstance().createDto(MemberDescriptor.class)
////                         .withUserId(member.getUserId())
////                         .withWorkspaceReference(wsRef)
////                         .withRoles(member.getRoles())
////                         .withLinks(links);
////    }
//
//    /**
//     * Converts {@link org.eclipse.che.api.workspace.server.dao.Workspace} to {@link org.eclipse.che.api.workspace.shared.dto.WorkspaceDescriptor}
//     */
//    /* used in tests */
//    UsersWorkspaceDto toWorkspaceDto(UsersWorkspace workspace, SecurityContext context) {
//        final UsersWorkspaceDto workspaceDescriptor = DtoFactory.getInstance().createDto(UsersWorkspaceDto.class)
//                                                                .withId(workspace.getId())
//                                                                .withName(workspace.getName())
//                .withTemporary(workspace.isTemporary())
////                                                                  .withAccountId(workspace.getAccountId())
//                .withAttributes(workspace.getAttributes());
//        final List<Link> links = new LinkedList<>();
//        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
//        if (context.isUserInRole("user")) {
//            //TODO replace hardcoded path with UriBuilder + ProjectService
//            links.add(LinksHelper.createLink(HttpMethod.GET,
//                                             getServiceContext().getBaseUriBuilder().clone()
//                                                                .path("/project/{ws-id}")
//                                                                .build(workspaceDescriptor.getId())
//                                                                .toString(),
//                                             null,
//                                             APPLICATION_JSON,
//                                             "get projects"));
//            links.add(LinksHelper.createLink(HttpMethod.GET,
//                                             uriBuilder.clone()
//                                                       .path(getClass(), "getMembershipsOfCurrentUser")
//                                                       .build()
//                                                       .toString(),
//                                             null,
//                                             APPLICATION_JSON,
//                                             Constants.LINK_REL_GET_CURRENT_USER_WORKSPACES));
//            links.add(LinksHelper.createLink(HttpMethod.GET,
//                                             uriBuilder.clone()
//                                                       .path(getClass(), "getMembershipOfCurrentUser")
//                                                       .build(workspaceDescriptor.getId())
//                                                       .toString(),
//                                             null,
//                                             APPLICATION_JSON,
//                                             Constants.LINK_REL_GET_CURRENT_USER_MEMBERSHIP));
//        }
//        if (context.isUserInRole("workspace/admin") || context.isUserInRole("workspace/developer") ||
//            context.isUserInRole("system/admin") || context.isUserInRole("system/manager") || context.isUserInRole("account/owner")) {
//            links.add(LinksHelper.createLink(HttpMethod.GET,
//                                             uriBuilder.clone().
//                                                     path(getClass(), "getByName")
//                                                       .queryParam("name", workspaceDescriptor.getName())
//                                                       .build()
//                                                       .toString(),
//                                             null,
//                                             APPLICATION_JSON,
//                                             Constants.LINK_REL_GET_WORKSPACE_BY_NAME));
//            links.add(LinksHelper.createLink(HttpMethod.GET,
//                                             uriBuilder.clone()
//                                                       .path(getClass(), "getById")
//                                                       .build(workspaceDescriptor.getId())
//                                                       .toString(),
//                                             null,
//                                             APPLICATION_JSON,
//                                             Constants.LINK_REL_GET_WORKSPACE_BY_ID));
//            links.add(LinksHelper.createLink(HttpMethod.GET,
//                                             uriBuilder.clone()
//                                                       .path(getClass(), "getMembers")
//                                                       .build(workspaceDescriptor.getId())
//                                                       .toString(),
//                                             null,
//                                             APPLICATION_JSON,
//                                             Constants.LINK_REL_GET_WORKSPACE_MEMBERS));
//        }
//        if (context.isUserInRole("account/owner") || context.isUserInRole("workspace/admin") || context.isUserInRole("system/admin")) {
//            links.add(LinksHelper.createLink(HttpMethod.DELETE,
//                                             uriBuilder.clone()
//                                                       .path(getClass(), "remove")
//                                                       .build(workspaceDescriptor.getId())
//                                                       .toString(),
//                                             null,
//                                             null,
//                                             Constants.LINK_REL_REMOVE_WORKSPACE));
//        }
//        return workspaceDescriptor.withLinks(links);
//    }
//
////    /**
////     * Checks object reference is not {@code null}
////     *
////     * @param object
////     *         object reference to check
////     * @param subject
////     *         used as subject of exception message "{subject} required"
////     * @throws org.eclipse.che.api.core.BadRequestException
////     *         when object reference is {@code null}
////     */
////    private void requiredNotNull(Object object, String subject) throws BadRequestException {
////        if (object == null) {
////            throw new BadRequestException(subject + " required");
////        }
////    }
//
//    /**
//     * Validates attribute name.
//     *
//     * @param attributeName
//     *         attribute name to check
//     * @throws org.eclipse.che.api.core.ConflictException
//     *         when attribute name is {@code null}, empty or it starts with "codenvy"
//     */
//    private void validateAttributeName(String attributeName) throws ConflictException {
//        if (attributeName == null || attributeName.isEmpty() || attributeName.toLowerCase().startsWith("codenvy")) {
//            throw new ConflictException(String.format("Attribute2 name '%s' is not valid", attributeName));
//        }
//    }
//
//    private void validateAttributes(Map<String, String> attributes) throws ConflictException {
//        for (String attributeName : attributes.keySet()) {
//            validateAttributeName(attributeName);
//        }
//    }
//
////    private void ensureCurrentUserOwnerOf(Account target) throws ServerException, NotFoundException, ConflictException {
////        final List<Account> accounts = accountDao.getByOwner(currentUser().getId());
////        for (Account account : accounts) {
////            if (account.getId().equals(target.getId())) {
////                return;
////            }
////        }
////        throw new ConflictException("You can create workspace associated only with your own account");
////    }
//
////    private boolean isCurrentUserAccountOwnerOf(String wsId) throws ServerException, NotFoundException {
////        final List<Account> accounts = accountDao.getByOwner(currentUser().getId());
////        final List<Workspace> workspaces = new LinkedList<>();
////        //fetch all workspaces related to accounts
////        for (Account account : accounts) {
////            workspaces.addAll(workspaceDao.getByAccount(account.getId()));
////        }
////        for (Workspace workspace : workspaces) {
////            if (workspace.getId().equals(wsId)) {
////                return true;
////            }
////        }
////        return false;
////    }
//
//    /**
//     * Generates workspace name based on current user email.
//     * Generating process is simple, assuming we have user with email user@codenvy.com,
//     * then first time we will check for workspace with name equal to "user" and if it is free
//     * it will be returned, but if it is reserved then  number suffix will be added to the end of "user" name
//     * and it will be checked again until free workspace name is not found.
//     */
//    private String generateWorkspaceName() throws ServerException {
//        //should be email
//        String userName = currentUser().getName();
//        int atIdx = userName.indexOf('@');
//        //if username contains email then fetch part before '@'
//        if (atIdx != -1) {
//            userName = userName.substring(0, atIdx);
//        }
//        //search first workspace name which is free
//        int suffix = 2;
//        String workspaceName = userName;
//        while (workspaceExists(workspaceName)) {
//            workspaceName = userName + suffix++;
//        }
//        return workspaceName;
//    }
//
//    private boolean workspaceExists(String name) throws ServerException {
//        try {
//            workspaceDao.getByName(name);
//        } catch (NotFoundException nfEx) {
//            return false;
//        }
//        return true;
//    }
//
//    private org.eclipse.che.commons.user.User currentUser() {
//        return EnvironmentContext.getCurrent().getUser();
//    }

    //TODO add links
    private UsersWorkspaceDto asDto(UsersWorkspace workspace) {
        final List<CommandDto> commands = new ArrayList<>(workspace.getCommands().size());
        for (Command command : workspace.getCommands()) {
            commands.add(asDto(command));
        }
        final List<ProjectConfigDto> projects = new ArrayList<>(workspace.getProjects().size());
        for (ProjectConfig project : workspace.getProjects()) {
            projects.add(asDto(project));
        }
        final Map<String, EnvironmentDto> environments = newHashMapWithExpectedSize(workspace.getEnvironments().size());
        for (Map.Entry<String, ? extends Environment> entry : workspace.getEnvironments().entrySet()) {
            environments.put(entry.getKey(), asDto(entry.getValue()));
        }
        return newDto(UsersWorkspaceDto.class)
                .withId(workspace.getId())
                .withName(workspace.getName())
                .withOwner(workspace.getOwner())
                .withDefaultEnvName(workspace.getDefaultEnvName())
                .withCommands(commands)
                .withProjects(projects)
                .withEnvironments(environments);
    }

    private UsersWorkspaceImpl asImpl(UsersWorkspaceDto workspaceDto) {
        final List<CommandImpl> commands = new ArrayList<>(workspaceDto.getCommands().size());
        for (CommandDto commandDto : workspaceDto.getCommands()) {
            commands.add(asImpl(commandDto));
        }
        final List<ProjectConfigImpl> projects = new ArrayList<>(workspaceDto.getProjects().size());
        for (ProjectConfigDto projectCfgDto : workspaceDto.getProjects()) {
            projects.add(asImpl(projectCfgDto));
        }
        final Map<String, EnvironmentImpl> environments = newHashMapWithExpectedSize(workspaceDto.getEnvironments().size());
        for (Map.Entry<String, EnvironmentDto> entry : workspaceDto.getEnvironments().entrySet()) {
            environments.put(entry.getKey(), asImpl(entry.getValue()));
        }
        return new UsersWorkspaceImpl(workspaceDto.getId(),
                                      workspaceDto.getName(),
                                      workspaceDto.getOwner(),
                                      workspaceDto.getAttributes(),
                                      commands,
                                      projects,
                                      environments,
                                      workspaceDto.getDefaultEnvName(),
                                      workspaceDto.getDescription());
    }

    private CommandDto asDto(Command command) {
        return newDto(CommandDto.class).withName(command.getName())
                                       .withCommandLine(command.getCommandLine())
                                       .withType(command.getType())
                                       .withVisibility(command.getVisibility())
                                       .withWorkingDir(command.getWorkingDir());
    }

    private CommandImpl asImpl(CommandDto commandDto) {
        return new CommandImpl(commandDto.getName(),
                               commandDto.getCommandLine(),
                               commandDto.getVisibility(),
                               commandDto.getType(),
                               commandDto.getWorkingDir());
    }

    private ProjectConfigDto asDto(ProjectConfig projectCfg) {
        return newDto(ProjectConfigDto.class)
                .withName(projectCfg.getName())
                .withDescription(projectCfg.getDescription())
                .withPath(projectCfg.getPath())
                .withType(projectCfg.getType())
                .withAttributes(projectCfg.getAttributes())
                .withMixinTypes(projectCfg.getMixinTypes())
                .withSourceStorage(newDto(SourceStorageDto.class)
                                           .withLocation(projectCfg.getSourceStorage().getLocation())
                                           .withType(projectCfg.getSourceStorage().getType())
                                           .withParameters(projectCfg.getSourceStorage().getParameters()));
    }

    private ProjectConfigImpl asImpl(ProjectConfigDto projectCfgDto) {
        return new ProjectConfigImpl().setName(projectCfgDto.getName())
                                      .setDescription(projectCfgDto.getDescription())
                                      .setPath(projectCfgDto.getPath())
                                      .setType(projectCfgDto.getType())
                                      .setAttributes(projectCfgDto.getAttributes())
                                      .setMixinTypes(projectCfgDto.getMixinTypes())
                                      .setSourceStorage(new SourceStorageImpl(projectCfgDto.getSourceStorage().getType(),
                                                                              projectCfgDto.getSourceStorage().getLocation(),
                                                                              projectCfgDto.getSourceStorage().getParameters()));
    }

    //TODO add recipe
    private EnvironmentDto asDto(Environment environment) {
        final List<MachineConfigDto> machineConfigs = new ArrayList<>(environment.getMachineConfigs().size());
        for (MachineConfig machineCfg : environment.getMachineConfigs()) {
            machineConfigs.add(asDto(machineCfg));
        }
        return newDto(EnvironmentDto.class).withName(environment.getName()).withMachineConfigs(machineConfigs);
    }

    //TODO add recipe
    private EnvironmentImpl asImpl(EnvironmentDto envDto) {
        final List<MachineConfigImpl> machineConfigs = new ArrayList<>(envDto.getMachineConfigs().size());
        for (MachineConfigDto machineCfgDto : envDto.getMachineConfigs()) {
            machineConfigs.add(asImpl(machineCfgDto));
        }
        return new EnvironmentImpl(envDto.getName(), envDto.getRecipe(), machineConfigs);
    }

    private MachineConfigDto asDto(MachineConfig config) {
        return newDto(MachineConfigDto.class).withName(config.getName())
                                             .withType(config.getType())
                                             .withDev(config.isDev())
                                             .withSource(newDto(MachineSourceDto.class).withType(config.getSource().getType())
                                                                                       .withLocation(config.getSource().getLocation()));
    }

    private MachineConfigImpl asImpl(MachineConfigDto machineCfgDto) {
        return new MachineConfigImpl().setName(machineCfgDto.getName())
                                      .setType(machineCfgDto.getType())
                                      .setIsDev(machineCfgDto.isDev())
                                      .setSource(new MachineSourceImpl(machineCfgDto.getSource().getType(),
                                                                       machineCfgDto.getSource().getLocation()));
    }
}