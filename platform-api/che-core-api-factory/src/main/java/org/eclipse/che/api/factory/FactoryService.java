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
package org.eclipse.che.api.factory;

import org.eclipse.che.api.account.server.dao.AccountDao;
import org.eclipse.che.api.account.server.dao.Member;
import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.rest.HttpJsonHelper;
import org.eclipse.che.api.core.rest.Service;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.factory.dto.Author;
import org.eclipse.che.api.factory.dto.Factory;
import org.eclipse.che.api.factory.dto.FactoryV2_1;
import org.eclipse.che.api.factory.dto.Workspace;
import org.eclipse.che.api.project.server.ProjectConfig;
import org.eclipse.che.api.project.server.ProjectJson;
import org.eclipse.che.api.project.server.type.AttributeValue;
import org.eclipse.che.api.project.shared.dto.Source;
import org.eclipse.che.api.project.server.Project;
import org.eclipse.che.api.project.server.ProjectManager;
import org.eclipse.che.api.project.shared.dto.ImportSourceDescriptor;
import org.eclipse.che.api.project.shared.dto.NewProject;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.lang.NameGenerator;
import org.eclipse.che.commons.lang.Pair;
import org.eclipse.che.commons.lang.URLEncodedUtils;
import org.eclipse.che.commons.user.User;
import org.eclipse.che.dto.server.DtoFactory;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.gson.JsonSyntaxException;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

import org.apache.commons.fileupload.FileItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Named;
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
import javax.ws.rs.core.Context;
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

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;

/** Service for factory rest api features */
@Api(value = "/factory",
     description = "Factory manager")
@Path("/factory")
public class FactoryService extends Service {
    private static final Logger LOG = LoggerFactory.getLogger(FactoryService.class);

    private String                 baseApiUrl;
    private FactoryStore           factoryStore;
    private FactoryEditValidator   factoryEditValidator;
    private FactoryCreateValidator createValidator;
    private FactoryAcceptValidator acceptValidator;
    private LinksHelper            linksHelper;
    private FactoryBuilder         factoryBuilder;
    private ProjectManager         projectManager;
    private AccountDao             accountDao;

    @Inject
    public FactoryService(@Named("api.endpoint") String baseApiUrl,
                          FactoryStore factoryStore,
                          AccountDao accountDao,
                          FactoryCreateValidator createValidator,
                          FactoryAcceptValidator acceptValidator,
                          FactoryEditValidator factoryEditValidator,
                          LinksHelper linksHelper,
                          FactoryBuilder factoryBuilder,
                          ProjectManager projectManager) {
        this.baseApiUrl = baseApiUrl;
        this.accountDao = accountDao;
        this.factoryStore = factoryStore;
        this.createValidator = createValidator;
        this.acceptValidator = acceptValidator;
        this.factoryEditValidator = factoryEditValidator;
        this.linksHelper = linksHelper;
        this.factoryBuilder = factoryBuilder;
        this.projectManager = projectManager;
    }

