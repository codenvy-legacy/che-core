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
package org.eclipse.che.ide.api.event.project;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;

/**
 * Event fires when project is updated or configured.
 *
 * @author Vlad Zhukovskiy
 */
public class ProjectUpdatedEvent extends GwtEvent<ProjectUpdatedEvent.ProjectUpdatedHandler> {
    public interface ProjectUpdatedHandler extends EventHandler {
        void onProjectUpdated(ProjectUpdatedEvent event);
    }

    private static Type<ProjectUpdatedHandler> TYPE;

    public static Type<ProjectUpdatedHandler> getType() {
        if (TYPE == null) {
            TYPE = new Type<>();
        }
        return TYPE;
    }

    private ProjectDescriptor updatedProjectDescriptor;
    private String            path;

    public ProjectUpdatedEvent(String path, ProjectDescriptor updatedProjectDescriptor) {
        this.path = path;
        this.updatedProjectDescriptor = updatedProjectDescriptor;
    }

    public ProjectDescriptor getUpdatedProjectDescriptor() {
        return updatedProjectDescriptor;
    }

    public String getPath() {
        return path;
    }

    /** {@inheritDoc} */
    @Override
    public Type<ProjectUpdatedHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(ProjectUpdatedHandler handler) {
        handler.onProjectUpdated(this);
    }
}
