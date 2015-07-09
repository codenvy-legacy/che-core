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
package org.eclipse.che.api.builder.internal;

import org.eclipse.che.api.builder.BuilderException;
import org.eclipse.che.api.builder.dto.BaseBuilderRequest;
import org.eclipse.che.api.builder.dto.BuildRequest;
import org.eclipse.che.api.builder.dto.BuilderEnvironment;
import org.eclipse.che.api.builder.dto.BuilderMetric;
import org.eclipse.che.api.builder.dto.DependencyRequest;
import org.eclipse.che.api.builder.internal.BuilderEvent.EventType;
import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.core.notification.EventSubscriber;
import org.eclipse.che.api.core.util.Cancellable;
import org.eclipse.che.api.core.util.CancellableProcessWrapper;
import org.eclipse.che.api.core.util.CommandLine;
import org.eclipse.che.api.core.util.ProcessUtil;
import org.eclipse.che.api.core.util.StreamPump;
import org.eclipse.che.api.core.util.Watchdog;
import org.eclipse.che.commons.lang.IoUtil;
import org.eclipse.che.dto.server.DtoFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ws.rs.core.MediaType;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Super-class for all implementation of Builder.
 *
 * @author andrew00x
 */
public abstract class Builder {
    private static final Logger LOG = LoggerFactory.getLogger(Builder.class);

    private static final AtomicLong buildIdSequence = new AtomicLong(1);

    private final ConcurrentMap<Long, FutureBuildTask> tasks;
    private final java.io.File                         rootDirectory;
    private final Set<BuildListener>                   buildListeners;
    private final long                                 keepResultTimeMillis;
    private final EventService                         eventService;
    private final int                                  queueSize;
    private final int                                  numberOfWorkers;
    private final AtomicBoolean                        started;

    private ThreadPoolExecutor       executor;
    private ScheduledExecutorService scheduler;
    private java.io.File             repository;
    private java.io.File             builds;
    private SourcesManagerImpl       sourcesManager;

    public Builder(java.io.File rootDirectory, int numberOfWorkers, int queueSize, int keepResultTime, EventService eventService) {
        this.rootDirectory = rootDirectory;
        this.numberOfWorkers = numberOfWorkers;
        this.queueSize = queueSize;
        this.keepResultTimeMillis = TimeUnit.SECONDS.toMillis(keepResultTime);
        this.eventService = eventService;

        buildListeners = new CopyOnWriteArraySet<>();
        tasks = new ConcurrentHashMap<>();
        started = new AtomicBoolean(false);
    }

    /**
     * Returns the name of the builder. All registered builders should have unique name.
     *
     * @return the name
     */
    public abstract String getName();

    /**
     * Returns the description of the builder. Description should help client to recognize correct type of builder for an application.
     *
     * @return the description of builder
     */
    public abstract String getDescription();

    /**
     * Gets environments that are supported by the builder. Each environment presupposes an existing some embedded pre-configured
     * environment for build, e.g. different versions of JVM. By default this method returns empty map that means usage single environment
     * for all builds.
     */
    public Map<String, BuilderEnvironment> getEnvironments() {
        return Collections.emptyMap();
    }

    /**
     * Gets result of FutureBuildTask. Getting result is implementation specific and mostly depends to build system, e.g. maven usually
     * stores build result in directory 'target' but it is not rule for ant. Regular users are not expected to use this method directly.
     * They should always use method {@link BuildTask#getResult()} instead.
     *
     * @param task
     *         task
     * @param successful
     *         reports whether build process terminated normally or not.
     *         Note: {@code true} is not indicated successful build but only normal process termination. Build itself may be unsuccessful
     *         because to compilation error, failed tests, etc.
     * @return BuildResult
     * @throws BuilderException
     *         if an error occurs when try to get result
     * @see BuildTask#getResult()
     */
    protected abstract BuildResult getTaskResult(FutureBuildTask task, boolean successful) throws BuilderException;

    protected abstract CommandLine createCommandLine(BuilderConfiguration config) throws BuilderException;

