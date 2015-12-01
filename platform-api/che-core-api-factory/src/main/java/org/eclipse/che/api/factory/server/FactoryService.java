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
package org.eclipse.che.api.factory.server;

import com.google.gson.JsonSyntaxException;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

import org.apache.commons.fileupload.FileItem;
import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.workspace.EnvironmentState;
import org.eclipse.che.api.core.rest.Service;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.factory.server.builder.FactoryBuilder;
import org.eclipse.che.api.factory.server.snippet.SnippetGenerator;
import org.eclipse.che.api.factory.shared.dto.Author;
import org.eclipse.che.api.factory.shared.dto.Factory;
import org.eclipse.che.api.machine.shared.dto.CommandDto;
import org.eclipse.che.api.machine.shared.dto.MachineConfigDto;
import org.eclipse.che.api.workspace.server.DtoConverter;
import org.eclipse.che.api.workspace.server.WorkspaceManager;
import org.eclipse.che.api.workspace.server.model.impl.EnvironmentStateImpl;
import org.eclipse.che.api.workspace.server.model.impl.ProjectConfigImpl;
import org.eclipse.che.api.workspace.server.model.impl.UsersWorkspaceImpl;
import org.eclipse.che.api.workspace.shared.dto.EnvironmentDto;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceConfigDto;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.lang.NameGenerator;
import org.eclipse.che.commons.lang.Pair;
import org.eclipse.che.commons.lang.URLEncodedUtils;
import org.eclipse.che.commons.user.User;
import org.eclipse.che.dto.server.DtoFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.eclipse.che.dto.server.DtoFactory.newDto;

/** Service for factory rest api features */
@Api(value = "/factory",
     description = "Factory manager")
@Path("/factory")
public class FactoryService extends Service {
    private static final Logger LOG = LoggerFactory.getLogger(FactoryService.class);

    private final String                 baseApiUrl;
    private final FactoryStore           factoryStore;
    private final FactoryEditValidator   factoryEditValidator;
    private final FactoryCreateValidator createValidator;
    private final FactoryAcceptValidator acceptValidator;
    private final LinksHelper            linksHelper;
    private final FactoryBuilder         factoryBuilder;
    private final WorkspaceManager       workspaceManager;

    @Inject
    public FactoryService(@Named("api.endpoint") String baseApiUrl,
                          FactoryStore factoryStore,
                          FactoryCreateValidator createValidator,
                          FactoryAcceptValidator acceptValidator,
                          FactoryEditValidator factoryEditValidator,
                          LinksHelper linksHelper,
                          FactoryBuilder factoryBuilder,
                          WorkspaceManager workspaceManager) {
        this.baseApiUrl = baseApiUrl;
        this.factoryStore = factoryStore;
        this.createValidator = createValidator;
        this.acceptValidator = acceptValidator;
        this.factoryEditValidator = factoryEditValidator;
        this.linksHelper = linksHelper;
        this.factoryBuilder = factoryBuilder;
        this.workspaceManager = workspaceManager;
    }

