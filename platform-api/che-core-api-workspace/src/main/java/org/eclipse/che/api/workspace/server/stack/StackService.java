/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.workspace.server.stack;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import org.apache.commons.fileupload.FileItem;
import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.rest.Service;
import org.eclipse.che.api.core.rest.annotations.GenerateLink;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.core.util.LinksHelper;
import org.eclipse.che.api.machine.server.recipe.PermissionsChecker;
import org.eclipse.che.api.workspace.server.dao.StackDao;
import org.eclipse.che.api.workspace.server.model.impl.stack.DecoratedStack;
import org.eclipse.che.api.workspace.server.model.impl.stack.DecoratedStackImpl;
import org.eclipse.che.api.workspace.server.stack.image.StackIcon;
import org.eclipse.che.api.workspace.shared.dto.stack.StackDto;
import org.eclipse.che.api.workspace.shared.dto.stack.StackDtoDescriptor;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.user.User;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static javax.ws.rs.core.Response.Status.CREATED;
import static org.eclipse.che.api.workspace.server.Constants.LINK_REL_CREATE_STACK;
import static org.eclipse.che.api.workspace.server.Constants.LINK_REL_GET_STACKS_BY_CREATOR;
import static org.eclipse.che.api.workspace.server.Constants.LINK_REL_GET_STACK_BY_ID;
import static org.eclipse.che.api.workspace.server.Constants.LINK_REL_REMOVE_STACK;
import static org.eclipse.che.api.workspace.server.Constants.LINK_REL_SEARCH_STACKS;
import static org.eclipse.che.api.workspace.server.Constants.LINK_REL_UPDATE_STACK;
import static org.eclipse.che.api.workspace.server.Constants.LINK_REL_UPLOAD_ICON;
import static org.eclipse.che.api.workspace.server.Constants.LINK_REL_GET_ICON;
import static org.eclipse.che.api.workspace.server.Constants.LINK_REL_DELETE_ICON;
import static org.eclipse.che.api.workspace.server.DtoConverter.asDto;

/**
 * Defines Stack REST API
 *
 * @author Alexander Andrinko
 */
@Api(value = "/stack", description = "Stack REST API")
@Path("/stack")
public class StackService extends Service {

    private final StackDao           stackDao;
    private final PermissionsChecker permissionChecker;

    @Inject
    public StackService(StackDao stackDao, PermissionsChecker permissionChecker) {
        this.stackDao = stackDao;
        this.permissionChecker = permissionChecker;
    }

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @GenerateLink(rel = LINK_REL_CREATE_STACK)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    @ApiOperation(value = "Create a new stack",
                  notes = "This operation can be performed only by authorized user",
                  response = StackDtoDescriptor.class)
    @ApiResponses({@ApiResponse(code = 201, message = "The stack successfully created"),
                   @ApiResponse(code = 400, message = "Missed required parameters, parameters are not valid"),
                   @ApiResponse(code = 403, message = "The user does not have access to create a new stack"),
                   @ApiResponse(code = 409, message = "Conflict error occurred during the stack creation" +
                                                      "(e.g. The stack with such name already exists)"),
                   @ApiResponse(code = 500, message = "Internal server error occurred")})
    public Response createStack(@ApiParam("The new stack") StackDto stackDto) throws ApiException {
        requireNonNull(stackDto, "Stack required");
        requireNonNullAndNonEmpty(stackDto.getName(), "Stack name required");
        if (stackDto.getSource() == null && stackDto.getWorkspaceConfig() == null) {
            throw new BadRequestException("Stack source required");
        }

        String userId = EnvironmentContext.getCurrent().getUser().getId();
        if (stackDto.getPermissions() != null && !permissionChecker.hasPublicAccess(stackDto.getPermissions())) {
            throw new ForbiddenException(format("User %s doesn't have access to use 'public: search' permission", userId));
        }

        DecoratedStackImpl newStack = new DecoratedStackImpl(stackDto, null);
        newStack.setCreator(userId);

        stackDao.create(newStack);

        return Response.status(CREATED)
                       .entity(asStackDto(newStack))
                       .build();
    }

