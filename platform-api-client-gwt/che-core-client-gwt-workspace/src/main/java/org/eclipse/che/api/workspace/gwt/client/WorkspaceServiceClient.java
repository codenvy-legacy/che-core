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
import org.eclipse.che.ide.rest.AsyncRequestCallback;

import java.util.List;
import java.util.Map;

/**
 * GWT Client for Workspace Service.
 *
 * @author Roman Nikitenko
 */
public interface WorkspaceServiceClient {
    /**
     * Get workspace information by ID.
     *
     * @param wsId
     *         workspace's id
     * @param callback
     */
    public void getWorkspace(String wsId, AsyncRequestCallback<WorkspaceDescriptor> callback);

    /**
     * Get membership of current user in the given workspace.
     *
     * @param callback
     */
    public void getMembership(String wsId, AsyncRequestCallback<MemberDescriptor> callback);

    /**
     * Get memberships of current user.
     *
     * @param callback
     */
    public void getMemberships(AsyncRequestCallback<List<MemberDescriptor>> callback);

    /**
     * Update workspace
     *
     * @param callback
     */
    public void update(String wsId, WorkspaceUpdate update, AsyncRequestCallback<WorkspaceDescriptor> callback);

    /**
     * Update attributes of current workspace
     */
    public void updateAttributes(Map<String, String> attributes, AsyncRequestCallback<WorkspaceDescriptor> callback);
}
