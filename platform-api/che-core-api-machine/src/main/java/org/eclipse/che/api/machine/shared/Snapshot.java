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
package org.eclipse.che.api.machine.shared;

import java.util.List;

/**
 * Represents saved state of a machine
 *
 * @author gazarenkov
 */
public interface Snapshot {

    /**
     * Unique identifier of snapshot
     */
    String getId();

    /**
     * Type of the instance implementation, e.g. docker
     */
    String getInstanceType();

    /**
     * Id of the user that is owner of the snapshot
     */
    String getOwner();

    /**
     * Creation date of the snapshot
     */
    long getCreationDate();

    /**
     * Id of the workspace that is bound to snapshot
     */
    String getWorkspaceId();

    List<? extends ProjectBinding> getProjects();

    String getLabel();

    /**
     * Description of the snapshot
     */
    String getDescription();
}
