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
 * An event that should be fired in order to open a project.
 *
 * @author Artem Zatsarynnyy
 */
public class OpenProjectEvent extends GwtEvent<OpenProjectHandler> {

    /** Type class used to register this event. */
    public static Type<OpenProjectHandler> TYPE = new Type<>();

    private final ProjectDescriptor descriptor;

    /**
     * Creates an event to initiate opening the specified project.
     *
     * @param descriptor
     *         name of the project to open
     */
    public OpenProjectEvent(ProjectDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    @Override
    public Type<OpenProjectHandler> getAssociatedType() {
        return TYPE;
    }

    /**
     * Returns descriptor of the project to open.
     *
     * @return descriptor of the project to open
     */
    public ProjectDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    protected void dispatch(OpenProjectHandler handler) {
        handler.onProjectOpened(this);
    }

}
