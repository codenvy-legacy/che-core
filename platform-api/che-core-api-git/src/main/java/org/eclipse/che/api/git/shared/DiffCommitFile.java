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

package org.eclipse.che.api.git.shared;

import org.eclipse.che.dto.shared.DTO;

/**
 * Created by I048384 on 24/05/2016.
 */

@DTO
public interface DiffCommitFile {
    /** @return get the file change type*/
    String getChangeType();
    /** set the file change type*/
    void setChangeType(String type);
    DiffCommitFile withChangeType(String type);

    /** @return get the file previous location*/
    String getOldPath();
    /** set the file previous location*/
    void setOldPath(String oldPath);
    DiffCommitFile withOldPath(String oldPath);

    /** @return get the file new location*/
    String getNewPath();
    /** set the file new location*/
    void setNewPath(String newPath);
    DiffCommitFile withNewPath(String newPath);

}
