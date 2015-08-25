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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

/**
 * Describes current state of application.
 * E.g. current project, current workspace and etc.
 *
 * @author Vitaly Parfonov
 * @author Dmitry Shnurenko
 */
@Singleton
public class AppContext {

    private UsersWorkspaceDto workspace;
    private CurrentProject    currentProject;
    private CurrentUser       currentUser;
    //    private Factory             factory;
    private String            devMachineId;

    private final List<ProjectDescriptor> openedProjects;

    public AppContext() {
        openedProjects = new ArrayList<>();
    }

    /** Returns all opened projects. */
    public List<ProjectDescriptor> getOpenedProjects() {
        return openedProjects;
    }

    /**
     * Add opened project to list of projects which are opened.
     *
     * @param openedProject
     *         project which will be opened
     */
    public void addOpenedProject(@Nonnull ProjectDescriptor openedProject) {
        if (!openedProjects.contains(openedProject)) {
            openedProjects.add(openedProject);
        }
    }

    /** Removes all opened projects. */
    public void removeOpenedProject(@Nonnull String deletedProjectName) {
        for (ProjectDescriptor descriptor : openedProjects) {

            if (deletedProjectName.equals(descriptor.getName())) {
                openedProjects.remove(descriptor);
            }
        }
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
     * <p/>
     * Note that current project may also represent a project's module.
     *
     * @return opened project or <code>null</code> if none opened
     */
    @Nullable
    public CurrentProject getCurrentProject() {
        return currentProject;
    }

    /**
     * Set the current project instance.
     * <p/>
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
    @Nullable
    public String getDevMachineId() {
        return devMachineId;
    }

    public void setDevMachineId(String id) {
        this.devMachineId = id;
    }
}
