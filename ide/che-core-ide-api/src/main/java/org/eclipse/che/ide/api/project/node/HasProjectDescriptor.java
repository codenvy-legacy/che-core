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
package org.eclipse.che.ide.api.project.node;

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;

import javax.validation.constraints.NotNull;
import org.eclipse.che.commons.annotation.Nullable;

/**
 * @author Vlad Zhukovskiy
 */
public interface HasProjectDescriptor {

    HasProjectDescriptor EMPTY = new HasProjectDescriptor() {
        @NotNull
        @Override
        public ProjectDescriptor getProjectDescriptor() {
            return null;
        }

        @Override
        public void setProjectDescriptor(@NotNull ProjectDescriptor projectDescriptor) {

        }
    };

    @NotNull
    ProjectDescriptor getProjectDescriptor();

    void setProjectDescriptor(@NotNull ProjectDescriptor projectDescriptor);
}
