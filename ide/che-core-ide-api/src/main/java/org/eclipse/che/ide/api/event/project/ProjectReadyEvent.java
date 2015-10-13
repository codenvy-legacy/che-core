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
 * Event that describes the fact that Project Action (opened/closing/closed) has been performed.
 *
 * @author Nikolay Zamosenchuk
 */
public class ProjectReadyEvent extends GwtEvent<ProjectReadyHandler> {

    /** Type class used to register this event. */
    public static Type<ProjectReadyHandler> TYPE = new Type<>();

    private final ProjectDescriptor project;

    /**
     * Create new {@link ProjectReadyEvent}.
     *
     * @param project
     *         an instance of affected project
     */
    protected ProjectReadyEvent(ProjectDescriptor project) {
        this.project = project;
    }

    /**
     * Creates a Project Opened Event.
     *
     * @param project
     *         opened project
     */
    public static ProjectReadyEvent createReadyEvent(ProjectDescriptor project) {
        return new ProjectReadyEvent(project);
    }

    @Override
    public Type<ProjectReadyHandler> getAssociatedType() {
        return TYPE;
    }

    /** @return the instance of affected project */
    public ProjectDescriptor getProject() {
        return project;
    }

    @Override
    protected void dispatch(ProjectReadyHandler handler) {
        handler.onProjectReady(this);
    }
}
