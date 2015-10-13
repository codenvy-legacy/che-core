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

import com.google.gwt.event.shared.GwtEvent;

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;

/**
 * The class store information about deleted project. This event should be fired when we delete project.
 *
 * @author Dmitry Shnurenko
 */
public class DeleteProjectEvent extends GwtEvent<DeleteProjectHandler> {

    public static Type<DeleteProjectHandler> TYPE = new Type<>();

    private final ProjectDescriptor descriptor;

    public DeleteProjectEvent(ProjectDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    /** Returns project descriptor. It contains information about project. */
    public ProjectDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public Type<DeleteProjectHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(DeleteProjectHandler handler) {
        handler.onProjectDeleted(this);
    }
}