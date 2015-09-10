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

import javax.annotation.Nonnull;

/**
 * @author Vlad Zhukovskiy
 */
public interface HasStorablePath {

    public class StorablePath implements HasStorablePath {

        private String path;

        public StorablePath(String path) {
            this.path = path;
        }

        @Nonnull
        @Override
        public String getStorablePath() {
            return path;
        }
    }

    @Nonnull
    String getStorablePath();
}
