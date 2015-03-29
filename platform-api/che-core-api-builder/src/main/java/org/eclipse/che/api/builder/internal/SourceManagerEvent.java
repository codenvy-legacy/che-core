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
package org.eclipse.che.api.builder.internal;

import java.io.File;

/**
 * @author andrew00x
 */
public class SourceManagerEvent {
    private final String    workspace;
    private final String    project;
    private final String    sourcesUrl;
    private final File      workDir;

    public SourceManagerEvent(String workspace, String project, String sourcesUrl, File workDir) {
        this.workspace = workspace;
        this.project = project;
        this.sourcesUrl = sourcesUrl;
        this.workDir = workDir;
    }

    public String getWorkspace() {
        return workspace;
    }

    public String getProject() {
        return project;
    }

    public String getSourcesUrl() {
        return sourcesUrl;
    }

    public File getWorkDir() {
        return workDir;
    }
}
