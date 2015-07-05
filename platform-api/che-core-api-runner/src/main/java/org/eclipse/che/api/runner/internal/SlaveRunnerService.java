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
package org.eclipse.che.api.runner.internal;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.rest.Service;
import org.eclipse.che.api.core.rest.ServiceContext;
import org.eclipse.che.api.core.rest.annotations.Description;
import org.eclipse.che.api.core.rest.annotations.GenerateLink;
import org.eclipse.che.api.core.rest.annotations.Required;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.core.rest.shared.dto.ServiceDescriptor;
import org.eclipse.che.api.core.util.SystemInfo;
import org.eclipse.che.api.project.shared.dto.RunnerEnvironment;
import org.eclipse.che.api.runner.ApplicationStatus;
import org.eclipse.che.api.runner.RunnerException;
import org.eclipse.che.api.runner.dto.RunRequest;
import org.eclipse.che.api.runner.dto.RunnerDescriptor;
import org.eclipse.che.api.runner.dto.RunnerServerDescriptor;
import org.eclipse.che.api.runner.dto.ServerState;
import org.eclipse.che.api.runner.dto.ApplicationProcessDescriptor;
import org.eclipse.che.api.runner.dto.PortMapping;
import org.eclipse.che.api.runner.dto.RunnerState;
import org.eclipse.che.dto.server.DtoFactory;

import com.google.common.io.Files;

import javax.annotation.CheckForNull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * RESTful API for slave-runners.
 *
 * @author andrew00x
 */
@Description("Internal Runner REST API")
@Path("internal/runner")
public class SlaveRunnerService extends Service {
    private static final String DOCKERFILES_REPO = "runner.docker.dockerfiles_repo";
    private static final String DOCKER_FILE_NAME = "/Dockerfile";
    private static final String SYSTEM_PREFIX    = "system:/";

    @com.google.inject.Inject(optional = true)
    @Named(Constants.RUNNER_ASSIGNED_TO_WORKSPACE)
    private static String assignedWorkspace;

    @com.google.inject.Inject(optional = true)
    @Named(Constants.RUNNER_ASSIGNED_TO_PROJECT)
    private static String assignedProject;

    @Inject
    private RunnerRegistry runners;

    @Inject
    private ResourceAllocators allocators;

    @Inject
    @Named(DOCKERFILES_REPO)
    @CheckForNull
    private String dockerfilesRepository;

    @GenerateLink(rel = Constants.LINK_REL_RUN)
    @Path("run")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ApplicationProcessDescriptor run(@Description("Parameters for run task in JSON format") RunRequest request) throws Exception {
        final Runner myRunner = getRunner(request.getRunner());
        final RunnerProcess process = myRunner.execute(request);
        return getDescriptor(process, getServiceContext()).withRunStats(myRunner.getStats(process.getId()));
    }

    @GET
    @Path("status/{runner:.*}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ApplicationProcessDescriptor getStatus(@PathParam("runner") String runner, @PathParam("id") Long id) throws Exception {
        final Runner myRunner = getRunner(runner);
        final RunnerProcess process = myRunner.getProcess(id);
        return getDescriptor(process, getServiceContext()).withRunStats(myRunner.getStats(id));
    }

    @POST
    @Path("stop/{runner:.*}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ApplicationProcessDescriptor stop(@PathParam("runner") String runner, @PathParam("id") Long id) throws Exception {
        final Runner myRunner = getRunner(runner);
        final RunnerProcess process = myRunner.getProcess(id);
        process.stop();
        return getDescriptor(process, getServiceContext()).withRunStats(myRunner.getStats(id));
    }

    @GET
    @Path("logs/{runner:.*}/{id}")
    public void getLogs(@PathParam("runner") String runner,
                        @PathParam("id") Long id,
                        @Context HttpServletResponse httpServletResponse) throws Exception {
        final Runner myRunner = getRunner(runner);
        final RunnerProcess process = myRunner.getProcess(id);
        final Throwable error = process.getError();
        if (error != null) {
            final PrintWriter output = httpServletResponse.getWriter();
            httpServletResponse.setContentType(MediaType.TEXT_PLAIN);
            if (error instanceof RunnerException) {
                // expect ot have nice messages from our API
                output.write(error.getMessage());
            } else {
                error.printStackTrace(output);
            }
            output.flush();
        } else {
            final ApplicationLogger logger = process.getLogger();
            final PrintWriter output = httpServletResponse.getWriter();
            httpServletResponse.setContentType(logger.getContentType());
            logger.getLogs(output);
            output.flush();
        }
    }

