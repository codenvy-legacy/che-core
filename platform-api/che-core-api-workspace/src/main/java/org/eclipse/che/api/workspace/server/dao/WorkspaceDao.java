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
package org.eclipse.che.api.workspace.server.dao;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;

import java.util.List;


/**
 * DAO interface offers means to perform CRUD operations with {@link Workspace} data. The
 * implementation is not
 * required
 * to be responsible for persistent layer data dto consistency. It simply transfers data from one layer to another,
 * so
 * if you're going to call any of implemented methods it is considered that all needed verifications are already done.
 * <p> <strong>Note:</strong> This particularly does not mean that method call will not make any inconsistency but this
 * mean that such kind of inconsistencies are expected by design and may be treated further. </p>
 */
public interface WorkspaceDao {
    /**
     * Adds workspace to persistent layer.
     *
     * @param workspace
     *         POJO representation of workspace entity
     */
    void create(Workspace workspace) throws ConflictException, ServerException;

    /**
     * Updates already present in persistent layer workspace.
     *
     * @param workspace
     *         POJO representation of workspace entity
     */
    void update(Workspace workspace) throws NotFoundException, ConflictException, ServerException;

    /**
     * Removes workspace from persistent layer.
     *
     * @param id
     *         workspace identifier
     */
    void remove(String id) throws ConflictException, NotFoundException, ServerException;

    /**
     * Gets workspace from persistent layer.
     *
     * @param id
     *         workspace identifier
     * @return workspace POJO
     * @throws org.eclipse.che.api.core.NotFoundException
     *         when workspace doesn't exist
     */
    Workspace getById(String id) throws NotFoundException, ServerException;

    /**
     * Gets workspace from persistent layer.
     *
     * @param name
     *         workspace identifier
     * @return workspace POJO
     * @throws org.eclipse.che.api.core.NotFoundException
     *         when workspace doesn't exist
     */
    Workspace getByName(String name) throws NotFoundException, ServerException;


    /**
     * Gets workspaces from persistent layer related to specified account.
     *
     * @param accountId
     *         account identifier
     * @return List of workspaces
     */
    List<Workspace> getByAccount(String accountId) throws ServerException;

    /**
     * Get all workspaces which are locked after RAM runner resources was exceeded.
     *
     * @return all locked workspaces
     */
    List<Workspace> getWorkspacesWithLockedResources() throws ServerException;
}
