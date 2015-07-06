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

import org.eclipse.che.api.builder.dto.BaseBuilderRequest;
import org.eclipse.che.api.builder.dto.BuildOptions;
import org.eclipse.che.api.builder.dto.BuildTaskDescriptor;
import org.eclipse.che.api.builder.dto.BuilderDescriptor;
import org.eclipse.che.api.builder.internal.Constants;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.rest.HttpServletProxyResponse;
import org.eclipse.che.api.core.rest.Service;
import org.eclipse.che.api.core.rest.annotations.Description;
import org.eclipse.che.api.core.rest.annotations.GenerateLink;
import org.eclipse.che.api.core.rest.annotations.Required;
import org.eclipse.che.api.core.rest.annotations.Valid;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.lang.Pair;
import org.eclipse.che.commons.user.User;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * RESTful frontend for BuildQueue.
 *
 * @author andrew00x
 * @author Eugene Voevodin
 */
@Api(value = "/builder",
     description = "Builder manager")
@Path("/builder/{ws-id}")
@Description("Builder API")
public class BuilderService extends Service {
    private static final Logger LOG = LoggerFactory.getLogger(BuilderService.class);
    @Inject
    private BuildQueue buildQueue;

    @ApiOperation(value = "Build a project",
                  notes = "Build a project. Optional build options are passed in a JSON",
                  response = BuildTaskDescriptor.class,
                  position = 1)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 500, message = "Server Error")})
    @GenerateLink(rel = Constants.LINK_REL_BUILD)
    @POST
    @Path("/build")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public BuildTaskDescriptor build(@PathParam("ws-id") String workspace,
                                     @ApiParam(value = "Project name", required = true)
                                     @Required @Description("project name") @QueryParam("project") String project,
                                     @ApiParam(
                                             value = "Build options. Here you specify optional build options like skip tests, build targets etc.")
                                     @Description("build options") BuildOptions options) throws Exception {
        return buildQueue.scheduleBuild(workspace, project, getServiceContext(), options).getDescriptor();
    }

    @ApiOperation(value = "Analyze dependencies",
                  notes = "Analyze dependencies",
                  response = BuildTaskDescriptor.class,
                  position = 2)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 500, message = "Server Error")})
    @GenerateLink(rel = Constants.LINK_REL_DEPENDENCIES_ANALYSIS)
    @POST
    @Path("/dependencies")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public BuildTaskDescriptor dependencies(@ApiParam(value = "Workspace ID", required = true)
                                            @PathParam("ws-id") String workspace,
                                            @ApiParam(value = "Project name", required = true)
                                            @Required @Description("project name") @QueryParam("project") String project,
                                            @ApiParam(value = "Analysis type. If dropped, list is used by default", defaultValue = "list",
                                                    allowableValues = "copy,list, copy-sources")
                                            @Valid({"copy", "list"}) @DefaultValue("list") @QueryParam("type") String analyzeType,
                                            @ApiParam(
                                                    value = "Build options. Here you specify optional build options like skip tests, " +
                                                            "build targets etc.")
                                            @Description("build options") BuildOptions options)
            throws Exception {
        return buildQueue.scheduleDependenciesAnalyze(workspace, project, analyzeType, getServiceContext(), options).getDescriptor();
    }

    @ApiOperation(value = "Get project build tasks",
                  notes = "Get build tasks that are related to a particular project. User can see only own processes related to own projects.",
                  response = BuildTaskDescriptor.class,
                  responseContainer = "List",
                  position = 3)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 500, message = "Server Error")})
    @GET
    @Path("/builds")
    @Produces(MediaType.APPLICATION_JSON)
    public List<BuildTaskDescriptor> builds(@ApiParam(value = "Workspace ID", required = true)
                                            @PathParam("ws-id") String workspace,
                                            @ApiParam(value = "Project name", required = false)
                                            @Required @Description("project name")
                                            @QueryParam("project") String project) {
        final List<BuildTaskDescriptor> builds = new LinkedList<>();
        final User user = EnvironmentContext.getCurrent().getUser();
        if (user != null) {
            List<BuildQueueTask> tasks = buildQueue.getTasks(workspace, project, user);
            for (BuildQueueTask task : tasks) {
                try {
                    builds.add(task.getDescriptor());
                } catch (NotFoundException e) {
                    // NotFoundException is possible and should not be treated as error in this case. Typically it occurs if slave
                    // builder already cleaned up the task by its internal cleaner but BuildQueue doesn't re-check yet slave builder and
                    // doesn't have actual info about state of slave builder.
                } catch (BuilderException e) {
                    // Decide ignore such error to be able show maximum available info.
                    LOG.error(e.getMessage(), e);
                }
            }
        }
        return builds;
    }

    @ApiOperation(value = "Get build status",
                  notes = "Get status of a specified build",
                  response = BuildTaskDescriptor.class,
                  position = 4)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Not Found"),
            @ApiResponse(code = 500, message = "Server error")})
    @GET
    @Path("/status/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public BuildTaskDescriptor getStatus(@ApiParam(value = "Workspace ID", required = true)
                                         @PathParam("ws-id") String workspace,
                                         @ApiParam(value = "Build ID", required = true)
                                         @PathParam("id")
                                         Long id) throws Exception {
        return buildQueue.getTask(id).getDescriptor();
    }

    @ApiOperation(value = "Cancel build",
                  notes = "Cancel build task",
                  response = BuildTaskDescriptor.class,
                  position = 5)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Not Found"),
            @ApiResponse(code = 500, message = "Server error")})
    @POST
    @Path("/cancel/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public BuildTaskDescriptor cancel(@ApiParam(value = "Workspace ID", required = true)
                                      @PathParam("ws-id") String workspace,
                                      @ApiParam(value = "Build ID", required = true)
                                      @PathParam("id") Long id) throws Exception {
        final BuildQueueTask task = buildQueue.getTask(id);
        task.cancel();
        return task.getDescriptor();
    }

    @ApiOperation(value = "Get build logs",
                  notes = "Get build logs",
                  position = 5)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Not Found"),
            @ApiResponse(code = 500, message = "Server error")})
    @GET
    @Path("/logs/{id}")
    public void getLogs(@ApiParam(value = "Workspace ID", required = true)
                        @PathParam("ws-id") String workspace,
                        @ApiParam(value = "Get build logs", required = true)
                        @PathParam("id") Long id,
                        @Context HttpServletResponse httpServletResponse) throws Exception {
        // Response write directly to the servlet request stream
        buildQueue.getTask(id).readLogs(new HttpServletProxyResponse(httpServletResponse));
    }


    @ApiOperation(value = "Get build report",
                  notes = "Get build report by build ID",
                  position = 6)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Not Found"),
            @ApiResponse(code = 500, message = "Server error")})
    @GET
    @Path("/report/{id}")
    public void getReport(@ApiParam(value = "Workspace ID", required = true)
                          @PathParam("ws-id") String workspace,
                          @ApiParam(value = "Build ID", required = true)
                          @PathParam("id") Long id,
                          @Context HttpServletResponse httpServletResponse) throws Exception {
        // Response write directly to the servlet request stream
        buildQueue.getTask(id).readReport(new HttpServletProxyResponse(httpServletResponse));
    }

    private static final Pattern JSON_CONTENT_TYPE_PATTERN = Pattern.compile("^application/json(\\s*;.*)?$");
    private static final Pattern HTML_CONTENT_TYPE_PATTERN = Pattern.compile("^text/htm(l)?(\\s*;.*)?$");

    @GET
    @Path("browse/{id}")
    public void browseDirectory(@PathParam("ws-id") String workspace,
                                @PathParam("id") Long id,
                                @DefaultValue(".") @QueryParam("path") String path,
                                @Context HttpServletResponse httpServletResponse) throws Exception {
        final BuildQueueTask myTask = buildQueue.getTask(id);
        final RemoteTask myRemoteTask = myTask.getRemoteTask();
        if (myRemoteTask == null) {
            return;
        }
        final String myBaseUri = getServiceContext().getServiceUriBuilder().build(workspace).toString();
        final String from = String.format("%s/(browse|download|view)/%s/%d",
                                          myRemoteTask.getBaseRemoteUrl(), myRemoteTask.getBuilder(), myRemoteTask.getId());
        final String to = String.format("%s/$1/%d", myBaseUri, myTask.getId());
        final List<Pair<String, String>> urlRewriteRules = new ArrayList<>(1);
        urlRewriteRules.add(Pair.of(from, to));
        // Response write directly to the servlet request stream
        final HttpServletProxyResponse proxyResponse = new HttpServletProxyResponse(httpServletResponse,
                                                                                    Collections.singletonMap(
                                                                                            HTML_CONTENT_TYPE_PATTERN,
                                                                                            urlRewriteRules));
        myRemoteTask.browseDirectory(path, proxyResponse);
    }

    @GET
    @Path("/view/{id}")
    public void viewFile(@PathParam("ws-id") String workspace,
                         @PathParam("id") Long id,
                         @Required @QueryParam("path") String path,
                         @Context HttpServletResponse httpServletResponse) throws Exception {
        // Response write directly to the servlet request stream
        buildQueue.getTask(id).readFile(path, new HttpServletProxyResponse(httpServletResponse));
    }

    @GET
    @Path("tree/{id}")
    public void listDirectory(@PathParam("ws-id") String workspace,
                              @PathParam("id") Long id,
                              @DefaultValue(".") @QueryParam("path") String path,
                              @Context HttpServletResponse httpServletResponse) throws Exception {
        final BuildQueueTask myTask = buildQueue.getTask(id);
        final RemoteTask myRemoteTask = myTask.getRemoteTask();
        if (myRemoteTask == null) {
            return;
        }
        final String myBaseUri = getServiceContext().getServiceUriBuilder().build(workspace).toString();
        final String from = String.format("%s/(tree|download|view)/%s/%d",
                                          myRemoteTask.getBaseRemoteUrl(), myRemoteTask.getBuilder(), myRemoteTask.getId());
        final String to = String.format("%s/$1/%d", myBaseUri, myTask.getId());
        final List<Pair<String, String>> urlRewriteRules = new ArrayList<>(1);
        urlRewriteRules.add(Pair.of(from, to));
        // Response write directly to the servlet request stream
        final HttpServletProxyResponse proxyResponse = new HttpServletProxyResponse(httpServletResponse,
                                                                                    Collections.singletonMap(
                                                                                            JSON_CONTENT_TYPE_PATTERN,
                                                                                            urlRewriteRules));
        myRemoteTask.listDirectory(path, proxyResponse);
    }

    @ApiOperation(value = "Download build artifact",
            notes = "Download build artifact",
            position = 7)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Not Found"),
            @ApiResponse(code = 500, message = "Server error")})
    @GET
    @Path("/download/{id}")
    public void downloadFile(@ApiParam(value = "Workspace ID", required = true)
                             @PathParam("ws-id") String workspace,
                             @ApiParam(value = "Build ID", required = true)
                             @PathParam("id") Long id,
                             @ApiParam(value = "Path to a build artifact as /target/{BuildArtifactName}", required = true)
                             @Required @QueryParam("path") String path,
                             @Context HttpServletResponse httpServletResponse) throws Exception {
        // Response write directly to the servlet request stream
        buildQueue.getTask(id).downloadFile(path, new HttpServletProxyResponse(httpServletResponse));
    }

    @ApiOperation(value = "Download all build artifact as tar or zip archive",
            notes = "Download all build artifacts as tar or zip archive",
            position = 8)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Not Found"),
            @ApiResponse(code = 500, message = "Server error")})
    @GET
    @Path("/download-all/{id}")
    public void downloadResultArchive(@ApiParam(value = "Workspace ID", required = true)
                                      @PathParam("ws-id") String workspace,
                                      @ApiParam(value = "Build ID", required = true)
                                      @PathParam("id") Long id,
                                      @ApiParam(value = "Archive type", defaultValue = Constants.RESULT_ARCHIVE_TAR, allowableValues = "tar,zip")
                                      @Required @QueryParam("arch") String arch,
                                      @Context HttpServletResponse httpServletResponse) throws Exception {
        // Response write directly to the servlet request stream
        buildQueue.getTask(id).downloadResultArchive(arch, new HttpServletProxyResponse(httpServletResponse));
    }

    @ApiOperation(value = "Get all builders",
            notes = "Get information on all registered builders",
            response = BuilderDescriptor.class,
            responseContainer = "List",
            position = 9)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Not Found"),
            @ApiResponse(code = 500, message = "Server error")})
    @GenerateLink(rel = Constants.LINK_REL_AVAILABLE_BUILDERS)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/builders")
    public List<BuilderDescriptor> getRegisteredServers(@ApiParam(value = "Workspace ID", required = true)
                                                        @PathParam("ws-id") String workspace) {
        final List<RemoteBuilderServer> builderServers = buildQueue.getRegisterBuilderServers();
        for (Iterator<RemoteBuilderServer> itr = builderServers.iterator(); itr.hasNext(); ) {
            final RemoteBuilderServer builderServer = itr.next();
            if (!builderServer.isAvailable()) {
                LOG.error("Builder server {} becomes unavailable", builderServer.getBaseUrl());
                itr.remove();
            }
        }

        final List<BuilderDescriptor> result = new LinkedList<>();
        for (RemoteBuilderServer builderServer : builderServers) {
            final String assignedWorkspace = builderServer.getAssignedWorkspace();
            if (assignedWorkspace == null || assignedWorkspace.equals(workspace)) {
                try {
                    result.addAll(builderServer.getBuilderDescriptors());
                } catch (BuilderException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        }
        return result;
    }
}
