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
package org.eclipse.che.ide.api.event;

import com.google.gwt.event.shared.GwtEvent;

/**
 * Event for restoring a project tree state.
 *
 * @author Roman Nikitenko
 */
public class RestoreProjectTreeStateEvent extends GwtEvent<RestoreProjectTreeStateHandler> {

    /** Type class used to register this event. */
    public static Type<RestoreProjectTreeStateHandler> TYPE = new Type<>();

    /** The full path of project.*/
    private String fullProjectPath;

    /** Create new {@link RestoreProjectTreeStateEvent} for restoring a project tree state. */
    public RestoreProjectTreeStateEvent(String fullProjectPath) {
        this.fullProjectPath = fullProjectPath;
    }

    @Override
    public Type<RestoreProjectTreeStateHandler> getAssociatedType() {
        return TYPE;
    }

    /** @return the full path of project to restore */
    public String getProjectPath() {
        return fullProjectPath;
    }

    @Override
    protected void dispatch(RestoreProjectTreeStateHandler handler) {
        handler.onRestore(this);
    }
}
