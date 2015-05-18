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

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.eclipse.che.api.builder.dto.BuildTaskDescriptor;
import org.eclipse.che.api.builder.internal.Constants;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.core.util.Cancellable;
import org.eclipse.che.api.core.util.DownloadPlugin;
import org.eclipse.che.api.core.util.FileCleaner;
import org.eclipse.che.api.core.util.HttpDownloadPlugin;
import org.eclipse.che.api.core.util.Watchdog;
import org.eclipse.che.api.project.shared.dto.RunnerEnvironment;
import org.eclipse.che.api.runner.RunnerException;
import org.eclipse.che.api.runner.dto.RunRequest;
import org.eclipse.che.api.runner.dto.RunnerMetric;
import org.eclipse.che.commons.lang.IoUtil;
import org.eclipse.che.commons.lang.TarUtils;
import org.eclipse.che.commons.lang.concurrent.ThreadLocalPropagateContext;
import org.eclipse.che.dto.server.DtoFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Super-class for all implementation of Runner.
 *
 * @author andrew00x
 * @author Eugene Voevodin
 */
public abstract class Runner {
    private static final Logger LOG = LoggerFactory.getLogger(Runner.class);

    private static final AtomicLong processIdSequence = new AtomicLong(1);

    private static final DeploymentSourcesValidator ALL_VALID = new DeploymentSourcesValidator() {
        @Override
        public boolean isValid(DeploymentSources deployment) {
            return true;
        }
    };

    private static final DeploymentSources NO_SOURCES = new DeploymentSources(null);

    private final Map<Long, RunnerProcessImpl> processes;
    private final Map<Long, RunnerProcessImpl> expiredProcesses;
    private final Map<Long, List<Disposer>>    applicationDisposers;
    private final Object                       applicationDisposersLock;
    private final AtomicInteger                runningAppsCounter;
    private final java.io.File                 deployDirectoryRoot;
    private final ResourceAllocators           allocators;
    private final EventService                 eventService;
    private final AtomicBoolean                started;

    protected final long cleanupDelayMillis;
    protected final long maxStartTime;

    private ExecutorService          executor;
    private ScheduledExecutorService cleanScheduler;
    private java.io.File             deployDirectory;

    protected final DownloadPlugin downloadPlugin;

    public Runner(java.io.File deployDirectoryRoot, int cleanupDelay, ResourceAllocators allocators, EventService eventService) {
        this.deployDirectoryRoot = deployDirectoryRoot;
        this.cleanupDelayMillis = TimeUnit.SECONDS.toMillis(cleanupDelay);
        this.maxStartTime = TimeUnit.MINUTES.toMillis(10); // TODO: configurable
        this.allocators = allocators;
        this.eventService = eventService;

        processes = new ConcurrentHashMap<>();
        expiredProcesses = new ConcurrentHashMap<>();
        applicationDisposers = new ConcurrentHashMap<>();
        applicationDisposersLock = new Object();
        runningAppsCounter = new AtomicInteger(0);
        downloadPlugin = new HttpDownloadPlugin();
        started = new AtomicBoolean(false);
    }

    /**
     * Returns the name of the runner. All registered runners should have unique name.
     *
     * @return the name of this runner
     */
    public abstract String getName();

    /**
     * Returns the description of the runner. Description should help client to recognize correct type of runner for an application.
     *
     * @return the description of this runner
     */
    public abstract String getDescription();

    /**
     * Gets environments that are supported by the runner. Each environment presupposes an existing some embedded pre-configured
     * environment for running application, e.g. type of server or its configuration. By default this method returns lis that contains one
     * environment with id: <i>default</i> without any options or environment variables.
     */
    public List<RunnerEnvironment> getEnvironments() {
        final DtoFactory dtoFactory = DtoFactory.getInstance();
        return Collections.singletonList(dtoFactory.createDto(RunnerEnvironment.class)
                                                   .withId("default")
                                                   .withDescription(String.format("Default '%s' environment", getName())));
    }