    /**
     * Save factory to storage and return stored data. Field 'factoryUrl' should contains factory url information.
     * Fields with images should be named 'image'. Acceptable image size 100x100 pixels.
     * If vcs is not set in factory URL it will be set with "git" value.
     *
     * @param formData
     *         - http request form data
     * @param uriInfo
     *         - url context
     * @return - stored data
     * @throws org.eclipse.che.api.core.ApiException
     *         - {@link org.eclipse.che.api.core.ConflictException} when factory url json is not found
     *         - {@link org.eclipse.che.api.core.ConflictException} when vcs is unsupported
     *         - {@link org.eclipse.che.api.core.ConflictException} when image content can't be read
     *         - {@link org.eclipse.che.api.core.ConflictException} when image media type is unsupported
     *         - {@link org.eclipse.che.api.core.ConflictException} when image height or length isn't equal to 100 pixels
     *         - {@link org.eclipse.che.api.core.ConflictException} when if image is too big
     *         - {@link org.eclipse.che.api.core.ServerException} when internal server error occurs
     */
    @ApiOperation(value = "Create a Factory and return data",
                  notes = "Save factory to storage and return stored data. Field 'factoryUrl' should contains factory url information.",
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
                if (fieldName.equals("factoryUrl")) {
                    try {
                        factory = factoryBuilder.buildEncoded(item.getInputStream());
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
                LOG.warn("No factory URL information found in 'factoryUrl' section of multipart form-data.");
                throw new ConflictException("No factory URL information found in 'factoryUrl' section of multipart/form-data.");
            }

            processDefaults(factory);
            createValidator.validateOnCreate(factory);
            String factoryId = factoryStore.saveFactory(factory, images);
            factory = factoryStore.getFactory(factoryId);
            factory = factory.withLinks(linksHelper.createLinks(factory, images, uriInfo));

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
            @ApiResponse(code = 409, message = "Failed to validate Factory URL"),
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
        Factory factoryUrl = factoryStore.getFactory(id);
        if (factoryUrl == null) {
            LOG.warn("Factory URL with id {} is not found.", id);
            throw new NotFoundException("Factory URL with id " + id + " is not found.");
        }

        if (legacy) {
            factoryUrl = factoryBuilder.convertToLatest(factoryUrl);
        }

        try {
            factoryUrl = factoryUrl.withLinks(linksHelper.createLinks(factoryUrl, factoryStore.getFactoryImages(id, null), uriInfo));
        } catch (UnsupportedEncodingException e) {
            throw new ServerException(e.getLocalizedMessage());
        }
        if (validate) {
            acceptValidator.validateOnAccept(factoryUrl, true);
        }
        return factoryUrl;
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
    @ApiOperation(value = "Removes Factory information by its ID",
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

        // Check we've a user
        final User user = EnvironmentContext.getCurrent().getUser();
        if (user == null) {
            // well this shouldn't happen if only user is authorized to call the method
            throw new ForbiddenException("No authenticated user");
        }

        // Do we have a factory for this id ?
        Factory factory = factoryStore.getFactory(id);
        if (factory == null) {
            throw new NotFoundException("Factory with id " + id + " is not found.");
        }

        // Gets the User id
        String userId = user.getId();

        // Validate the factory against the current user
        factoryEditValidator.validate(factory, userId);

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

        // Do we have a factory for this id ?
        Factory existingFactory = factoryStore.getFactory(id);
        if (existingFactory == null) {
            throw new NotFoundException("Factory with id " + id + " does not exist.");
        }

        // Gets the User id
        final User user = EnvironmentContext.getCurrent().getUser();
        String userId = user.getId();

        // Validate the factory against the current user
        factoryEditValidator.validate(existingFactory, userId);

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
        URI uri = UriBuilder.fromUri(uriInfo.getRequestUri()).replaceQueryParam("token", null).build();
        Map<String, Set<String>> queryParams = URLEncodedUtils.parse(uri, "UTF-8");
        if (queryParams.isEmpty()) {
            throw new IllegalArgumentException("Query must contain at least one attribute.");
        }
        ArrayList<Pair> pairs = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : queryParams.entrySet()) {
            if (!entry.getValue().isEmpty())
                pairs.add(Pair.of(entry.getKey(), entry.getValue().iterator().next()));
        }
        List<Factory> factories = factoryStore.findByAttribute(pairs.toArray(new Pair[pairs.size()]));
        for (Factory factory : factories) {
            result.add(DtoFactory.getInstance().createDto(Link.class)
                                 .withMethod("GET")
                                 .withRel("self")
                                 .withProduces(MediaType.APPLICATION_JSON)
                                 .withConsumes(null)
                                 .withHref(UriBuilder.fromUri(uriInfo.getBaseUri())
                                                     .path(FactoryService.class)
                                                     .path(FactoryService.class, "getFactory").build(factory.getId())
                                                     .toString())
                                 .withParameters(null));
        }
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
     *         - {@link org.eclipse.che.api.core.NotFoundException} when imgId is not set in request and there is no default image for factory
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
        if (factoryImages == null) {
            LOG.warn("Factory URL with id {} is not found.", factoryId);
            throw new NotFoundException("Factory URL with id " + factoryId + " is not found.");
        }
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
        if (factory == null) {
            LOG.warn("Factory URL with id {} is not found.", id);
            throw new NotFoundException("Factory URL with id " + id + " is not found.");
        }

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
     * Generate project configuration.
     *
     * @param workspace
     *         - workspace id.
     * @param path
     *         - project path.
     * @throws org.eclipse.che.api.core.ApiException
     *         - {@link org.eclipse.che.api.core.ConflictException} when project is not under source control.
     */
    @GET
    @Path("/{ws-id}/{path:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFactoryJson(@PathParam("ws-id") String workspace, @PathParam("path") String path) throws ApiException {
        final Project project = projectManager.getProject(workspace, path);

        if (project == null) {
            throw new NotFoundException("Project " + path + " are not found in workspace " + workspace);
        }
        final DtoFactory dtoFactory = DtoFactory.getInstance();
        ImportSourceDescriptor source;
        NewProject newProject;
        try {
            final ProjectConfig projectDescription = project.getConfig();
            Map<String, AttributeValue> attributes = projectDescription.getAttributes();
            if (attributes.containsKey("vcs.provider.name") && attributes.get("vcs.provider.name").getList().contains("git")) {
                final Link importSourceLink = dtoFactory.createDto(Link.class)
                                                        .withMethod("GET")
                                                        .withHref(UriBuilder.fromUri(baseApiUrl)
                                                                            .path("git")
                                                                            .path(workspace)
                                                                            .path("import-source-descriptor")
                                                                            .build().toString());
                source = HttpJsonHelper.request(ImportSourceDescriptor.class, importSourceLink, new Pair<>("projectPath", path));
            } else {
                throw new ConflictException("Not able to generate project configuration, project has to be under version control system");
            }
            // Read again project.json file even we already have all information about project in 'projectDescription' variable.
            // We do so because 'projectDescription' variable contains all attributes of project including 'calculated' attributes but we
            // don't need 'calculated' attributes in this case. Such attributes exists only in runtime and may be restored from the project.
            // TODO: improve this once we will be able to detect different type of attributes. In this case just need get attributes from
            // 'projectDescription' variable and skip all attributes that aren't defined in project.json file.
            final ProjectJson projectJson = ProjectJson.load(project);
            newProject = dtoFactory.createDto(NewProject.class)
                                   .withName(project.getName())
                                   .withType(projectJson.getType())
                                   .withAttributes(projectJson.getAttributes())
                                   .withVisibility(project.getVisibility())
                                   .withDescription(projectJson.getDescription());
            newProject.setMixinTypes(projectJson.getMixinTypes());
            newProject.setRecipe(projectJson.getRecipe());

//            final Builders builders = projectJson.getBuilders();
//            if (builders != null) {
//                newProject.withBuilders(DtoConverter.toDto(builders));
//            }
//            final Runners runners = projectJson.getRunners();
//            if (runners != null) {
//                newProject.withRunners(DtoConverter.toDto(runners));
//            }
        } catch (IOException e) {
            throw new ServerException(e.getLocalizedMessage());
        }
        return Response.ok(dtoFactory.createDto(FactoryV2_1.class)
                                     .withProject(newProject)
                                     .withSource(dtoFactory.createDto(Source.class).withProject(source))
                                     .withV("2.1"), MediaType.APPLICATION_JSON)
                       .header("Content-Disposition", "attachment; filename=" + path + ".json")
                       .build();
    }

    private void processDefaults(Factory factory) throws ApiException {

        User currentUser =  EnvironmentContext.getCurrent().getUser();
        if (factory.getCreator() == null) {
            factory.setCreator(DtoFactory.getInstance().createDto(Author.class).withUserId(currentUser.getId()).withCreated(
                    System.currentTimeMillis()));
        } else {
            if (isNullOrEmpty(factory.getCreator().getUserId())){
                factory.getCreator().setUserId(currentUser.getId());
            }
            if (factory.getCreator().getCreated() == null) {
                factory.getCreator().setCreated(System.currentTimeMillis());
            }
        }

        if (factory.getWorkspace() ==  null) {
            factory.setWorkspace(DtoFactory.getInstance().createDto(Workspace.class).withType("temp").withLocation("owner"));
        } else {
            if (isNullOrEmpty(factory.getWorkspace().getType())) {
                factory.getWorkspace().setType("temp");
            }
            if (isNullOrEmpty(factory.getWorkspace().getLocation())) {
                factory.getWorkspace().setLocation("owner");
            }
        }

        if (factory.getWorkspace().getLocation().equals("owner") && factory.getCreator().getAccountId() == null) {
            List<Member> ownedAccounts = FluentIterable.from(accountDao.getByMember(currentUser.getId())).filter(new Predicate<Member>() {
                @Override
                public boolean apply(Member input) {
                    return input.getRoles().contains("account/owner");
                }
            }).toList();
            switch (ownedAccounts.size()) {
                case 0: {
                    // must never happen but who knows
                    throw new ForbiddenException(
                            "You are not owner of any account, so you can't create factory with such workspace location.");
                }
                case 1: {
                    factory.getCreator().setAccountId(ownedAccounts.get(0).getAccountId());
                    break;
                }
                default: {
                    throw new ForbiddenException(
                            "You are owner of more than one account. Please indicate which one to use using creator/accountId property.");
                }
            }
        }
    }
}
