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
import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.callback.AsyncPromiseHelper;
import org.eclipse.che.api.promises.client.js.JsPromiseError;
import org.eclipse.che.api.promises.client.js.Promises;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.ide.api.event.project.DeleteProjectEvent;
import org.eclipse.che.ide.api.project.node.HasDataObject;
import org.eclipse.che.ide.api.project.node.HasProjectDescriptor;
import org.eclipse.che.ide.api.project.node.HasStorablePath;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.project.node.ModuleDescriptorNode;
import org.eclipse.che.ide.project.node.ProjectDescriptorNode;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;

import javax.validation.constraints.NotNull;

import static org.eclipse.che.api.promises.client.callback.PromiseHelper.newCallback;
import static org.eclipse.che.api.promises.client.callback.PromiseHelper.newPromise;

/**
 * @author Vlad Zhukovskiy
 */
public class ProjectDescriptorProcessor extends AbstractResourceProcessor<ProjectDescriptor> {

    @Inject
    public ProjectDescriptorProcessor(EventBus eventBus, ProjectServiceClient projectService, DtoUnmarshallerFactory unmarshallerFactory) {
        super(eventBus, projectService, unmarshallerFactory);
    }

    @Override
    public Promise<ProjectDescriptor> delete(@NotNull final HasDataObject<ProjectDescriptor> node) {
        if (node instanceof ProjectDescriptorNode) {
            return newPromise(new AsyncPromiseHelper.RequestCall<Void>() {
                @Override
                public void makeCall(AsyncCallback<Void> callback) {
                    projectService.delete(node.getData().getPath(), newCallback(callback));
                }
            }).then(new Function<Void, ProjectDescriptor>() {
                @Override
                public ProjectDescriptor apply(Void arg) throws FunctionException {
                    eventBus.fireEvent(new DeleteProjectEvent(((ProjectDescriptorNode)node).getProjectDescriptor()));
                    return node.getData();
                }
            });
        }

        return Promises.reject(JsPromiseError.create("Internal error"));
    }

    @Override
    public Promise<ProjectDescriptor> rename(@Nullable final HasStorablePath parent, @NotNull final HasDataObject<ProjectDescriptor> node,
                                             @NotNull final String newName) {

        return Promises.reject(JsPromiseError.create(""));
    }
}
