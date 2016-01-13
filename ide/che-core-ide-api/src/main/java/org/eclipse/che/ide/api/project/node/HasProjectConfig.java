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
package org.eclipse.che.ide.api.project.node;

import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;

import javax.validation.constraints.NotNull;

/**
 * @author Vlad Zhukovskiy
 */
public interface HasProjectConfig {

    HasProjectConfig EMPTY = new HasProjectConfig() {
        @Override
        public ProjectConfigDto getProjectConfig() {
            return null;
        }

        @Override
        public void setProjectConfig(@NotNull ProjectConfigDto projectConfig) {

        }
    };

    ProjectConfigDto getProjectConfig();

    void setProjectConfig(ProjectConfigDto projectConfig);
}
