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
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.callback.AsyncPromiseHelper;
import org.eclipse.che.api.promises.client.js.JsPromiseError;
import org.eclipse.che.api.promises.client.js.Promises;
import org.eclipse.che.api.workspace.shared.dto.ModuleConfigDto;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.ide.api.project.node.HasDataObject;
import org.eclipse.che.ide.api.project.node.HasProjectDescriptor;
import org.eclipse.che.ide.api.project.node.HasStorablePath;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.project.node.ModuleDescriptorNode;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;

import javax.validation.constraints.NotNull;

/**
 * @author Dmitry Shnurenko
 */
public class ModuleConfigProcessor extends AbstractResourceProcessor<ModuleConfigDto> {

    @Inject
    public ModuleConfigProcessor(EventBus eventBus, ProjectServiceClient projectService, DtoUnmarshallerFactory unmarshallerFactory) {
        super(eventBus, projectService, unmarshallerFactory);
    }

    @Override
    public Promise<ModuleConfigDto> delete(final HasDataObject<ModuleConfigDto> node) {
        if (node instanceof ModuleDescriptorNode) {
            Node parent = ((ModuleDescriptorNode)node).getParent();
            if (!(parent instanceof HasProjectDescriptor)) {
                return Promises.reject(JsPromiseError.create("Failed to search parent project descriptor"));
            }

            final String parentPath = ((HasProjectDescriptor)parent).getProjectDescriptor().getPath();
            final String modulePath = node.getData().getPath();

            final String relPath = modulePath.substring(parentPath.length() + 1);

            return AsyncPromiseHelper.createFromAsyncRequest(new AsyncPromiseHelper.RequestCall<ModuleConfigDto>() {
                @Override
                public void makeCall(final AsyncCallback<ModuleConfigDto> callback) {
                    projectService.deleteModule(parentPath, relPath, new AsyncRequestCallback<Void>() {
                        @Override
                        protected void onSuccess(Void result) {
                            deleteFolder(node, modulePath, callback);
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

    private void deleteFolder(final HasDataObject<ModuleConfigDto> node, String path, final AsyncCallback<ModuleConfigDto> callback) {
        projectService.delete(path, new AsyncRequestCallback<Void>() {
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

    @Override
    public Promise<ModuleConfigDto> rename(@Nullable HasStorablePath parent, @NotNull HasDataObject<ModuleConfigDto> node,
                                           @NotNull String newName) {
        return Promises.reject(JsPromiseError.create(""));
    }
}
