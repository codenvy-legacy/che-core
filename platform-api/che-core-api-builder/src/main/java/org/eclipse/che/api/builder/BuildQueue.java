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

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.eclipse.che.api.builder.dto.BaseBuilderRequest;
import org.eclipse.che.api.builder.dto.BuildOptions;
import org.eclipse.che.api.builder.dto.BuildRequest;
import org.eclipse.che.api.builder.dto.BuilderServerAccessCriteria;
import org.eclipse.che.api.builder.dto.BuilderServerLocation;
import org.eclipse.che.api.builder.dto.BuilderServerRegistration;
import org.eclipse.che.api.builder.dto.BuilderState;
import org.eclipse.che.api.builder.dto.DependencyRequest;
import org.eclipse.che.api.builder.internal.BuilderEvent;
import org.eclipse.che.api.builder.internal.Constants;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.UnauthorizedException;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.core.notification.EventSubscriber;
import org.eclipse.che.api.core.rest.HttpJsonHelper;
import org.eclipse.che.api.core.rest.ServiceContext;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.project.server.ProjectService;
import org.eclipse.che.api.project.shared.dto.BuilderConfiguration;
import org.eclipse.che.api.project.shared.dto.BuildersDescriptor;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.workspace.server.WorkspaceService;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceDescriptor;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.lang.cache.Cache;
import org.eclipse.che.commons.lang.cache.SLRUCache;
import org.eclipse.che.commons.lang.cache.SynchronizedCache;
import org.eclipse.che.commons.lang.concurrent.ThreadLocalPropagateContext;
import org.eclipse.che.commons.user.User;
import org.eclipse.che.dto.server.DtoFactory;
import org.everrest.core.impl.provider.json.JsonUtils;
import org.everrest.websockets.WSConnectionContext;
import org.everrest.websockets.message.ChannelBroadcastMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.MoreObjects.firstNonNull;

//import org.eclipse.che.commons.lang.CollectionUtils;

/**
 * Accepts all build request and redirects them to the slave-builders. If there is no any available slave-builder at the moment it stores
 * build request and tries send request again. Requests don't stay in this queue forever. Max time (in minutes) for request to be in the
 * queue set up by configuration parameter {@link org.eclipse.che.api.builder.internal.Constants#WAITING_TIME}.
 *
 * @author andrew00x
 * @author Eugene Voevodin
 */
@Singleton
public class BuildQueue {
    private static final Logger LOG = LoggerFactory.getLogger(BuildQueue.class);

    private static final long CHECK_AVAILABLE_BUILDER_DELAY = 2000;

    private static final AtomicLong sequence = new AtomicLong(1);

    private final ConcurrentMap<String, RemoteBuilderServer> builderServices;
    private final BuilderSelectionStrategy                   builderSelector;
    private final ConcurrentMap<Long, BuildQueueTask>        tasks;
    private final ConcurrentMap<BuilderListKey, BuilderList> builderListMapping;
    private final String                                     baseWorkspaceApiUrl;
    private final String                                     baseProjectApiUrl;
    private final int                                        maxExecutionTimeMillis;
    private final EventService                               eventService;
    /** Max time for request to be in queue in milliseconds. */
    private final long                                       waitingTimeMillis;
    private final Cache<BaseBuilderRequest, RemoteTask>      successfulBuilds;
    private final AtomicBoolean                              started;
    private final long                                       keepResultTimeMillis;

    private ExecutorService          executor;
    private ScheduledExecutorService scheduler;

    /** Optional pre-configured slave builders. */
    @com.google.inject.Inject(optional = true)
    @Named(Constants.BUILDER_SLAVE_BUILDER_URLS)
    private String[] slaves = new String[0];

