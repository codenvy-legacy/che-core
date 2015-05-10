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
package org.eclipse.che.api.workspace.gwt.client;

import org.eclipse.che.api.workspace.shared.dto.MemberDescriptor;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceDescriptor;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceUpdate;
import org.eclipse.che.ide.rest.RestContext;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.AsyncRequestFactory;
import org.eclipse.che.ide.rest.AsyncRequestLoader;

import com.google.gwt.user.client.Window;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.util.Map;

import static org.eclipse.che.ide.MimeType.APPLICATION_JSON;
import static org.eclipse.che.ide.rest.HTTPHeader.ACCEPT;
import static org.eclipse.che.ide.rest.HTTPHeader.CONTENT_TYPE;

/**
 * Implementation for {@link WorkspaceServiceClient}.
 *
 * @author Roman Nikitenko
 */
public class WorkspaceServiceClientImpl implements WorkspaceServiceClient {

    private final AsyncRequestLoader  loader;
    private final String              restContext;
    private final AsyncRequestFactory asyncRequestFactory;
    private final DtoFactory          dtoFactory;
    private final String              workspaceId;

    @Inject
    protected WorkspaceServiceClientImpl(AsyncRequestLoader loader,
                                         @RestContext String restContext,
                                         @Named("workspaceId") String workspaceId,
                                         AsyncRequestFactory asyncRequestFactory, DtoFactory dtoFactory) {
        this.loader = loader;
        this.restContext = restContext;
        this.asyncRequestFactory = asyncRequestFactory;
        this.dtoFactory = dtoFactory;
        this.workspaceId = workspaceId;
    }

    /** {@inheritDoc} */
    @Override
    public void getWorkspace(String wsId, AsyncRequestCallback<WorkspaceDescriptor> callback) {
        asyncRequestFactory.createGetRequest(restContext + "/workspace/" + wsId)
                           .loader(loader)
                           .header(ACCEPT, APPLICATION_JSON)
                           .send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void getMembership(String wsId, AsyncRequestCallback<MemberDescriptor> callback) {
        asyncRequestFactory.createGetRequest(restContext + "/workspace/" + wsId + "/membership")
                           .loader(loader)
                           .header(ACCEPT, APPLICATION_JSON)
                           .send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void getMemberships(AsyncRequestCallback<Array<MemberDescriptor>> callback) {
        asyncRequestFactory.createGetRequest(restContext + "/workspace/all")
                           .loader(loader, "Getting memberships")
                           .header(ACCEPT, APPLICATION_JSON)
                           .send(callback);
    }

    @Override
    public void update(String wsId, WorkspaceUpdate update, AsyncRequestCallback<WorkspaceDescriptor> callback) {
        asyncRequestFactory.createPostRequest(restContext + "/workspace/all/" + wsId, update)
                           .loader(loader, "Updating workspace")
                           .header(ACCEPT, APPLICATION_JSON)
                           .header(CONTENT_TYPE, APPLICATION_JSON)
                           .send(callback);
    }

    @Override
    public void updateAttributes(Map<String, String> attributes, AsyncRequestCallback<WorkspaceDescriptor> callback) {
        WorkspaceUpdate workspaceUpdate = dtoFactory.createDto(WorkspaceUpdate.class).withAttributes(attributes);

        asyncRequestFactory.createPostRequest(restContext + "/workspace/" + workspaceId, workspaceUpdate)
                           .loader(loader)
                           .header(ACCEPT, APPLICATION_JSON)
                           .send(callback);
    }
}
