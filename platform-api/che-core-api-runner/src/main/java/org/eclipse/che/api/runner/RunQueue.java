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
package org.eclipse.che.api.runner;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.eclipse.che.api.builder.BuildStatus;
import org.eclipse.che.api.builder.BuilderService;
import org.eclipse.che.api.builder.dto.BuildOptions;
import org.eclipse.che.api.builder.dto.BuildTaskDescriptor;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.UnauthorizedException;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.core.notification.EventSubscriber;
import org.eclipse.che.api.core.rest.HttpJsonHelper;
import org.eclipse.che.api.core.rest.RemoteServiceDescriptor;
import org.eclipse.che.api.core.rest.ServiceContext;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.core.util.ValueHolder;
import org.eclipse.che.api.project.server.ProjectService;
import org.eclipse.che.api.project.shared.EnvironmentId;
import org.eclipse.che.api.project.shared.dto.BuildersDescriptor;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.project.shared.dto.RunnerConfiguration;
import org.eclipse.che.api.project.shared.dto.RunnersDescriptor;
import org.eclipse.che.api.runner.dto.ApplicationProcessDescriptor;
import org.eclipse.che.api.runner.dto.ResourcesDescriptor;
import org.eclipse.che.api.runner.dto.RunOptions;
import org.eclipse.che.api.runner.dto.RunRequest;
import org.eclipse.che.api.runner.dto.RunnerMetric;
import org.eclipse.che.api.runner.dto.RunnerServerAccessCriteria;
import org.eclipse.che.api.runner.dto.RunnerServerLocation;
import org.eclipse.che.api.runner.dto.RunnerServerRegistration;
import org.eclipse.che.api.runner.dto.RunnerState;
import org.eclipse.che.api.runner.internal.Constants;
import org.eclipse.che.api.runner.internal.RunnerEvent;
import org.eclipse.che.api.workspace.server.WorkspaceService;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceDescriptor;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.lang.Pair;
import org.eclipse.che.commons.lang.Size;
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
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author andrew00x
 * @author Eugene Voevodin
 */
@Singleton
public class RunQueue {
    private static final Logger LOG = LoggerFactory.getLogger(RunQueue.class);

    /** Pause in milliseconds for checking the result of build process. */
    private static final long CHECK_BUILD_RESULT_PERIOD     = 2000;
    private static final long CHECK_AVAILABLE_RUNNER_PERIOD = 2000;

    private static final long PROCESS_CLEANER_PERIOD = TimeUnit.MINUTES.toMillis(1);

    private static final int DEFAULT_MAX_MEMORY_SIZE = 1000;

    private static final int APPLICATION_CHECK_URL_TIMEOUT = 2000;
    private static final int APPLICATION_CHECK_URL_COUNT   = 30;

    private static final AtomicLong sequence = new AtomicLong(1);

    private final ConcurrentMap<String, RemoteRunnerServer>       runnerServers;
    private final RunnerSelectionStrategy                         runnerSelector;
    private final ConcurrentMap<RunnerListKey, Set<RemoteRunner>> runnerListMapping;
    private final ConcurrentMap<Long, RunQueueTask>               tasks;
    private final int                                             defMemSize;
    private final EventService                                    eventService;
    private final String                                          baseWorkspaceApiUrl;
    private final String                                          baseProjectApiUrl;
    private final String                                          baseBuilderApiUrl;
    private final int                                             defLifetime;
    private final long                                            maxWaitingTimeMillis;
    private final AtomicBoolean                                   started;
    private final long                                            appCleanupTime;
    // Helps to reduce lock contentions when check available resources.
    private final Lock[]                                          resourceCheckerLocks;
    private final int                                             resourceCheckerMask;

    private ExecutorService          executor;
    private ScheduledExecutorService cleanScheduler;

    /** Optional pre-configured slave runners. */
    @com.google.inject.Inject(optional = true)
    @Named(Constants.RUNNER_SLAVE_RUNNER_URLS)
    private String[] slaves = new String[0];

    /** Optional pre-configured slave runners for 'paid' infra. */
    @com.google.inject.Inject(optional = true)
    @Named(Constants.RUNNER_SLAVE_RUNNER_URLS_PAID)
    private String[] slavesPaid = new String[0];

    /** Optional pre-configured slave runners for 'always_on' infra. */
    @com.google.inject.Inject(optional = true)
    @Named(Constants.RUNNER_SLAVE_RUNNER_URLS_ALWAYS_ON)
    private String[] slavesAlwaysOn = new String[0];

    @com.google.inject.Inject(optional = true)
    @Named(Constants.RUNNER_WS_MAX_MEMORY_SIZE)
    private int defMaxMemorySize = DEFAULT_MAX_MEMORY_SIZE;

    // Switched to default for test.
    // private
    long cleanerPeriod              = PROCESS_CLEANER_PERIOD;
    // Switched to default for test.
    // private
    long checkAvailableRunnerPeriod = CHECK_AVAILABLE_RUNNER_PERIOD;
    // Switched to default for test.
    // private
    long checkBuildResultPeriod     = CHECK_BUILD_RESULT_PERIOD;

    /**
     * @param baseWorkspaceApiUrl
     *         workspace api url. Configuration parameter that points to the Workspace API location. If such parameter isn't specified than
     *         use the same base URL as runner API has, e.g. suppose we have runner API at URL: <i>http://codenvy
     *         .com/api/runner/my_workspace</i>,
     *         in this case base URL is <i>http://codenvy.com/api</i> so we will try to find workspace API at URL:
     *         <i>http://codenvy.com/api/workspace/my_workspace</i>
     * @param baseProjectApiUrl
     *         project api url. Configuration parameter that points to the Project API location. If such parameter isn't specified than use
     *         the same base URL as runner API has, e.g. suppose we have runner API at URL: <i>http://codenvy
     *         .com/api/runner/my_workspace</i>,
     *         in this case base URL is <i>http://codenvy.com/api</i> so we will try to find project API at URL:
     *         <i>http://codenvy.com/api/project/my_workspace</i>
     * @param baseBuilderApiUrl
     *         builder api url. Configuration parameter that points to the base Builder API location. If such parameter isn't specified
     *         than use the same base URL as runner API has, e.g. suppose we have runner API at URL:
     *         <i>http://codenvy.com/api/runner/my_workspace</i>, in this case base URL is <i>http://codenvy.com/api</i> so we will try to
     *         find builder API at URL: <i>http://codenvy.com/api/builder/my_workspace</i>.
     * @param defMemSize
     *         default size of memory for application in megabytes. This value used is there is nothing specified in properties of project.
     * @param maxWaitingTime
     *         max time for request to be in queue in seconds
     * @param defLifetime
     *         default application life time in seconds. After this time the application may be terminated.
     */
    @Inject
    @SuppressWarnings("unchecked")
    public RunQueue(@Nullable @Named("workspace.base_api_url") String baseWorkspaceApiUrl,
                    @Nullable @Named("project.base_api_url") String baseProjectApiUrl,
                    @Nullable @Named("builder.base_api_url") String baseBuilderApiUrl,
                    @Named(Constants.APP_DEFAULT_MEM_SIZE) int defMemSize,
                    @Named(Constants.WAITING_TIME) int maxWaitingTime,
                    @Named(Constants.APP_LIFETIME) int defLifetime,
                    @Named(Constants.APP_CLEANUP_TIME) int appCleanupTime,
                    RunnerSelectionStrategy runnerSelector,
                    EventService eventService) {
        this.baseWorkspaceApiUrl = baseWorkspaceApiUrl;
        this.baseProjectApiUrl = baseProjectApiUrl;
        this.baseBuilderApiUrl = baseBuilderApiUrl;
        this.defMemSize = defMemSize;
        this.eventService = eventService;
        this.maxWaitingTimeMillis = TimeUnit.SECONDS.toMillis(maxWaitingTime);
        this.defLifetime = defLifetime;
        this.runnerSelector = runnerSelector;
        this.appCleanupTime = TimeUnit.SECONDS.toMillis(appCleanupTime);

        runnerServers = new ConcurrentHashMap<>();
        tasks = new ConcurrentHashMap<>();
        runnerListMapping = new ConcurrentHashMap<>();
        started = new AtomicBoolean(false);
        final int partitions = 1 << 4;
        resourceCheckerMask = partitions - 1;
        resourceCheckerLocks = new Lock[partitions];
        for (int i = 0; i < partitions; i++) {
            resourceCheckerLocks[i] = new ReentrantLock();
        }
    }

