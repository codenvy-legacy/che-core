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
package org.eclipse.che.api.factory.gwt.client;

import org.eclipse.che.api.factory.dto.Factory;
import org.eclipse.che.ide.MimeType;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.AsyncRequestFactory;
import org.eclipse.che.ide.rest.HTTPHeader;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.validation.constraints.NotNull;

/**
 * Implementation of {@link FactoryServiceClient} service.
 *
 * @author Vladyslav Zhukovskii
 */
@Singleton
public class FactoryServiceClientImpl implements FactoryServiceClient {
    private AsyncRequestFactory asyncRequestFactory;

    @Inject
    public FactoryServiceClientImpl(AsyncRequestFactory asyncRequestFactory) {
        this.asyncRequestFactory = asyncRequestFactory;
    }

    /** {@inheritDoc} */
    @Override
    public void getFactory(@NotNull String raw, @NotNull AsyncRequestCallback<Factory> callback) {
        StringBuilder url = new StringBuilder("/api/factory");
        url.append("/").append(raw).append("?").append("legacy=true");
        asyncRequestFactory.createGetRequest(url.toString()).header(HTTPHeader.ACCEPT, MimeType.APPLICATION_JSON)
                           .send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void getFactorySnippet(String factoryId, String type, AsyncRequestCallback<String> callback) {
        final String requestUrl = "/api/factory/" + factoryId + "/snippet?type=" + type;
        asyncRequestFactory.createGetRequest(requestUrl).header(HTTPHeader.ACCEPT, MimeType.TEXT_PLAIN).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void getFactoryJson(String workspaceId, String path, AsyncRequestCallback<Factory> callback) {
        final String requestUrl = "/api/factory/" + workspaceId + "/" + path;
        asyncRequestFactory.createGetRequest(requestUrl).header(HTTPHeader.ACCEPT, MimeType.APPLICATION_JSON).send(callback);
    }
}
