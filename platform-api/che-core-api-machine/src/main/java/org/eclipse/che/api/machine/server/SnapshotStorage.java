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

import org.eclipse.che.api.core.ForbiddenException;
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
    SnapshotImpl getSnapshot(String snapshotId) throws NotFoundException, ServerException;

    void saveSnapshot(SnapshotImpl snapshot) throws ServerException, ForbiddenException;

    List<SnapshotImpl> findSnapshots(String owner, String workspaceId, ProjectBinding project) throws ServerException;

    void removeSnapshot(String snapshotId) throws NotFoundException, ServerException;
}
