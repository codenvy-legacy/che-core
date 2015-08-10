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

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.workspace.RuntimeWorkspace;
import org.eclipse.che.api.core.model.workspace.UsersWorkspace;

import java.util.List;

/**
 * @author Alexander Garagatyi
 */
public interface RuntimeWorkspaceRegistry {
    RuntimeWorkspace start(UsersWorkspace ws, String envName, boolean temp)
            throws ForbiddenException, NotFoundException, ServerException, ConflictException;

    void stop(String workspaceId) throws ForbiddenException, NotFoundException, ServerException;

    RuntimeWorkspace get(String workspaceId) throws NotFoundException, ServerException;

    List<RuntimeWorkspace> getList(String ownerId) throws ServerException;

//    boolean share(String id, String userId) throws ForbiddenException, NotFoundException, ServerException;
//    boolean unshare(String id, String userId) throws ForbiddenException, NotFoundException, ServerException;
}
