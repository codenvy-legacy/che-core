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

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;

/**
 * Event fires when project state has been restored.
 *
 * @author Vlad Zhukovskiy
 */
public class ProjectStateRestoredEvent extends GwtEvent<ProjectStateRestoredEvent.ProjectStateRestoredHandler> {

    public interface ProjectStateRestoredHandler extends EventHandler {
        void onProjectStateRestored(ProjectStateRestoredEvent event);
    }
    private static Type<ProjectStateRestoredHandler> TYPE;

    public static Type<ProjectStateRestoredHandler> getType() {
        if (TYPE == null) {
            TYPE = new Type<>();
        }
        return TYPE;
    }

    private ProjectDescriptor descriptor;

    public ProjectStateRestoredEvent(ProjectDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public ProjectDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public Type<ProjectStateRestoredHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(ProjectStateRestoredHandler handler) {
        handler.onProjectStateRestored(this);
    }
}
