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
package org.eclipse.che.api.workspace.server.spi;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.workspace.UsersWorkspace;
import org.eclipse.che.api.workspace.server.model.impl.UsersWorkspaceImpl;

import java.util.List;


/**
 * DAO interface offers means to perform CRUD operations with {@link org.eclipse.che.api.workspace.server.dao.Workspace} data.
 * Workspace DAO should secure referential integrity for dependent objects (such as workspace {@link org.eclipse.che.api.workspace.server.dao.Member}).
 * It is up to implementation to define policy for that, either:
 *  - not to let remove the object or
 *  - perform cascade deletion of dependent objects
 *
 *  @author Eugene Voevodin
 */

public interface WorkspaceDao {

    /**
     * Adds workspace to persistent layer.
     *
     * @param workspace
     *         POJO representation of workspace entity
     * @throws ServerException if other error occurs
     */
    UsersWorkspaceImpl create(UsersWorkspace workspace) throws ConflictException, ServerException;

    /**
     * Updates already present in persistent layer workspace.
     *
     * @param workspace
     *         POJO representation of workspace entity
     * @throws ServerException if other error occurs
     */
    UsersWorkspaceImpl update(UsersWorkspace workspace) throws NotFoundException, ConflictException, ServerException;

    /**
     * Removes workspace from persistent layer.
     *
     * @param id
     *         workspace identifier
     * @throws ServerException if other error occurs
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
     * @throws ServerException if other error occurs
     */
    UsersWorkspaceImpl get(String id) throws NotFoundException, ServerException;

    /**
     * Gets workspace from persistent layer by name.
     *
     * @param owner owner of workspace
     * @param name
     *         workspace identifier
     * @return workspace POJO
     * @throws org.eclipse.che.api.core.NotFoundException
     *         when workspace doesn't exist
     * @throws ServerException if other error occurs
     */
    UsersWorkspaceImpl get(String name, String owner) throws NotFoundException, ServerException;

    /**
     * Gets list of workspaces of specified user from persistent layer
     *
     * @param owner owner of workspaces
     * @return list of workspaces of the user
     * @throws ServerException if any error occurs
     */
    List<UsersWorkspaceImpl> getList(String owner) throws ServerException;
}