    /**
     * @param baseWorkspaceApiUrl
     *         workspace api url. Configuration parameter that points to the Workspace API location. If such parameter isn't specified than
     *         use the same base URL as builder API has, e.g. suppose we have builder API at URL: <i>http://codenvy
     *         .com/api/builder/my_workspace</i>,
     *         in this case base URL is <i>http://codenvy.com/api</i> so we will try to find workspace API at URL:
     *         <i>http://codenvy.com/api/workspace/my_workspace</i>
     * @param baseProjectApiUrl
     *         project api url. Configuration parameter that points to the Project API location. If such parameter isn't specified than use
     *         the same base URL as builder API has, e.g. suppose we have builder API at URL: <i>http://codenvy
     *         .com/api/builder/my_workspace</i>,
     *         in this case base URL is <i>http://codenvy.com/api</i> so we will try to find project API at URL:
     *         <i>http://codenvy.com/api/project/my_workspace</i>
     * @param waitingTime
     *         max time for request to be in queue in seconds. Configuration parameter that sets max time (in seconds) which request may be
     *         in this queue. After this time the results of build may be removed.
     * @param maxExecutionTime
     *         build timeout. Configuration parameter that provides build timeout is seconds. After this time build may be terminated.
     */
    @Inject
    public BuildQueue(@Nullable @Named("workspace.base_api_url") String baseWorkspaceApiUrl,
                      @Nullable @Named("project.base_api_url") String baseProjectApiUrl,
                      @Named(Constants.WAITING_TIME) int waitingTime,
                      @Named(Constants.MAX_EXECUTION_TIME) int maxExecutionTime,
                      @Named(Constants.KEEP_RESULT_TIME) int keepResultTime,
                      BuilderSelectionStrategy builderSelector,
                      EventService eventService) {
        this.baseWorkspaceApiUrl = baseWorkspaceApiUrl;
        this.baseProjectApiUrl = baseProjectApiUrl;
        this.maxExecutionTimeMillis = maxExecutionTime;
        this.eventService = eventService;
        this.waitingTimeMillis = TimeUnit.SECONDS.toMillis(waitingTime);
        this.builderSelector = builderSelector;
        this.keepResultTimeMillis = TimeUnit.SECONDS.toMillis(keepResultTime);

        tasks = new ConcurrentHashMap<>();
        builderListMapping = new ConcurrentHashMap<>();
        successfulBuilds = new SynchronizedCache<>(new SLRUCache<BaseBuilderRequest, RemoteTask>(200, 400));
        builderServices = new ConcurrentHashMap<>();
        started = new AtomicBoolean(false);
    }

    /**
     * Get total size of queue of tasks.
     *
     * @return total size of queue of tasks
     */
    public int getTotalNum() {
        checkStarted();
        return tasks.size();
    }

    /**
     * Get number of tasks which are waiting for processing.
     *
     * @return number of tasks which are waiting for processing
     */
    public int getWaitingNum() {
        checkStarted();
        int count = 0;
        for (BuildQueueTask task : tasks.values()) {
            if (task.isWaiting()) {
                count++;
            }
        }
        return count;
    }

    public List<RemoteBuilderServer> getRegisterBuilderServers() {
        return new ArrayList<>(builderServices.values());
    }

    /**
     * Register remote SlaveBuildService which can process builds.
     *
     * @param registration
     *         BuilderServerRegistration
     * @return {@code true} if set of available Builders changed as result of the call
     * @throws BuilderException
     *         if an error occurs
     */
    public boolean registerBuilderServer(BuilderServerRegistration registration) throws BuilderException {
        checkStarted();
        final String url = registration.getBuilderServerLocation().getUrl();
        final RemoteBuilderServer builderServer = createRemoteBuilderServer(url);
        String workspace = null;
        String project = null;
        final BuilderServerAccessCriteria accessCriteria = registration.getBuilderServerAccessCriteria();
        if (accessCriteria != null) {
            workspace = accessCriteria.getWorkspace();
            project = accessCriteria.getProject();
        }
        if (workspace != null) {
            builderServer.setAssignedWorkspace(workspace);
            if (project != null) {
                builderServer.setAssignedProject(project);
            }
        }
        return doRegisterBuilderServer(builderServer);
    }

    // Switched to default for test.
    // private
    RemoteBuilderServer createRemoteBuilderServer(String url) {
        return new RemoteBuilderServer(url);
    }

    // Switched to default for test.
    // private
    boolean doRegisterBuilderServer(RemoteBuilderServer builderServer) throws BuilderException {
        builderServices.put(builderServer.getBaseUrl(), builderServer);
        final BuilderListKey key = new BuilderListKey(builderServer.getAssignedWorkspace(), builderServer.getAssignedProject());
        BuilderList builderList = builderListMapping.get(key);
        if (builderList == null) {
            final BuilderList newBuilderList = new BuilderList(builderSelector);
            builderList = builderListMapping.putIfAbsent(key, newBuilderList);
            if (builderList == null) {
                builderList = newBuilderList;
            }
        }
        return builderList.addBuilders(builderServer.getRemoteBuilders());
    }

    /**
     * Unregister remote SlaveBuildService.
     *
     * @param location
     *         BuilderServerLocation
     * @return {@code true} if set of available Builders changed as result of the call
     * @throws BuilderException
     *         if an error occurs
     */
    public boolean unregisterBuilderServer(BuilderServerLocation location) throws BuilderException {
        checkStarted();
        final String url = location.getUrl();
        if (url == null) {
            return false;
        }
        final RemoteBuilderServer builderServer = builderServices.remove(url);
        return builderServer != null && doUnregisterBuilders(url);
    }

