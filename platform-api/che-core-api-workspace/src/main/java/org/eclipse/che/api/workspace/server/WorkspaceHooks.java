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
package org.eclipse.che.api.workspace.server;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.workspace.WorkspaceConfig;

/**
 * Generic interface for methods called on particular workspace events if we some additional actions needed
 * The most common usecase - to register/unregister workspace in the Account
 *
 * @author gazarenkov
 */
public abstract class WorkspaceHooks {
    public static final WorkspaceHooks NOOP_WORKSPACE_HOOKS = new WorkspaceHooks() {};

    /**
     * Called before creating Workspace
     * @param workspace
     * @param accountId
     * @throws NotFoundException
     * @throws ServerException
     */
    public void beforeCreate(WorkspaceConfig workspace, String accountId) throws NotFoundException, ServerException {
    }

    /**
     * Called after creating Workspace
     * @param workspace
     * @param accountId
     * @throws NotFoundException
     * @throws ServerException
     */
    public void afterCreate(WorkspaceConfig workspace, String accountId) throws ServerException {
    }

    /**
     * Called after removing Workspace
     * @param workspace
     * @param accountId
     * @throws NotFoundException
     * @throws ServerException
     */
    public void afterRemove(String workspaceId) {
    }
}
