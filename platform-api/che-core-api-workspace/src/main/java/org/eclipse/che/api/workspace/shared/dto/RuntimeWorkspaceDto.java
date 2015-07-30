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
package org.eclipse.che.api.workspace.shared.dto;

import org.eclipse.che.api.core.model.workspace.Machine;
import org.eclipse.che.api.core.model.workspace.RuntimeWorkspace;
import org.eclipse.che.api.core.rest.shared.dto.Hyperlinks;

/**
 * @author gazarenkov
 */
public interface RuntimeWorkspaceDto extends RuntimeWorkspace, UsersWorkspaceDto, Hyperlinks {
    void setDevMachine(Machine devMachine);

    RuntimeWorkspaceDto withDevMachine(Machine devMachine);

    void setTemporary(Boolean temporary);

    RuntimeWorkspaceDto withTemporary(Boolean temporary);

    void setRunning(Boolean running);

    RuntimeWorkspaceDto withRunning(Boolean running);

    void setRootFolder(String rootFolder);

    RuntimeWorkspaceDto withRootFolder(String rootFolder);
}