    @GET
    @Path("recipe/{runner:.*}/{id}")
    public void getRecipeFile(@PathParam("runner") String runner,
                              @PathParam("id") Long id,
                              @Context HttpServletResponse httpServletResponse) throws Exception {
        final Runner myRunner = getRunner(runner);
        final RunnerProcess process = myRunner.getProcess(id);
        final java.io.File recipeFile = process.getConfiguration().getRecipeFile();
        if (recipeFile == null) {
            throw new NotFoundException("Recipe file isn't available. ");
        }
        final PrintWriter output = httpServletResponse.getWriter();
        httpServletResponse.setContentType(MediaType.TEXT_PLAIN);
        Files.copy(recipeFile, Charset.forName("UTF-8"), output);
        output.flush();
    }


    @GenerateLink(rel = Constants.LINK_REL_SERVER_STATE)
    @GET
    @Path("server-state")
    @Produces(MediaType.APPLICATION_JSON)
    public ServerState getServerState() {
        return DtoFactory.getInstance().createDto(ServerState.class)
                         .withCpuPercentUsage(SystemInfo.cpu())
                         .withTotalMemory(allocators.totalMemory())
                         .withFreeMemory(allocators.freeMemory());
    }

    @GenerateLink(rel = Constants.LINK_REL_AVAILABLE_RUNNERS)
    @GET
    @Path("available")
    @Produces(MediaType.APPLICATION_JSON)
    public List<RunnerDescriptor> getAvailableRunners() {
        final Set<Runner> all = runners.getAll();
        final List<RunnerDescriptor> list = new LinkedList<>();
        final DtoFactory dtoFactory = DtoFactory.getInstance();
        for (Runner runner : all) {
            list.add(dtoFactory.createDto(RunnerDescriptor.class)
                               .withName(runner.getName())
                               .withDescription(runner.getDescription())
                               .withEnvironments(runner.getEnvironments()));
        }
        return list;
    }

    @GenerateLink(rel = Constants.LINK_REL_RUNNER_STATE)
    @GET
    @Path("state")
    @Produces(MediaType.APPLICATION_JSON)
    public RunnerState getRunnerState(@Required
                                      @Description("Name of the runner")
                                      @QueryParam("runner") String runner) throws Exception {
        final Runner myRunner = getRunner(runner);
        return DtoFactory.getInstance().createDto(RunnerState.class)
                         .withName(myRunner.getName())
                         .withStats(myRunner.getStats())
                         .withServerState(getServerState());
    }

    @GenerateLink(rel = Constants.LINK_REL_RUNNER_ENVIRONMENTS)
    @GET
    @Path("environments")
    @Produces(MediaType.APPLICATION_JSON)
    public List<RunnerEnvironment> getRunnerEnvironments(@Required
                                                         @Description("Name of the runner")
                                                         @QueryParam("runner") String runner) throws Exception {
        return getRunner(runner).getEnvironments();
    }

    private Runner getRunner(String name) throws NotFoundException {
        final Runner myRunner = runners.get(name);
        if (myRunner == null) {
            throw new NotFoundException(String.format("Unknown runner %s", name));
        }
        return myRunner;
    }

