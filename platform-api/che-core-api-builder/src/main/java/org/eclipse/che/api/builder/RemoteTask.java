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

import org.eclipse.che.api.builder.dto.BuildTaskDescriptor;
import org.eclipse.che.api.builder.internal.Constants;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.UnauthorizedException;
import org.eclipse.che.api.core.rest.HttpJsonHelper;
import org.eclipse.che.api.core.rest.HttpOutputMessage;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.dto.server.DtoFactory;

import com.google.common.io.ByteStreams;
import com.google.common.io.Closer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.HttpHeaders;

/**
 * Representation of remote builder's task.
 *
 * @author andrew00x
 */
public class RemoteTask {
    private static final Logger LOG = LoggerFactory.getLogger(RemoteTask.class);

    private final String baseUrl;
    private final String builder;
    private final Long   taskId;
    private final long   created;

    /* Package visibility, not expected to be created by api users. They should use RemoteBuilder instead and get an instance of remote
    task. */
    RemoteTask(String baseUrl, String builder, Long taskId) {
        this.baseUrl = baseUrl;
        this.builder = builder;
        this.taskId = taskId;
        created = System.currentTimeMillis();
    }

    /**
     * Get unique id of this task.
     *
     * @return unique id of this task
     */
    public Long getId() {
        return taskId;
    }

    /** Get date when this task was created. */
    public long getCreationTime() {
        return created;
    }

    public String getBaseRemoteUrl() {
        return baseUrl;
    }

    public String getBuilder() {
        return builder;
    }

    /**
     * Get actual status of remote build process.
     *
     * @return status of remote build process
     * @throws BuilderException
     *         if an error occurs
     * @throws NotFoundException
     *         if can't get status of remote task because isn't available anymore, e.g. its already removed on remote server
     */
    public BuildTaskDescriptor getBuildTaskDescriptor() throws BuilderException, NotFoundException {
        try {
            return HttpJsonHelper.get(BuildTaskDescriptor.class, String.format("%s/status/%s/%d", baseUrl, builder, taskId));
        } catch (IOException e) {
            throw new BuilderException(e);
        } catch (ServerException | UnauthorizedException | ForbiddenException | ConflictException e) {
            throw new BuilderException(e.getServiceError());
        }
    }

    /**
     * Cancel a remote build process.
     *
     * @return status of remote build process after the call
     * @throws BuilderException
     *         if an error occurs
     * @throws NotFoundException
     *         if can't cancel remote task because isn't available anymore, e.g. its already removed on remote server
     */
    public BuildTaskDescriptor cancel() throws BuilderException, NotFoundException {
        final BuildTaskDescriptor descriptor = getBuildTaskDescriptor();
        final Link link = descriptor.getLink(Constants.LINK_REL_CANCEL);
        if (link == null) {
            switch (descriptor.getStatus()) {
                case SUCCESSFUL:
                case FAILED:
                case CANCELLED:
                    LOG.debug("Can't cancel build, status is {}", descriptor.getStatus());
                    return descriptor;
                default:
                    throw new BuilderException("Can't cancel task. Cancellation link is not available");
            }
        }
        try {
            return HttpJsonHelper.request(BuildTaskDescriptor.class, DtoFactory.getInstance().clone(link));
        } catch (IOException e) {
            throw new BuilderException(e);
        } catch (ServerException | UnauthorizedException | ForbiddenException | ConflictException e) {
            throw new BuilderException(e.getServiceError());
        }
    }

    /**
     * Copy logs of build process to specified {@code output}.
     *
     * @param output
     *         output for logs content
     * @throws IOException
     *         if an i/o error occurs
     * @throws BuilderException
     *         if other error occurs
     */
    public void readLogs(HttpOutputMessage output) throws IOException, BuilderException, NotFoundException {
        final BuildTaskDescriptor descriptor = getBuildTaskDescriptor();
        final Link link = descriptor.getLink(Constants.LINK_REL_VIEW_LOG);
        if (link == null) {
            throw new BuilderException("Logs are not available.");
        }
        readFromUrl(link.getHref(), output);
    }