    /**
     * Save factory to storage and return stored data. Field 'factory' should contains factory information.
     * Fields with images should be named 'image'. Acceptable image size 100x100 pixels.
     *
     * @param formData
     *         - http request form data
     * @param uriInfo
     *         - url context
     * @return - stored data
     * @throws org.eclipse.che.api.core.ApiException
     *         - {@link org.eclipse.che.api.core.ConflictException} when factory json is not found
     *         - {@link org.eclipse.che.api.core.ConflictException} when image content can't be read
     *         - {@link org.eclipse.che.api.core.ConflictException} when image media type is unsupported
     *         - {@link org.eclipse.che.api.core.ConflictException} when image height or length isn't equal to 100 pixels
     *         - {@link org.eclipse.che.api.core.ConflictException} when if image is too big
     *         - {@link org.eclipse.che.api.core.ServerException} when internal server error occurs
     */
    @ApiOperation(value = "Create a Factory and return data",
                  notes = "Save factory to storage and return stored data. Field 'factory' should contains factory information.",
                  response = Factory.class,
                  position = 1)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 409, message = "Conflict error. Some parameter is missing"),
            @ApiResponse(code = 500, message = "Unable to identify user from context")})
    @RolesAllowed("user")
    @POST
    @Consumes({MediaType.MULTIPART_FORM_DATA})
    @Produces({MediaType.APPLICATION_JSON})
    public Factory saveFactory(Iterator<FileItem> formData,
                               @Context UriInfo uriInfo)
            throws ApiException {
        try {
            EnvironmentContext context = EnvironmentContext.getCurrent();
            if (context.getUser() == null || context.getUser().getName() == null || context.getUser().getId() == null) {
                throw new ServerException("Unable to identify user from context");
            }

            Set<FactoryImage> images = new HashSet<>();
            Factory factory = null;

            while (formData.hasNext()) {
                FileItem item = formData.next();
                String fieldName = item.getFieldName();
                if (fieldName.equals("factory")) {
                    try {
                        factory = factoryBuilder.build(item.getInputStream());
                    } catch (JsonSyntaxException e) {
                        throw new ConflictException(
                                "You have provided an invalid JSON.  For more information, " +
                                "please visit http://docs.codenvy.com/user/creating-factories/factory-parameter-reference/");
                    }
                } else if (fieldName.equals("image")) {
                    try (InputStream inputStream = item.getInputStream()) {
                        FactoryImage factoryImage =
                                FactoryImage.createImage(inputStream, item.getContentType(), NameGenerator.generate(null, 16));
                        if (factoryImage.hasContent()) {
                            images.add(factoryImage);
                        }
                    }
                }
            }
            if (factory == null) {
                LOG.warn("No factory information found in 'factory' section of multipart form-data.");
                throw new ConflictException("No factory information found in 'factory' section of multipart/form-data.");
            }

            processDefaults(factory);
            createValidator.validateOnCreate(factory);
            String factoryId = factoryStore.saveFactory(factory, images);
            factory = factoryStore.getFactory(factoryId);
            factory = factory.withLinks(linksHelper.createLinks(factory, images, uriInfo));

            /*
            LOG.info(
                    "EVENT#factory-created# WS#{}# USER#{}# PROJECT#{}# TYPE#{}# REPO-URL#{}# FACTORY-URL#{}# AFFILIATE-ID#{}# ORG-ID#{}#",
                    "",
                    context.getUser().getName(),
                    "",
                    nullToEmpty(factory.getProject() != null ? factory.getProject().getType() : null),
                    factory.getSource().getProject().getLocation(),
                    linksHelper.getLinkByRelation(factory.getLinks(), "create-project").iterator().next().getHref(),
                    "",
                    nullToEmpty(factory.getCreator().getAccountId()));
              */
            return factory;
        } catch (IOException e) {
            LOG.error(e.getLocalizedMessage(), e);
            throw new ServerException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Get factory information from storage by its id.
     *
     * @param id
     *         - id of factory
     * @param uriInfo
     *         - url context
     * @return - stored data, if id is correct.
     * @throws org.eclipse.che.api.core.ApiException
     *         - {@link org.eclipse.che.api.core.NotFoundException} when factory with given id doesn't exist
     */

    @ApiOperation(value = "Get Factory information by its ID",
                  notes = "Get JSON with Factory information. Factory ID is passed in a path parameter",
                  response = Factory.class,
                  position = 2)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Factory not found"),
            @ApiResponse(code = 409, message = "Failed to validate Factory"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public Factory getFactory(@ApiParam(value = "Factory ID", required = true)
                              @PathParam("id") String id,
                              @ApiParam(value = "Legacy. Whether or not to transform Factory into the most recent format",
                                        allowableValues = "true,false", defaultValue = "false")
                              @DefaultValue("false") @QueryParam("legacy") Boolean legacy,
                              @ApiParam(value = "Whether or not to validate values like it is done when accepting a Factory",
                                        allowableValues = "true,false", defaultValue = "false")
                              @DefaultValue("false") @QueryParam("validate") Boolean validate,
                              @Context UriInfo uriInfo) throws ApiException {
        Factory factory = factoryStore.getFactory(id);

        if (legacy) {
            factory = factoryBuilder.convertToLatest(factory);
        }

        try {
            factory = factory.withLinks(linksHelper.createLinks(factory, factoryStore.getFactoryImages(id, null), uriInfo));
        } catch (UnsupportedEncodingException e) {
            throw new ServerException(e.getLocalizedMessage());
        }
        if (validate) {
            acceptValidator.validateOnAccept(factory);
        }
        return factory;
    }

    /**
     * Removes factory information from storage by its id.
     *
     * @param id
     *         id of factory
     * @param uriInfo
     *         url context
     * @throws NotFoundException
     *         when factory with given id doesn't exist
     */
    @ApiOperation(value = "Removes Factory by its ID",
                  notes = "Removes factory based on the Factory ID which is passed in a path parameter")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 403, message = "User not authorized to call this operation"),
            @ApiResponse(code = 404, message = "Factory not found"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @DELETE
    @Path("/{id}")
    @RolesAllowed("user")
    public void removeFactory(@ApiParam(value = "Factory ID", required = true)
                              @PathParam("id") String id,
                              @Context UriInfo uriInfo) throws ApiException {
        Factory factory = factoryStore.getFactory(id);

        // Validate the factory against the current user
        factoryEditValidator.validate(factory);

        // if validator didn't fail it means that the access is granted
        factoryStore.removeFactory(id);
    }


    /**
     * Updates factory with a new factory content
     *
     * @param id
     *         id of factory
     * @param newFactory
     *         the new data for the factory
     * @throws NotFoundException
     *         when factory with given id doesn't exist
     * @throws org.eclipse.che.api.core.ServerException
     *         if given factory is null
     */
    @ApiOperation(value = "Updates factory information by its ID",
                  notes = "Updates factory based on the Factory ID which is passed in a path parameter")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 403, message = "User not authorized to call this operation"),
            @ApiResponse(code = 404, message = "Factory not found"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @PUT
    @Path("/{id}")
    @RolesAllowed("user")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Factory updateFactory(@ApiParam(value = "Factory ID", required = true)
                                 @PathParam("id") String id,
                                 Factory newFactory) throws ApiException {

        // forbid null update
        if (newFactory == null) {
            throw new ServerException("The updating factory shouldn't be null");
        }

        Factory existingFactory = factoryStore.getFactory(id);

        // Validate the factory against the current user
        factoryEditValidator.validate(existingFactory);

        processDefaults(newFactory);

        newFactory.getCreator().withCreated(existingFactory.getCreator().getCreated());
        newFactory.setId(existingFactory.getId());

        // validate the new content
        createValidator.validateOnCreate(newFactory);

        // access granted, user can update the factory
        factoryStore.updateFactory(id, newFactory);

        // create links
        try {
            newFactory.setLinks(linksHelper.createLinks(newFactory, factoryStore.getFactoryImages(id, null), uriInfo));
        } catch (UnsupportedEncodingException e) {
            throw new ServerException(e.getLocalizedMessage());
        }

        return newFactory;
    }


    /**
     * Get list of factory links which conform specified attributes.
     *
     * @param uriInfo
     *         - url context
     * @return - stored data, if id is correct.
     * @throws org.eclipse.che.api.core.ApiException
     *         - {@link org.eclipse.che.api.core.NotFoundException} when factory with given id doesn't exist
     */
    @RolesAllowed({"user", "system/manager"})
    @GET
    @Path("/find")
    @Produces({MediaType.APPLICATION_JSON})
    @SuppressWarnings("unchecked")
    public List<Link> getFactoryByAttribute(@Context UriInfo uriInfo) throws ApiException {
        List<Link> result = new ArrayList<>();
        URI uri = UriBuilder.fromUri(uriInfo.getRequestUri()).replaceQueryParam("token").build();
        Map<String, Set<String>> queryParams = URLEncodedUtils.parse(uri, "UTF-8");
        if (queryParams.isEmpty()) {
            throw new IllegalArgumentException("Query must contain at least one attribute.");
        }
        ArrayList<Pair> pairs = new ArrayList<>();
        queryParams.entrySet().stream()
                   .filter(entry -> !entry.getValue().isEmpty())
                   .forEach(entry -> pairs.add(Pair.of(entry.getKey(), entry.getValue().iterator().next())));
        List<Factory> factories = factoryStore.findByAttribute(pairs.toArray(new Pair[pairs.size()]));
        result.addAll(factories.stream()
                               .map(factory -> DtoFactory.getInstance()
                                                         .createDto(Link.class)
                                                         .withMethod(HttpMethod.GET)
                                                         .withRel("self")
                                                         .withProduces(MediaType.APPLICATION_JSON)
                                                         .withConsumes(null)
                                                         .withHref(UriBuilder.fromUri(uriInfo.getBaseUri())
                                                                             .path(FactoryService.class)
                                                                             .path(FactoryService.class, "getFactory")
                                                                             .build(factory.getId())
                                                                             .toString())
                                                         .withParameters(null))
                               .collect(toList()));
        return result;
    }

    /**
     * Get image information by its id.
     *
     * @param factoryId
     *         - id of factory
     * @param imageId
     *         - image id.
     * @return - image information if ids are correct. If imageId is not set, random image of factory will be returned. But if factory has
     * no images, exception will be thrown.
     * @throws org.eclipse.che.api.core.ApiException
     *         - {@link org.eclipse.che.api.core.NotFoundException} when factory with given id doesn't exist
     *         - {@link org.eclipse.che.api.core.NotFoundException} when imgId is not set in request and there is no default image for
     *         factory
     *         with given id
     *         - {@link org.eclipse.che.api.core.NotFoundException} when image with given image id doesn't exist
     */
    @ApiOperation(value = "Get Factory image information",
                  notes = "Get Factory image information by Factory and image ID",
                  position = 3)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Factory or Image ID Not Found"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/{factoryId}/image")
    @Produces("image/*")
    public Response getImage(@ApiParam(value = "Factory ID", required = true)
                             @PathParam("factoryId") String factoryId,
                             @ApiParam(value = "Image ID", required = true)
                             @DefaultValue("") @QueryParam("imgId") String imageId)
            throws ApiException {
        Set<FactoryImage> factoryImages = factoryStore.getFactoryImages(factoryId, null);
        if (imageId.isEmpty()) {
            if (factoryImages.size() > 0) {
                FactoryImage image = factoryImages.iterator().next();
                return Response.ok(image.getImageData(), image.getMediaType()).build();
            } else {
                LOG.warn("Default image for factory {} is not found.", factoryId);
                throw new NotFoundException("Default image for factory " + factoryId + " is not found.");
            }
        } else {
            for (FactoryImage image : factoryImages) {
                if (image.getName().equals(imageId)) {
                    return Response.ok(image.getImageData(), image.getMediaType()).build();
                }
            }
        }
        LOG.warn("Image with id {} is not found.", imageId);
        throw new NotFoundException("Image with id " + imageId + " is not found.");
    }

    /**
     * Get factory snippet by factory id and snippet type. If snippet type is not set, "url" type will be used as default.
     *
     * @param id
     *         - factory id.
     * @param type
     *         - type of snippet.
     * @param uriInfo
     *         - url context
     * @return - snippet content.
     * @throws org.eclipse.che.api.core.ApiException
     *         - {@link org.eclipse.che.api.core.NotFoundException} when factory with given id doesn't exist - with response code 400 if
     *         snippet
     *         type
     *         is unsupported
     */
    @ApiOperation(value = "Get Factory snippet by ID",
                  notes = "Get Factory snippet by ID",
                  position = 4)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Factory not Found"),
            @ApiResponse(code = 409, message = "Unknown snippet type"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/{id}/snippet")
    @Produces({MediaType.TEXT_PLAIN})
    public String getFactorySnippet(@ApiParam(value = "Factory ID", required = true)
                                    @PathParam("id") String id,
                                    @ApiParam(value = "Snippet type", required = true, allowableValues = "url,html,iframe,markdown",
                                              defaultValue = "url")
                                    @DefaultValue("url") @QueryParam("type") String type,
                                    @Context UriInfo uriInfo)
            throws ApiException {
        Factory factory = factoryStore.getFactory(id);

        final String baseUrl = UriBuilder.fromUri(uriInfo.getBaseUri()).replacePath("").build().toString();

        switch (type) {
            case "url":
                return UriBuilder.fromUri(uriInfo.getBaseUri()).replacePath("factory").queryParam("id", id).build().toString();
            case "html":
                return SnippetGenerator.generateHtmlSnippet(baseUrl, id);
            case "iframe":
                return SnippetGenerator.generateiFrameSnippet(baseUrl, id);
            case "markdown":
                Set<FactoryImage> factoryImages = factoryStore.getFactoryImages(id, null);
                String imageId = (factoryImages.size() > 0) ? factoryImages.iterator().next().getName() : null;

                try {
                    return SnippetGenerator.generateMarkdownSnippet(baseUrl, factory, imageId);
                } catch (IllegalArgumentException e) {
                    throw new ConflictException(e.getMessage());
                }
            default:
                LOG.warn("Snippet type {} is unsupported", type);
                throw new ConflictException("Snippet type \"" + type + "\" is unsupported.");
        }
    }

    /**
     * Generate factory containing workspace configuration.
     * Only projects that have {@code SourceStorage} configured can be included.
     *
     * @param workspace
     *         workspace id to generate factory from.
     * @param path
     *         Optional project path. If set, only this project will be included into result projects set.
     * @throws org.eclipse.che.api.core.ApiException
     *
     */
    @ApiOperation(value = "Construct Factory from workspace",
                  notes = "This call returns a Factory.json that is used to create a Factory. ")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 403, message = "Access to workspace denied"),
            @ApiResponse(code = 404, message = "Workspace not found"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/workspace/{ws-id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFactoryJson(@ApiParam(value = "Workspace ID", required = true)
                                   @PathParam("ws-id") String workspace,
                                   @ApiParam(value = "Project path")
                                   @QueryParam("path") String path) throws ApiException {

        final UsersWorkspaceImpl usersWorkspace = workspaceManager.getWorkspace(workspace);
        final String userId = EnvironmentContext.getCurrent().getUser().getId();
        if (!usersWorkspace.getOwner().equals(userId)) {
            throw new ForbiddenException("User '" + userId + "' doesn't have access to '" + usersWorkspace.getId() + "' workspace");
        }

        Factory factory = newDto(Factory.class)
                .withWorkspace(asDto(usersWorkspace, path))
                .withV("4.0");

        return Response.ok(factory, MediaType.APPLICATION_JSON)
                       .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=factory.json")
                       .build();
    }

    private static WorkspaceConfigDto asDto(UsersWorkspaceImpl workspace, String projectPath) throws ConflictException {
        final List<CommandDto> commands = workspace.getCommands()
                                                   .stream()
                                                   .map(DtoConverter::asDto)
                                                   .collect(toList());

        // Filter out projects by path and source storage presence.
        Predicate<ProjectConfigImpl> predicate = new Predicate<ProjectConfigImpl>() {
            @Override
            public boolean test(ProjectConfigImpl projectConfig) {

                if (projectPath != null && !projectConfig.getPath().equals(projectPath)) {
                    return false;
                }
                return projectConfig.getSource() != null
                       && !isNullOrEmpty(projectConfig.getSource().getType())
                       && !isNullOrEmpty(projectConfig.getSource().getLocation());
            }
        };

        final List<ProjectConfigDto> projects = workspace.getProjects()
                                                         .stream()
                                                         .filter(predicate)
                                                         .map(DtoConverter::asDto)
                                                         .collect(toList());

        if (projects.isEmpty()) {
            throw new ConflictException(
                    "Unable to create factory from this workspace, because it does not contains projects with source storage set and/or specified path");
        }

        final Map<String, EnvironmentDto> environments = workspace.getEnvironments()
                                                                  .values()
                                                                  .stream()
                                                                  .collect(toMap(EnvironmentStateImpl::getName, FactoryService::asDto));

        return newDto(WorkspaceConfigDto.class)
                .withName(workspace.getName())
                .withDefaultEnvName(workspace.getDefaultEnvName())
                .withCommands(commands)
                .withProjects(projects)
                .withEnvironments(environments)
                .withDescription(workspace.getDescription())
                .withAttributes(workspace.getAttributes());
    }

    public static EnvironmentDto asDto(EnvironmentState environment) {
        return newDto(EnvironmentDto.class).withName(environment.getName())
                                           .withMachineConfigs(environment.getMachineConfigs().stream()
                                                                          .map(org.eclipse.che.api.machine.server.DtoConverter::asDto)
                                                                          .collect(toList()));
    }


    private void processDefaults(Factory factory) throws ApiException {
        User currentUser = EnvironmentContext.getCurrent().getUser();
        if (factory.getCreator() == null) {
            factory.setCreator(DtoFactory.getInstance().createDto(Author.class).withUserId(currentUser.getId()).withCreated(
                    System.currentTimeMillis()));
        } else {
            if (isNullOrEmpty(factory.getCreator().getUserId())) {
                factory.getCreator().setUserId(currentUser.getId());
            }
            if (factory.getCreator().getCreated() == null) {
                factory.getCreator().setCreated(System.currentTimeMillis());
            }
        }
    }
}
