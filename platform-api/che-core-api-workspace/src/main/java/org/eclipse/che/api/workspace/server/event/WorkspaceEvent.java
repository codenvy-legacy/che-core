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
package org.eclipse.che.api.workspace.server.event;


import org.eclipse.che.api.workspace.shared.model.Workspace;

/** @author Sergii Leschenko */
public abstract class WorkspaceEvent {
    public static enum ChangeType {
        BEFORE_CREATE("before_create"),
        CREATED("created"),
        DELETED("deleted");

        private final String value;

        private ChangeType(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }

        @Override
        public String toString() {
            return value;
        }

    }

    private final ChangeType type;
    private final Workspace workspace;

    protected WorkspaceEvent(ChangeType type, Workspace workspace) {
        this.type = type;
        this.workspace = workspace;
    }

    public ChangeType getType() {
        return type;
    }

    public Workspace getWorkspace() {
        return workspace;
    }
}
