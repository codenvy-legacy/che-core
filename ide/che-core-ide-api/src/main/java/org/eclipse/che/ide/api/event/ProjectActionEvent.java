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

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;

/**
 * Event that describes the fact that Project Action (opened/closing/closed) has been performed.
 *
 * @author Nikolay Zamosenchuk
 */
public class ProjectActionEvent extends GwtEvent<ProjectActionHandler> {

    /** Type class used to register this event. */
    public static Type<ProjectActionHandler> TYPE = new Type<>();
    private final ProjectDescriptor project;
    private final ProjectAction     projectAction;

    /**
     * Create new {@link ProjectActionEvent}.
     *
     * @param project
     *         an instance of affected project
     * @param projectAction
     *         the type of action
     */
    protected ProjectActionEvent(ProjectDescriptor project, ProjectAction projectAction) {
        this.project = project;
        this.projectAction = projectAction;
    }

    /**
     * Creates a Project Opened Event.
     *
     * @param project
     *         opened project
     */
    public static ProjectActionEvent projectCreatedEvent(ProjectDescriptor project) {
        return new ProjectActionEvent(project, ProjectAction.CREATE);
    }

    /**
     * Creates a Project Closed Event.
     *
     * @param project
     *         closed project
     */
    public static ProjectActionEvent projectDeletedEvent(ProjectDescriptor project) {
        return new ProjectActionEvent(project, ProjectAction.DELETE);
    }

    @Override
    public Type<ProjectActionHandler> getAssociatedType() {
        return TYPE;
    }

    /** @return the instance of affected project */
    public ProjectDescriptor getProject() {
        return project;
    }

    /** @return the type of action */
    public ProjectAction getProjectAction() {
        return projectAction;
    }

    @Override
    protected void dispatch(ProjectActionHandler handler) {
        switch (projectAction) {
            case CREATE:
                handler.onProjectCreated(this);
                break;
            case DELETE:
                handler.onProjectDeleted(this);
                break;
            default:
                break;
        }
    }

    /** Set of possible Project Actions */
    public enum ProjectAction {
        /**
         * Project opened in project explorer, but can be not fully initialized
         */
        CREATE,
        /**
         * Project closed
         */
        DELETE
    }
}
