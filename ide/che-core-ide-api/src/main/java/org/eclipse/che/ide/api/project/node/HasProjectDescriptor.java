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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Vlad Zhukovskiy
 */
public interface HasProjectDescriptor {

    HasProjectDescriptor EMPTY = new HasProjectDescriptor() {
        @Nonnull
        @Override
        public ProjectDescriptor getProjectDescriptor() {
            return null;
        }

        @Override
        public void setProjectDescriptor(@Nonnull ProjectDescriptor projectDescriptor) {

        }
    };

    @Nonnull
    ProjectDescriptor getProjectDescriptor();

    void setProjectDescriptor(@Nonnull ProjectDescriptor projectDescriptor);
}
