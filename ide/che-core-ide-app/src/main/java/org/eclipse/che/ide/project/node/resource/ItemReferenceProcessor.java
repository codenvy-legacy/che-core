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
import org.eclipse.che.ide.api.project.node.HasStorablePath;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
    public Promise<ItemReference> rename(@Nullable final HasStorablePath parent, final @Nonnull HasDataObject<ItemReference> node, final @Nonnull String newName) {
        return AsyncPromiseHelper.createFromAsyncRequest(new RequestCall<ItemReference>() {
            @Override
            public void makeCall(final AsyncCallback<ItemReference> callback) {
                projectService.rename(node.getData().getPath(), newName, null, new AsyncRequestCallback<Void>() {
                    @Override
                    protected void onSuccess(Void result) {
                        String newPath = parent.getStorablePath() + "/" + newName;
                        projectService.getItem(newPath, new AsyncRequestCallback<ItemReference>(unmarshallerFactory.newUnmarshaller(ItemReference.class)) {
                            @Override
                            protected void onSuccess(ItemReference result) {
                                callback.onSuccess(result);
                            }

                            @Override
                            protected void onFailure(Throwable exception) {
                                callback.onFailure(exception);
                            }
                        });
                    }

                    @Override
                    protected void onFailure(Throwable exception) {
                        callback.onFailure(exception);
                    }
                });
            }
        });
    }
}
