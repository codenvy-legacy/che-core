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
package org.eclipse.che.api.core.model.machine;

/**
 * @author gazarenkov
 */
public interface MachineConfig {

    /**
     * Display name
     * @return
     */
    String getName();

    /**
     * From where to create this Machine
     * (Recipe/Snapshot)
     * @return
     */
    MachineSource getSource();

    /**
     * Is workspace bound to machine or not
     */
    boolean isDev();

    /**
     * Id of workspace this machine belongs to
     */
    String getWorkspaceId();

    /**
     * Machine type (i.e. "docker")
     */
    String getType();
}
