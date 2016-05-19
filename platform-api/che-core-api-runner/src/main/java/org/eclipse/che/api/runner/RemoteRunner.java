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

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.UnauthorizedException;
import org.eclipse.che.api.core.rest.HttpJsonHelper;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.project.shared.dto.RunnerEnvironment;
import org.eclipse.che.api.runner.dto.ApplicationProcessDescriptor;
import org.eclipse.che.api.runner.dto.RunRequest;
import org.eclipse.che.api.runner.dto.RunnerState;
import org.eclipse.che.api.runner.internal.Constants;
import org.eclipse.che.commons.lang.Pair;
import org.eclipse.che.dto.server.DtoFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents remote {@code Runner}.
 * <p/>
 * Usage:
 * <pre>
 *     String baseUrl = ...
 *     String runnerName = ...
 *     RemoteRunnerServer runnerServer = new RemoteRunnerServer(baseUrl);
 *     RemoteRunner runner = runnerServer.getRemoteRunner(runnerName);
 *     RunRequest request = ...
 *     RemoteRunnerProcess remote = runner.run(request);
 *     // do something with RemoteRunnerProcess, e.g. check status
 *     System.out.println(remote.getApplicationProcessDescriptor());
 * </pre>
 *
 * @author andrew00x
 * @see RemoteRunnerServer
 */
public class RemoteRunner {
    private final String     baseUrl;
    private final String     name;
    private final int        hashCode;
    private final List<Link> links;

    private volatile long lastUsage = -1;

    /* Package visibility, not expected to be created by api users. They should use RemoteRunnerServer to get an instance of RemoteRunner. */
    RemoteRunner(String baseUrl, String name, List<Link> links) {
        this.baseUrl = baseUrl;
        this.name = name;
        this.links = new ArrayList<>(links);
        int hashCode = 7;
        hashCode = hashCode * 31 + baseUrl.hashCode();
        hashCode = hashCode * 31 + this.name.hashCode();
        this.hashCode = hashCode;
    }

    public final String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Get name of this runner.
     *
     * @return name of this runner
     * @see org.eclipse.che.api.runner.internal.Runner#getName()
     */
    public final String getName() {
        return name;
    }

    /**
     * Get last time of usage of this runner.
     *
     * @return last time of usage of this runner
     */
    public long getLastUsageTime() {
        return lastUsage;
    }

    boolean hasEnvironment(String name) throws RunnerException {
        for (RunnerEnvironment environment : getEnvironments()) {
            if (environment.getId().equals(name)) {
                return true;
            }
        }
        return false;
    }

    List<RunnerEnvironment> getEnvironments() throws RunnerException {
        final Link link = getLink(Constants.LINK_REL_RUNNER_ENVIRONMENTS);
        if (link == null) {
            throw new RunnerException("Unable get URL for retrieving runner's environments");
        }
        try {
            return HttpJsonHelper.requestArray(RunnerEnvironment.class, link, Pair.of("runner", name));
        } catch (IOException e) {
            throw new RunnerException(e);
        } catch (ServerException | UnauthorizedException | ForbiddenException | NotFoundException | ConflictException e) {
            throw new RunnerException(e.getServiceError());
        }
    }

    /**
     * Stats new application process.
     *
     * @param request
     *         build request
     * @return build task
     * @throws RunnerException
     *         if an error occurs
     */
    public RemoteRunnerProcess run(RunRequest request) throws RunnerException {
        final Link link = getLink(Constants.LINK_REL_RUN);
        if (link == null) {
            throw new RunnerException("Unable get URL for starting application's process");
        }
        final ApplicationProcessDescriptor process;
        try {
            process = HttpJsonHelper.request(ApplicationProcessDescriptor.class, link, request);
        } catch (IOException e) {
            throw new RunnerException(e);
        } catch (ServerException | UnauthorizedException | ForbiddenException | NotFoundException | ConflictException e) {
            throw new RunnerException(e.getServiceError());
        }
        lastUsage = System.currentTimeMillis();
        return new RemoteRunnerProcess(baseUrl, name, process.getProcessId());
    }

    /**
     * Get current state of remote runner.
     *
     * @return current state of remote runner.
     * @throws RunnerException
     *         if an error occurs
     */
    public RunnerState getRemoteRunnerState() throws RunnerException {
        final Link stateLink = getLink(Constants.LINK_REL_RUNNER_STATE);
        if (stateLink == null) {
            throw new RunnerException(
                    String.format("Unable get URL for getting state of a remote runner '%s' at '%s'", name, baseUrl));
        }
        try {
            return HttpJsonHelper.request(RunnerState.class, 10000, stateLink, Pair.of("runner", name));
        } catch (IOException e) {
            throw new RunnerException(e);
        } catch (ServerException | UnauthorizedException | ForbiddenException | NotFoundException | ConflictException e) {
            throw new RunnerException(e.getServiceError());
        }
    }

    private Link getLink(String rel) {
        for (Link link : links) {
            if (rel.equals(link.getRel())) {
                // create copy of link since we pass it outside from this class
                return DtoFactory.getInstance().clone(link);
            }
        }
        return null;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RemoteRunner)) {
            return false;
        }
        RemoteRunner other = (RemoteRunner)o;
        return baseUrl.equals(other.baseUrl) && name.equals(other.name);
    }

    @Override
    public final int hashCode() {
        return hashCode;
    }
}
