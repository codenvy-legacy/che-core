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

import org.eclipse.che.api.builder.dto.BuildTaskDescriptor;
import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.core.rest.OutputProvider;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.core.util.Cancellable;
import org.eclipse.che.api.core.util.ValueHolder;
import org.eclipse.che.api.runner.dto.ApplicationProcessDescriptor;
import org.eclipse.che.api.runner.dto.RunRequest;
import org.eclipse.che.api.runner.dto.RunnerMetric;
import org.eclipse.che.api.runner.internal.Constants;
import org.eclipse.che.api.runner.internal.RunnerEvent;
import org.eclipse.che.dto.server.DtoFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Wraps RemoteRunnerProcess.
 *
 * @author andrew00x
 */
public class RunQueueTask implements Cancellable {
    private final Long                             id;
    private final RunRequest                       request;
    private final Future<RemoteRunnerProcess>      future;
    private final ValueHolder<BuildTaskDescriptor> buildTaskHolder;
    private final EventService                     eventService;
    private final long                             created;
    private final long                             waitingTimeout;
    private final String                           originalEnvironmentId;
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private Long stopTime;

    /* NOTE: don't use directly! Always use getter that makes copy of this UriBuilder. */
    private final UriBuilder uriBuilder;

    private UriBuilder getUriBuilder() {
        return uriBuilder.clone();
    }
    /* ~~~~ */

    private RemoteRunnerProcess myRemoteProcess;

    RunQueueTask(Long id,
                 RunRequest request,
                 long waitingTimeout,
                 Future<RemoteRunnerProcess> future,
                 ValueHolder<BuildTaskDescriptor> buildTaskHolder,
                 EventService eventService,
                 String originalEnvironmentId,
                 UriBuilder uriBuilder) {
        this.id = id;
        this.future = future;
        this.request = request;
        this.waitingTimeout = waitingTimeout;
        this.buildTaskHolder = buildTaskHolder;
        this.eventService = eventService;
        this.originalEnvironmentId = originalEnvironmentId;
        this.uriBuilder = uriBuilder;
        created = System.currentTimeMillis();
    }

    public Long getId() {
        return id;
    }

    public RunRequest getRequest() {
        return DtoFactory.getInstance().clone(request);
    }

    public long getCreationTime() {
        return created;
    }

    public ApplicationProcessDescriptor getDescriptor() throws RunnerException, NotFoundException {
        final DtoFactory dtoFactory = DtoFactory.getInstance();
        ApplicationProcessDescriptor descriptor;
        if (isStopped()) {
            final List<RunnerMetric> runStats = new ArrayList<>(1);
            if (stopTime != null) {
                runStats.add(dtoFactory.createDto(RunnerMetric.class).withName(RunnerMetric.STOP_TIME)
                                       .withValue(Long.toString(stopTime))
                                       .withDescription("Time when application was stopped"));
            }
            descriptor = dtoFactory.createDto(ApplicationProcessDescriptor.class)
                                   .withProcessId(id)
                                   .withCreationTime(created)
                                   .withStatus(ApplicationStatus.STOPPED)
                                   .withRunStats(runStats);
        } else if (future.isCancelled()) {
            descriptor = dtoFactory.createDto(ApplicationProcessDescriptor.class)
                                   .withProcessId(id)
                                   .withCreationTime(created)
                                   .withStatus(ApplicationStatus.CANCELLED);
        } else {
            final RemoteRunnerProcess remoteProcess = getRemoteProcess();
            if (remoteProcess == null) {
                final List<Link> links = new ArrayList<>(2);
                links.add(dtoFactory.createDto(Link.class)
                                    .withRel(Constants.LINK_REL_GET_STATUS)
                                    .withHref(getUriBuilder().path(RunnerService.class, "getStatus")
                                                             .build(request.getWorkspace(), id).toString()).withMethod("GET")
                                    .withProduces(MediaType.APPLICATION_JSON));
                links.add(dtoFactory.createDto(Link.class)
                                    .withRel(Constants.LINK_REL_STOP)
                                    .withHref(getUriBuilder().path(RunnerService.class, "stop")
                                                             .build(request.getWorkspace(), id).toString())
                                    .withMethod("POST")
                                    .withProduces(MediaType.APPLICATION_JSON));
                final List<RunnerMetric> runStats = new ArrayList<>(2);
                runStats.add(dtoFactory.createDto(RunnerMetric.class).withName(RunnerMetric.WAITING_TIME_LIMIT)
                                       .withValue(Long.toString(created + waitingTimeout))
                                       .withDescription("Waiting for start limit (ms)"));
                final long lifetime = request.getLifetime();
                runStats.add(dtoFactory.createDto(RunnerMetric.class).withName(RunnerMetric.LIFETIME)
                                       .withValue(lifetime >= Integer.MAX_VALUE ? RunnerMetric.ALWAYS_ON
                                                                                : Long.toString(TimeUnit.SECONDS.toMillis(lifetime)))
                                       .withDescription("Application lifetime (ms)"));
                descriptor = dtoFactory.createDto(ApplicationProcessDescriptor.class)
                                       .withProcessId(id)
                                       .withCreationTime(created)
                                       .withStatus(ApplicationStatus.NEW)
                                       .withRunStats(runStats)
                                       .withLinks(links)
                                       .withWorkspace(request.getWorkspace())
                                       .withProject(request.getProject())
                                       .withUserId(request.getUserId())
                                       .withMemorySize(request.getMemorySize());
            } else {
                final ApplicationProcessDescriptor remoteDescriptor = remoteProcess.getApplicationProcessDescriptor();
                // re-write some parameters, we are working as revers-proxy
                descriptor = dtoFactory.clone(remoteDescriptor)
                                       .withProcessId(id)
                                       .withCreationTime(created)
                                       .withMemorySize(request.getMemorySize())
                                       .withLinks(rewriteKnownLinks(remoteDescriptor.getLinks()));
                final long started = descriptor.getStartTime();
                final long waitingTimeMillis = started > 0 ? started - created : System.currentTimeMillis() - created;
                final List<RunnerMetric> runStats = descriptor.getRunStats();
                runStats.add(dtoFactory.createDto(RunnerMetric.class).withName(RunnerMetric.WAITING_TIME)
                                       .withValue(Long.toString(waitingTimeMillis))
                                       .withDescription("Waiting for start duration (ms)"));
                final long lifetime = request.getLifetime();
                runStats.add(dtoFactory.createDto(RunnerMetric.class).withName(RunnerMetric.LIFETIME)
                                       .withValue(lifetime >= Integer.MAX_VALUE ? RunnerMetric.ALWAYS_ON
                                                                                : Long.toString(TimeUnit.SECONDS.toMillis(lifetime)))
                                       .withDescription("Application lifetime (ms)"));
            }
            final BuildTaskDescriptor buildTaskDescriptor = buildTaskHolder.get();
            if (buildTaskDescriptor != null) {
                descriptor.setBuildStats(buildTaskDescriptor.getBuildStats());
            }
        }
        //we set this id to detect environment scope on client side
        descriptor.setEnvironmentId(originalEnvironmentId);

        return descriptor;
    }

