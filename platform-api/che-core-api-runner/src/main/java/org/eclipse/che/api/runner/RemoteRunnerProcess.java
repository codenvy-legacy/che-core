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

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.UnauthorizedException;
import org.eclipse.che.api.core.rest.HttpJsonHelper;
import org.eclipse.che.api.core.rest.HttpOutputMessage;
import org.eclipse.che.api.core.rest.OutputProvider;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.runner.dto.ApplicationProcessDescriptor;
import org.eclipse.che.dto.server.DtoFactory;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closer;

import org.eclipse.che.api.runner.internal.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Representation of remote application process.
 *
 * @author andrew00x
 */
public class RemoteRunnerProcess {
    private static final Logger LOG = LoggerFactory.getLogger(RemoteRunnerProcess.class);

    private final String baseUrl;
    private final String runner;

    private final Long processId;
    private final long created;

    RemoteRunnerProcess(String baseUrl, String runner, Long processId) {
        this.baseUrl = baseUrl;
        this.runner = runner;
        this.processId = processId;
        created = System.currentTimeMillis();
    }

    public Long getProcessId() {
        return processId;
    }

    public long getCreationTime() {
        return created;
    }

    /** URL of server where application was launched. */
    public String getServerUrl() {
        return baseUrl;
    }

    /**
     * Get actual status of remote application process.
     *
     * @return status of remote application process
     * @throws RunnerException
     *         if an error occurs
     * @throws NotFoundException
     *         if can't get status of remote process because isn't available anymore, e.g. its already removed on remote server
     */
    public ApplicationProcessDescriptor getApplicationProcessDescriptor() throws RunnerException, NotFoundException {
        try {
            return HttpJsonHelper.get(ApplicationProcessDescriptor.class, 10000, baseUrl + "/status/" + runner + '/' + processId);
        } catch (IOException e) {
            throw new RunnerException(e);
        } catch (ServerException | UnauthorizedException | ForbiddenException | ConflictException e) {
            throw new RunnerException(e.getServiceError());
        }
    }

    /**
     * Stop a remote application process.
     *
     * @return status of remote application process after the call
     * @throws RunnerException
     *         if an error occurs
     * @throws NotFoundException
     *         if can't stop remote application because isn't available anymore, e.g. its already removed on remote server
     */
    public ApplicationProcessDescriptor stop() throws RunnerException, NotFoundException {
        final ApplicationProcessDescriptor descriptor = getApplicationProcessDescriptor();
        final Link link = descriptor.getLink(Constants.LINK_REL_STOP);
        if (link == null) {
            switch (descriptor.getStatus()) {
                case STOPPED:
                case CANCELLED:
                    LOG.debug("Can't stop process, status is {}", descriptor.getStatus());
                    return descriptor;
                default:
                    throw new RunnerException("Can't stop application. Link for stop application is not available.");
            }
        }
        try {
            return HttpJsonHelper.request(ApplicationProcessDescriptor.class, DtoFactory.getInstance().clone(link));
        } catch (IOException e) {
            throw new RunnerException(e);
        } catch (ServerException | UnauthorizedException | ForbiddenException | ConflictException e) {
            throw new RunnerException(e.getServiceError());
        }
    }

    public void readLogs(OutputProvider output) throws IOException, RunnerException, NotFoundException {
        final ApplicationProcessDescriptor descriptor = getApplicationProcessDescriptor();
        final Link link = descriptor.getLink(Constants.LINK_REL_VIEW_LOG);
        if (link == null) {
            throw new RunnerException("Logs are not available.");
        }
        doRequest(link.getHref(), link.getMethod(), output);
    }

    public void readRecipeFile(OutputProvider output) throws IOException, RunnerException {
        doRequest(String.format("%s/recipe/%s/%d", baseUrl, runner, processId), "GET", output);
    }

    private void doRequest(String url, String method, final OutputProvider output) throws IOException {
        final HttpURLConnection conn = (HttpURLConnection)new URL(url).openConnection();
        conn.setConnectTimeout(60 * 1000);
        conn.setReadTimeout(60 * 1000);
        conn.setRequestMethod(method);
        try {
            if (output instanceof HttpOutputMessage) {
                HttpOutputMessage httpOutput = (HttpOutputMessage)output;
                httpOutput.setStatus(conn.getResponseCode());
                final String contentType = conn.getContentType();
                if (contentType != null) {
                    httpOutput.addHttpHeader("Content-Type", contentType);
                }
                // for download files
                final String contentDisposition = conn.getHeaderField("Content-Disposition");
                if (contentDisposition != null) {
                    httpOutput.addHttpHeader("Content-Disposition", contentDisposition);
                }
            }
            Closer closer = Closer.create();
            try {
                ByteStreams.copy(closer.register(conn.getInputStream()), closer.register(output.getOutputStream()));
            } catch (Throwable e) {
                throw closer.rethrow(e);
            } finally {
                closer.close();
            }

        } finally {
            conn.disconnect();
        }
    }
}
