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
package org.eclipse.che.ide.api.app;

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.workspace.shared.dto.UsersWorkspaceDto;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

/**
 * Describes current state of application.
 * E.g. current project, current workspace and etc.
 *
 * @author Vitaly Parfonov
 */
@Singleton
public class AppContext {

    private UsersWorkspaceDto workspace;
    private CurrentProject    currentProject;
    private CurrentUser       currentUser;
    private String            devMachineId;

    private final List<CurrentProject> existingProjects;

    public AppContext() {
        existingProjects = new ArrayList<>();
    }

    public UsersWorkspaceDto getWorkspace() {
        return workspace;
    }

    public void setWorkspace(UsersWorkspaceDto workspace) {
        this.workspace = workspace;
    }

    /**
     * Returns {@link CurrentProject} instance that describes the project
     * that is currently opened or <code>null</code> if none opened.
     * <p>
     * Note that current project may also represent a project's module.
     *
     * @return opened project or <code>null</code> if none opened
     */
    public CurrentProject getCurrentProject() {
        return currentProject;
    }

    /**
     * Set the current project instance.
     * <p>
     * Should not be called directly as the current
     * project is managed by the core.
     */
    public void setCurrentProject(CurrentProject currentProject) {
        this.currentProject = currentProject;
    }

    /**
     * Returns current user.
     *
     * @return current user
     */
    public CurrentUser getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(CurrentUser currentUser) {
        this.currentUser = currentUser;
    }

    /** Returns ID of the developer machine (where workspace is bound). */
    public String getDevMachineId() {
        return devMachineId;
    }

    public void setDevMachineId(String id) {
        this.devMachineId = id;
    }

    /**
     * Add opened project to list of projects which are opened.
     *
     * @param createdProject
     *         project which will be created
     */
    public void addProject(CurrentProject createdProject) {
        if (!existingProjects.contains(createdProject)) {
            existingProjects.add(createdProject);
        }
    }

    /** Removes all existing projects. */
    public void removeProject(ProjectDescriptor removedProject) {
        for (CurrentProject currentProject : existingProjects) {
            if (currentProject.getProjectDescription().equals(removedProject)) {
                existingProjects.remove(currentProject);
            }
        }
    }

    public List<CurrentProject> getProjects() {
        return existingProjects;
    }
}