    /**
     * Gets global stats for this runner.
     *
     * @throws org.eclipse.che.api.runner.RunnerException
     *         if any error occurs while getting runner metrics
     */
    public List<RunnerMetric> getStats() throws RunnerException {
        List<RunnerMetric> global = new LinkedList<>();
        final DtoFactory dtoFactory = DtoFactory.getInstance();
        global.add(dtoFactory.createDto(RunnerMetric.class).withName(RunnerMetric.TOTAL_APPS)
                             .withValue(Integer.toString(getTotalAppsNum())));
        global.add(dtoFactory.createDto(RunnerMetric.class).withName(RunnerMetric.RUNNING_APPS)
                             .withValue(Integer.toString(getRunningAppsNum())));
        return global;
    }

    public int getRunningAppsNum() {
        return runningAppsCounter.get();
    }

    public int getTotalAppsNum() {
        return processes.size();
    }

    /**
     * Gets root directory for deploy all applications.
     *
     * @return root directory for deploy all applications.
     */
    public java.io.File getDeployDirectory() {
        return deployDirectory;
    }

    /**
     * Gets process by its {@code id}.
     *
     * @param id
     *         id of process
     * @return runner process with specified id
     * @throws NotFoundException
     *         if id of RunnerProcess is invalid
     */
    public final RunnerProcess getProcess(Long id) throws NotFoundException {
        RunnerProcessImpl process = processes.get(id);
        if (process == null) {
            process = expiredProcesses.get(id);
            if (process == null) {
                throw new NotFoundException(String.format("Invalid run task id: %d", id));
            }
        }
        return process;
    }

    /**
     * Gets stats related to the specified process.
     *
     * @throws NotFoundException
     *         if id of RunnerProcess is invalid
     * @throws RunnerException
     *         if any other error occurs
     * @see #getProcess(Long)
     */
    public List<RunnerMetric> getStats(Long id) throws NotFoundException, RunnerException {
        return getStats(getProcess(id));
    }

    protected List<RunnerMetric> getStats(RunnerProcess process) throws RunnerException {
        final List<RunnerMetric> result = new LinkedList<>();
        final DtoFactory dtoFactory = DtoFactory.getInstance();
        final long started = process.getStartTime();
        final long stopped = process.getStopTime();
        if (started > 0) {
            result.add(dtoFactory.createDto(RunnerMetric.class).withName(RunnerMetric.START_TIME).withValue(Long.toString(started))
                                 .withDescription("Time when application was started"));
            if (stopped <= 0) {
                final long lifetime = process.getConfiguration().getRequest().getLifetime();
                final String terminationTime = lifetime >= Integer.MAX_VALUE ? RunnerMetric.ALWAYS_ON
                                                                             : Long.toString(started + TimeUnit.SECONDS.toMillis(lifetime));
                result.add(dtoFactory.createDto(RunnerMetric.class).withName(RunnerMetric.TERMINATION_TIME).withValue(terminationTime)
                                     .withDescription("Time after that this application might be terminated"));
            }
        }
        if (stopped > 0) {
            result.add(dtoFactory.createDto(RunnerMetric.class).withName(RunnerMetric.STOP_TIME).withValue(Long.toString(stopped))
                                 .withDescription("Time when application was stopped"));
        }
        final long uptime = process.getUptime();
        if (uptime > 0) {
            result.add(dtoFactory.createDto(RunnerMetric.class).withName(RunnerMetric.UP_TIME).withValue(Long.toString(uptime))
                                 .withDescription("Application's uptime"));
        }
        final int memory = process.getConfiguration().getMemory();
        result.add(dtoFactory.createDto(RunnerMetric.class).withName(RunnerMetric.MEMORY).withValue(Integer.toString(memory))
                             .withDescription("Amount of memory in megabytes assigned for application"));
        return result;
    }