    /**
     * Copy report file of build process to specified {@code output}.
     *
     * @param output
     *         output for report
     * @throws IOException
     *         if an i/o error occurs
     * @throws BuilderException
     *         if other error occurs
     * @see org.eclipse.che.api.builder.internal.BuildResult#getBuildReport()
     */
    public void readReport(HttpOutputMessage output) throws IOException, BuilderException, NotFoundException {
        final BuildTaskDescriptor descriptor = getBuildTaskDescriptor();
        final Link link = descriptor.getLink(Constants.LINK_REL_VIEW_REPORT);
        if (link == null) {
            throw new BuilderException("Report is not available.");
        }
        readFromUrl(link.getHref(), output);
    }

    /**
     * Download file to specified {@code output}.
     *
     * @param path
     *         path to build artifact
     * @param output
     *         output for download content
     * @throws IOException
     *         if an i/o error occurs
     * @throws BuilderException
     *         if other error occurs
     * @see org.eclipse.che.api.builder.internal.SlaveBuilderService#downloadFile(String, Long, String)
     * @see org.eclipse.che.api.builder.internal.BuildResult#getResults()
     */
    public void downloadFile(String path, HttpOutputMessage output) throws IOException, BuilderException {
        readFromUrl(String.format("%s/download/%s/%d?path=%s", baseUrl, builder, taskId, path), output);
    }

    /**
     * Read file to specified {@code output}.
     *
     * @param path
     *         path to build artifact
     * @param output
     *         output for download content
     * @throws IOException
     *         if an i/o error occurs
     * @throws BuilderException
     *         if other error occurs
     * @see org.eclipse.che.api.builder.internal.SlaveBuilderService#viewFile(String, Long, String)
     * @see org.eclipse.che.api.builder.internal.BuildResult#getResults()
     */
    public void readFile(String path, HttpOutputMessage output) throws IOException, BuilderException {
        readFromUrl(String.format("%s/view/%s/%d?path=%s", baseUrl, builder, taskId, path), output);
    }

    public void browseDirectory(String path, HttpOutputMessage output) throws IOException, BuilderException {
        readFromUrl(String.format("%s/browse/%s/%d?path=%s", baseUrl, builder, taskId, path), output);
    }

    public void listDirectory(String path, HttpOutputMessage output) throws IOException, BuilderException {
        readFromUrl(String.format("%s/tree/%s/%d?path=%s", baseUrl, builder, taskId, path), output);
    }

    public void downloadResultArchive(String archType, HttpOutputMessage output) throws IOException, BuilderException, NotFoundException {
        final BuildTaskDescriptor descriptor = getBuildTaskDescriptor();
        Link link = null;
        if (archType.equals(Constants.RESULT_ARCHIVE_ZIP)) {
            link = descriptor.getLink(Constants.LINK_REL_DOWNLOAD_RESULTS_ZIPBALL);
        } else if (archType.equals(Constants.RESULT_ARCHIVE_TAR)) {
            link = descriptor.getLink(Constants.LINK_REL_DOWNLOAD_RESULTS_TARBALL);
        }
        if (link == null) {
            throw new BuilderException(String.format("%s archive with build result is not available.", archType));
        }
        readFromUrl(link.getHref(), output);
    }

    private void readFromUrl(String url, final HttpOutputMessage output) throws IOException {
        final HttpURLConnection conn = (HttpURLConnection)new URL(url).openConnection();
        conn.setConnectTimeout(60 * 1000);
        conn.setReadTimeout(60 * 1000);
        conn.setRequestMethod(HttpMethod.GET);
        try {
            output.setStatus(conn.getResponseCode());
            final String contentType = conn.getContentType();
            if (contentType != null) {
                output.setContentType(contentType);
            }
            // for download files
            final String contentDisposition = conn.getHeaderField(HttpHeaders.CONTENT_DISPOSITION);
            if (contentDisposition != null) {
                output.addHttpHeader(HttpHeaders.CONTENT_DISPOSITION, contentDisposition);
            }


            Closer closer = Closer.create();
            try {
                InputStream errorStream = closer.register(conn.getErrorStream());
                InputStream in = errorStream != null ? errorStream : closer.register(conn.getInputStream());
                ByteStreams.copy(in, closer.register(output.getOutputStream()));
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
