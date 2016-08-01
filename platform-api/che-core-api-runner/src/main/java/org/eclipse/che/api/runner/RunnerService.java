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

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

import org.eclipse.che.api.account.server.dao.AccountDao;
import org.eclipse.che.api.account.server.dao.Member;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.rest.HttpJsonHelper;
import org.eclipse.che.api.core.rest.HttpJsonRequestFactory;
import org.eclipse.che.api.core.rest.HttpServletProxyResponse;
import org.eclipse.che.api.core.rest.Service;
import org.eclipse.che.api.core.rest.annotations.Description;
import org.eclipse.che.api.core.rest.annotations.GenerateLink;
import org.eclipse.che.api.core.rest.annotations.Required;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.project.shared.EnvironmentId;
import org.eclipse.che.api.project.shared.dto.RunnerEnvironment;
import org.eclipse.che.api.project.shared.dto.RunnerEnvironmentLeaf;
import org.eclipse.che.api.project.shared.dto.RunnerEnvironmentTree;
import org.eclipse.che.api.runner.dto.ApplicationProcessDescriptor;
import org.eclipse.che.api.runner.dto.ResourcesDescriptor;
import org.eclipse.che.api.runner.dto.RunOptions;
import org.eclipse.che.api.runner.dto.RunnerDescriptor;
import org.eclipse.che.api.runner.internal.Constants;
import org.eclipse.che.api.workspace.server.dao.WorkspaceDao;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.lang.Pair;
import org.eclipse.che.commons.user.User;
import org.eclipse.che.dto.server.DtoFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * RESTful API for RunQueue.
 *
 * @author andrew00x
 */
@Api(value = "/runner",
     description = "Runner manager")
@Path("/runner")
@Description("Runner REST API")
public class RunnerService extends Service {
    private static final Logger LOG   = LoggerFactory.getLogger(RunnerService.class);
    private static final String START = "{ \"recipe\":\"";
    private static final String END   = "\" }";

    private final RunQueue     runQueue;
    private final WorkspaceDao workspaceDao;
    private final AccountDao   accountDao;
    private final HttpJsonRequestFactory requestFactory;

    @Inject
    public RunnerService(RunQueue runQueue,
                         WorkspaceDao workspaceDao,
                         HttpJsonRequestFactory requestFactory,
                         AccountDao accountDao) {
        this.runQueue = runQueue;
        this.workspaceDao = workspaceDao;
        this.accountDao = accountDao;
        this.requestFactory = requestFactory;
    }


    @ApiOperation(value = "Run project",
                  notes = "Run selected project",
                  response = ApplicationProcessDescriptor.class,
                  position = 1)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GenerateLink(rel = Constants.LINK_REL_RUN)
    @Path("/{ws-id}/run")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ApplicationProcessDescriptor run(@ApiParam(value = "Workspace ID", required = true)
                                            @PathParam("ws-id") String workspace,
                                            @ApiParam(value = "Project name", required = true)
                                            @Required @Description("project name") @QueryParam("project") String project,
                                            @ApiParam(value = "Run options")
                                            @Description("run options") RunOptions options) throws Exception {
        if (project != null && !project.startsWith("/")) {
            project = '/' + project;
        }
        return runQueue.run(workspace, project, getServiceContext(), options).getDescriptor();
    }

    @ApiOperation(value = "Get run status",
                  notes = "Get status of a selected run process",
                  response = ApplicationProcessDescriptor.class,
                  position = 2)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/{ws-id}/status/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ApplicationProcessDescriptor getStatus(@ApiParam(value = "Workspace ID", required = true)
                                                  @PathParam("ws-id") String workspace,
                                                  @ApiParam(value = "Run ID", required = true)
                                                  @PathParam("id") Long id) throws Exception {
        return runQueue.getTask(id).getDescriptor();
    }