    public RunnerProcess execute(final RunRequest request) throws RunnerException {
        checkStarted();
        final RunnerProcess.Callback callback = new RunnerProcess.Callback() {
            @Override
            public void started(RunnerProcess process) {
                final RunRequest runRequest = process.getConfiguration().getRequest();
                notify(RunnerEvent.startedEvent(runRequest.getId(), runRequest.getWorkspace(), runRequest.getProject()));
            }

            @Override
            public void stopped(RunnerProcess process) {
                final RunRequest runRequest = process.getConfiguration().getRequest();
                notify(RunnerEvent.stoppedEvent(runRequest.getId(), runRequest.getWorkspace(), runRequest.getProject()));
            }

            @Override
            public void error(RunnerProcess process, Throwable t) {
                final RunRequest runRequest = process.getConfiguration().getRequest();
                notify(RunnerEvent.errorEvent(runRequest.getId(), runRequest.getWorkspace(), runRequest.getProject(), t.getMessage()));
            }

            private void notify(RunnerEvent re) {
                try {
                    eventService.publish(re);
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        };
        return doExecute(request, callback);
    }

    protected RunnerProcess doExecute(final RunRequest request, final RunnerProcess.Callback callback) throws RunnerException {
        final long startTime = System.currentTimeMillis();
        final RunnerConfiguration runnerCfg = getRunnerConfigurationFactory().createRunnerConfiguration(request);
        final int mem = runnerCfg.getMemory();
        final ResourceAllocator memoryAllocator = allocators.newMemoryAllocator(mem);
        final Watchdog watcher = new Watchdog(getName().toUpperCase() + "-WATCHDOG", request.getLifetime(), TimeUnit.SECONDS);
        final Long internalId = processIdSequence.getAndIncrement();
        final RunnerProcessImpl process = new RunnerProcessImpl(internalId, getName(), runnerCfg, callback);
        final Runnable r = ThreadLocalPropagateContext.wrap(new Runnable() {
            @Override
            public void run() {
                try {
                    memoryAllocator.allocate();
                    final java.io.File downloadDir =
                            Files.createTempDirectory(deployDirectory.toPath(), ("download_" + getName().replace("/", "."))).toFile();
                    final DeploymentSources deploymentSources = createDeploymentSources(request, downloadDir);
                    if (deploymentSources.getFile() == null) {
                        throw new RunnerException("[ERROR] No build artifacts found.");
                    }
                    process.addToCleanupList(downloadDir);
                    if (!getDeploymentSourcesValidator().isValid(deploymentSources)) {
                        throw new RunnerException(
                                String.format("Unsupported project. Cannot deploy project %s from workspace %s with runner %s",
                                              request.getProject(), request.getWorkspace(), getName())
                        );
                    }
                    final ApplicationProcess realProcess = newApplicationProcess(deploymentSources, runnerCfg);
                    realProcess.start();
                    process.started(realProcess);
                    watcher.start(new Cancellable() {
                        @Override
                        public void cancel() throws Exception {
                            process.getLogger()
                                   .writeLine(
                                           "[ERROR] Your run has been shutdown due to timeout.");
                            process.internalStop(true);
                        }
                    });
                    runningAppsCounter.incrementAndGet();
                    LOG.debug("Started {}", process);
                    final long endTime = System.currentTimeMillis();
                    LOG.debug("Application {}/{} startup in {} ms", request.getWorkspace(), request.getProject(), (endTime - startTime));
                    realProcess.waitFor();
                    process.stopped();
                    LOG.debug("Stopped {}", process);
                } catch (Throwable e) {
                    LOG.warn(e.getMessage(), e);
                    process.setError(e);
                } finally {
                    watcher.stop();
                    memoryAllocator.release();
                    runningAppsCounter.decrementAndGet();
                }
            }
        });
        processes.put(internalId, process);
        final FutureTask<Void> future = new FutureTask<>(r, null);
        process.setTask(future);
        executor.execute(future);
        return process;
    }

    /** @see RunnerConfiguration */
    public abstract RunnerConfigurationFactory getRunnerConfigurationFactory();

    protected abstract ApplicationProcess newApplicationProcess(DeploymentSources toDeploy, RunnerConfiguration runnerCfg)
            throws RunnerException;

    protected ExecutorService getExecutor() {
        return executor;
    }

    protected EventService getEventService() {
        return eventService;
    }

    /**
     * Gets builder for DeploymentSources. By default this method returns builder that does nothing. Sub-classes may override this
     * method
     * and provide proper implementation of DeploymentSourcesValidator.
     *
     * @return builder for DeploymentSources
     */
    protected DeploymentSourcesValidator getDeploymentSourcesValidator() {
        return ALL_VALID;
    }

    protected DeploymentSources createDeploymentSources(RunRequest request, java.io.File dir) throws IOException {
        Link link = null;
        final BuildTaskDescriptor buildTaskDescriptor = request.getBuildTaskDescriptor();
        boolean artifactTarball = false;
        if (buildTaskDescriptor != null) {
            final List<Link> artifactLinks =
                    buildTaskDescriptor.getLinks(org.eclipse.che.api.builder.internal.Constants.LINK_REL_DOWNLOAD_RESULT);
            if (artifactLinks.size() == 1) {
                link = artifactLinks.get(0);
            } else if (artifactLinks.size() > 1) {
                link = buildTaskDescriptor.getLink(Constants.LINK_REL_DOWNLOAD_RESULTS_TARBALL);
                artifactTarball = link != null;
            }
        } else {
            link = request.getProjectDescriptor().getLink(org.eclipse.che.api.project.server.Constants.LINK_REL_EXPORT_ZIP);
        }
        String url = null;
        if (link != null) {
            final String href = link.getHref();
            final String token = request.getUserToken();
            if (href.indexOf('?') > 0) {
                url = href + "&token=" + token;
            } else {
                url = href + "?token=" + token;
            }
        }
        if (url == null) {
            return NO_SOURCES;
        }
        final DownloadCallback callback = new DownloadCallback();
        downloadPlugin.download(url, dir, callback);
        if (callback.getError() != null) {
            throw callback.getError();
        }
        final java.io.File downloaded = callback.getDownloadedFile();
        if (artifactTarball && downloaded != null) {
            final java.io.File parent = downloaded.getParentFile();
            final java.io.File unpack = new java.io.File(parent, downloaded.getName() + "_untar");
            TarUtils.untar(downloaded, unpack);
            FileCleaner.addFile(downloaded);
            return new DeploymentSources(unpack);
        }
        return new DeploymentSources(downloaded);
    }

    private static class DownloadCallback implements DownloadPlugin.Callback {
        java.io.File downloaded;
        IOException  error;

        @Override
        public void done(java.io.File downloaded) {
            this.downloaded = downloaded;
        }

        @Override
        public void error(IOException e) {
            error = e;
        }

        public java.io.File getDownloadedFile() {
            return downloaded;
        }

        public IOException getError() {
            return error;
        }
    }

    protected java.io.File downloadFile(String url, java.io.File downloadDir, String fileName, boolean replaceExisting) throws IOException {
        downloadPlugin.download(url, downloadDir, fileName, replaceExisting);
        return new java.io.File(downloadDir, fileName);
    }

    protected void registerDisposer(ApplicationProcess application, Disposer disposer) {
        final Long id = application.getId();
        synchronized (applicationDisposersLock) {
            List<Disposer> disposers = applicationDisposers.get(id);
            if (disposers == null) {
                applicationDisposers.put(id, disposers = new LinkedList<>());
            }
            disposers.add(0, disposer);
        }
    }

    protected class RunnerProcessImpl implements RunnerProcess {
        private final Long                id;
        private final String              runner;
        private final RunnerConfiguration configuration;
        private final Callback            callback;
        private final long                created;

        private Future<Void>       task;
        private ApplicationProcess realProcess;
        private long               startTime;
        private long               stopTime;
        private Throwable          error;
        private List<java.io.File> forCleanup;
        private boolean            cancelled;

        protected RunnerProcessImpl(Long id, String runner, RunnerConfiguration configuration, Callback callback) {
            this.id = id;
            this.runner = runner;
            this.configuration = configuration;
            this.callback = callback;
            created = System.currentTimeMillis();
            startTime = -1L;
            stopTime = -1L;
        }

        synchronized void setTask(Future<Void> task) {
            this.task = task;
        }

        synchronized void started(ApplicationProcess realProcess) {
            this.realProcess = realProcess;
            startTime = System.currentTimeMillis();
            if (callback != null) {
                // NOTE: important to do it in separate thread!
                getExecutor().execute(ThreadLocalPropagateContext.wrap(new Runnable() {
                    @Override
                    public void run() {
                        callback.started(RunnerProcessImpl.this);
                    }
                }));
            }
        }

        synchronized void stopped() {
            stopTime = System.currentTimeMillis();
            if (callback != null) {
                // NOTE: important to do it in separate thread!
                getExecutor().execute(ThreadLocalPropagateContext.wrap(new Runnable() {
                    @Override
                    public void run() {
                        callback.stopped(RunnerProcessImpl.this);
                    }
                }));
            }
        }

        synchronized void setError(final Throwable error) {
            this.error = error;
            if (callback != null) {
                // NOTE: important to do it in separate thread!
                getExecutor().execute(ThreadLocalPropagateContext.wrap(new Runnable() {
                    @Override
                    public void run() {
                        callback.error(RunnerProcessImpl.this, error);
                    }
                }));
            }
        }

        synchronized void internalStop(boolean cancelled) throws RunnerException {
            if (task != null && !task.isDone()) {
                task.cancel(true);
            }
            if (realProcess != null && realProcess.isRunning()) {
                realProcess.stop();
            }
            this.cancelled = cancelled;
        }

        synchronized boolean isExpired() {
            return (startTime < 0 && ((created + maxStartTime) < System.currentTimeMillis())) ||
                   (stopTime > 0 && ((stopTime + cleanupDelayMillis) < System.currentTimeMillis()));
        }

        synchronized void addToCleanupList(java.io.File file) {
            if (forCleanup == null) {
                forCleanup = new LinkedList<>();
            }
            forCleanup.add(file);
        }

        synchronized List<java.io.File> getCleanupList() {
            return forCleanup;
        }

        @Override
        public final Long getId() {
            return id;
        }

        @Override
        public synchronized ApplicationProcess getApplicationProcess() {
            return realProcess;
        }

        @Override
        public String getRunner() {
            return runner;
        }

        @Override
        public RunnerConfiguration getConfiguration() {
            return configuration;
        }

        @Override
        public synchronized Throwable getError() {
            return error;
        }

        @Override
        public final synchronized boolean isStarted() {
            return startTime > 0;
        }

        @Override
        public final synchronized long getStartTime() {
            return startTime;
        }

        @Override
        public void stop() throws RunnerException {
            internalStop(false);
        }

        @Override
        public final synchronized boolean isStopped() {
            return stopTime > 0;
        }

        @Override
        public final synchronized long getStopTime() {
            return stopTime;
        }

        @Override
        public synchronized long getUptime() {
            return startTime > 0
                   ? stopTime > 0
                     ? (stopTime - startTime) : (System.currentTimeMillis() - startTime)
                   : 0;
        }

        @Override
        public synchronized boolean isCancelled() {
            return cancelled;
        }

        @Override
        public synchronized ApplicationLogger getLogger() throws RunnerException {
            if (realProcess == null) {
                return ApplicationLogger.DUMMY;
            }
            return realProcess.getLogger();
        }

        @Override
        public String toString() {
            return "RunnerProcessImpl{" +
                   "\nworkspace='" + configuration.getRequest().getWorkspace() + '\'' +
                   "\nproject='" + configuration.getRequest().getProject() + '\'' +
                   "\nrunner='" + runner + '\'' +
                   "\ncreated=" + created +
                   "\nstartTime=" + startTime +
                   "\nstopTime=" + stopTime +
                   "\nid=" + id +
                   "\n}";
        }
    }

    /** Initializes Runner. Sub-classes should invoke {@code super.start} at the begin of this method. */
    @PostConstruct
    public void start() {
        if (started.compareAndSet(false, true)) {
            deployDirectory = new java.io.File(deployDirectoryRoot, getName().replace("/", "."));
            if (!(deployDirectory.exists() || deployDirectory.mkdirs())) {
                throw new IllegalStateException(String.format("Unable create directory %s", deployDirectory.getAbsolutePath()));
            }
            executor = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat(getName() + "-Runner-")
                                                                               .setDaemon(true).build());
            cleanScheduler =
                    Executors.newSingleThreadScheduledExecutor(
                            new ThreadFactoryBuilder().setNameFormat(getName() + "-RunnerCleanSchedulerPool-").setDaemon(true).build());
            cleanScheduler.scheduleAtFixedRate(new CleanupTask(), 1, 1, TimeUnit.MINUTES);
        } else {
            throw new IllegalStateException("Already started");
        }
    }

    private class CleanupTask implements Runnable {
        public void run() {
            for (Iterator<RunnerProcessImpl> i = expiredProcesses.values().iterator(); i.hasNext(); ) {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                final RunnerProcessImpl process = i.next();
                i.remove();
                Disposer[] appDisposers = null;
                final ApplicationProcess realProcess = process.realProcess;
                if (realProcess != null) {
                    synchronized (applicationDisposersLock) {
                        final List<Disposer> disposers = applicationDisposers.remove(realProcess.getId());
                        if (disposers != null) {
                            appDisposers = disposers.toArray(new Disposer[disposers.size()]);
                        }
                    }
                }
                if (appDisposers != null) {
                    for (Disposer disposer : appDisposers) {
                        try {
                            disposer.dispose();
                        } catch (RuntimeException e) {
                            LOG.error(e.getMessage(), e);
                        }
                    }
                }
                final List<java.io.File> cleanupList = process.getCleanupList();
                if (cleanupList != null) {
                    for (java.io.File file : cleanupList) {
                        if (!IoUtil.deleteRecursive(file)) {
                            LOG.warn("Failed delete {}", file);
                        }
                    }
                }
            }
            for (Iterator<RunnerProcessImpl> i = processes.values().iterator(); i.hasNext(); ) {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                final RunnerProcessImpl process = i.next();
                if (process.isExpired()) {
                    try {
                        process.internalStop(true);
                        if (process.getApplicationProcess() == null) {
                            // it is incorrect situation so mark process as failed
                            process.setError(new RunnerException(
                                    "Running process is terminated due to exceeded max allowed time for start."));
                        }
                    } catch (Exception e) {
                        LOG.error(e.getMessage(), e);
                        continue; // try next time
                    }
                    i.remove();
                    expiredProcesses.put(process.getId(), process);
                }
            }
        }
    }

    protected void checkStarted() {
        if (!started.get()) {
            throw new IllegalStateException("Is not started yet.");
        }
    }

    /**
     * Stops Runner and releases any resources associated with the Runner.
     * <p/>
     * Sub-classes should invoke {@code super.stop} at the end of this method.
     */
    @PreDestroy
    public void stop() {
        if (started.compareAndSet(true, false)) {
            boolean interrupted = false;
            cleanScheduler.shutdownNow();
            try {
                if (!cleanScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    LOG.warn("Unable terminate cleanup scheduler");
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
            final List<Disposer> allDisposers = new LinkedList<>();
            synchronized (applicationDisposersLock) {
                for (List<Disposer> disposers : applicationDisposers.values()) {
                    if (disposers != null) {
                        allDisposers.addAll(disposers);
                    }
                }
                applicationDisposers.clear();
            }
            for (Disposer disposer : allDisposers) {
                try {
                    disposer.dispose();
                } catch (RuntimeException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
            final java.io.File[] files = getDeployDirectory().listFiles();
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
            processes.clear();
            expiredProcesses.clear();
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        } else {
            throw new IllegalStateException("Is not started yet.");
        }
    }
}