    @GET
    @Path("/{id}")
    @Produces(APPLICATION_JSON)
    @GenerateLink(rel = LINK_REL_GET_STACK_BY_ID)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    @ApiOperation(value = "Get the stack by id",
                  notes = "This operation can be performed for stack owner, or for predefined stacks")
    @ApiResponses({@ApiResponse(code = 200, message = "The response contains requested stack entity"),
                   @ApiResponse(code = 404, message = "The requested stack was not found"),
                   @ApiResponse(code = 403, message = "The user has not permission get requested stack"),
                   @ApiResponse(code = 500, message = "Internal server error occurred")})
    public StackDtoDescriptor getStack(@ApiParam("The stack id") @PathParam("id") String id) throws ApiException {
        final DecoratedStack stack = stackDao.getById(id);

        User user = EnvironmentContext.getCurrent().getUser();
        if (!user.isMemberOf("system/admin") &&
            !user.isMemberOf("system/manager") &&
            !permissionChecker.hasAccess(stack, user.getId(), stack.getCreator(), "read")) {
            throw new ForbiddenException(format("User %s doesn't have access to stack %s", user.getId(), id));
        }

        return asStackDto(stack);
    }

    @PUT
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    @GenerateLink(rel = LINK_REL_UPDATE_STACK)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    @ApiOperation(value = "Update the stack by replacing all the existing data (exclude field \"creator\") with update",
                  notes = "This operation can be performed only by stack owner. But user with roles \"system/admin\" or \"system/manager\" " +
                          "can update predefined stack.")
    @ApiResponses({@ApiResponse(code = 200, message = "The stack successfully updated"),
                   @ApiResponse(code = 400, message = "Missed required parameters, parameters are not valid"),
                   @ApiResponse(code = 403, message = "The user does not have access to update the stack"),
                   @ApiResponse(code = 409, message = "Conflict error occurred during stack update" +
                                                      "(e.g. Stack with such name already exists)"),
                   @ApiResponse(code = 500, message = "Internal server error occurred")})
    public StackDtoDescriptor updateStack(@ApiParam(value = "The stack update", required = true) StackDto updateDto) throws ApiException {
        requireNonNull(updateDto, "Stack required");
        requireNonNullAndNonEmpty(updateDto.getId(), "Stack id required");
        if (updateDto.getSource() == null && updateDto.getWorkspaceConfig() == null) {
            throw new BadRequestException("Stack source required");
        }

        final DecoratedStack stack = stackDao.getById(updateDto.getId());

        final User user = EnvironmentContext.getCurrent().getUser();
        final String userId = user.getId();
        if (!user.isMemberOf("system/admin") && !permissionChecker.hasAccess(stack, user.getId(), stack.getCreator(), "write")) {
            throw new ForbiddenException(format("User %s doesn't have access to update stack %s", userId, stack.getId()));
        }
        if (updateDto.getPermissions() != null) {
            //ensure that user has access to update stack permissions
            if (!user.isMemberOf("system/admin") && !permissionChecker.hasAccess(stack, userId, stack.getCreator(), "update_acl")) {
                throw new ForbiddenException(format("User %s doesn't have access to update stack %s permissions", userId, stack.getId()));
            }
            if (!permissionChecker.hasPublicAccess(updateDto.getPermissions())) {
                throw new ForbiddenException(format("User %s doesn't have access to use 'public: search' permission", userId));
            }
        }

        /* user can not edit field creator */
        updateDto.setCreator(user.getId());
        stackDao.update(new DecoratedStackImpl(updateDto, stack.getIcon()));

        return asStackDto(stackDao.getById(updateDto.getId()));
    }

    @DELETE
    @Path("/{id}")
    @GenerateLink(rel = LINK_REL_REMOVE_STACK)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    @ApiOperation(value = "Removes the stack",
                  notes = "But user with roles \"system/admin\" or \"system/manager\" " + "can delete predefined stack.")
    @ApiResponses({@ApiResponse(code = 204, message = "The stack successfully removed"),
                   @ApiResponse(code = 403, message = "The user does not have access to remove the stack"),
                   @ApiResponse(code = 404, message = "The stack doesn't exist"),
                   @ApiResponse(code = 500, message = "Internal server error occurred")})
    public void removeStack(@ApiParam("The stack id") @PathParam("id") String id) throws ApiException {
        final DecoratedStack stack = stackDao.getById(id);

        User user = EnvironmentContext.getCurrent().getUser();

        if (!user.isMemberOf("system/admin") && !permissionChecker.hasAccess(stack, user.getId(), stack.getCreator(), "write")) {
            throw new ForbiddenException(format("User %s doesn't have access to stack %s", user.getId(), id));
        }

        stackDao.remove(id);
    }

