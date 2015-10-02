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
package org.eclipse.che.ide.workspace.start;

import com.google.gwt.event.shared.EventHandler;

import org.eclipse.che.api.workspace.shared.dto.UsersWorkspaceDto;

/**
 * Provides method which is called when workspace started.
 *
 * @author Dmitry Shnurenko
 */
public interface StartWorkspaceHandler extends EventHandler {

    /**
     * Performs some actions when workspace started.
     *
     * @param workspace
     *         workspace which was started
     */
    void onWorkspaceStarted(UsersWorkspaceDto workspace);
}
