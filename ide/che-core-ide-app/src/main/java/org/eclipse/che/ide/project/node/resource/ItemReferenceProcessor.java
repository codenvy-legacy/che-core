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
package org.eclipse.che.ide.project.node.resource;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.callback.AsyncPromiseHelper;
import org.eclipse.che.api.promises.client.callback.AsyncPromiseHelper.RequestCall;
import org.eclipse.che.ide.api.project.node.HasDataObject;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;

import javax.annotation.Nonnull;

/**
 * @author Vlad Zhukovskiy
 */
public class ItemReferenceProcessor extends AbstractResourceProcessor<ItemReference> {
    @Inject
    public ItemReferenceProcessor(EventBus eventBus,
                                  ProjectServiceClient projectService,
                                  DtoUnmarshallerFactory unmarshallerFactory) {
        super(eventBus, projectService, unmarshallerFactory);
    }

    @Override
    public Promise<ItemReference> delete(@Nonnull final HasDataObject<ItemReference> node) {
//        return AsyncPromiseHelper.createFromAsyncRequest(createDeleteRequest(node.getData().getPath()))
//                                 .thenPromise(returnSelf(node.getData()));

        return AsyncPromiseHelper.createFromAsyncRequest(new RequestCall<ItemReference>() {
            @Override
            public void makeCall(final AsyncCallback<ItemReference> callback) {
                projectService.delete(node.getData().getPath(), new AsyncRequestCallback<Void>() {
                    @Override
                    protected void onSuccess(Void result) {
                        callback.onSuccess(node.getData());
                    }

                    @Override
                    protected void onFailure(Throwable exception) {
                        callback.onFailure(exception);
                    }
                });
            }
        });
    }

    @Override
    public Promise<ItemReference> rename(@Nonnull HasDataObject<ItemReference> node, @Nonnull String newName) {
        return null;
    }

    private <T> RequestCall<T> createRequestCall() {
        return new RequestCall<T>() {
            @Override
            public void makeCall(AsyncCallback<T> callback) {

            }
        };
    }

    private RequestCall<Void> createDeleteRequest(final String path) {
        return new RequestCall<Void>() {
            @Override
            public void makeCall(AsyncCallback<Void> callback) {
                projectService.delete(path, _createCallback(callback, unmarshallerFactory.newUnmarshaller(Void.class)));
            }
        };
    }
}
