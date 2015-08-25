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
    private final String            projectName;

    /**
     * Create new {@link ProjectActionEvent}.
     *
     * @param project
     *         an instance of affected project
     * @param projectAction
     *         the type of action
     */
    protected ProjectActionEvent(ProjectDescriptor project, String projectName, ProjectAction projectAction) {
        this.project = project;
        this.projectName = projectName;
        this.projectAction = projectAction;
    }

    /**
     * Creates a Project Opened Event.
     *
     * @param project
     *         opened project
     */
    public static ProjectActionEvent createProjectCreatedEvent(ProjectDescriptor project) {
        return new ProjectActionEvent(project, project.getName(), ProjectAction.CREATED);
    }

    /** Creates a Project Deleting Event. */
    public static ProjectActionEvent createProjectDeletedEvent(String projectName) {
        return new ProjectActionEvent(null, projectName, ProjectAction.DELETED);
    }

    @Override
    public Type<ProjectActionHandler> getAssociatedType() {
        return TYPE;
    }

    /** @return the instance of affected project */
    public ProjectDescriptor getProject() {
        return project;
    }

    /** Returns project name */
    public String getProjectName() {
        return projectName;
    }

    /** @return the type of action */
    public ProjectAction getProjectAction() {
        return projectAction;
    }

    @Override
    protected void dispatch(ProjectActionHandler handler) {
        switch (projectAction) {
            case CREATED:
                handler.onProjectCreated(this);
                break;
            case DELETED:
                handler.onProjectDeleted(this);
                break;
            default:
                break;
        }
    }

    /** Set of possible Project Actions */
    public static enum ProjectAction {
        CREATED, DELETED
    }
}
