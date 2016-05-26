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
package org.eclipse.che.api.runner;

import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.rest.Service;
import org.eclipse.che.api.core.rest.annotations.Description;
import org.eclipse.che.api.core.rest.annotations.GenerateLink;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.runner.dto.ApplicationProcessDescriptor;
import org.eclipse.che.api.runner.dto.RunnerDescriptor;
import org.eclipse.che.api.runner.dto.RunnerServer;
import org.eclipse.che.api.runner.dto.RunnerServerLocation;
import org.eclipse.che.api.runner.dto.RunnerServerRegistration;
import org.eclipse.che.api.runner.internal.Constants;
import org.eclipse.che.dto.server.DtoFactory;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

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
 * RESTful API for administration.
 *
 * @author andrew00x
 */
@Api(value = "/admin/runner",
     description = "Runner manager (admin)")
@Path("/admin/runner")
@Description("Runner administration REST API")
@RolesAllowed("system/admin")
public class RunnerAdminService extends Service {
    private static final Logger LOG = LoggerFactory.getLogger(RunnerAdminService.class);
    @Inject
    private RunQueue runner;

    @ApiOperation(value = "Register a new runner",
                  notes = "Register a new runner service",
                  response = RunnerServerRegistration.class,
                  position = 1)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 403, message = "User not authorized to call this method"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GenerateLink(rel = Constants.LINK_REL_REGISTER_RUNNER_SERVER)
    @POST
    @Path("/server/register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registerRunnerServer(@ApiParam(value = "JSON with runner location and options")
                                         RunnerServerRegistration registration) throws Exception {
        runner.registerRunnerServer(registration);
        return Response.status(Response.Status.OK).build();
    }

    @ApiOperation(value = "Unregister runner",
                  notes = "Unregister runner service",
                  response = RunnerServerLocation.class,
                  position = 2)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 403, message = "User not authorized to call this method"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GenerateLink(rel = Constants.LINK_REL_UNREGISTER_RUNNER_SERVER)
    @POST
    @Path("/server/unregister")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response unregisterRunnerServer(@ApiParam(value = "Location of a runner to unregister", required = true)
                                           RunnerServerLocation location) throws Exception {
        runner.unregisterRunnerServer(location);
        return Response.status(Response.Status.OK).build();
    }

    private static final String[] SERVER_LINK_RELS = new String[]{Constants.LINK_REL_AVAILABLE_RUNNERS,
                                                                  Constants.LINK_REL_SERVER_STATE,
                                                                  Constants.LINK_REL_RUNNER_STATE};

    @ApiOperation(value = "Get all registered runners",
                  notes = "Get all registered runners",
                  response = RunnerServer.class,
                  responseContainer = "List",
                  position = 3)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 403, message = "User not authorized to call this method"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GenerateLink(rel = Constants.LINK_REL_REGISTERED_RUNNER_SERVER)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/server")
    public List<RunnerServer> getRegisteredServers() {
        final List<RemoteRunnerServer> runnerServers = runner.getRegisterRunnerServers();
        final List<RunnerServer> result = new LinkedList<>();
        final DtoFactory dtoFactory = DtoFactory.getInstance();
        for (RemoteRunnerServer runnerServer : runnerServers) {
            final RunnerServer runnerServerDTO = dtoFactory.createDto(RunnerServer.class);
            runnerServerDTO.withUrl(runnerServer.getBaseUrl());
            try {
                runnerServerDTO.withDedicated(runnerServer.isDedicated())
                               .withWorkspace(runnerServer.getAssignedWorkspace())
                               .withProject(runnerServer.getAssignedProject());
                final List<Link> adminLinks = new LinkedList<>();
                for (String linkRel : SERVER_LINK_RELS) {
                    final Link link = runnerServer.getLink(linkRel);
                    if (link != null) {
                        if (Constants.LINK_REL_RUNNER_STATE.equals(linkRel)) {
                            for (RunnerDescriptor runner : runnerServer.getRunnerDescriptors()) {
                                final String href = link.getHref();
                                final String hrefWithRunner = href + ((href.indexOf('?') > 0 ? '&' : '?') + "runner=" + runner.getName());
                                final Link linkCopy = dtoFactory.clone(link);
                                linkCopy.getParameters().clear();
                                linkCopy.setHref(hrefWithRunner);
                                adminLinks.add(linkCopy);
                            }
                        } else {
                            adminLinks.add(link);
                        }
                    }
                }
                runnerServerDTO.withDescription(runnerServer.getServiceDescriptor().getDescription())
                                          .withServerState(runnerServer.getServerState())
                                          .withLinks(adminLinks);
            } catch (IOException | ServerException e) {
                LOG.error(e.getMessage(), e);
            }
            result.add(runnerServerDTO);
        }

        return result;
    }

    @ApiOperation(value = "Check runner queue",
                  notes = "Check runner queue. JSON with queue state and details is returned",
                  response = ApplicationProcessDescriptor.class,
                  responseContainer = "List",
                  position = 4)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 403, message = "User not authorized to call this method"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GenerateLink(rel = Constants.LINK_REL_RUNNER_TASKS)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/queue")
    public List<ApplicationProcessDescriptor> getTasks() throws Exception {
        final List<ApplicationProcessDescriptor> result = new LinkedList<>();
        for (RunQueueTask queueTask : runner.getTasks()) {
            final ApplicationProcessDescriptor descriptor = queueTask.getDescriptor();
            final RemoteRunnerProcess remoteProcess = queueTask.getRemoteProcess();
            if (remoteProcess != null) {
                descriptor.setServerUrl(remoteProcess.getServerUrl());
            }
            result.add(descriptor);
        }
        return result;
    }
}
