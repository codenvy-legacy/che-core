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
package org.eclipse.che.api.builder;

import org.eclipse.che.api.builder.dto.BuilderDescriptor;
import org.eclipse.che.api.builder.dto.BuilderServer;
import org.eclipse.che.api.builder.dto.BuilderServerLocation;
import org.eclipse.che.api.builder.dto.BuilderServerRegistration;
import org.eclipse.che.api.builder.internal.Constants;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.rest.Service;
import org.eclipse.che.api.core.rest.annotations.Description;
import org.eclipse.che.api.core.rest.annotations.GenerateLink;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.dto.server.DtoFactory;
import com.wordnik.swagger.annotations.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Builder admin API.
 *
 * @author andrew00x
 */
@Api(value = "/admin/builder",
     description = "Builder manager (admin)")
@Path("/admin/builder")
@Description("Builder API")
@RolesAllowed("system/admin")
public class BuilderAdminService extends Service {
    private static final Logger LOG = LoggerFactory.getLogger(BuilderAdminService.class);
    @Inject
    private BuildQueue buildQueue;

    @ApiOperation(value = "Register a new builder",
                  notes = "Register a new builder service",
                  response = BuilderServerRegistration.class,
                  position = 1)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 403, message = "User not authorized to call this method"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GenerateLink(rel = Constants.LINK_REL_REGISTER_BUILDER_SERVICE)
    @POST
    @Path("/server/register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response register(@ApiParam(value = "JSON with builder location and options", required = true)
                             BuilderServerRegistration registration) throws Exception {
        buildQueue.registerBuilderServer(registration);
        return Response.status(Response.Status.OK).build();
    }

    @ApiOperation(value = "Unregister builder",
                  notes = "Unregister builder service",
                  response = BuilderServerLocation.class,
                  position = 2)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 403, message = "User not authorized to call this method"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GenerateLink(rel = Constants.LINK_REL_UNREGISTER_BUILDER_SERVICE)
    @POST
    @Path("/server/unregister")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response unregister(@ApiParam(value = "JSON with builder location", required = true)
                               BuilderServerLocation location) throws Exception {
        buildQueue.unregisterBuilderServer(location);
        return Response.status(Response.Status.OK).build();
    }

    private static final String[] SERVER_LINK_RELS = new String[]{Constants.LINK_REL_AVAILABLE_BUILDERS,
                                                                  Constants.LINK_REL_SERVER_STATE,
                                                                  Constants.LINK_REL_BUILDER_STATE};

    @ApiOperation(value = "Get all registered builders",
                  notes = "Get all registered builders",
                  response = BuilderServer.class,
                  responseContainer = "List",
                  position = 3)
    @ApiResponses(value = {
                  @ApiResponse(code = 200, message = "OK"),
                  @ApiResponse(code = 403, message = "User not authorized to call this method"),
                  @ApiResponse(code = 500, message = "Internal Server Error")})
    @GenerateLink(rel = Constants.LINK_REL_REGISTERED_BUILDER_SERVER)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/server")
    public List<BuilderServer> getRegisteredServers() {
        final List<RemoteBuilderServer> builderServers = buildQueue.getRegisterBuilderServers();
        final List<BuilderServer> result = new LinkedList<>();
        final DtoFactory dtoFactory = DtoFactory.getInstance();
        for (RemoteBuilderServer builderServer : builderServers) {
            final BuilderServer builderServerDTO = dtoFactory.createDto(BuilderServer.class);
            builderServerDTO.withUrl(builderServer.getBaseUrl())
                            .withDedicated(builderServer.isDedicated())
                            .withWorkspace(builderServer.getAssignedWorkspace())
                            .withProject(builderServer.getAssignedProject());
            try {
                final List<Link> adminLinks = new LinkedList<>();
                for (String linkRel : SERVER_LINK_RELS) {
                    final Link link = builderServer.getLink(linkRel);
                    if (link != null) {
                        if (Constants.LINK_REL_BUILDER_STATE.equals(linkRel)) {
                            for (BuilderDescriptor builderImpl : builderServer.getBuilderDescriptors()) {
                                final String href = link.getHref();
                                final String hrefWithBuilder =
                                        href + ((href.indexOf('?') > 0 ? '&' : '?') + "builder=" + builderImpl.getName());
                                final Link linkCopy = dtoFactory.clone(link);
                                linkCopy.getParameters().clear();
                                linkCopy.setHref(hrefWithBuilder);
                                adminLinks.add(linkCopy);
                            }
                        } else {
                            adminLinks.add(link);
                        }
                    }
                }
                builderServerDTO.withDescription(builderServer.getServiceDescriptor().getDescription())
                                .withServerState(builderServer.getServerState())
                                .withLinks(adminLinks);
            } catch (ServerException | IOException e) {
                LOG.error(e.getMessage(), e);
            }
            result.add(builderServerDTO);
        }

        return result;
    }
}