    public RunQueueTask getTask(Long id) throws NotFoundException {
        checkStarted();
        final RunQueueTask task = tasks.get(id);
        if (task == null) {
            throw new NotFoundException(String.format("Not found task %d. It may be canceled by timeout.", id));
        }
        return task;
    }

    public List<? extends RunQueueTask> getTasks() {
        return new ArrayList<>(tasks.values());
    }

    @PostConstruct
    public void start() {
        if (started.compareAndSet(false, true)) {
            executor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
                                              new ThreadFactoryBuilder().setNameFormat("RunQueue-").setDaemon(true).build()) {
                @Override
                protected void afterExecute(Runnable runnable, Throwable error) {
                    boolean isInterrupted = Thread.interrupted();
                    try {
                        super.afterExecute(runnable, error);
                        if (runnable instanceof InternalRunTask) {
                            final InternalRunTask internalRunTask = (InternalRunTask)runnable;
                            if (error == null) {
                                try {
                                    internalRunTask.get();
                                } catch (CancellationException e) {
                                    LOG.warn("Task {}, workspace '{}', project '{}' was cancelled",
                                             internalRunTask.id, internalRunTask.workspace, internalRunTask.project);
                                    error = e;
                                } catch (ExecutionException e) {
                                    error = e.getCause();
                                    logError(internalRunTask, error == null ? e : error);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            } else {
                                logError(internalRunTask, error);
                            }
                            if (error != null) {
                                eventService.publish(RunnerEvent.errorEvent(internalRunTask.id, internalRunTask.workspace,
                                                                            internalRunTask.project, error.getMessage()));
                            }
                        }
                    } finally {
                        if (isInterrupted) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }

                private void logError(InternalRunTask runTask, Throwable t) {
                    String errorMessage = t.getMessage();
                    if (errorMessage != null) {
                        LOG.warn("Execution error, task {}, workspace '{}', project '{}', message '{}'",
                                 runTask.id, runTask.workspace, runTask.project, errorMessage);
                    } else {
                        LOG.warn(String.format("Execution error, task %d, workspace '%s', project '%s', message '%s'",
                                               runTask.id, runTask.workspace, runTask.project, ""), t);
                    }
                }
            };
            cleanScheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("RunQueueScheduler-")
                                                                                                  .setDaemon(true).build());
            cleanScheduler.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    int num = 0;
                    int waitingNum = 0;
                    for (Iterator<RunQueueTask> i = tasks.values().iterator(); i.hasNext(); ) {
                        if (Thread.currentThread().isInterrupted()) {
                            return;
                        }
                        final RunQueueTask task = i.next();
                        final boolean waiting = task.isWaiting();
                        final RunRequest request = task.getRequest();
                        if (waiting) {
                            try {
                                if ((task.getCreationTime() + maxWaitingTimeMillis) < System.currentTimeMillis() || task.isStopped()) {
                                    try {
                                        task.cancel();
                                        eventService.publish(
                                                RunnerEvent
                                                        .queueTerminatedEvent(task.getId(), request.getWorkspace(), request.getProject()));
                                    } catch (Exception e) {
                                        LOG.warn(e.getMessage(), e);
                                    }
                                    i.remove();
                                    waitingNum++;
                                    num++;
                                }
                            } catch (RunnerException e) {
                                LOG.warn(e.getMessage(), e);
                            }
                        } else {
                            RemoteRunnerProcess remote = null;
                            try {
                                remote = task.getRemoteProcess();
                            } catch (Exception e) {
                                LOG.warn(e.getMessage(), e);
                            }
                            if (remote == null) {
                                i.remove();
                                num++;
                            } else if ((remote.getCreationTime() + request.getLifetime() + appCleanupTime) < System.currentTimeMillis()) {
                                try {
                                    remote.getApplicationProcessDescriptor();
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
            }, cleanerPeriod, cleanerPeriod, TimeUnit.MILLISECONDS);

            // sending message by websocket connection for notice about used memory size changing
            eventService.subscribe(new ResourcesChangesMessenger());
            eventService.subscribe(new ProcessStartedMessenger());
            eventService.subscribe(new RunStatusMessenger());
            //Log events for analytics
            eventService.subscribe(new AnalyticsMessenger());

            if (slaves.length > 0) {
                executor.execute(new RegisterSlaveRunnerTask(slaves, null));
            }
            if (slavesPaid.length > 0) {
                executor.execute(new RegisterSlaveRunnerTask(slavesPaid, "paid"));
            }
            if (slavesAlwaysOn.length > 0) {
                executor.execute(new RegisterSlaveRunnerTask(slavesAlwaysOn, "always_on"));
            }
        } else {
            throw new IllegalStateException("Already started");
        }
    }

    protected void checkStarted() {
        if (!started.get()) {
            throw new IllegalStateException("The runner has not started yet and there is a delay.");
        }
    }

    @PreDestroy
    public void stop() {
        if (started.compareAndSet(true, false)) {
            boolean interrupted = false;
            cleanScheduler.shutdownNow();
            try {
                if (!cleanScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    LOG.warn("Unable terminate cleanScheduler");
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
            runnerListMapping.clear();
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        } else {
            throw new IllegalStateException("Is not started yet.");
        }
    }

    public RunQueueTask run(String workspace, String project, ServiceContext serviceContext, RunOptions runOptions) throws RunnerException {
        checkStarted();
        final DtoFactory dtoFactory = DtoFactory.getInstance();
        if (runOptions == null) {
            runOptions = dtoFactory.createDto(RunOptions.class);
        }
        final ProjectDescriptor projectDescriptor = getProjectDescriptor(workspace, project, serviceContext);
        final User user = EnvironmentContext.getCurrent().getUser();
        final RunRequest request = dtoFactory.createDto(RunRequest.class)
                                             .withWorkspace(workspace)
                                             .withProject(project)
                                             .withProjectDescriptor(projectDescriptor)
                                             .withUserId(user == null ? "" : user.getId())
                                             .withUserToken(getUserToken());
        String notParsedEnvironmentId = runOptions.getEnvironmentId();
        // Project configuration for runner.
        final RunnersDescriptor runners = projectDescriptor.getRunners();
        if (notParsedEnvironmentId == null) {
            if (runners != null) {
                notParsedEnvironmentId = runners.getDefault();
            }
            if (notParsedEnvironmentId == null) {
                throw new RunnerException("Name of runner environment is not specified, be sure corresponded property of project is set.");
            }
        }
        final WorkspaceDescriptor workspaceDescriptor = getWorkspaceDescriptor(workspace, serviceContext);
        String infra = workspaceDescriptor.getAttributes().get(Constants.RUNNER_INFRA);
        if (infra == null) {
            infra = "community";
        }
        final EnvironmentId parsedEnvironmentId = EnvironmentId.parse(notParsedEnvironmentId);
        final List<RemoteRunner> matchedRunners = new LinkedList<>();
        switch (parsedEnvironmentId.getScope()) {
            // This may be fixed in next versions but for now use following agreements.
            // Runner environment id must have format: <scope>:/<category>/<name>.
            //   scope - 'system' or 'project'
            //   category - hierarchical name separated by '/' (omitted if scope is project)
            //   name - name of runner environment
            // Category is used as runner identifier
            // Here is chain how we have this in RunQueue:
            //   org.eclipse.che.api.runner.internal.Runner.getName()
            //   org.eclipse.che.api.runner.internal.SlaveRunnerService.getAvailableRunners()
            //   org.eclipse.che.api.runner.dto.RunnerDescriptor.getName()
            //   RemoteRunnerServer
            //   RemoteRunner.getName()
            // In case if category doesn't exist (should be only in case of 'project' scope).
            // Check just runner existence at this stage. Later will check amount of memory.
            case system:
                // In case of system runner
                request.setRunner(parsedEnvironmentId.getCategory());
                request.setEnvironmentId(parsedEnvironmentId.getName());
                final Set<RemoteRunner> runnerList = getRunnerList(infra, workspace, project);
                if (runnerList != null) {
                    for (RemoteRunner runner : runnerList) {
                        if (request.getRunner().equals(runner.getName()) && runner.hasEnvironment(request.getEnvironmentId())) {
                            matchedRunners.add(runner);
                        }
                    }
                }
                if (matchedRunners.isEmpty()) {
                    throw new RunnerException(String.format("Runner environment '%s' is not available for workspace '%s' on infra '%s'.",
                                                            notParsedEnvironmentId, workspace, infra));
                }
                break;
            case project:
                resolveProjectRunnerEnvironments(infra, request, projectDescriptor, parsedEnvironmentId.getName(), matchedRunners);
                if (matchedRunners.isEmpty()) {
                    throw new RunnerException(String.format("Runner '%s' is not available for workspace '%s' on infra '%s'.",
                                                            request.getRunner(), workspace, infra));
                }
                break;
            default:
                // Not expected
                throw new RunnerException(String.format("Invalid environment scope ''%s'", parsedEnvironmentId.getScope()));
        }

        // Get runner configuration.
        final RunnerConfiguration runnerConfig = runners == null ? null : runners.getConfigs().get(notParsedEnvironmentId);
        int mem = runOptions.getMemorySize();
        // If nothing is set in user request try to determine memory size for application.
        if (mem <= 0) {
            if (runnerConfig != null) {
                mem = runnerConfig.getRam();
            }
            if (mem <= 0) {
                // If nothing is set use value from our configuration.
                mem = defMemSize;
            }
        }
        request.setMemorySize(mem);
        // When get memory size check available resources.
        checkResources(workspaceDescriptor, request);
        // Enables or disables debug mode
        request.setInDebugMode(runOptions.isInDebugMode());
        // Get application lifetime.
        final String lifetimeAttr = workspaceDescriptor.getAttributes().get(Constants.RUNNER_LIFETIME);
        int lifetime = lifetimeAttr != null ? Integer.parseInt(lifetimeAttr) : defLifetime;
        if (lifetime <= 0) {
            lifetime = Integer.MAX_VALUE;
        }
        request.setLifetime(lifetime);
        // Options for runner.
        final Map<String, String> options = runOptions.getOptions();
        if (!options.isEmpty()) {
            request.setOptions(options);
        } else if (runnerConfig != null) {
            request.setOptions(runnerConfig.getOptions());
        }
        final Map<String, String> envVariables = runOptions.getVariables();
        if (!envVariables.isEmpty()) {
            request.setVariables(envVariables);
        } else if (runnerConfig != null) {
            request.setVariables(runnerConfig.getVariables());
        }
        // Options for web shell that runner may provide to the server with running application.
        request.setShellOptions(runOptions.getShellOptions());
        final ValueHolder<BuildTaskDescriptor> buildTaskHolder = new ValueHolder<>();
        // Sometime user may request to skip build of project before run.
        final boolean skipBuild = runOptions.getSkipBuild();
        BuildOptions buildOptions = runOptions.getBuildOptions();
        BuildersDescriptor builders;
        if (!skipBuild
            && ((buildOptions != null && buildOptions.getBuilderName() != null)
                || ((builders = projectDescriptor.getBuilders()) != null) && builders.getDefault() != null)) {
            LOG.debug("Need build project '{}' from workspace '{}'", project, workspace);
            if (buildOptions == null) {
                buildOptions = dtoFactory.createDto(BuildOptions.class);
            }
            // We want bundle of application with all dependencies (libraries) that application needs.
            buildOptions.setIncludeDependencies(true);
            buildOptions.setSkipTest(true);
            final RemoteServiceDescriptor builderService = getBuilderServiceDescriptor(workspace, serviceContext);
            // schedule build
            buildTaskHolder.set(startBuild(builderService, project, buildOptions));
        }
        final Callable<RemoteRunnerProcess> callable = createTaskFor(matchedRunners, request, buildTaskHolder);
        final Long id = sequence.getAndIncrement();
        final InternalRunTask future = new InternalRunTask(ThreadLocalPropagateContext.wrap(callable), id, workspace, project);
        request.setId(id); // for getting callback events from remote runner
        final RunQueueTask task = new RunQueueTask(id,
                                                   request,
                                                   maxWaitingTimeMillis,
                                                   future,
                                                   buildTaskHolder,
                                                   eventService,
                                                   notParsedEnvironmentId,
                                                   serviceContext.getServiceUriBuilder());
        tasks.put(id, task);
        eventService.publish(RunnerEvent.queueStartedEvent(id, workspace, project));
        executor.execute(future);
        return task;
    }

    private void resolveProjectRunnerEnvironments(String infra, RunRequest request, ProjectDescriptor projectDescriptor,
                                                  String envName, List<RemoteRunner> matchedRunners) throws RunnerException {
        final List<String> recipesUrls = new LinkedList<>();
        for (ItemReference recipe : getProjectRunnerRecipes(projectDescriptor, envName)) {
            // interesting only about files!!
            if ("file".equals(recipe.getType())) {
                // TODO: Need improve that but it's OK for now since we have just docker for user's defined environments.
                if (recipe.getName().equals("Dockerfile")) {
                    request.setRunner("docker");
                }
                final Link contentLink = recipe.getLink(org.eclipse.che.api.project.server.Constants.LINK_REL_GET_CONTENT);
                recipesUrls.add(contentLink.getHref());
            }
        }
        // If don't find any files that we are able to recognize as runner recipe.
        if (request.getRunner() == null) {
            throw new RunnerException("You requested a run and your project with custom environment." +
                                      " The runner was unable to get any supported recipe files in environment '" + envName + "'");
        }
        request.setRecipeUrls(recipesUrls);
        final Set<RemoteRunner> runnerList = getRunnerList(infra, request.getWorkspace(), request.getProject());
        if (runnerList != null) {
            for (RemoteRunner runner : runnerList) {
                // In case of user's defined environment don't need to check environment name. Runner must accept any recipe files.
                // That is related to way how we determine runner name from set of recipe files available in custom environment.
                if (request.getRunner().equals(runner.getName())) {
                    matchedRunners.add(runner);
                }
            }
        }
    }

    // Switched to default for test.
    // private
    WorkspaceDescriptor getWorkspaceDescriptor(String workspace, ServiceContext serviceContext) throws RunnerException {
        final UriBuilder baseWorkspaceUriBuilder = baseWorkspaceApiUrl == null || baseWorkspaceApiUrl.isEmpty()
                                                   ? serviceContext.getBaseUriBuilder()
                                                   : UriBuilder.fromUri(baseWorkspaceApiUrl);
        final String workspaceUrl = baseWorkspaceUriBuilder.path(WorkspaceService.class)
                                                           .path(WorkspaceService.class, "getById")
                                                           .build(workspace).toString();
        try {
            return HttpJsonHelper.get(WorkspaceDescriptor.class, workspaceUrl);
        } catch (IOException e) {
            throw new RunnerException(e);
        } catch (ServerException | UnauthorizedException | ForbiddenException | NotFoundException | ConflictException e) {
            throw new RunnerException(e.getServiceError());
        }
    }

    // Switched to default for test.
    // private
    ProjectDescriptor getProjectDescriptor(String workspace, String project, ServiceContext serviceContext) throws RunnerException {
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
            throw new RunnerException(e);
        } catch (ServerException | UnauthorizedException | ForbiddenException | NotFoundException | ConflictException e) {
            throw new RunnerException(e.getServiceError());
        }
    }

    // Switched to default for test.
    // private
    Set<RemoteRunner> getRunnerList(String infra, String workspace, String project) {
        Set<RemoteRunner> runnerList = runnerListMapping.get(new RunnerListKey(infra, workspace, project));
        if (runnerList == null) {
            if (project != null || workspace != null) {
                if (workspace != null) {
                    // have dedicated runners for whole workspace (omit project) ?
                    runnerList = runnerListMapping.get(new RunnerListKey(infra, workspace, null));
                }
                if (runnerList == null) {
                    // seems there is no dedicated runners for specified request, use shared one then
                    runnerList = runnerListMapping.get(new RunnerListKey(infra, null, null));
                }
            }
        }
        return runnerList == null ? null : FluentIterable.from(runnerList).filter(new Predicate<RemoteRunner>() {
            private final Map<String, Boolean> serverAvailability = new HashMap<>();

            private boolean isAvailable(String serverUrl) throws ServerException {
                Boolean result = serverAvailability.get(serverUrl);
                if (result == null) {
                    RemoteRunnerServer runnerServer = runnerServers.get(serverUrl);
                    if (runnerServer == null) {
                        throw new ServerException("Server with id " + serverUrl + " is not found");
                    }
                    result = runnerServer.isAvailable();
                    serverAvailability.put(serverUrl, result);
                }
                return result;
            }

            @Override
            public boolean apply(@Nullable RemoteRunner input) {
                try {
                    return isAvailable(input.getBaseUrl());
                } catch (ServerException e) {
                    LOG.warn(e.getLocalizedMessage());
                }
                return false;
            }
        }).toSet();
    }

    // Switched to default for test.
    // private
    void checkResources(WorkspaceDescriptor workspace, RunRequest request) throws RunnerException {
        final String wsId = workspace.getId();
        final int index = wsId.hashCode() & resourceCheckerMask;
        // Lock to be sure other threads don't try to start application in the same workspace.
        resourceCheckerLocks[index].lock();
        try {
            final int availableMem = getTotalMemory(workspace);
            if (availableMem < request.getMemorySize()) {
                throw new RunnerException(
                        String.format("Not enough resources to start application. Available memory %dM but %dM required.",
                                      availableMem < 0 ? 0 : availableMem, request.getMemorySize())
                );
            }
            checkMemory(wsId, availableMem, request.getMemorySize());
        } finally {
            resourceCheckerLocks[index].unlock();
        }
    }

    // Switched to default for test.
    // private
    void checkMemory(String wsId, int availableMem, int mem) throws RunnerException {
        for (RunQueueTask task : tasks.values()) {
            final RunRequest request = task.getRequest();
            if (wsId.equals(request.getWorkspace())) {
                try {
                    ApplicationStatus status;
                    if (task.isStopped()) {
                        continue;
                    }
                    if (task.isWaiting()
                        || (status = task.getRemoteProcess().getApplicationProcessDescriptor().getStatus()) == ApplicationStatus.RUNNING
                        || status == ApplicationStatus.NEW) {
                        availableMem -= request.getMemorySize();
                        if (availableMem < mem) {
                            throw new RunnerException(
                                    String.format("Not enough resources to start application. Available memory %dM but %dM required.",
                                                  availableMem < 0 ? 0 : availableMem, mem)
                            );
                        }
                    }
                } catch (NotFoundException ignored) {
                    // If remote process is not found, it is stopped and removed from remote server.
                }
            }
        }
    }

    int getUsedMemory(String workspaceId) {
        int usedMemory = 0;
        for (RunQueueTask task : tasks.values()) {
            final RunRequest request = task.getRequest();
            if (workspaceId.equals(request.getWorkspace())) {
                try {
                    ApplicationStatus status;
                    if (task.isWaiting()
                        || (!task.isStopped() &&
                            ((status = task.getRemoteProcess().getApplicationProcessDescriptor().getStatus()) == ApplicationStatus.RUNNING
                             || (status == ApplicationStatus.NEW)))) {
                        usedMemory += request.getMemorySize();
                    }
                } catch (NotFoundException ignored) {
                    // If remote process is not found, it is stopped and removed from remote server.
                } catch (RunnerException e) {
                    // If can't get remote process in some reason, probably it was not started at all or we aren't able to connect to
                    // remote runner. Such errors should not prevent get info about available resources.
                    LOG.warn("Unable get amount of memory used by application '{}' from workspace '{}'. Get error when try access " +
                             "status of remote process. Error: {}", request.getProject(), request.getWorkspace(), e.getMessage());
                }
            }
        }
        return usedMemory;
    }

    int getTotalMemory(WorkspaceDescriptor workspace) throws RunnerException {
        if (workspace.getAttributes().containsKey(org.eclipse.che.api.account.server.Constants.RESOURCES_LOCKED_PROPERTY)) {
            throw new RunnerException("Run action for this workspace is locked");
        }
        final String availableMemAttr = workspace.getAttributes().get(Constants.RUNNER_MAX_MEMORY_SIZE);
        return availableMemAttr != null ? Integer.parseInt(availableMemAttr) : defMaxMemorySize;
    }

    int getTotalMemory(String workspaceId, ServiceContext serviceContext) throws RunnerException {
        return getTotalMemory(getWorkspaceDescriptor(workspaceId, serviceContext));
    }

    // Switched to default for test.
    // private
    RemoteServiceDescriptor getBuilderServiceDescriptor(String workspace, ServiceContext serviceContext) {
        final UriBuilder baseBuilderUriBuilder = baseBuilderApiUrl == null || baseBuilderApiUrl.isEmpty()
                                                 ? serviceContext.getBaseUriBuilder()
                                                 : UriBuilder.fromUri(baseBuilderApiUrl);
        final String builderUrl = baseBuilderUriBuilder.path(BuilderService.class).build(workspace).toString();
        return new RemoteServiceDescriptor(builderUrl);
    }

    // Switched to default for test.
    // private
    BuildTaskDescriptor startBuild(RemoteServiceDescriptor builderService, String project, BuildOptions buildOptions)
            throws RunnerException {
        final BuildTaskDescriptor buildDescriptor;
        try {
            final Link buildLink = builderService.getLink(org.eclipse.che.api.builder.internal.Constants.LINK_REL_BUILD);
            if (buildLink == null) {
                throw new RunnerException("You requested a run and your project has not been built." +
                                          " The runner was unable to get the proper build URL to initiate a build.");
            }
            buildDescriptor = HttpJsonHelper.request(BuildTaskDescriptor.class, buildLink, buildOptions, Pair.of("project", project));
        } catch (IOException e) {
            throw new RunnerException(e);
        } catch (ServerException | UnauthorizedException | ForbiddenException | NotFoundException | ConflictException e) {
            throw new RunnerException(e.getServiceError());
        }
        return buildDescriptor;
    }

    protected Callable<RemoteRunnerProcess> createTaskFor(final List<RemoteRunner> matched,
                                                          final RunRequest request,
                                                          final ValueHolder<BuildTaskDescriptor> buildTaskHolder) {
        return new RemoteRunnerProcessCallable(buildTaskHolder, request, matched);
    }

    // Switched to default for test.
    // private
    List<ItemReference> getProjectRunnerRecipes(ProjectDescriptor projectDescriptor, String envName) throws RunnerException {
        final Link childrenLink = projectDescriptor.getLink(org.eclipse.che.api.project.server.Constants.LINK_REL_CHILDREN);
        if (childrenLink == null) {
            throw new RunnerException("You requested a run and your project with custom environment." +
                                      " The runner was unable to get the proper URL to load runner environments from project.");
        }
        try {
            return HttpJsonHelper.requestArray(ItemReference.class, DtoFactory.getInstance()
                                                                              .clone(childrenLink)
                                                                              .withHref(String.format("%s/.codenvy/runners/environments/%s",
                                                                                                      childrenLink.getHref(), envName)));
        } catch (IOException e) {
            throw new RunnerException(e);
        } catch (ServerException | UnauthorizedException | ForbiddenException | NotFoundException | ConflictException e) {
            throw new RunnerException(e.getServiceError());
        }
    }

    // Switched to default for test.
    // private
    boolean tryCancelBuild(BuildTaskDescriptor buildDescriptor) {
        final Link cancelLink = buildDescriptor.getLink(org.eclipse.che.api.builder.internal.Constants.LINK_REL_CANCEL);
        if (cancelLink == null) {
            LOG.error("Can't cancel build process since cancel link is not available.");
            return false;
        } else {
            try {
                final BuildTaskDescriptor result = HttpJsonHelper.request(BuildTaskDescriptor.class,
                                                                          DtoFactory.getInstance().clone(cancelLink));
                LOG.debug("Build cancellation result: {}", result);
                return result != null && result.getStatus() == BuildStatus.CANCELLED;
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                return false;
            }
        }
    }

    protected EventService getEventService() {
        return eventService;
    }

    public List<RemoteRunnerServer> getRegisterRunnerServers() {
        return new ArrayList<>(runnerServers.values());
    }

    /**
     * Register remote SlaveRunnerService which can process run application.
     *
     * @param registration
     *         RunnerServerRegistration
     * @return {@code true} if set of available Runners changed as result of the call
     * if we access remote SlaveRunnerService successfully but get error response
     * @throws RunnerException
     *         if an error occurs
     */
    public boolean registerRunnerServer(RunnerServerRegistration registration) throws RunnerException {
        checkStarted();
        final String url = registration.getRunnerServerLocation().getUrl();
        final RemoteRunnerServer runnerServer = createRemoteRunnerServer(url);
        String infra = null;
        String workspace = null;
        String project = null;
        final RunnerServerAccessCriteria accessCriteria = registration.getRunnerServerAccessCriteria();
        if (accessCriteria != null) {
            infra = accessCriteria.getInfra();
            workspace = accessCriteria.getWorkspace();
            project = accessCriteria.getProject();
        }
        if (infra != null) {
            runnerServer.setInfra(infra);
        }
        if (workspace != null) {
            runnerServer.setAssignedWorkspace(workspace);
            if (project != null) {
                runnerServer.setAssignedProject(project);
            }
        }
        return doRegisterRunnerServer(runnerServer);
    }

    // Switched to default for test.
    // private
    RemoteRunnerServer createRemoteRunnerServer(String url) {
        return new RemoteRunnerServer(url);
    }

    // Switched to default for test.
    // private
    boolean doRegisterRunnerServer(RemoteRunnerServer runnerServer) throws RunnerException {
        runnerServers.put(runnerServer.getBaseUrl(), runnerServer);
        final RunnerListKey key = new RunnerListKey(runnerServer.getInfra(),
                                                    runnerServer.getAssignedWorkspace(),
                                                    runnerServer.getAssignedProject());
        Set<RemoteRunner> runnerList = runnerListMapping.get(key);
        if (runnerList == null) {
            final Set<RemoteRunner> newRunnerList = new CopyOnWriteArraySet<>();
            runnerList = runnerListMapping.putIfAbsent(key, newRunnerList);
            if (runnerList == null) {
                runnerList = newRunnerList;
            }
        }
        return runnerList.addAll(runnerServer.getRemoteRunners());
    }

    /**
     * Unregister remote SlaveRunnerService.
     *
     * @param location
     *         RunnerServerLocation
     * @return {@code true} if set of available Runners changed as result of the call
     * if we access remote SlaveRunnerService successfully but get error response
     * @throws RunnerException
     *         if an error occurs
     */
    public boolean unregisterRunnerServer(RunnerServerLocation location) throws RunnerException {
        checkStarted();
        final String url = location.getUrl();
        if (url == null) {
            return false;
        }
        final RemoteRunnerServer runnerService = runnerServers.remove(url);
        return runnerService != null && doUnregisterRunners(url);
    }

    // Switched to default for test.
    // private
    boolean doUnregisterRunners(String url) {
        boolean modified = false;
        for (Iterator<Set<RemoteRunner>> i = runnerListMapping.values().iterator(); i.hasNext(); ) {
            final Set<RemoteRunner> runnerList = i.next();
            for (RemoteRunner runner : runnerList) {
                if (url.equals(runner.getBaseUrl())) {
                    modified |= runnerList.remove(runner);
                }
            }
            if (runnerList.size() == 0) {
                i.remove();
            }
        }
        return modified;
    }

    private String getUserToken() {
        User user = EnvironmentContext.getCurrent().getUser();
        if (user != null) {
            return user.getToken();
        }
        return null;
    }

    /* ============================================================================================ */

    private class RegisterSlaveRunnerTask implements Runnable {
        final String[] mySlaves;
        final String   infra;

        RegisterSlaveRunnerTask(String[] mySlaves, String infra) {
            this.mySlaves = mySlaves;
            this.infra = infra;
        }

        @Override
        public void run() {
            final LinkedList<RemoteRunnerServer> servers = new LinkedList<>();
            for (String slaveUrl : mySlaves) {
                try {
                    RemoteRunnerServer server = createRemoteRunnerServer(slaveUrl);
                    if (infra != null) {
                        server.setInfra(infra);
                    }
                    servers.add(server);
                } catch (IllegalArgumentException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
            final LinkedList<RemoteRunnerServer> offline = new LinkedList<>();
            for (; ; ) {
                while (!servers.isEmpty()) {
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    final RemoteRunnerServer server = servers.pop();
                    if (server.isAvailable()) {
                        try {
                            doRegisterRunnerServer(server);
                            LOG.debug("Pre-configured slave runner server '{}' registered.", server.getBaseUrl());
                        } catch (RunnerException e) {
                            LOG.error(e.getMessage(), e);
                            offline.add(server);
                        }
                    } else {
                        LOG.warn("Pre-configured slave runner server '{}' isn't responding.", server.getBaseUrl());
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
    }


    private class RemoteRunnerProcessCallable implements Callable<RemoteRunnerProcess> {
        private final ValueHolder<BuildTaskDescriptor> buildTaskHolder;
        private final RunRequest                       request;
        private final List<RemoteRunner>               matchedRunners;
        private final Set<Pair<String, String>>        lowDiskSpaceRunners;
        private final Set<Pair<String, String>>        criticalDiskSpaceRunners;

        public RemoteRunnerProcessCallable(ValueHolder<BuildTaskDescriptor> buildTaskHolder, RunRequest request,
                                           List<RemoteRunner> matchedRunners) {
            this.buildTaskHolder = buildTaskHolder;
            this.request = request;
            this.matchedRunners = matchedRunners;
            lowDiskSpaceRunners = new HashSet<>();
            criticalDiskSpaceRunners = new HashSet<>();
        }

        @Override
        public RemoteRunnerProcess call() throws Exception {
            BuildTaskDescriptor buildDescriptor = buildTaskHolder.get();
            if (buildDescriptor != null) {
                final Link buildStatusLink = buildDescriptor.getLink(org.eclipse.che.api.builder.internal.Constants.LINK_REL_GET_STATUS);
                if (buildStatusLink == null) {
                    throw new RunnerException("Invalid response from builder service. Unable get URL for checking build status");
                }
                for (; ; ) {
                    if (Thread.currentThread().isInterrupted()) {
                        // Expected to get here if task is canceled. Try to cancel related runner process.
                        tryCancelBuild(buildDescriptor);
                        return null;
                    }
                    synchronized (this) {
                        try {
                            wait(checkBuildResultPeriod);
                        } catch (InterruptedException e) {
                            // Expected to get here if task is canceled. Try to cancel related build process.
                            tryCancelBuild(buildDescriptor);
                            return null;
                        }
                    }
                    buildDescriptor = HttpJsonHelper.request(BuildTaskDescriptor.class, DtoFactory.getInstance().clone(buildStatusLink));
                    // to be able show current state of build process with RunQueueTask.
                    buildTaskHolder.set(buildDescriptor);
                    final BuildStatus buildStatus = buildDescriptor.getStatus();
                    if (buildStatus == BuildStatus.SUCCESSFUL) {
                        request.withBuildTaskDescriptor(buildDescriptor);
                        break; // get out from loop
                    } else if (buildStatus == BuildStatus.CANCELLED || buildStatus == BuildStatus.FAILED) {
                        String msg = "Unable start application. Build of application is failed or cancelled.";
                        final Link logLink = buildDescriptor.getLink(org.eclipse.che.api.builder.internal.Constants.LINK_REL_VIEW_LOG);
                        if (logLink != null) {
                            msg += (" Build logs: " + logLink.getHref());
                        }
                        throw new RunnerException(msg);
                    } else if (buildStatus == BuildStatus.IN_PROGRESS || buildStatus == BuildStatus.IN_QUEUE) {
                        // wait
                        LOG.debug("Build in of project '{}' from workspace '{}' is progress", request.getProject(), request.getWorkspace());
                    }
                }
            }

            // List of runners that have enough resources for launch application.
            final List<RemoteRunner> available = new LinkedList<>();
            for (; ; ) {
                for (RemoteRunner runner : matchedRunners) {
                    if (Thread.currentThread().isInterrupted()) {
                        // Expected to get here if task is canceled. Stop immediately.
                        return null;
                    }
                    RunnerState runnerState;
                    try {
                        runnerState = runner.getRemoteRunnerState();
                    } catch (Exception e) {
                        LOG.error(e.getMessage(), e);
                        continue;
                    }
                    if (runnerState.getServerState().getFreeMemory() >= request.getMemorySize()
                        && hasEnoughSpaceOnDisk(runner.getName(), runner.getBaseUrl(), runnerState)) {

                        available.add(runner);
                    }
                }
                if (available.isEmpty()) {
                    synchronized (this) {
                        try {
                            // Wait and try again.
                            wait(checkAvailableRunnerPeriod);
                        } catch (InterruptedException e) {
                            // Expected to get here if task is canceled.
                            Thread.currentThread().interrupt();
                            return null;
                        }
                    }
                } else {
                    final RemoteRunner runner = available.size() > 1 ? runnerSelector.select(available) : available.get(0);
                    LOG.info("Use runner '{}' at '{}'", runner.getName(), runner.getBaseUrl());
                    return runner.run(request);
                }
            }
        }

        private boolean hasEnoughSpaceOnDisk(String name, String baseUrl, RunnerState runnerState) {
            final long diskSpace = getTotalDiskSpace(runnerState);
            if (diskSpace > 0) {
                final long usedDiskSpace = getUsedDiskSpace(runnerState);
                if (usedDiskSpace > 0) {
                    final long freePercent = (long)((((double)diskSpace - usedDiskSpace) / diskSpace) * 100);
                    if (freePercent < 5) {
                        if (criticalDiskSpaceRunners.add(Pair.of(name, baseUrl))) {
                            // In production error messages cause sending email with SMTPAppender.
                            // Need remember runners with low disk space to avoid sending multiple emails.
                            LOG.error("Skip runner '{}' at '{}' because of low disk space, {}% left", name, baseUrl, freePercent);
                        }
                        return false;
                    } else if (freePercent < 10) {
                        if (lowDiskSpaceRunners.add(Pair.of(name, baseUrl))) {
                            // In production error messages cause sending email with SMTPAppender.
                            // Need remember runners with low disk space to avoid sending multiple emails.
                            LOG.error("Runner '{}' at '{}' is running out of disk space, {}% left.", name, baseUrl, freePercent);
                        }
                    }
                }
            }
            // If don't have information about disk status let application run.
            return true;
        }

        /** Gets total disk space available for running application in bytes or {@code -1} if this operation is not supported. */
        private long getTotalDiskSpace(RunnerState runnerState) {
            for (RunnerMetric metric : runnerState.getStats()) {
                if (RunnerMetric.DISK_SPACE_TOTAL.equals(metric.getName())) {
                    return Size.parseSize(metric.getValue());
                }
            }
            return -1;
        }

        /** Gets disk space used for running application in bytes or {@code -1} if this operation is not supported. */
        private long getUsedDiskSpace(RunnerState runnerState) {
            for (RunnerMetric metric : runnerState.getStats()) {
                if (RunnerMetric.DISK_SPACE_USED.equals(metric.getName())) {
                    return Size.parseSize(metric.getValue());
                }
            }
            return -1;
        }
    }

    // for store workspace, project and id of process with FutureTask
    private static class InternalRunTask extends FutureTask<RemoteRunnerProcess> {
        final Long   id;
        final String workspace;
        final String project;

        InternalRunTask(Callable<RemoteRunnerProcess> callable, Long id, String workspace, String project) {
            super(callable);
            this.id = id;
            this.workspace = workspace;
            this.project = project;
        }
    }

    // >>>>>>>>>>>>>>>>>>>>> Groups runners by infra + workspace + project.

    // Switched to default for test.
    // private
    static class RunnerListKey {
        final String infra;
        final String project;
        final String workspace;

        RunnerListKey(String infra, String workspace, String project) {
            this.infra = infra;
            this.workspace = workspace;
            this.project = project;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof RunnerListKey)) {
                return false;
            }
            RunnerListKey other = (RunnerListKey)o;
            return infra.equals(other.infra)
                   && (workspace == null ? other.workspace == null : workspace.equals(other.workspace))
                   && (project == null ? other.project == null : project.equals(other.project));

        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = hash * 31 + infra.hashCode();
            hash = hash * 31 + (workspace == null ? 0 : workspace.hashCode());
            hash = hash * 31 + (project == null ? 0 : project.hashCode());
            return hash;
        }

        @Override
        public String toString() {
            return "RunnerListKey{" +
                   "infra='" + infra + '\'' +
                   ", workspace='" + workspace + '\'' +
                   ", project='" + project + '\'' +
                   '}';
        }
    }

    // >>>>>>>>>>>>>>>>>>>>>>>>>>>>> application start checker

    private static class ApplicationUrlChecker implements Runnable {
        final long taskId;
        final URL  url;
        final int  healthCheckerTimeout;
        final int  healthCheckAttempts;

        ApplicationUrlChecker(long taskId, URL url, int healthCheckerTimeout, int healthCheckAttempts) {
            this.taskId = taskId;
            this.url = url;
            this.healthCheckerTimeout = healthCheckerTimeout;
            this.healthCheckAttempts = healthCheckAttempts;
        }

        @Override
        public void run() {
            boolean ok = false;
            String requestMethod = "HEAD";
            for (int i = 0; !ok && i < healthCheckAttempts; i++) {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                try {
                    Thread.sleep(healthCheckerTimeout);
                } catch (InterruptedException e) {
                    return;
                }
                HttpURLConnection conn = null;
                try {
                    conn = (HttpURLConnection)url.openConnection();
                    conn.setRequestMethod(requestMethod);
                    conn.setConnectTimeout(1000);
                    conn.setReadTimeout(1000);

                    LOG.debug(String.format("Response code: %d.", conn.getResponseCode()));
                    if (405 == conn.getResponseCode()) {
                        // In case of Method not allowed, we use get instead of HEAD. X-HTTP-Method-Override would be nice but support is
                        // to weak and will trigger much more GET than with this fallback.
                        // Note: Response.Status in JAX-WS in JEE6 hasn't any status matching 405, so here we use int code comparison. Fixed
                        // in JEE7.
                        requestMethod = "GET";
                    }
                    Response.Status status = Response.Status.fromStatusCode(conn.getResponseCode());
                    if (status == null) {
                        continue;
                    }
                    if (Response.Status.Family.SUCCESSFUL == status.getFamily()
                        || Response.Status.Family.REDIRECTION == status.getFamily()
                        || Response.Status.Family.INFORMATIONAL == status.getFamily()) {
                        ok = true;
                        LOG.debug("Application URL '{}' - OK", url);
                        final ChannelBroadcastMessage bm = new ChannelBroadcastMessage();
                        bm.setChannel(String.format("runner:app_health:%d", taskId));
                        bm.setBody(String.format("{\"url\":%s,\"status\":\"%s\"}", JsonUtils.getJsonString(url.toString()), "OK"));
                        try {
                            WSConnectionContext.sendMessage(bm);
                        } catch (Exception e) {
                            LOG.error(e.getMessage(), e);
                        }
                    }
                } catch (IOException ignored) {
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                    }
                }
            }
        }
    }

    // >>>>>>>>>>>>>>>>>>>>>>>> Events

    private class ResourcesChangesMessenger implements EventSubscriber<RunnerEvent> {
        @Override
        public void onEvent(RunnerEvent event) {
            switch (event.getType()) {
                case RUN_TASK_ADDED_IN_QUEUE:
                case STOPPED:
                case ERROR:
                case RUN_TASK_QUEUE_TIME_EXCEEDED:
                case CANCELED:
                    try {
                        final ChannelBroadcastMessage bm = new ChannelBroadcastMessage();
                        String workspaceId = event.getWorkspace();
                        bm.setChannel(String.format("workspace:resources:%s", workspaceId));

                        final ResourcesDescriptor resourcesDescriptor = DtoFactory.getInstance().createDto(ResourcesDescriptor.class)
                                                                                  .withUsedMemory(
                                                                                          String.valueOf(getUsedMemory(workspaceId)));
                        bm.setBody(DtoFactory.getInstance().toJson(resourcesDescriptor));
                        WSConnectionContext.sendMessage(bm);
                    } catch (Exception e) {
                        LOG.error(e.getMessage(), e);
                    }
                    break;
            }
        }
    }

    private class ProcessStartedMessenger implements EventSubscriber<RunnerEvent> {
        @Override
        public void onEvent(RunnerEvent event) {
            if (event.getType() == RunnerEvent.EventType.STARTED) {
                try {
                    final ChannelBroadcastMessage bm = new ChannelBroadcastMessage();
                    final ApplicationProcessDescriptor descriptor = getTask(event.getProcessId()).getDescriptor();
                    bm.setChannel(String.format("runner:process_started:%s:%s:%s", event.getWorkspace(), event.getProject(),
                                                descriptor.getUserId()));
                    bm.setBody(DtoFactory.getInstance().toJson(descriptor));
                    WSConnectionContext.sendMessage(bm);
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        }
    }

    private class RunStatusMessenger implements EventSubscriber<RunnerEvent> {
        @Override
        public void onEvent(RunnerEvent event) {
            try {
                final ChannelBroadcastMessage bm = new ChannelBroadcastMessage();
                final long id = event.getProcessId();
                switch (event.getType()) {
                    case PREPARATION_STARTED:
                    case STARTED:
                    case STOPPED:
                    case CANCELED:
                    case ERROR:
                        bm.setChannel(String.format("runner:status:%d", id));
                        try {
                            final ApplicationProcessDescriptor descriptor = getTask(id).getDescriptor();
                            bm.setBody(DtoFactory.getInstance().toJson(descriptor));

                            if (event.getType() == RunnerEvent.EventType.STARTED) {
                                final Link appLink = descriptor.getLink(Constants.LINK_REL_WEB_URL);
                                if (appLink != null) {
                                    executor.execute(new ApplicationUrlChecker(id,
                                                                               new URL(appLink.getHref()),
                                                                               APPLICATION_CHECK_URL_TIMEOUT,
                                                                               APPLICATION_CHECK_URL_COUNT));
                                }
                            }
                        } catch (RunnerException re) {
                            bm.setType(ChannelBroadcastMessage.Type.ERROR);
                            bm.setBody(String.format("{\"message\":%s}", JsonUtils.getJsonString(re.getMessage())));
                        } catch (NotFoundException re) {
                            // task was not create in some reason in this case post error message directly
                            bm.setType(ChannelBroadcastMessage.Type.ERROR);
                            bm.setBody(String.format("{\"message\":%s}", JsonUtils.getJsonString(event.getError())));
                        }
                        break;
                    case RUN_TASK_QUEUE_TIME_EXCEEDED:
                        bm.setChannel(String.format("runner:status:%d", id));
                        bm.setType(ChannelBroadcastMessage.Type.ERROR);
                        bm.setBody(String.format("{\"message\":%s}",
                                                 "Unable to start application, currently there are no resources to start your application." +
                                                 " Max waiting time for available resources has been reached. Contact support for assistance."));
                        break;
                    case MESSAGE_LOGGED:
                        final RunnerEvent.LoggedMessage message = event.getMessage();
                        if (message != null) {
                            bm.setChannel(String.format("runner:output:%d", id));
                            bm.setBody(String.format("{\"num\":%d, \"line\":%s}",
                                                     message.getLineNum(), JsonUtils.getJsonString(message.getMessage())));
                        }
                        break;
                }
                WSConnectionContext.sendMessage(bm);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    private class AnalyticsMessenger implements EventSubscriber<RunnerEvent> {

        @Override
        public void onEvent(RunnerEvent event) {
            if (event.getType() == RunnerEvent.EventType.PREPARATION_STARTED
                || event.getType() == RunnerEvent.EventType.STARTED
                || event.getType() == RunnerEvent.EventType.STOPPED
                || event.getType() == RunnerEvent.EventType.RUN_TASK_ADDED_IN_QUEUE
                || event.getType() == RunnerEvent.EventType.RUN_TASK_QUEUE_TIME_EXCEEDED) {
                try {
                    final long id = event.getProcessId();
                    final RunQueueTask task = getTask(id);
                    final RunRequest request = task.getRequest();
                    final String analyticsID = task.getCreationTime() + "-" + id;
                    final String project = extractProjectName(event.getProject());
                    final String workspace = request.getWorkspace();
                    final long time = System.currentTimeMillis();
                    final int memorySize = request.getMemorySize();
                    final long waitingTime = time - task.getCreationTime();
                    final long lifetime;
                    if (request.getLifetime() == Integer.MAX_VALUE) {
                        lifetime = -1;
                    } else {
                        lifetime = request.getLifetime() * 1000; // to ms
                    }
                    final String projectTypeId = request.getProjectDescriptor().getType();
                    final boolean debug = request.isInDebugMode();
                    final String user = request.getUserId();
                    switch (event.getType()) {
                        case STARTED:
                            LOG.info("EVENT#run-queue-waiting-finished# TIME#{}# WS#{}# USER#{}# PROJECT#{}# TYPE#{}# ID#{} WAITING-TIME#{}#",
                                     time,
                                     workspace,
                                     user,
                                     project,
                                     projectTypeId,
                                     analyticsID,
                                     waitingTime);
                            final String startLineFormat =
                                    debug ? "EVENT#debug-started# TIME#{}# WS#{}# USER#{}# PROJECT#{}# TYPE#{}# ID#{}# MEMORY#{}# LIFETIME#{}#"
                                          : "EVENT#run-started# TIME#{}# WS#{}# USER#{}# PROJECT#{}# TYPE#{}# ID#{}# MEMORY#{}# LIFETIME#{}#";
                            LOG.info(startLineFormat,
                                     time,
                                     workspace,
                                     user,
                                     project,
                                     projectTypeId,
                                     analyticsID,
                                     memorySize,
                                     lifetime);
                            break;
                        case STOPPED:
                            final String stopLineFormat =
                                    debug
                                    ? "EVENT#debug-finished# TIME#{}# WS#{}# USER#{}# PROJECT#{}# TYPE#{}# ID#{}# MEMORY#{}# LIFETIME#{}#"
                                    : "EVENT#run-finished# TIME#{}# WS#{}# USER#{}# PROJECT#{}# TYPE#{}# ID#{}# MEMORY#{}# LIFETIME#{}#";
                            LOG.info(stopLineFormat,
                                     time,
                                     workspace,
                                     user,
                                     project,
                                     projectTypeId,
                                     analyticsID,
                                     memorySize,
                                     lifetime);
                            break;
                        case RUN_TASK_ADDED_IN_QUEUE:
                            LOG.info("EVENT#run-queue-waiting-started# TIME#{}# WS#{}# USER#{}# PROJECT#{}# TYPE#{}# ID#{}#",
                                     time,
                                     workspace,
                                     user,
                                     project,
                                     projectTypeId,
                                     analyticsID);
                            break;
                        case RUN_TASK_QUEUE_TIME_EXCEEDED:
                            LOG.info("EVENT#run-queue-terminated# TIME#{}# WS#{}# USER#{}# PROJECT#{}# TYPE#{}# ID#{}# WAITING-TIME#{}#",
                                     time,
                                     workspace,
                                     user,
                                     project,
                                     projectTypeId,
                                     analyticsID,
                                     waitingTime);
                            break;
                    }
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
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
}
