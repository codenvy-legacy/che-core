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
package org.eclipse.che.api.machine.server;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.machine.shared.ProjectBinding;

import java.util.List;

/**
 * Stores metadata of snapshots
 *
 * @author andrew00x
 */
public interface SnapshotStorage {
    /**
     * Retrieve snapshot metadata by id
     *
     * @param snapshotId
     *         id of required snapshot
     * @return {@link SnapshotImpl} with specified id
     * @throws NotFoundException
     *         if snapshot with specified id not found
     * @throws ServerException
     *         if other error occurs
     */
    SnapshotImpl getSnapshot(String snapshotId) throws NotFoundException, ServerException;

    /**
     * Save snapshot metadata
     *
     * @param snapshot
     *         snapshot metadata to store
     * @throws ServerException
     *         if error occurs
     */
    void saveSnapshot(SnapshotImpl snapshot) throws ServerException;

    /**
     * Find snapshots by owner, workspace, project
     *
     * @param owner
     *         id of the owner of desired snapshot
     * @param workspaceId
     *         workspace specified in desired snapshot, optional
     * @param project
     *         project specified in desired snapshot, optional
     * @return list of snapshot that satisfy provided queries, or empty list if no desired snapshots found
     * @throws ServerException
     *         if error occurs
     */
    List<SnapshotImpl> findSnapshots(String owner, String workspaceId, ProjectBinding project) throws ServerException;

    /**
     * Remove snapshot by id
     *
     * @param snapshotId
     *         id of snapshot that should be removed
     * @throws NotFoundException
     *         if snapshot with specified id not found
     * @throws ServerException
     *         if other error occur
     */
    void removeSnapshot(String snapshotId) throws NotFoundException, ServerException;
}