    @ApiOperation(value = "Get all user running processes",
                  notes = "Get info on all running processes in the scope of account id and project name",
                  response = ApplicationProcessDescriptor.class,
                  responseContainer = "List",
                  position = 3)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/processes")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ApplicationProcessDescriptor> getAllRunningProcesses(@ApiParam(value = "Project name")
                                                                     @Description("project name")
                                                                     @QueryParam("project") String project,
                                                                     @ApiParam(value = "Account Id")
                                                                     @Description("account id")
                                                                     @QueryParam("account") String accountId)
            throws ForbiddenException, ServerException {
        return getRunningProcesses(null, project, accountId);
    }

    @ApiOperation(value = "Get run processes for a given workspace",
                  notes = "Get info on all running processes for a given workspace",
                  response = ApplicationProcessDescriptor.class,
                  responseContainer = "List",
                  position = 4)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/{ws-id}/processes")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ApplicationProcessDescriptor> getWorkspaceRunningProcesses(@ApiParam(value = "Workspace ID", required = true)
                                                                           @PathParam("ws-id") String workspace,
                                                                           @ApiParam(value = "Project name")
                                                                           @Description("project name")
                                                                           @QueryParam("project") String project)
            throws ForbiddenException, ServerException {
        return getRunningProcesses(workspace, project, null);
    }

    protected List<ApplicationProcessDescriptor> getRunningProcesses(String workspaceId, String project, String accountId)
            throws ForbiddenException, ServerException {
        //fix project path
        if (project != null && !project.startsWith("/")) {
            project = '/' + project;
        }
        final List<ApplicationProcessDescriptor> processes = new LinkedList<>();
        final User user = EnvironmentContext.getCurrent().getUser();
        if (user == null) {
            return processes;
        }

        final String userId = user.getId();

        if (accountId != null) {
            Optional<Member> owner = accountDao.getMembers(accountId)
                                               .stream()
                                               .filter(member -> userId.equals(member.getUserId()) &&
                                                                 member.getRoles().contains("account/owner"))
                                               .findAny();
            if (!owner.isPresent()) {
                throw new ForbiddenException("You are not an owner of the specified account");
            }
        }

        for (RunQueueTask task : runQueue.getTasks()) {
            // skip task if it does not run in given workspace
            if (workspaceId != null && !task.getRequest().getWorkspace().equals(workspaceId)) {
                continue;
            }

            // skip task if it does not run in given project
            if (project != null && !task.getRequest().getProject().equals(project)) {
                continue;
            }

            // skip task if it is not running by given account or by given user if account is not specified
            if (!isRelatedToAccountOrUser(task, accountId, userId)) {
                continue;
            }

            try {
                processes.add(task.getDescriptor());
            } catch (NotFoundException ignored) {
                // NotFoundException is possible and should not be treated as error in this case. Typically it occurs if slave
                // runner already cleaned up the task by its internal cleaner but RunQueue doesn't re-check yet slave runner and
                // doesn't have actual info about state of slave runner.
            } catch (RunnerException e) {
                // Decide ignore such error to be able show maximum available info.
                LOG.error(e.getMessage(), e);
            }
        }
        return processes;
    }

    private boolean isRelatedToAccountOrUser(RunQueueTask task, String accountId, String userId) throws ServerException {
        if (accountId != null) {
            try {
                return accountId.equals(workspaceDao.getById(task.getRequest().getWorkspace()).getAccountId());
            } catch (NotFoundException e) {
                //Skip task because workspace with id from request is not found
            }
            return false;
        }

        return userId.equals(task.getRequest().getUserId());
    }

    @ApiOperation(value = "Stop run process",
                  notes = "Stop running process",
                  response = ApplicationProcessDescriptor.class,
                  position = 5)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @POST
    @Path("/{ws-id}/stop/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ApplicationProcessDescriptor stop(@ApiParam(value = "Workspace ID", required = true)
                                             @PathParam("ws-id") String workspace,
                                             @ApiParam(value = "Run ID", required = true)
                                             @PathParam("id") Long id) throws Exception {
        final RunQueueTask task = runQueue.getTask(id);
        task.stop();
        return task.getDescriptor();
    }

    @ApiOperation(value = "Get logs",
                  notes = "Get logs from a running application",
                  position = 6)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/{ws-id}/logs/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    public void getLogs(@ApiParam(value = "Workspace ID", required = true)
                        @PathParam("ws-id") String workspace,
                        @ApiParam(value = "Run ID", required = true)
                        @PathParam("id") Long id, @Context HttpServletResponse httpServletResponse) throws Exception {
        // Response is written directly to the servlet request stream
        runQueue.getTask(id).readLogs(new HttpServletProxyResponse(httpServletResponse));
    }

    @ApiOperation(value = "Get available RAM resources",
                  notes = "Get RAM resources of a workspace: used and free RAM",
                  response = ResourcesDescriptor.class,
                  position = 7)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/{ws-id}/resources")
    @Produces(MediaType.APPLICATION_JSON)
    public ResourcesDescriptor getResources(@ApiParam(value = "Workspace ID", required = true)
                                            @PathParam("ws-id") String workspace) throws Exception {
        return DtoFactory.getInstance().createDto(ResourcesDescriptor.class)
                         .withTotalMemory(String.valueOf(runQueue.getTotalMemory(workspace, getServiceContext())))
                         .withUsedMemory(String.valueOf(runQueue.getUsedMemory(workspace)));
    }

    @ApiOperation(value = "Get available runner environments",
                  notes = "Get available runner environments",
                  response = RunnerEnvironmentTree.class,
                  position = 8)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GenerateLink(rel = Constants.LINK_REL_AVAILABLE_RUNNERS)
    @GET
    @Path("/{ws-id}/available")
    @Produces(MediaType.APPLICATION_JSON)
    public RunnerEnvironmentTree getRunnerEnvironments(@ApiParam(value = "Workspace ID", required = true)
                                                       @PathParam("ws-id") String workspace,
                                                       @ApiParam(value = "Project name")
                                                       @Description("project name") @QueryParam("project") String project) {
        // Here merge environments from all know runner servers and represent them as tree.
        final DtoFactory dtoFactory = DtoFactory.getInstance();
        final RunnerEnvironmentTree root = dtoFactory.createDto(RunnerEnvironmentTree.class).withDisplayName("system");
        final List<RemoteRunnerServer> registerRunnerServers = runQueue.getRegisterRunnerServers();
        for (Iterator<RemoteRunnerServer> itr = registerRunnerServers.iterator(); itr.hasNext(); ) {
            final RemoteRunnerServer runnerServer = itr.next();
            if (!runnerServer.isAvailable()) {
                LOG.error("Runner server {} becomes unavailable", runnerServer.getBaseUrl());
                itr.remove();
            }
        }
        for (RemoteRunnerServer runnerServer : registerRunnerServers) {
            final String assignedWorkspace;
            final String assignedProject;
            try {
                assignedWorkspace = runnerServer.getAssignedWorkspace();
                assignedProject = runnerServer.getAssignedProject();
            } catch (RunnerException e) {
                LOG.error(e.getMessage(), e);
                continue;
            }
            if (((assignedWorkspace != null && assignedWorkspace.equals(workspace)) || assignedWorkspace == null)
                && ((assignedProject != null && assignedProject.equals(project)) || assignedProject == null)) {

                final List<RunnerDescriptor> runners;
                try {
                    runners = runnerServer.getRunnerDescriptors();
                } catch (RunnerException e) {
                    LOG.error(e.getMessage(), e);
                    continue;
                }
                for (RunnerDescriptor runnerDescriptor : runners) {
                    for (RunnerEnvironment runnerEnvironment : runnerDescriptor.getEnvironments()) {
                        RunnerEnvironmentTree node = root;
                        for (String s : runnerDescriptor.getName().split("/")) {
                            RunnerEnvironmentTree child = node.getNode(s);
                            if (child == null) {
                                child = dtoFactory.createDto(RunnerEnvironmentTree.class).withDisplayName(s);
                                node.addNode(child);
                            }
                            node = child;
                        }
                        // Clone environment and use its id as display name and replace its id with new one in format scope:/runner/environment.
                        // This is global id that shown scope of this environment, e.g. system , project, etc.
                        final String unique =
                                new EnvironmentId(EnvironmentId.Scope.system, runnerDescriptor.getName(), runnerEnvironment.getId())
                                        .toString();
                        final String envId = runnerEnvironment.getId();
                        final RunnerEnvironmentLeaf environment = node.getEnvironment(envId);
                        if (environment == null) {
                            node.addLeaf(dtoFactory.createDto(RunnerEnvironmentLeaf.class)
                                                   .withDisplayName(envId)
                                                   .withEnvironment(dtoFactory.clone(runnerEnvironment).withId(unique)));
                        }
                    }
                }
            }
        }
        return root;
    }


    @ApiOperation(value = "Get runtime recipe",
                  notes = "Get content of a Dockerfile used to 'cook' runtime environment",
                  position = 9)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/{ws-id}/recipe/{id}")
    public void getRecipeFile(@ApiParam(value = "Workspace ID", required = true)
                              @PathParam("ws-id") String workspace,
                              @ApiParam(value = "Run ID", required = true)
                              @PathParam("id") Long id,
                              @Context HttpServletResponse httpServletResponse) throws Exception {
        // Response write directly to the servlet request stream
        runQueue.getTask(id).readRecipeFile(new HttpServletProxyResponse(httpServletResponse));
    }

    @ApiOperation(value = "Get recipe",
                  notes = "Get content of a Dockerfile",
                  position = 10)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Not found"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GenerateLink(rel = Constants.LINK_REL_GET_RECIPE)
    @GET
    @Path("/{ws-id}/recipe")
    @Produces(MediaType.TEXT_PLAIN)
    public String getRecipe(@QueryParam("id") String id) throws Exception {
        List<RemoteRunnerServer> servers = runQueue.getRegisterRunnerServers();
        if (servers.isEmpty()) {
            throw new NotFoundException("Docker configuration wasn't found.");
        }

        RemoteRunnerServer server = servers.get(0);
        Link link = server.getLink(Constants.LINK_REL_GET_CURRENT_RECIPE);
        if (link == null) {
            throw new NotFoundException("Get recipe link wasn't found.");
        }

        // TODO needs to improve this code
        String json = requestFactory.fromUrl(link.getHref()).addQueryParam("id", id).request().asString();
        json = json.substring(START.length());
        json = json.substring(0, json.length() - END.length());

        return json;
    }

}
