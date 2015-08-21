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
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.callback.AsyncPromiseHelper;
import org.eclipse.che.api.promises.client.js.JsPromiseError;
import org.eclipse.che.api.promises.client.js.Promises;
import org.eclipse.che.ide.api.event.CloseCurrentProjectEvent;
import org.eclipse.che.ide.api.project.node.HasDataObject;
import org.eclipse.che.ide.api.project.node.HasProjectDescriptor;
import org.eclipse.che.ide.api.project.node.HasStorablePath;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.project.node.ModuleDescriptorNode;
import org.eclipse.che.ide.project.node.ProjectDescriptorNode;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Vlad Zhukovskiy
 */
public class ProjectDescriptorProcessor extends AbstractResourceProcessor<ProjectDescriptor> {

    @Inject
    public ProjectDescriptorProcessor(EventBus eventBus, ProjectServiceClient projectService, DtoUnmarshallerFactory unmarshallerFactory) {
        super(eventBus, projectService, unmarshallerFactory);
    }

    @Override
    public Promise<ProjectDescriptor> delete(@Nonnull final HasDataObject<ProjectDescriptor> node) {
        if (node instanceof ProjectDescriptorNode) {
            return AsyncPromiseHelper.createFromAsyncRequest(new AsyncPromiseHelper.RequestCall<ProjectDescriptor>() {
                @Override
                public void makeCall(final AsyncCallback<ProjectDescriptor> callback) {
                    projectService.delete(node.getData().getPath(), new AsyncRequestCallback<Void>() {
                        @Override
                        protected void onSuccess(Void result) {
                            eventBus.fireEvent(new CloseCurrentProjectEvent());
                        }

                        @Override
                        protected void onFailure(Throwable exception) {
                            callback.onFailure(exception);
                        }
                    });
                }
            });

        } else if (node instanceof ModuleDescriptorNode) {
            Node parent = ((ModuleDescriptorNode)node).getParent();
            if (!(parent instanceof HasProjectDescriptor)) {
                return Promises.reject(JsPromiseError.create("Failed to search parent project descriptor"));
            }

            final String parentPath = ((HasProjectDescriptor)parent).getProjectDescriptor().getPath();
            final String modulePath = node.getData().getPath();

            final String relPath = modulePath.substring(modulePath.lastIndexOf(parentPath) + 1);

            return AsyncPromiseHelper.createFromAsyncRequest(new AsyncPromiseHelper.RequestCall<ProjectDescriptor>() {
                @Override
                public void makeCall(final AsyncCallback<ProjectDescriptor> callback) {
                    projectService.deleteModule(parentPath, relPath, new AsyncRequestCallback<Void>() {
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

        return Promises.reject(JsPromiseError.create("Internal error"));
    }

    @Override
    public Promise<ProjectDescriptor> rename(@Nullable final HasStorablePath parent, @Nonnull final HasDataObject<ProjectDescriptor> node, @Nonnull final String newName) {
        if (node instanceof ModuleDescriptorNode) {
            return AsyncPromiseHelper.createFromAsyncRequest(new AsyncPromiseHelper.RequestCall<ProjectDescriptor>() {
                @Override
                public void makeCall(final AsyncCallback<ProjectDescriptor> callback) {
                    projectService.rename(node.getData().getPath(), newName, null, new AsyncRequestCallback<Void>() {
                        @Override
                        protected void onSuccess(Void result) {
                            String newPath = parent.getStorablePath() + "/" + newName;
                            projectService.getProject(newPath, new AsyncRequestCallback<ProjectDescriptor>(
                                    unmarshallerFactory.newUnmarshaller(ProjectDescriptor.class)) {
                                @Override
                                protected void onSuccess(ProjectDescriptor result) {
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

        return Promises.reject(JsPromiseError.create(""));
    }
}
