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
package org.eclipse.che.api.project.server.type;

import org.eclipse.che.api.project.server.FolderEntry;

/**
 * Contains logic for detecting project type.
 *
 * @author Roman Nikitenko
 */
public interface ProjectTypeDetector {

    /**
     * Detects project type and creates {@link org.eclipse.che.api.project.server.ProjectConfig} for a project
     *
     * @param projectFolder
     *         base project folder
     * @return <code>true</code> if creation {@link org.eclipse.che.api.project.server.ProjectConfig} was successfully,
     *         <code>false</code> if failed to detect the project type
     */
    boolean detect(FolderEntry projectFolder);
}