    // Switched to default for test.
    // private
    boolean doUnregisterBuilders(String url) {
        boolean modified = false;
        for (Iterator<BuilderList> i = builderListMapping.values().iterator(); i.hasNext(); ) {
            final BuilderList builderList = i.next();
            for (RemoteBuilder builder : builderList.getBuilders()) {
                if (url.equals(builder.getBaseUrl())) {
                    modified |= builderList.removeBuilder(builder);
                }
            }
            if (builderList.size() == 0) {
                i.remove();
            }
        }
        return modified;
    }

    /**
     * Schedule new build.
     *
     * @param wsId
     *         id of workspace to which project belongs
     * @param project
     *         name of project
     * @param serviceContext
     *         ServiceContext
     * @return BuildQueueTask
     */
    public BuildQueueTask scheduleBuild(String wsId, String project, ServiceContext serviceContext, BuildOptions buildOptions)
            throws BuilderException {
        checkStarted();
        final WorkspaceDescriptor workspace = getWorkspaceDescriptor(wsId, serviceContext);
        if (workspace.getAttributes().containsKey(org.eclipse.che.api.account.server.Constants.RESOURCES_LOCKED_PROPERTY)) {
            throw new BuilderException("Build action for this workspace is locked");
        }

        final ProjectDescriptor projectDescription = getProjectDescription(wsId, project, serviceContext);
        final User user = EnvironmentContext.getCurrent().getUser();
        final BuildRequest request = (BuildRequest)DtoFactory.getInstance().createDto(BuildRequest.class)
                                                             .withWorkspace(wsId)
                                                             .withProject(project)
                                                             .withUserId(user == null ? "" : user.getId());
        if (buildOptions != null) {
            request.setBuilder(buildOptions.getBuilderName());
            request.setOptions(buildOptions.getOptions());
            request.setTargets(buildOptions.getTargets());
            request.setIncludeDependencies(buildOptions.isIncludeDependencies());
            request.setSkipTest(buildOptions.isSkipTest());
        }
        fillRequestFromProjectDescriptor(projectDescription, request);
        if (!hasBuilder(request)) {
            throw new BuilderException(String.format("Builder '%s' is not available for workspace %s.", request.getBuilder(), wsId));
        }
        final RemoteTask successfulTask = successfulBuilds.get(request);
        Callable<RemoteTask> callable = null;
        boolean reuse = false;
        if (successfulTask != null) {
            try {
                reuse = projectDescription.getModificationDate() < successfulTask.getBuildTaskDescriptor().getEndTime();
            } catch (Exception ignored) {
            }
            if (reuse) {
                LOG.debug("Reuse successful build {}", successfulTask.getId());
                callable = new Callable<RemoteTask>() {
                    @Override
                    public RemoteTask call() throws Exception {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ignored) {
                        }
                        return successfulTask;
                    }
                };
            } else {
                successfulBuilds.remove(request);
            }
        }
        if (callable == null) {
            request.setTimeout(getBuildTimeout(workspace));
            callable = createTaskFor(request);
        }
        final Long id = sequence.getAndIncrement();
        final InternalBuildTask future = new InternalBuildTask(ThreadLocalPropagateContext.wrap(callable), id, wsId, project, reuse);
        request.setId(id);
        final BuildQueueTask task = new BuildQueueTask(id, request, waitingTimeMillis, future, eventService, serviceContext.getServiceUriBuilder());
        tasks.put(id, task);
        eventService.publish(BuilderEvent.queueStartedEvent(id, wsId, project));
        executor.execute(future);
        return task;
    }

    protected Callable<RemoteTask> createTaskFor(final BuildRequest request) {
        return new Callable<RemoteTask>() {
            @Override
            public RemoteTask call() throws BuilderException {
                return getBuilder(request).perform(request);
            }
        };
    }

    /**
     * Schedule new dependencies analyze.
     *
     * @param wsId
     *         id of workspace to which project belongs
     * @param project
     *         name of project
     * @param type
     *         type of analyze dependencies. Depends to implementation of slave-builder.
     * @param serviceContext
     *         ServiceContext
     * @param buildOptions
     * @return BuildQueueTask
     */
    public BuildQueueTask scheduleDependenciesAnalyze(String wsId, String project, String type, ServiceContext serviceContext,
                                                      BuildOptions buildOptions)
            throws BuilderException {
        checkStarted();
        final ProjectDescriptor descriptor = getProjectDescription(wsId, project, serviceContext);
        final User user = EnvironmentContext.getCurrent().getUser();
        final DependencyRequest request = (DependencyRequest)DtoFactory.getInstance().createDto(DependencyRequest.class)
                                                                       .withType(type)
                                                                       .withWorkspace(wsId)
                                                                       .withProject(project)
                                                                       .withUserId(user == null ? "" : user.getName());
        if (buildOptions != null) {
            request.setBuilder(buildOptions.getBuilderName());
            request.setOptions(buildOptions.getOptions());
            request.setTargets(buildOptions.getTargets());
            request.setIncludeDependencies(buildOptions.isIncludeDependencies());
        }
        fillRequestFromProjectDescriptor(descriptor, request);
        if (!hasBuilder(request)) {
            throw new BuilderException(String.format("Builder '%s' is not available for workspace '%s'.", request.getBuilder(), wsId));
        }
        final WorkspaceDescriptor workspace = getWorkspaceDescriptor(wsId, serviceContext);
        request.setTimeout(getBuildTimeout(workspace));
        final Callable<RemoteTask> callable = createTaskFor(request);
        final Long id = sequence.getAndIncrement();
        final InternalBuildTask future = new InternalBuildTask(ThreadLocalPropagateContext.wrap(callable), id, wsId, project, false);
        request.setId(id);
        final BuildQueueTask task = new BuildQueueTask(id, request, waitingTimeMillis, future, eventService, serviceContext.getServiceUriBuilder());
        tasks.put(id, task);
        executor.execute(future);
        return task;
    }

    protected Callable<RemoteTask> createTaskFor(final DependencyRequest request) {
        return new Callable<RemoteTask>() {
            @Override
            public RemoteTask call() throws BuilderException {
                return getBuilder(request).perform(request);
            }
        };
    }

    private void fillRequestFromProjectDescriptor(ProjectDescriptor descriptor, BaseBuilderRequest request) throws BuilderException {
        String builder = request.getBuilder();
        final BuildersDescriptor builders = descriptor.getBuilders();
        if (builder == null) {
            if (builders != null) {
                builder = builders.getDefault();
                //if builder not set in request we will use builder that set in ProjectDescriptor
                request.setBuilder(builder);
                //fill build configuration from ProjectDescriptor for default builder
                fillBuildConfig(request,
                                builder,
                                firstNonNull(builders.getConfigs(), Collections.<String, BuilderConfiguration>emptyMap()));
            }
            if (builder == null) {
                throw new BuilderException("Name of builder is not specified, be sure corresponded property of project is set");
            }

        } else {
            //fill build configuration from ProjectDescriptor for builder from request
            fillBuildConfig(request,
                            builder,
                            firstNonNull(builders.getConfigs(), Collections.<String, BuilderConfiguration>emptyMap()));
        }
        request.setProjectDescriptor(descriptor);
        request.setProjectUrl(descriptor.getBaseUrl());
        final Link zipballLink = descriptor.getLink(org.eclipse.che.api.project.server.Constants.LINK_REL_EXPORT_ZIP);
        if (zipballLink != null) {
            final String zipballLinkHref = zipballLink.getHref();
            final String token = getAuthenticationToken();
            request.setSourcesUrl(token != null ? String.format("%s?token=%s", zipballLinkHref, token) : zipballLinkHref);
        }
    }

    private void fillBuildConfig(BaseBuilderRequest request, String builder, Map<String, BuilderConfiguration> buildersConfigs) {
        //here we going to check is ProjectDescriptor have some setting for giving builder form ProjectDescriptor
        BuilderConfiguration builderConfig = buildersConfigs.get(builder);
        if (builderConfig != null) {
            request.setOptions(firstNonNull(builderConfig.getOptions(), Collections.<String, String>emptyMap()));
            request.setTargets(firstNonNull(builderConfig.getTargets(), Collections.<String>emptyList()));
        }
    }

    private ProjectDescriptor getProjectDescription(String workspace, String project, ServiceContext serviceContext)
            throws BuilderException {
        final UriBuilder baseProjectUriBuilder = baseProjectApiUrl == null || baseProjectApiUrl.isEmpty()
                                                 ? serviceContext.getBaseUriBuilder()
                                                 : UriBuilder.fromUri(baseProjectApiUrl);
        final String projectUrl = baseProjectUriBuilder.path(ProjectService.class)
                                                       .path(ProjectService.class, "getProject")
                                                       .build(workspace, project.startsWith("/") ? project.substring(1) : project)
                                                       .toString();
        try {
            return HttpJsonHelper.get(ProjectDescriptor.class, projectUrl);
        } catch (IOException e) {
            throw new BuilderException(e);
        } catch (ServerException | UnauthorizedException | ForbiddenException | NotFoundException | ConflictException e) {
            throw new BuilderException(e.getServiceError());
        }
    }

    private WorkspaceDescriptor getWorkspaceDescriptor(String workspace, ServiceContext serviceContext) throws BuilderException {
        final UriBuilder baseWorkspaceUriBuilder = baseWorkspaceApiUrl == null || baseWorkspaceApiUrl.isEmpty()
                                                   ? serviceContext.getBaseUriBuilder()
                                                   : UriBuilder.fromUri(baseWorkspaceApiUrl);
        final String workspaceUrl = baseWorkspaceUriBuilder.path(WorkspaceService.class)
                                                           .path(WorkspaceService.class, "getById")
                                                           .build(workspace).toString();
        try {
            return HttpJsonHelper.get(WorkspaceDescriptor.class, workspaceUrl);
        } catch (IOException e) {
            throw new BuilderException(e);
        } catch (ServerException | UnauthorizedException | ForbiddenException | NotFoundException | ConflictException e) {
            throw new BuilderException(e.getServiceError());
        }
    }

    // Switched to default for test.
    // private
    boolean hasBuilder(BaseBuilderRequest request) {
        final BuilderList builderList = getBuilderList(request.getWorkspace(), request.getProject());
        return builderList != null && builderList.hasBuilder(request.getBuilder());
    }

    // Switched to default for test.
    // private
    BuilderList getBuilderList(String workspace, String project) {
        BuilderList builderList = builderListMapping.get(new BuilderListKey(project, workspace));
        if (builderList == null) {
            if (project != null || workspace != null) {
                if (workspace != null) {
                    // have dedicated builders for whole workspace (omit project) ?
                    builderList = builderListMapping.get(new BuilderListKey(null, workspace));
                }
                if (builderList == null) {
                    // seems there is no dedicated builders for specified request, use shared one then
                    builderList = builderListMapping.get(new BuilderListKey(null, null));
                }
            }
        }
        return builderList;
    }

    // Switched to default for test.
    // private
    RemoteBuilder getBuilder(BaseBuilderRequest request) throws BuilderException {
        final BuilderList builderList = getBuilderList(request.getWorkspace(), request.getProject());
        if (builderList == null) {
            // Cannot continue, typically should never happen. At least shared builders should be available for everyone.
            throw new BuilderException("There is no any builder to process this request. ");
        }
        final RemoteBuilder builder = builderList.getBuilder(request);
        if (builder == null) {
            throw new BuilderException("There is no any builder available. ");
        }
        LOG.info("Use builder '{}' at '{}'", builder.getName(), builder.getBaseUrl());
        return builder;
    }

    private long getBuildTimeout(WorkspaceDescriptor workspace) throws BuilderException {
        final String timeoutAttr = workspace.getAttributes().get(Constants.BUILDER_EXECUTION_TIME);
        return timeoutAttr != null ? Integer.parseInt(timeoutAttr) : maxExecutionTimeMillis;
    }

    private String getAuthenticationToken() {
        User user = EnvironmentContext.getCurrent().getUser();
        if (user != null) {
            return user.getToken();
        }
        return null;
    }

    /**
     * Return tasks of this queue.
     */
    public List<BuildQueueTask> getTasks() {
        return new ArrayList<>(tasks.values());
    }

    public BuildQueueTask getTask(Long id) throws NotFoundException {
        final BuildQueueTask task = tasks.get(id);
        if (task == null) {
            throw new NotFoundException(String.format("Not found task %d. It may be canceled by timeout.", id));
        }
        return task;
    }

    @PostConstruct
    public void start() {
        if (started.compareAndSet(false, true)) {
            executor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
                                              new ThreadFactoryBuilder().setNameFormat("BuildQueue-").setDaemon(true).build()) {
                @Override
                protected void afterExecute(Runnable runnable, Throwable error) {
                    super.afterExecute(runnable, error);
                    if (runnable instanceof InternalBuildTask) {
                        final InternalBuildTask internalBuildTask = (InternalBuildTask)runnable;
                        if (internalBuildTask.reused) {
                            // Emulate event from remote builder. In fact we didn't send request to remote builder just reuse result from previous
                            // build.
                            eventService.publish(BuilderEvent.doneEvent(internalBuildTask.id,
                                                                        internalBuildTask.workspace,
                                                                        internalBuildTask.project,
                                                                        true));
                        }
                    }
                }
            };
            scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("BuildQueueScheduler-")
                                                                                             .setDaemon(true).build());
            scheduler.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    int num = 0;
                    int waitingNum = 0;
                    for (Iterator<BuildQueueTask> i = tasks.values().iterator(); i.hasNext(); ) {
                        if (Thread.currentThread().isInterrupted()) {
                            return;
                        }
                        final BuildQueueTask task = i.next();
                        final boolean waiting = task.isWaiting();
                        final BaseBuilderRequest request = task.getRequest();
                        if (waiting) {
                            if ((task.getCreationTime() + waitingTimeMillis) < System.currentTimeMillis()) {
                                try {
                                    task.cancel();
                                    eventService.publish(
                                            BuilderEvent.terminatedEvent(task.getId(), request.getWorkspace(), request.getProject()));
                                } catch (Exception e) {
                                    LOG.warn(e.getMessage(), e);
                                }
                                i.remove();
                                waitingNum++;
                                num++;
                            }
                        } else {
                            RemoteTask remote = null;
                            try {
                                remote = task.getRemoteTask();
                            } catch (Exception e) {
                                LOG.warn(e.getMessage(), e);
                            }
                            if (remote == null) {
                                i.remove();
                                successfulBuilds.remove(DtoFactory.getInstance().clone(request).withId(0L).withTimeout(0L));
                                num++;
                            } else if ((remote.getCreationTime() + keepResultTimeMillis) < System.currentTimeMillis()) {
                                try {
                                    remote.getBuildTaskDescriptor();
                                } catch (NotFoundException e) {
                                    i.remove();
                                    num++;
                                } catch (Exception e) {
                                    LOG.warn(e.getMessage(), e);
                                    i.remove();
                                    num++;
                                }
                            }
                        }
                    }
                    if (num > 0) {
                        LOG.debug("Remove {} expired tasks, {} of them were waiting for processing", num, waitingNum);
                    }
                }
            }, 1, 1, TimeUnit.MINUTES);

            eventService.subscribe(new EventSubscriber<BuilderEvent>() {
                @Override
                public void onEvent(BuilderEvent event) {
                    if (event.getType() == BuilderEvent.EventType.DONE && !event.isReused()) {
                        final long id = event.getTaskId();
                        try {
                            final BuildQueueTask task = getTask(id);
                            final BaseBuilderRequest request = task.getRequest();
                            if (task.getDescriptor().getStatus() == BuildStatus.SUCCESSFUL) {
                                // Clone request and replace its id and timeout with 0.
                                successfulBuilds
                                        .put(DtoFactory.getInstance().clone(request).withId(0L).withTimeout(0L), task.getRemoteTask());
                            }
                        } catch (NotFoundException ignored) {
                        } catch (Exception e) {
                            LOG.warn(String.format("%s: %s", event, e.getMessage()));
                        }
                    }
                }
            });

            eventService.subscribe(new BuildStatusMessenger());

            //Log events for analytics
            eventService.subscribe(new AnalyticsMessenger());

            if (slaves.length > 0) {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        final LinkedList<RemoteBuilderServer> servers = new LinkedList<>();
                        for (String slave : slaves) {
                            try {
                                servers.add(createRemoteBuilderServer(slave));
                            } catch (IllegalArgumentException e) {
                                LOG.error(e.getMessage(), e);
                            }
                        }
                        final LinkedList<RemoteBuilderServer> offline = new LinkedList<>();
                        for (; ; ) {
                            while (!servers.isEmpty()) {
                                if (Thread.currentThread().isInterrupted()) {
                                    return;
                                }
                                final RemoteBuilderServer server = servers.pop();
                                if (server.isAvailable()) {
                                    try {
                                        doRegisterBuilderServer(server);
                                        LOG.debug("Pre-configured slave builder server {} registered. ", server.getBaseUrl());
                                    } catch (BuilderException e) {
                                        LOG.error(e.getMessage(), e);
                                        offline.add(server);
                                    }
                                } else {
                                    LOG.warn("Pre-configured slave builder server {} isn't responding. ", server.getBaseUrl());
                                    offline.add(server);
                                }
                            }
                            if (offline.isEmpty()) {
                                return;
                            } else {
                                servers.addAll(offline);
                                offline.clear();
                                synchronized (this) {
                                    try {
                                        wait(5000);
                                    } catch (InterruptedException e) {
                                        Thread.currentThread().interrupt();
                                        return;
                                    }
                                }
                            }
                        }
                    }
                });
            }
        } else {
            throw new IllegalStateException("Already started");
        }
    }

    protected void checkStarted() {
        if (!started.get()) {
            throw new IllegalStateException("Is not started yet.");
        }
    }

    @PreDestroy
    public void stop() {
        if (started.compareAndSet(true, false)) {
            boolean interrupted = false;
            scheduler.shutdownNow();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    LOG.warn("Unable terminate scheduler");
                }
            } catch (InterruptedException e) {
                interrupted = true;
            }
            executor.shutdown();
            try {
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                    if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                        LOG.warn("Unable terminate main pool");
                    }
                }
            } catch (InterruptedException e) {
                interrupted |= true;
                executor.shutdownNow();
            }
            tasks.clear();
            builderListMapping.clear();
            successfulBuilds.clear();
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        } else {
            throw new IllegalStateException("Is not started yet.");
        }
    }

    protected EventService getEventService() {
        return eventService;
    }

    private static class InternalBuildTask extends FutureTask<RemoteTask> {
        final Long    id;
        final String  workspace;
        final String  project;
        final boolean reused;

        InternalBuildTask(Callable<RemoteTask> callable, Long id, String workspace, String project, boolean reused) {
            super(callable);
            this.id = id;
            this.workspace = workspace;
            this.project = project;
            this.reused = reused;
        }
    }

    private static class BuilderListKey {
        final String project;
        final String workspace;

        BuilderListKey(String project, String workspace) {
            this.project = project;
            this.workspace = workspace;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof BuilderListKey)) {
                return false;
            }
            BuilderListKey other = (BuilderListKey)o;
            return (workspace == null ? other.workspace == null : workspace.equals(other.workspace))
                   && (project == null ? other.project == null : project.equals(other.project));

        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = hash * 31 + (workspace == null ? 0 : workspace.hashCode());
            hash = hash * 31 + (project == null ? 0 : project.hashCode());
            return hash;
        }

        @Override
        public String toString() {
            return "ProjectWithWorkspace{" +
                   "workspace='" + workspace + '\'' +
                   ", project='" + project + '\'' +
                   '}';
        }
    }


    private static class BuilderList {
        final Collection<RemoteBuilder> builders;
        final BuilderSelectionStrategy  builderSelector;

        BuilderList(BuilderSelectionStrategy builderSelector) {
            this.builderSelector = builderSelector;
            builders = new LinkedHashSet<>();
        }

        synchronized List<RemoteBuilder> getBuilders() {
            return new ArrayList<>(builders);
        }

        synchronized boolean hasBuilder(String name) {
            for (RemoteBuilder builder : builders) {
                if (name.equals(builder.getName())) {
                    return true;
                }
            }
            return false;
        }

        synchronized boolean addBuilders(Collection<? extends RemoteBuilder> list) {
            return builders.addAll(list);
        }

        synchronized boolean removeBuilders(Collection<? extends RemoteBuilder> list) {
            return builders.removeAll(list);
        }

        synchronized boolean removeBuilder(RemoteBuilder builder) {
            return builders.remove(builder);
        }

        synchronized int size() {
            return builders.size();
        }

        synchronized RemoteBuilder getBuilder(BaseBuilderRequest request) {
            final List<RemoteBuilder> matched = new ArrayList<>();
            for (RemoteBuilder builder : builders) {
                if (request.getBuilder().equals(builder.getName())) {
                    matched.add(builder);
                }
            }
            final int size = matched.size();
            if (size == 0) {
                return null;
            }
            final List<RemoteBuilder> available = new ArrayList<>(matched.size());
            for (; ; ) {
                for (RemoteBuilder builder : matched) {
                    if (Thread.currentThread().isInterrupted()) {
                        return null; // stop immediately
                    }
                    BuilderState builderState;
                    try {
                        builderState = builder.getBuilderState();
                    } catch (Exception e) {
                        LOG.error(e.getMessage(), e);
                        continue;
                    }
                    if (builderState.getFreeWorkers() > 0) {
                        available.add(builder);
                    }
                }

                if (available.isEmpty()) {
                    try {
                        wait(CHECK_AVAILABLE_BUILDER_DELAY); // wait and try again
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return null; // expected to get here if task is canceled
                    }
                } else {
                    if (available.size() > 1) {
                        return builderSelector.select(available);
                    }
                    return available.get(0);
                }
            }
        }
    }

    private class AnalyticsMessenger implements EventSubscriber<BuilderEvent> {
        @Override
        public void onEvent(BuilderEvent event) {
            if (event.getType() == BuilderEvent.EventType.BEGIN
                || event.getType() == BuilderEvent.EventType.DONE
                || event.getType() == BuilderEvent.EventType.BUILD_TASK_ADDED_IN_QUEUE
                || event.getType() == BuilderEvent.EventType.BUILD_TASK_QUEUE_TIME_EXCEEDED) {
                try {
                    final long taskId = event.getTaskId();
                    final BaseBuilderRequest request = getTask(taskId).getRequest();
                    if (request instanceof BuildRequest) {
                        BuildQueueTask task = getTask(taskId);
                        final String analyticsID = task.getCreationTime() + "-" + taskId;
                        final String project = extractProjectName(event.getProject());
                        final String workspace = request.getWorkspace();
                        final long time = System.currentTimeMillis();
                        final long waitingTime = time - task.getCreationTime();
                        final long timeout;
                        if (request.getTimeout() == Integer.MAX_VALUE) {
                            timeout = -1;
                        } else {
                            timeout = request.getTimeout() * 1000; // to ms
                        }
                        final String projectTypeId = request.getProjectDescriptor().getType();
                        final String user = request.getUserId();

                        switch (event.getType()) {
                            case BEGIN:
                                LOG.info(
                                        "EVENT#build-queue-waiting-finished# TIME#{}# WS#{}# USER#{}# PROJECT#{}# TYPE#{}# ID#{}# WAITING-TIME#{}#",
                                        time,
                                        workspace,
                                        user,
                                        project,
                                        projectTypeId,
                                        analyticsID,
                                        waitingTime);
                                LOG.info("EVENT#build-started# TIME#{}# WS#{}# USER#{}# PROJECT#{}# TYPE#{}# ID#{}# TIMEOUT#{}#",
                                         time,
                                         workspace,
                                         user,
                                         project,
                                         projectTypeId,
                                         analyticsID,
                                         timeout);
                                break;
                            case DONE:
                                if (event.isReused()) {
                                    LOG.info(
                                            "EVENT#build-queue-waiting-finished# TIME#{}# WS#{}# USER#{}# PROJECT#{}# TYPE#{}# ID#{}# " +
                                            "WAITING-TIME#{}#",
                                            time,
                                            workspace,
                                            user,
                                            project,
                                            projectTypeId,
                                            analyticsID,
                                            0);
                                } else {
                                    LOG.info(
                                            "EVENT#build-finished# TIME#{}# WS#{}# USER#{}# PROJECT#{}# TYPE#{}# ID#{}# TIMEOUT#{}#",
                                            time,
                                            workspace,
                                            user,
                                            project,
                                            projectTypeId,
                                            analyticsID,
                                            timeout);
                                }
                                break;
                            case BUILD_TASK_ADDED_IN_QUEUE:
                                LOG.info("EVENT#build-queue-waiting-started# TIME#{}# WS#{}# USER#{}# PROJECT#{}# TYPE#{}# ID#{}#",
                                         time,
                                         workspace,
                                         user,
                                         project,
                                         projectTypeId,
                                         analyticsID);
                                break;
                            case BUILD_TASK_QUEUE_TIME_EXCEEDED:
                                LOG.info(
                                        "EVENT#build-queue-terminated# TIME#{}# WS#{}# USER#{}# PROJECT#{}# TYPE#{}# ID#{}# WAITING-TIME#{}",
                                        time,
                                        workspace,
                                        user,
                                        project,
                                        projectTypeId,
                                        analyticsID,
                                        waitingTime);
                                break;
                        }
                    }
                } catch (Exception e) {
                    LOG.warn(e.getMessage(), e);
                }
            }
        }

        private String extractProjectName(String path) {
            int beginIndex = path.startsWith("/") ? 1 : 0;
            int i = path.indexOf("/", beginIndex);
            int endIndex = i < 0 ? path.length() : i;
            return path.substring(beginIndex, endIndex);
        }
    }

    private class BuildStatusMessenger implements EventSubscriber<BuilderEvent> {
        @Override
        public void onEvent(BuilderEvent event) {
            try {
                final ChannelBroadcastMessage bm = new ChannelBroadcastMessage();
                final long id = event.getTaskId();
                switch (event.getType()) {
                    case BEGIN:
                    case DONE:
                        bm.setChannel(String.format("builder:status:%d", id));
                        try {
                            bm.setBody(DtoFactory.getInstance().toJson(getTask(id).getDescriptor()));
                        } catch (BuilderException re) {
                            bm.setType(ChannelBroadcastMessage.Type.ERROR);
                            bm.setBody(String.format("{\"message\":%s}", JsonUtils.getJsonString(re.getMessage())));
                        }
                        break;
                    case MESSAGE_LOGGED:
                        final BuilderEvent.LoggedMessage message = event.getMessage();
                        if (message != null) {
                            bm.setChannel(String.format("builder:output:%d", id));
                            bm.setBody(String.format("{\"num\":%d, \"line\":%s}",
                                                     message.getLineNum(), JsonUtils.getJsonString(message.getMessage())));
                        }
                        break;
                }
                WSConnectionContext.sendMessage(bm);
            } catch (Exception e) {
                LOG.warn(e.getMessage(), e);
            }
        }
    }
}
