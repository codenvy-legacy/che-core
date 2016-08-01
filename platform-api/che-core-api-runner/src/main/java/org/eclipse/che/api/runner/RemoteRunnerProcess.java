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

import com.google.common.io.ByteStreams;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.UnauthorizedException;
import org.eclipse.che.api.core.rest.HttpJsonHelper;
import org.eclipse.che.api.core.rest.HttpJsonRequestFactory;
import org.eclipse.che.api.core.rest.HttpOutputMessage;
import org.eclipse.che.api.core.rest.OutputProvider;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.runner.dto.ApplicationProcessDescriptor;
import org.eclipse.che.api.runner.internal.Constants;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.dto.server.DtoFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.HttpHeaders;

import static org.eclipse.che.api.runner.RunnerUtils.runnerRequest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
    private final HttpJsonRequestFactory requestFactory;
    private final Long processId;
    private final long created;

    RemoteRunnerProcess(String baseUrl, String runner, Long processId, HttpJsonRequestFactory requestFactory) {
        this.baseUrl = baseUrl;
        this.runner = runner;
        this.processId = processId;
        this.requestFactory = requestFactory;
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
    	String url = baseUrl + "/status/" + runner + '/' + processId;
    	return runnerRequest(requestFactory.fromUrl(url).setTimeout(10000)).asDto(ApplicationProcessDescriptor.class);
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
        return runnerRequest(requestFactory.fromLink(link)).asDto(ApplicationProcessDescriptor.class);
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
        doRequest(String.format("%s/recipe/%s/%d", baseUrl, runner, processId), HttpMethod.GET, output);
    }

    private void doRequest(String url, String method, final OutputProvider output) throws IOException {
        final HttpURLConnection conn = (HttpURLConnection)new URL(url).openConnection();
        conn.setConnectTimeout(60 * 1000);
        conn.setReadTimeout(60 * 1000);
        conn.setRequestMethod(method);
        final EnvironmentContext context = EnvironmentContext.getCurrent();
        if (context.getUser() != null && context.getUser().getToken() != null) {
            conn.setRequestProperty(HttpHeaders.AUTHORIZATION, context.getUser().getToken());
        }
        try {
            if (output instanceof HttpOutputMessage) {
                HttpOutputMessage httpOutput = (HttpOutputMessage)output;
                httpOutput.setStatus(conn.getResponseCode());
                final String contentType = conn.getContentType();
                if (contentType != null) {
                    httpOutput.addHttpHeader(HttpHeaders.CONTENT_TYPE, contentType);
                }
                // for download files
                final String contentDisposition = conn.getHeaderField(HttpHeaders.CONTENT_DISPOSITION);
                if (contentDisposition != null) {
                    httpOutput.addHttpHeader(HttpHeaders.CONTENT_DISPOSITION, contentDisposition);
                }
            }

            try (InputStream in = conn.getInputStream();
                 OutputStream out = output.getOutputStream()) {
                ByteStreams.copy(in, out);
            }

        } finally {
            conn.disconnect();
        }
    }
}