    /** Initialize Builder. Sub-classes should invoke {@code super.start} at the begin of this method. */
    @PostConstruct
    public void start() {
        if (started.compareAndSet(false, true)) {
            repository = new java.io.File(rootDirectory, getName());
            if (!(repository.exists() || repository.mkdirs())) {
                throw new IllegalStateException(String.format("Unable create directory %s", repository.getAbsolutePath()));
            }
            final java.io.File sources = new java.io.File(repository, "sources");
            if (!(sources.exists() || sources.mkdirs())) {
                throw new IllegalStateException(String.format("Unable create directory %s", sources.getAbsolutePath()));
            }
            builds = new java.io.File(repository, "builds");
            if (!(builds.exists() || builds.mkdirs())) {
                throw new IllegalStateException(String.format("Unable create directory %s", builds.getAbsolutePath()));
            }
            // TODO: use single instance of SourceManager
            sourcesManager = new SourcesManagerImpl(sources);
            sourcesManager.start(); // TODO: guice must do this
            executor = new MyThreadPoolExecutor(numberOfWorkers <= 0 ? Runtime.getRuntime().availableProcessors() : numberOfWorkers,
                                                queueSize);
            scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat(
                    getName() + "-BuilderSchedulerPool-%d").setDaemon(true).build());
            scheduler.scheduleAtFixedRate(new Runnable() {
                public void run() {
                    int num = 0;
                    for (Iterator<FutureBuildTask> i = tasks.values().iterator(); i.hasNext(); ) {
                        if (Thread.currentThread().isInterrupted()) {
                            return;
                        }
                        final FutureBuildTask task = i.next();
                        if (task.isExpired()) {
                            i.remove();
                            try {
                                cleanup(task);
                            } catch (RuntimeException e) {
                                LOG.error(e.getMessage(), e);
                            }
                            num++;
                        }
                    }
                    if (num > 0) {
                        LOG.debug("Remove {} expired tasks", num);
                    }
                }
            }, 1, 1, TimeUnit.MINUTES);
        } else {
            throw new IllegalStateException("Already started");
        }
    }

    protected void checkStarted() {
        if (!started.get()) {
            throw new IllegalStateException("Is not started yet.");
        }
    }

    /**
     * Stops Builder and releases any resources associated with the Builder.
     * <p/>
     * Sub-classes should invoke {@code super.stop} at the end of this method.
     */
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
            final java.io.File[] files = repository.listFiles();
            if (files != null && files.length > 0) {
                for (java.io.File f : files) {
                    boolean deleted;
                    if (f.isDirectory()) {
                        deleted = IoUtil.deleteRecursive(f);
                    } else {
                        deleted = f.delete();
                    }
                    if (!deleted) {
                        LOG.warn("Failed delete {}", f);
                    }
                }
            }
            tasks.clear();
            buildListeners.clear();
            sourcesManager.stop(); // TODO: guice must do this
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        } else {
            throw new IllegalStateException("Is not started yet.");
        }
    }

    public java.io.File getRepository() {
        checkStarted();
        return repository;
    }

    public java.io.File getBuildDirectory() {
        checkStarted();
        return builds;
    }

    public SourcesManager getSourcesManager() {
        checkStarted();
        return sourcesManager;
    }

    public java.io.File getSourcesDirectory() {
        checkStarted();
        return getSourcesManager().getDirectory();
    }

    public int getNumberOfWorkers() {
        checkStarted();
        return executor.getCorePoolSize();
    }

    public int getNumberOfActiveWorkers() {
        checkStarted();
        return executor.getActiveCount();
    }

    public int getInternalQueueSize() {
        checkStarted();
        return executor.getQueue().size();
    }

    public int getMaxInternalQueueSize() {
        checkStarted();
        return queueSize;
    }

    /**
     * Get global stats for this builder.
     *
     * @throws BuilderException
     *         if any error occurs while getting builder metrics
     */
    public List<BuilderMetric> getStats() throws BuilderException {
        List<BuilderMetric> global = new LinkedList<>();
        final DtoFactory dtoFactory = DtoFactory.getInstance();
        global.add(dtoFactory.createDto(BuilderMetric.class).withName(BuilderMetric.NUMBER_OF_WORKERS)
                             .withValue(Integer.toString(getNumberOfWorkers())));
        global.add(dtoFactory.createDto(BuilderMetric.class).withName(BuilderMetric.NUMBER_OF_ACTIVE_WORKERS)
                             .withValue(Integer.toString(getNumberOfActiveWorkers())));
        global.add(dtoFactory.createDto(BuilderMetric.class).withName(BuilderMetric.QUEUE_SIZE)
                             .withValue(Integer.toString(getInternalQueueSize())));
        global.add(dtoFactory.createDto(BuilderMetric.class).withName(BuilderMetric.MAX_QUEUE_SIZE)
                             .withValue(Integer.toString(getMaxInternalQueueSize())));
        return global;
    }

    /**
     * Add new BuildListener.
     *
     * @param listener
     *         BuildListener
     * @return {@code true} if {@code listener} was added
     */
    public boolean addBuildListener(BuildListener listener) {
        return buildListeners.add(listener);
    }

    /**
     * Remove BuildListener.
     *
     * @param listener
     *         BuildListener
     * @return {@code true} if {@code listener} was removed
     */
    public boolean removeBuildListener(BuildListener listener) {
        return buildListeners.remove(listener);
    }

    /**
     * Get all registered build listeners. Modifications to the returned {@code Set} will not affect the internal {@code Set}.
     *
     * @return all available download plugins
     */
    public Set<BuildListener> getBuildListeners() {
        return new LinkedHashSet<>(buildListeners);
    }

    public BuilderConfigurationFactory getBuilderConfigurationFactory() {
        return new DefaultBuilderConfigurationFactory(this);
    }

    /**
     * Starts new build process.
     *
     * @param request
     *         build request
     * @return build task
     * @throws BuilderException
     *         if an error occurs
     */
    public BuildTask perform(BuildRequest request) throws BuilderException {
        checkStarted();
        final BuilderConfiguration configuration = getBuilderConfigurationFactory().createBuilderConfiguration(request);
        final java.io.File workDir = configuration.getWorkDir();
        final java.io.File logFile = new java.io.File(workDir.getParentFile(), workDir.getName() + ".log");
        final BuildLogger logger = createBuildLogger(configuration, logFile);
        return execute(configuration, logger);
    }

    /**
     * Starts new process of analysis dependencies.
     *
     * @param request
     *         build request
     * @return build task
     * @throws BuilderException
     *         if an error occurs
     */
    public BuildTask perform(DependencyRequest request) throws BuilderException {
        checkStarted();
        final BuilderConfiguration configuration = getBuilderConfigurationFactory().createBuilderConfiguration(request);
        final java.io.File workDir = configuration.getWorkDir();
        final java.io.File logFile = new java.io.File(workDir.getParentFile(), workDir.getName() + ".log");
        final BuildLogger logger = createBuildLogger(configuration, logFile);
        return execute(configuration, logger);
    }

    protected BuildTask execute(BuilderConfiguration configuration, BuildLogger logger) throws BuilderException {
        final CommandLine commandLine = createCommandLine(configuration);
        final BaseBuilderRequest request = configuration.getRequest();
        final BuildLogger myLogger =
                new BuildLogsPublisher(logger, eventService, request.getId(), request.getWorkspace(), request.getProject());
        final Callable<Boolean> callable = createTaskFor(commandLine, myLogger, request.getTimeout(), configuration);
        final Long internalId = buildIdSequence.getAndIncrement();
        final BuildTask.Callback callback = new BuildTask.Callback() {
            @Override
            public void begin(BuildTask task) {}

            @Override
            public void done(BuildTask task) {
                final BaseBuilderRequest buildRequest = task.getConfiguration().getRequest();
                eventService.publish(BuilderEvent.doneEvent(buildRequest.getId(), buildRequest.getWorkspace(), buildRequest.getProject()));
                try {
                    myLogger.close();
                    LOG.debug("Close build logger {}", myLogger);
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        };
        final FutureBuildTask task = new FutureBuildTask(callable, internalId, commandLine, getName(), configuration, myLogger, callback);
        tasks.put(internalId, task);
        executor.execute(task);
        return task;
    }

    protected BuildLogger createBuildLogger(BuilderConfiguration buildConfiguration, java.io.File logFile) throws BuilderException {
        try {
            return new DefaultBuildLogger(logFile, MediaType.TEXT_PLAIN);
        } catch (IOException e) {
            throw new BuilderException(e);
        }
    }

    protected Callable<Boolean> createTaskFor(final CommandLine commandLine,
                                              final BuildLogger logger,
                                              final long timeout,
                                              final BuilderConfiguration configuration) {
        return new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                BaseBuilderRequest request = configuration.getRequest();
                getSourcesManager()
                        .getSources(logger, request.getWorkspace(), request.getProject(), request.getSourcesUrl(), configuration.getWorkDir());
                // build effectively starts right after sources downloading is done
                eventService.publish(BuilderEvent.buildTimeStartedEvent(request.getId(), request.getWorkspace(), request.getProject(),
                                                                        System.currentTimeMillis()));
                eventService.publish(BuilderEvent.beginEvent(request.getId(), request.getWorkspace(), request.getProject()));
                StreamPump output = null;
                Watchdog watcher = null;
                int result = -1;
                try {
                    ProcessBuilder processBuilder = new ProcessBuilder().command(commandLine.toShellCommand()).directory(
                            configuration.getWorkDir()).redirectErrorStream(true);
                    Process process = processBuilder.start();

                    if (timeout > 0) {
                        watcher = new Watchdog(getName().toUpperCase() + "-WATCHDOG", timeout, TimeUnit.SECONDS);
                        watcher.start(new CancellableProcessWrapper(process, new Cancellable.Callback() {
                            @Override
                            public void cancelled(Cancellable cancellable) {
                                try {
                                    logger.writeLine("[ERROR] Your build has been shutdown due to timeout.");
                                } catch (IOException e) {
                                    LOG.error(e.getMessage(), e);
                                }
                            }
                        }));
                    }
                    output = new StreamPump();
                    output.start(process, logger);
                    try {
                        result = process.waitFor();
                    } catch (InterruptedException e) {
                        Thread.interrupted(); // we interrupt thread when cancel task
                        ProcessUtil.kill(process);
                    }
                    try {
                        output.await(); // wait for logger
                    } catch (InterruptedException e) {
                        Thread.interrupted(); // we interrupt thread when cancel task, NOTE: logs may be incomplete
                    }
                } finally {
                    if (watcher != null) {
                        watcher.stop();
                    }
                    if (output != null) {
                        output.stop();
                    }
                }
                LOG.debug("Done: {}, exit code: {}", commandLine, result);
                return result == 0;
            }
        };
    }

    /**
     * Cleanup task. Cleanup means removing all local files which were created by build process, e.g logs, sources, build reports, etc.
     * <p/>
     * Sub-classes should invoke {@code super.cleanup} at the start of this method.
     *
     * @param task
     *         build task
     */
    protected void cleanup(BuildTask task) {
        final BuilderConfiguration configuration = task.getConfiguration();
        final java.io.File workDir = configuration.getWorkDir();
        if (workDir != null && workDir.exists()) {
            if (!IoUtil.deleteRecursive(workDir)) {
                LOG.warn("Unable delete directory {}", workDir);
            }
        }
        final java.io.File log = task.getBuildLogger().getFile();
        if (log != null && log.exists()) {
            if (!log.delete()) {
                LOG.warn("Unable delete file {}", log);
            }
        }
        BuildResult result = null;
        try {
            result = task.getResult();
        } catch (BuilderException e) {
            LOG.error("Skip cleanup of the task {}. Unable get task result.", task);
        }
        if (result != null) {
            List<java.io.File> artifacts = result.getResults();
            if (!artifacts.isEmpty()) {
                for (java.io.File artifact : artifacts) {
                    if (artifact.exists()) {
                        if (!artifact.delete()) {
                            LOG.warn("Unable delete file {}", artifact);
                        }
                    }
                }
            }
            if (result.hasBuildReport()) {
                java.io.File report = result.getBuildReport();
                if (report != null && report.exists()) {
                    if (!report.delete()) {
                        LOG.warn("Unable delete file {}", report);
                    }
                }
            }
        }
        final java.io.File buildDir = configuration.getBuildDir();
        if (buildDir != null && buildDir.exists()) {
            if (!IoUtil.deleteRecursive(buildDir)) {
                LOG.warn("Unable delete directory {}", buildDir);
            }
        }
    }

    /**
     * Get build task by its {@code id}. Typically build process takes some time, so client start process of build or analyze dependencies
     * and periodically check is process already done. Client also may use {@link BuildListener} to be notified when build process starts
     * or ends.
     *
     * @param id
     *         id of BuildTask
     * @return BuildTask
     * @throws NotFoundException
     *         if id of BuildTask is invalid
     * @see #addBuildListener(BuildListener)
     * @see #removeBuildListener(BuildListener)
     */
    public final BuildTask getBuildTask(Long id) throws NotFoundException {
        final FutureBuildTask task = tasks.get(id);
        if (task == null) {
            throw new NotFoundException(String.format("Invalid build task id: %d", id));
        }
        return task;
    }

    /**
     * Get stats related to the specified build task.
     *
     * @throws NotFoundException
     *         if id of BuildTask is invalid
     * @throws BuilderException
     *         if any other error occurs
     * @see #getBuildTask(Long)
     */
    public List<BuilderMetric> getStats(Long id) throws NotFoundException, BuilderException {
        return getStats(getBuildTask(id));
    }

    protected List<BuilderMetric> getStats(BuildTask task) throws BuilderException {
        final List<BuilderMetric> result = new LinkedList<>();
        final DtoFactory dtoFactory = DtoFactory.getInstance();
        final long started = task.getStartTime();
        final long ended = task.getEndTime();
        if (started > 0) {
            result.add(dtoFactory.createDto(BuilderMetric.class).withName(BuilderMetric.START_TIME).withValue(Long.toString(started))
                                 .withDescription("Time when build task was started"));
            if (ended <= 0) {
                long terminationTimeMillis = started + TimeUnit.SECONDS.toMillis(task.getConfiguration().getRequest().getTimeout());
                result.add(dtoFactory.createDto(BuilderMetric.class).withName(BuilderMetric.TERMINATION_TIME)
                                     .withValue(Long.toString(terminationTimeMillis))
                                     .withDescription("Time after that build task might be terminated"));
            }
        }
        if (ended > 0) {
            result.add(dtoFactory.createDto(BuilderMetric.class).withName(BuilderMetric.END_TIME).withValue(Long.toString(ended))
                                 .withDescription("Time when build task was finished"));
        }
        final long runningTime = task.getRunningTime();
        if (runningTime > 0) {
            result.add(dtoFactory.createDto(BuilderMetric.class).withName(BuilderMetric.RUNNING_TIME).withValue(Long.toString(runningTime))
                                 .withDescription("Running time of build task"));
        }
        return result;

    }

    protected ExecutorService getExecutor() {
        return executor;
    }

    protected EventService getEventService() {
        return eventService;
    }

    protected class FutureBuildTask extends FutureTask<Boolean> implements BuildTask {
        private final Long                 id;
        private final CommandLine          commandLine;
        private final String               builder;
        private final BuilderConfiguration configuration;
        private final BuildLogger          buildLogger;
        private final Callback             callback;

        private BuildResult result;
        private long        startTime;
        private long        endTime;

        protected FutureBuildTask(Callable<Boolean> callable,
                                  Long id,
                                  CommandLine commandLine,
                                  String builder,
                                  BuilderConfiguration configuration,
                                  BuildLogger buildLogger,
                                  Callback callback) {
            super(callable);
            this.id = id;
            this.commandLine = commandLine;
            this.builder = builder;
            this.configuration = configuration;
            this.buildLogger = buildLogger;
            this.callback = callback;
            startTime = -1L;
            endTime = -1L;

            eventService.subscribe(new EventSubscriber<BuilderEvent>() {
                @Override
                public void onEvent(BuilderEvent event) {
                    if (event.getType() == EventType.BUILD_TIME_STARTED) {
                        final BuilderEvent.LoggedMessage message = event.getMessage();
                        startTime = Long.parseLong(message.getMessage());
                    }
                }
            });
        }

        @Override
        public final Long getId() {
            return id;
        }

        @Override
        public String getBuilder() {
            return builder;
        }

        @Override
        public CommandLine getCommandLine() {
            return commandLine;
        }

        @Override
        public BuildLogger getBuildLogger() {
            return buildLogger;
        }

        @Override
        public void cancel() {
            super.cancel(true);
        }

        @Override
        public final BuildResult getResult() throws BuilderException {
            if (!isDone()) {
                return null;
            }
            if (result == null) {
                boolean successful;
                try {
                    successful = super.get();
                } catch (InterruptedException e) {
                    // Should not happen since we checked is task done or not.
                    Thread.currentThread().interrupt();
                    successful = false;
                } catch (ExecutionException e) {
                    final Throwable cause = e.getCause();
                    if (cause instanceof Error) {
                        throw (Error)cause;
                    } else if (cause instanceof BuilderException) {
                        throw (BuilderException)cause;
                    } else if (cause instanceof ApiException) {
                        throw new BuilderException(((ApiException)cause).getServiceError());
                    } else {
                        throw new BuilderException(cause.getMessage(), cause);
                    }
                } catch (CancellationException ce) {
                    successful = false;
                }
                result = Builder.this.getTaskResult(this, successful);
            }
            return result;
        }

        @Override
        public BuilderConfiguration getConfiguration() {
            return configuration;
        }

        @Override
        public final synchronized boolean isStarted() {
            return startTime > 0;
        }

        @Override
        public final synchronized long getStartTime() {
            return startTime;
        }

        final synchronized void started() {
            if (callback != null) {
                // NOTE: important to do it in separate thread!
                getExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // Need a bit time for process that post this task to finish. Problem arises if builder is easy loaded. In this
                            // case BuildQueue gets notification event about starting build task even before process that posts build task
                            // ends. This might make problem to see all phases of build process:
                            // IN_QUEUE, IN_PROGRESS, SUCCESSFUL|FAILED|CANCELLED.
                            Thread.sleep(300);
                        } catch (InterruptedException ignored) {
                        }
                        callback.begin(FutureBuildTask.this);
                    }
                });
            }
        }

        @Override
        public final synchronized long getEndTime() {
            return endTime;
        }

        @Override
        public synchronized long getRunningTime() {
            return startTime > 0
                   ? endTime > 0
                     ? (endTime - startTime) : (System.currentTimeMillis() - startTime)
                   : 0;
        }

        final synchronized void ended() {
            endTime = System.currentTimeMillis();
            if (callback != null) {
                // NOTE: important to do it in separate thread!
                getExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        callback.done(FutureBuildTask.this);
                    }
                });
            }
        }

        synchronized boolean isExpired() {
            return endTime > 0
                   && (endTime + keepResultTimeMillis) < System.currentTimeMillis();
        }

        @Override
        public String toString() {
            return "FutureBuildTask{" +
                   "id=" + id +
                   ", builder='" + builder + '\'' +
                   ", workDir=" + configuration.getWorkDir() +
                   '}';
        }
    }

    private class MyThreadPoolExecutor extends ThreadPoolExecutor {
        private MyThreadPoolExecutor(int workerNumber, int queueSize) {
            super(workerNumber, workerNumber, 0L, TimeUnit.MILLISECONDS,
                  new LinkedBlockingQueue<Runnable>(queueSize),
                  new ThreadFactoryBuilder().setNameFormat(Builder.this.getName() + "-Builder-[%d]").setDaemon(true).build(),
                  new ManyBuildTasksRejectedExecutionPolicy(new AbortPolicy()));
        }

        @Override
        protected void beforeExecute(Thread t, Runnable r) {
            if (r instanceof FutureBuildTask) {
                final FutureBuildTask futureBuildTask = (FutureBuildTask)r;
                for (BuildListener buildListener : getBuildListeners()) {
                    try {
                        buildListener.begin(futureBuildTask);
                    } catch (RuntimeException e) {
                        LOG.error(e.getMessage(), e);
                    }
                }
                futureBuildTask.started();
            }
            super.beforeExecute(t, r);
        }

        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);
            if (r instanceof FutureBuildTask) {
                final FutureBuildTask futureBuildTask = (FutureBuildTask)r; // We know it is FutureBuildTask
                for (BuildListener buildListener : getBuildListeners()) {
                    try {
                        buildListener.end(futureBuildTask);
                    } catch (RuntimeException e) {
                        LOG.error(e.getMessage(), e);
                    }
                }
                futureBuildTask.ended();
            }
        }
    }
}
