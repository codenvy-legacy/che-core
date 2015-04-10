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

    String getId();

    String getImageType();

    String getOwner();

    long getCreationDate();

    String getWorkspaceId();

    List<? extends ProjectBinding> getProjects();

    String getLabel();

    String getDescription();
}
