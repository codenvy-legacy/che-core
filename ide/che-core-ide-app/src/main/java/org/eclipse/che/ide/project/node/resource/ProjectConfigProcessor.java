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
import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.callback.AsyncPromiseHelper;
import org.eclipse.che.api.promises.client.js.JsPromiseError;
import org.eclipse.che.api.promises.client.js.Promises;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.ide.api.event.project.DeleteProjectEvent;
import org.eclipse.che.ide.api.project.node.HasDataObject;
import org.eclipse.che.ide.api.project.node.HasStorablePath;
import org.eclipse.che.ide.project.node.ProjectNode;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;

import javax.validation.constraints.NotNull;

import static org.eclipse.che.api.promises.client.callback.PromiseHelper.newCallback;
import static org.eclipse.che.api.promises.client.callback.PromiseHelper.newPromise;

/**
 * @author Vlad Zhukovskiy
 * @author Dmitry Shnurenko
 */
public class ProjectConfigProcessor extends AbstractResourceProcessor<ProjectConfigDto> {

    @Inject
    public ProjectConfigProcessor(EventBus eventBus, ProjectServiceClient projectService, DtoUnmarshallerFactory unmarshallerFactory) {
        super(eventBus, projectService, unmarshallerFactory);
    }

    @Override
    public Promise<ProjectConfigDto> delete(@NotNull final HasDataObject<ProjectConfigDto> node) {
        if (node instanceof ProjectNode) {
            return newPromise(new AsyncPromiseHelper.RequestCall<Void>() {
                @Override
                public void makeCall(AsyncCallback<Void> callback) {
                    projectService.delete(node.getData().getPath(), newCallback(callback));
                }
            }).then(new Function<Void, ProjectConfigDto>() {
                @Override
                public ProjectConfigDto apply(Void arg) throws FunctionException {
                    eventBus.fireEvent(new DeleteProjectEvent(((ProjectNode)node).getProjectConfig()));
                    return node.getData();
                }
            });
        }

        return Promises.reject(JsPromiseError.create("Internal error"));
    }

    @Override
    public Promise<ProjectConfigDto> rename(final HasStorablePath parent,
                                            final HasDataObject<ProjectConfigDto> node,
                                            final String newName) {

        return Promises.reject(JsPromiseError.create(""));
    }
}
