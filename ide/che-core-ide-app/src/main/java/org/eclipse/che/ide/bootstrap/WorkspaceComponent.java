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

package org.eclipse.che.ide.bootstrap;

import org.eclipse.che.api.workspace.gwt.client.WorkspaceServiceClient;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceDescriptor;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.core.Component;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.util.Config;
import org.eclipse.che.ide.util.loging.Log;
import com.google.gwt.core.client.Callback;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author Evgen Vidolob
 */
@Singleton
public class WorkspaceComponent implements Component {

    private final WorkspaceServiceClient workspaceServiceClient;
    private final DtoUnmarshallerFactory dtoUnmarshallerFactory;
    private final AppContext             appContext;

    @Inject
    public WorkspaceComponent(WorkspaceServiceClient workspaceServiceClient, DtoUnmarshallerFactory dtoUnmarshallerFactory,
                              AppContext appContext) {
        this.workspaceServiceClient = workspaceServiceClient;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.appContext = appContext;
    }

    @Override
    public void start(final Callback<Component, Exception> callback) {
        AsyncRequestCallback<WorkspaceDescriptor> asyncRequestCallback = new AsyncRequestCallback<WorkspaceDescriptor>(
                dtoUnmarshallerFactory.newUnmarshaller(WorkspaceDescriptor.class)) {
            @Override
            protected void onSuccess(WorkspaceDescriptor result) {
                Config.setCurrentWorkspace(result);
                appContext.setWorkspace(result);
                callback.onSuccess(WorkspaceComponent.this);
            }

            @Override
            protected void onFailure(Throwable throwable) {
                Log.error(WorkspaceComponent.class, "Unable to get Workspace", throwable);
                callback.onFailure(new Exception("Unable to get Workspace", throwable));
            }
        };
        workspaceServiceClient.getWorkspace(Config.getWorkspaceId(), asyncRequestCallback);
    }
}
