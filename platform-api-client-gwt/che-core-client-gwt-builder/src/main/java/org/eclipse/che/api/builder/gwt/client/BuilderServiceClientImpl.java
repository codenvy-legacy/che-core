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
package org.eclipse.che.api.builder.gwt.client;

import org.eclipse.che.api.builder.dto.BuildOptions;
import org.eclipse.che.api.builder.dto.BuildTaskDescriptor;
import org.eclipse.che.api.builder.dto.BuilderDescriptor;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.ide.MimeType;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.AsyncRequestFactory;
import org.eclipse.che.ide.rest.AsyncRequestLoader;
import org.eclipse.che.ide.rest.HTTPHeader;
import org.eclipse.che.ide.rest.RestContext;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

/**
 * Implementation of {@link BuilderServiceClient} service.
 *
 * @author Artem Zatsarynnyy
 */
@Singleton
public class BuilderServiceClientImpl implements BuilderServiceClient {
    private final String              baseUrl;
    private final AsyncRequestLoader  loader;
    private final AsyncRequestFactory asyncRequestFactory;

    @Inject
    public BuilderServiceClientImpl(@RestContext String baseUrl,
                                    @Named("workspaceId") String workspaceId,
                                    AsyncRequestLoader loader,
                                    AsyncRequestFactory asyncRequestFactory) {
        this.asyncRequestFactory = asyncRequestFactory;
        this.baseUrl = baseUrl + "/builder/" + workspaceId;
        this.loader = loader;
    }

    /** {@inheritDoc} */
    @Override
    public void build(String projectName, AsyncRequestCallback<BuildTaskDescriptor> callback) {
        build(projectName, null, callback);
    }

    @Override
    public void build(String projectName, BuildOptions buildOptions, AsyncRequestCallback<BuildTaskDescriptor> callback) {
        final String requestUrl = baseUrl + "/build";
        String params = "project=" + projectName;
        callback.setSuccessCodes(new int[]{200, 201, 202, 204, 207, 1223});
        asyncRequestFactory.createPostRequest(requestUrl + "?" + params, buildOptions)
                           .header(HTTPHeader.CONTENT_TYPE, MimeType.APPLICATION_JSON)
                           .send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void cancel(String buildId, AsyncRequestCallback<StringBuilder> callback) {
        final String requestUrl = baseUrl + "/cancel/" + buildId;
        asyncRequestFactory.createGetRequest(requestUrl).loader(loader)
                           .header(HTTPHeader.CONTENT_TYPE, MimeType.APPLICATION_JSON)
                           .send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void status(Link link, AsyncRequestCallback<String> callback) {
        callback.setSuccessCodes(new int[]{200, 201, 202, 204, 207, 1223});
        asyncRequestFactory.createGetRequest(link.getHref())
                           .header(HTTPHeader.CONTENT_TYPE, MimeType.APPLICATION_JSON)
                           .send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void log(Link link, AsyncRequestCallback<String> callback) {
        asyncRequestFactory.createGetRequest(link.getHref()).loader(loader)
                           .header(HTTPHeader.CONTENT_TYPE, MimeType.APPLICATION_JSON)
                           .send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void result(String buildId, AsyncRequestCallback<String> callback) {
        final String requestUrl = baseUrl + "/result/" + buildId;
        callback.setSuccessCodes(new int[]{200, 201, 202, 204, 207, 1223});
        asyncRequestFactory.createGetRequest(requestUrl)
                           .header(HTTPHeader.CONTENT_TYPE, MimeType.APPLICATION_JSON)
                           .send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void getRegisteredServers(AsyncRequestCallback<Array<BuilderDescriptor>> callback) {
        final String requestUrl = baseUrl + "/builders";
        asyncRequestFactory.createGetRequest(requestUrl).loader(loader)
                           .header(HTTPHeader.CONTENT_TYPE, MimeType.APPLICATION_JSON)
                           .send(callback);
    }
}