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
package org.eclipse.che.ide.statepersistance.dto;

import org.eclipse.che.dto.shared.DTO;

import java.util.Map;

/**
 * DTO describes Codenvy application's state that may be saved/restored.
 *
 * @author Artem Zatsarynnyi
 */
@DTO
public interface AppState {

    /** Returns workspace id which was previously stopped. */
    String getRecentWorkspaceId();

    /**
     * Set stopped workspace id to app state
     *
     * @param workspaceId
     *         workspace id which will be saved to app state
     */
    void setRecentWorkspaceId(String workspaceId);

    /** Get recent project info. */
    RecentProject getRecentProject();

    void setRecentProject(RecentProject recentProject);

    /** Get the mapping of project's path to it's state. */
    Map<String, ProjectState> getProjects();

    void setProjects(Map<String, ProjectState> projects);

    AppState withProjects(Map<String, ProjectState> projects);
}