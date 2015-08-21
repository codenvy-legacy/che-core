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
import org.eclipse.che.api.project.shared.dto.ProjectReference;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.callback.AsyncPromiseHelper;
import org.eclipse.che.ide.api.project.node.HasDataObject;
import org.eclipse.che.ide.api.project.node.HasStorablePath;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * @author Vlad Zhukovskiy
 */
public class ProjectReferenceProcessor extends AbstractResourceProcessor<ProjectReference> {
    @Inject
    public ProjectReferenceProcessor(EventBus eventBus,
                                     ProjectServiceClient projectService,
                                     DtoUnmarshallerFactory unmarshallerFactory) {
        super(eventBus, projectService, unmarshallerFactory);
    }

    @Override
    public Promise<ProjectReference> delete(@Nonnull final HasDataObject<ProjectReference> node) {
        return AsyncPromiseHelper.createFromAsyncRequest(new AsyncPromiseHelper.RequestCall<ProjectReference>() {
            @Override
            public void makeCall(final AsyncCallback<ProjectReference> callback) {
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
    public Promise<ProjectReference> rename(@Nullable final HasStorablePath parent, @Nonnull final HasDataObject<ProjectReference> node, @Nonnull final String newName) {
        return AsyncPromiseHelper.createFromAsyncRequest(new AsyncPromiseHelper.RequestCall<ProjectReference>() {
            @Override
            public void makeCall(final AsyncCallback<ProjectReference> callback) {
                projectService.rename(node.getData().getPath(), newName, null, new AsyncRequestCallback<Void>() {
                    @Override
                    protected void onSuccess(Void result) {
                        projectService.getProjects(new AsyncRequestCallback<List<ProjectReference>>(unmarshallerFactory.newListUnmarshaller(ProjectReference.class)) {
                            @Override
                            protected void onSuccess(List<ProjectReference> projects) {
                                for (ProjectReference reference : projects) {
                                    if (newName.equals(reference.getName())) {
                                        callback.onSuccess(reference);
                                        return;
                                    }
                                }

                                callback.onFailure(new IllegalStateException("Failed to search renamed project"));
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