    @GET
    @GenerateLink(rel = LINK_REL_GET_STACKS_BY_CREATOR)
    @Produces(APPLICATION_JSON)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    @ApiOperation(value = "Get the list stacks owned by current user",
                  notes = "This operation can be performed only by authorized user",
                  response = StackDtoDescriptor.class,
                  responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "The response contains requested list stack entity"),
                   @ApiResponse(code = 403, message = "The user does not have access to get stack entity list"),
                   @ApiResponse(code = 500, message = "Internal server error occurred")})
    public List<StackDtoDescriptor> getCreatedStacks(@ApiParam("The number of the items to skip")
                                                     @DefaultValue("0")
                                                     @QueryParam("skipCount") Integer skipCount,
                                                     @ApiParam("The limit of the items in the response, default is 30")
                                                     @DefaultValue("30")
                                                     @QueryParam("maxItems") Integer maxItems) throws ServerException {
        String creator = EnvironmentContext.getCurrent().getUser().getId();
        List<DecoratedStackImpl> stacks = stackDao.getByCreator(creator, skipCount, maxItems);

        return stacks.stream()
                     .map(this::asStackDto)
                     .collect(Collectors.toList());
    }

    @GET
    @Path("/list")
    @Produces(APPLICATION_JSON)
    @GenerateLink(rel = LINK_REL_SEARCH_STACKS)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    @ApiOperation(value = "Get the list stacks with required tags",
                  notes = "This operation can be performed only by authorized user",
                  response = StackDtoDescriptor.class,
                  responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "The response contains requested list stack entity with required tags"),
                   @ApiResponse(code = 403, message = "The user does not have access to get stack entity list with required tags"),
                   @ApiResponse(code = 500, message = "Internal server error occurred")})
    public List<StackDtoDescriptor> searchStacks(@ApiParam("List tags for search")
                                                 @QueryParam("tags") List<String> tags,
                                                 @ApiParam("The number of the items to skip")
                                                 @DefaultValue("0")
                                                 @QueryParam("skipCount") Integer skipCount,
                                                 @ApiParam("The limit of the items in the response, default is 30")
                                                 @DefaultValue("30")
                                                 @QueryParam("maxItems") Integer maxItems) throws ServerException {
        List<DecoratedStackImpl> stacks = stackDao.searchStacks(tags, skipCount, maxItems);
        return stacks.stream()
                     .map(this::asStackDto)
                     .collect(Collectors.toList());
    }

    @GET
    @Path("/{id}/icon")
    @Produces("image/*")
    @GenerateLink(rel = LINK_REL_GET_ICON)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    @ApiOperation(value = "Get icon by stack id",
                  notes = "This operation can be performed only by authorized user",
                  response = byte[].class)
    @ApiResponses({@ApiResponse(code = 200, message = "The response contains requested image entity"),
                   @ApiResponse(code = 403, message = "The user does not have access to get image entity"),
                   @ApiResponse(code = 500, message = "Internal server error occurred")})
    public Response getIcon(@ApiParam("The stack id") @PathParam("id") String stackId) throws NotFoundException, ServerException {
        DecoratedStack stack = stackDao.getById(stackId);

        if (stack == null) {
            throw new NotFoundException("Stack " + stackId + " was not found.");
        }

        StackIcon image = stack.getIcon();

        if (image == null) {
            throw new NotFoundException("Image for stack " + stackId + " was not found.");
        }
        return Response.ok(image.getData(), image.getMediaType()).build();
    }

    @POST
    @Path("/{id}/icon")
    @Consumes(MULTIPART_FORM_DATA)
    @Produces(TEXT_PLAIN)
    @GenerateLink(rel = LINK_REL_UPLOAD_ICON)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    @ApiOperation(value = "Upload icon for required stack",
                  notes = "This operation can be performed only by authorized stack owner")
    @ApiResponses({@ApiResponse(code = 200, message = "Image was successfully uploaded"),
                   @ApiResponse(code = 400, message = "Missed required parameters, parameters are not valid"),
                   @ApiResponse(code = 403, message = "The user does not have access upload image for stack with required id"),
                   @ApiResponse(code = 404, message = "The stack doesn't exist"),
                   @ApiResponse(code = 500, message = "Internal server error occurred")})
    public Response uploadIcon(@ApiParam("The image for stack") Iterator<FileItem> formData,
                               @ApiParam("The stack id") @PathParam("id") String stackId)
            throws NotFoundException, ServerException, BadRequestException, ForbiddenException {
        requireNonNullAndNonEmpty(stackId, "Stack id required");
        if (formData.hasNext()) {
            FileItem fileItem = formData.next();
            String iconName = fileItem.getName();
            try {
                StackIcon stackIcon = new StackIcon(fileItem.getContentType(), fileItem.get());

                DecoratedStackImpl stack = stackDao.getById(stackId);

                User user = EnvironmentContext.getCurrent().getUser();
                if (!user.isMemberOf("system/admin") && !permissionChecker.hasAccess(stack, user.getId(), stack.getCreator(), "write")) {
                    throw new ForbiddenException(format("User %s doesn't have access to stack %s", stack.getId(), stack.getId()));
                }

                stack.setIcon(stackIcon);
                stackDao.update(stack);
            } catch (IOException | ConflictException e) {
                throw new ServerException(format("Failed upload icon %s for stack %s", iconName, stackId), e);
            }
        }
        return Response.ok().build();
    }

    @DELETE
    @Path("/{id}/icon")
    @GenerateLink(rel = LINK_REL_DELETE_ICON)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    @ApiOperation(value = "Delete icon for required stack",
                  notes = "This operation can be performed only by authorized stack owner")
    @ApiResponses({@ApiResponse(code = 204, message = "Icon was successfully removed"),
                   @ApiResponse(code = 400, message = "Missed required parameters, parameters are not valid"),
                   @ApiResponse(code = 403, message = "The user does not have access upload image for stack with required id"),
                   @ApiResponse(code = 404, message = "The stack or icon doesn't exist"),
                   @ApiResponse(code = 409, message = "Conflict error occurred during stack update" +
                                                      "(e.g. Stack with such name already exists)"),
                   @ApiResponse(code = 500, message = "Internal server error occurred")})
    public void removeIcon(@ApiParam("The stack Id") @PathParam("id") String stackId)
            throws NotFoundException, ServerException, ConflictException, ForbiddenException, BadRequestException {
        requireNonNullAndNonEmpty(stackId, "Stack id required");
        DecoratedStackImpl stack = stackDao.getById(stackId);

        User user = EnvironmentContext.getCurrent().getUser();
        if (!user.isMemberOf("system/admin") && !permissionChecker.hasAccess(stack, user.getId(), stack.getCreator(), "write")) {
            throw new ForbiddenException(format("User %s doesn't have access to stack %s", stack.getId(), stackId));
        }

        stack.setIcon(null);
        stackDao.update(stack);
    }

    private StackDtoDescriptor asStackDto(DecoratedStack stack) {
        if (stack == null) {
            return null;
        }
        final UriBuilder builder = getServiceContext().getServiceUriBuilder();

        List<Link> links = new ArrayList<>();
        final Link removeLink = LinksHelper.createLink("DELETE",
                                                       builder.clone()
                                                              .path(getClass(), "removeStack")
                                                              .build(stack.getId())
                                                              .toString(),
                                                       LINK_REL_REMOVE_STACK);
        final Link getLink = LinksHelper.createLink("GET",
                                                    builder.clone()
                                                           .path(getClass(), "getStack")
                                                           .build(stack.getId())
                                                           .toString(),
                                                    APPLICATION_JSON,
                                                    LINK_REL_GET_STACK_BY_ID);
        links.add(removeLink);
        links.add(getLink);

        if (stack.getIcon() != null) {
            Link deleteIcon = LinksHelper.createLink("DELETE",
                                                     builder.clone()
                                                            .path(getClass(), "removeIcon")
                                                            .build(stack.getId())
                                                            .toString(),
                                                     stack.getIcon().getMediaType(),
                                                     LINK_REL_DELETE_ICON);
            Link getIconLink = LinksHelper.createLink("GET",
                                                      builder.clone()
                                                             .path(getClass(), "getIcon")
                                                             .build(stack.getId())
                                                             .toString(),
                                                      stack.getIcon().getMediaType(),
                                                      LINK_REL_GET_ICON);
            links.add(deleteIcon);
            links.add(getIconLink);
        }

        return asDto(stack).withLinks(links);
    }

    private void requireNonNull(Object object, String message) throws BadRequestException {
        if (object == null) {
            throw new BadRequestException(message);
        }
    }

    private void requireNonNullAndNonEmpty(String parameter, String message) throws BadRequestException {
        if (isNullOrEmpty(parameter)) {
            throw new BadRequestException(message);
        }
    }
}