    private List<Link> rewriteKnownLinks(List<Link> links) {
        final List<Link> rewritten = new ArrayList<>();
        for (Link link : links) {
            if (Constants.LINK_REL_GET_STATUS.equals(link.getRel())) {
                final Link copy = DtoFactory.getInstance().clone(link);
                copy.setHref(getUriBuilder().path(RunnerService.class, "getStatus").build(request.getWorkspace(), id).toString());
                rewritten.add(copy);
            } else if (Constants.LINK_REL_STOP.equals(link.getRel())) {
                final Link copy = DtoFactory.getInstance().clone(link);
                copy.setHref(getUriBuilder().path(RunnerService.class, "stop").build(request.getWorkspace(), id).toString());
                rewritten.add(copy);
            } else if (Constants.LINK_REL_VIEW_LOG.equals(link.getRel())) {
                final Link copy = DtoFactory.getInstance().clone(link);
                copy.setHref(getUriBuilder().path(RunnerService.class, "getLogs").build(request.getWorkspace(), id).toString());
                rewritten.add(copy);
            } else if (Constants.LINK_REL_RUNNER_RECIPE.equals(link.getRel())) {
                final Link copy = DtoFactory.getInstance().clone(link);
                copy.setHref(getUriBuilder().path(RunnerService.class, "getRecipeFile").build(request.getWorkspace(), id).toString());
                rewritten.add(copy);
            } else {
                rewritten.add(DtoFactory.getInstance().clone(link));
            }
        }
        return rewritten;
    }

    @Override
    public void cancel() throws Exception {
        if (future.isCancelled()) {
            return;
        }
        stopTime = System.currentTimeMillis();
        doStop(getRemoteProcess());
    }

    public void stop() throws Exception {
        if (stopped.compareAndSet(false, true)) {
            cancel();
        }
    }

    public boolean isStopped() throws RunnerException {
        return stopped.get() || future.isCancelled();
    }

    public boolean isWaiting() {
        return !future.isDone();
    }

    boolean isCancelled() {
        return future.isCancelled();
    }

    private void doStop(RemoteRunnerProcess remoteProcess) throws RunnerException, NotFoundException {
        if (remoteProcess != null) {
            remoteProcess.stop();
        } else {
            future.cancel(true);
            eventService.publish(RunnerEvent.canceledEvent(id, request.getWorkspace(), request.getProject()));
        }
    }

    public void readLogs(OutputProvider output) throws IOException, RunnerException, NotFoundException {
        final RemoteRunnerProcess remoteProcess = getRemoteProcess();
        if (remoteProcess == null) {
            throw new RunnerException("Application isn't started yet, logs aren't available");
        }
        remoteProcess.readLogs(output);
    }

    public void readRecipeFile(OutputProvider output) throws RunnerException, IOException, NotFoundException {
        final RemoteRunnerProcess remoteProcess = getRemoteProcess();
        if (remoteProcess == null) {
            throw new RunnerException("Application isn't started yet, recipe file isn't available");
        }
        remoteProcess.readRecipeFile(output);
    }

    RemoteRunnerProcess getRemoteProcess() throws RunnerException, NotFoundException {
        if (!future.isDone() || future.isCancelled()) {
            return null;
        }
        if (myRemoteProcess == null) {
            try {
                myRemoteProcess = future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                final Throwable cause = e.getCause();
                if (cause instanceof Error) {
                    throw (Error)cause; // lets caller to get Error as is
                } else if (cause instanceof RuntimeException) {
                    throw (RuntimeException)cause;
                } else if (cause instanceof RunnerException) {
                    throw (RunnerException)cause;
                } else if (cause instanceof NotFoundException) {
                    throw (NotFoundException)cause;
                } else if (cause instanceof ApiException) {
                    throw new RunnerException(((ApiException)cause).getServiceError());
                } else {
                    throw new RunnerException(cause.getMessage(), cause);
                }
            }
        }
        return myRemoteProcess;
    }

    @Override
    public String toString() {
        return "RunQueueTask{" +
               "id=" + id +
               ", request=" + request +
               '}';
    }
}
