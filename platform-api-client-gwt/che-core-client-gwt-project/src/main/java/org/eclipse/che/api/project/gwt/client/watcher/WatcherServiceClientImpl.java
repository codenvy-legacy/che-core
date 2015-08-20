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
package org.eclipse.che.api.project.gwt.client.watcher;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;

import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.callback.AsyncPromiseHelper;
import org.eclipse.che.ide.rest.AsyncRequestFactory;
import org.eclipse.che.ide.rest.RestContext;

import javax.annotation.Nonnull;

import static org.eclipse.che.api.promises.client.callback.PromiseHelper.newCallback;
import static org.eclipse.che.api.promises.client.callback.PromiseHelper.newPromise;

/**
 * The class contains business logic which allows to send request to
 * {@link org.eclipse.che.api.project.server.watcher.WatcherService}
 *
 * @author Dmitry Shnurenko
 */
final class WatcherServiceClientImpl implements WatcherServiceClient {

    private final AsyncRequestFactory asyncRequestFactory;
    private final String              baseUrl;

    @Inject
    public WatcherServiceClientImpl(@RestContext String restContext, AsyncRequestFactory asyncRequestFactory) {
        this.asyncRequestFactory = asyncRequestFactory;

        this.baseUrl = restContext + "/watcher/";
    }

    /** {@inheritDoc} */
    @Override
    public Promise<Void> registerRecursiveWatcher(@Nonnull final String workspaceId) {
        return newPromise(new AsyncPromiseHelper.RequestCall<Void>() {
            @Override
            public void makeCall(AsyncCallback<Void> callback) {
                String url = baseUrl + "register/" + workspaceId;

                asyncRequestFactory.createGetRequest(url).send(newCallback(callback));
            }
        });
    }
}
