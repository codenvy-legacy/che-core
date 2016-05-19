/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.project.server;

import org.eclipse.che.api.core.notification.EventOrigin;

/**
 * Publish when new project created.
 *
 * @author Evgen Vidolob
 */
@EventOrigin("project")
public class ProjectCreatedEvent {
    private final String workspaceId;
    private final String projectPath;

    public ProjectCreatedEvent(String workspaceId, String projectPath) {
        this.workspaceId = workspaceId;
        this.projectPath = projectPath;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public String getProjectPath() {
        return projectPath;
    }

    @Override
    public String toString() {
        return "ProjectCreatedEvent{" +
               "workspaceId='" + workspaceId + '\'' +
               ", projectPath='" + projectPath + '\'' +
               '}';
    }
}
