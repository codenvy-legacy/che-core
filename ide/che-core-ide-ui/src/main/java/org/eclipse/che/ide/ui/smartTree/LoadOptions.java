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
package org.eclipse.che.ide.ui.smartTree;

/**
 * @author Vlad Zhukovskiy
 */
public class LoadOptions {
    private boolean               dummyMode;
    private boolean               recursive;

    private LoadOptions(Builder builder) {
        this.dummyMode = builder.dummyMode;
        this.recursive = builder.recursive;
    }

    public boolean isDummyMode() {
        return dummyMode;
    }

    public boolean isRecursive() {
        return recursive;
    }

    public static class Builder {
        private boolean               dummyMode;
        private boolean               recursive;

        public Builder withDummyMode(boolean dummyMode) {
            this.dummyMode = dummyMode;

            return this;
        }

        public Builder withRecursive(boolean recursive) {
            this.recursive = recursive;

            return this;
        }

        public LoadOptions build() {
            return new LoadOptions(this);
        }
    }
}