    private ApplicationProcessDescriptor getDescriptor(RunnerProcess process, ServiceContext restfulRequestContext) throws RunnerException {
        final ApplicationStatus status = process.getError() == null ? (process.isCancelled() ? ApplicationStatus.CANCELLED
                                                                                             : (process.isStopped()
                                                                                                ? ApplicationStatus.STOPPED
                                                                                                : (process.isStarted()
                                                                                                   ? ApplicationStatus.RUNNING
                                                                                                   : ApplicationStatus.NEW)))
                                                                    : ApplicationStatus.FAILED;
        final List<Link> links = new LinkedList<>();
        final UriBuilder servicePathBuilder = restfulRequestContext.getServiceUriBuilder();
        final DtoFactory dtoFactory = DtoFactory.getInstance();
        links.add(dtoFactory.createDto(Link.class)
                            .withRel(Constants.LINK_REL_GET_STATUS)
                            .withHref(servicePathBuilder.clone().path(getClass(), "getStatus")
                                                        .build(process.getRunner(), process.getId()).toString())
                            .withMethod(HttpMethod.GET)
                            .withProduces(MediaType.APPLICATION_JSON));
        links.add(dtoFactory.createDto(Link.class)
                            .withRel(Constants.LINK_REL_VIEW_LOG)
                            .withHref(servicePathBuilder.clone().path(getClass(), "getLogs")
                                                        .build(process.getRunner(), process.getId()).toString())
                            .withMethod(HttpMethod.GET));
        switch (status) {
            case NEW:
            case RUNNING:
                links.add(dtoFactory.createDto(Link.class)
                                    .withRel(Constants.LINK_REL_STOP)
                                    .withHref(servicePathBuilder.clone().path(getClass(), "stop")
                                                                .build(process.getRunner(), process.getId()).toString())
                                    .withMethod(HttpMethod.POST)
                                    .withProduces(MediaType.APPLICATION_JSON));
                break;
        }
        final RunnerConfiguration configuration = process.getConfiguration();
        final RunRequest request = configuration.getRequest();
        final java.io.File recipeFile = configuration.getRecipeFile();
        if (recipeFile != null) {
            links.add(dtoFactory.createDto(Link.class)
                                .withRel(Constants.LINK_REL_RUNNER_RECIPE)
                                .withHref(servicePathBuilder.clone().path(getClass(), "getRecipeFile")
                                                            .build(process.getRunner(), process.getId()).toString())
                                .withMethod(HttpMethod.GET)
                                .withProduces(MediaType.TEXT_PLAIN));
        }
        final List<Link> additionalLinks = new LinkedList<>();
        PortMapping portMapping = null;
        switch (status) {
            case NEW:
            case RUNNING:
                for (Link link : configuration.getLinks()) {
                    additionalLinks.add(dtoFactory.clone(link));
                }
                final Map<String, String> ports = configuration.getPortMapping();
                if (!ports.isEmpty()) {
                    portMapping = dtoFactory.createDto(PortMapping.class).withHost(configuration.getHost()).withPorts(new HashMap<>(ports));
                }
                break;
            default:
                for (Link link : configuration.getLinks()) {
                    if ("web url".equals(link.getRel()) || "shell url".equals(link.getRel())) {
                        // Hide web and shell links if application is not running.
                        continue;
                    }
                    additionalLinks.add(dtoFactory.clone(link));
                }
                break;
        }

        links.addAll(additionalLinks);
        return dtoFactory.createDto(ApplicationProcessDescriptor.class)
                         .withProcessId(process.getId())
                         .withStatus(status)
                         .withStartTime(process.getStartTime())
                         .withStopTime(process.getStopTime())
                         .withLinks(links)
                         .withWorkspace(request.getWorkspace())
                         .withProject(request.getProject())
                         .withUserId(request.getUserId())
                         .withDebugHost(configuration.getDebugHost())
                         .withDebugPort(configuration.getDebugPort())
                         .withPortMapping(portMapping);
    }

    @Override
    protected ServiceDescriptor createServiceDescriptor() {
        return DtoFactory.getInstance().createDto(RunnerServerDescriptor.class).withAssignedWorkspace(assignedWorkspace)
                         .withAssignedProject(assignedProject);
    }

    @GenerateLink(rel = Constants.LINK_REL_GET_CURRENT_RECIPE)
    @GET
    @Path("/recipe")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRecipe(@QueryParam("id") String id) throws Exception {
        java.nio.file.Path dockerParentPath = Paths.get(dockerfilesRepository);
        if (dockerfilesRepository == null || !java.nio.file.Files.exists(dockerParentPath) ||
            !java.nio.file.Files.isDirectory(dockerParentPath)) {
            throw new NotFoundException(
                    "The configuration of docker repository wasn't found or some problem with configuration was found.");
        }

        final String path = id.replace(SYSTEM_PREFIX, "") + DOCKER_FILE_NAME;

        java.nio.file.Path dockerFile = Paths.get(dockerParentPath.toAbsolutePath().toString(), path);
        if (!java.nio.file.Files.exists(dockerFile)) {
            throw new NotFoundException("The docker file with given id wasn't found.");
        }

        return Response.ok("{ \"recipe\":\"" +
                           new String(java.nio.file.Files.readAllBytes(dockerFile)) +
                           "\" }",
                           MediaType.APPLICATION_JSON_TYPE)
                       .build();
    }

}