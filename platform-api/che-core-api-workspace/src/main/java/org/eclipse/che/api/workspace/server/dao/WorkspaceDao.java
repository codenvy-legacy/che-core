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
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;

import java.util.List;


/**
 * DAO interface offers means to perform CRUD operations with {@link Workspace} data.
 * Workspace DAO should secure referential integrity for dependent objects (such as workspace {@link Member}).
 * It is up to implementation to define policy for that, either:
 *  - not to let remove the object or
 *  - perform cascade deletion of dependent objects
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
     * Gets workspace from persistent layer by id.
     *
     * @param id
     *         workspace identifier
     * @return workspace POJO
     * @throws org.eclipse.che.api.core.NotFoundException
     *         when workspace doesn't exist
     */
    Workspace getById(String id) throws NotFoundException, ServerException;

    /**
     * Gets workspace from persistent layer by name.
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
}
