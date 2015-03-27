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
package org.eclipse.che.api.project.gwt.client;

import org.eclipse.che.api.project.shared.dto.ImportProject;
import org.eclipse.che.api.project.shared.dto.ImportResponse;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.project.shared.dto.NewProject;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.project.shared.dto.ProjectReference;
import org.eclipse.che.api.project.shared.dto.ProjectUpdate;
import org.eclipse.che.api.project.shared.dto.RunnerEnvironmentTree;
import org.eclipse.che.api.project.shared.dto.TreeElement;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.rest.AsyncRequestCallback;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Client for Project service.
 *
 * @author Vitaly Parfonov
 * @author Artem Zatsarynnyy
 */
public interface ProjectServiceClient {

    /**
     * Get all projects in current workspace.
     *
     * @param callback
     *         the callback to use for the response
     */
    public void getProjects(AsyncRequestCallback<Array<ProjectReference>> callback);

    /**
     * Get all projects in specific workspace.
     *
     * @param callback
     *         the callback to use for the response
     */
    public void getProjectsInSpecificWorkspace(String wsId, AsyncRequestCallback<Array<ProjectReference>> callback);

    /**
     * Clone project from some workspace.
     *
     * @param callback
     *         the callback to use for the response
     */
    public void cloneProjectToCurrentWorkspace(String srcWorkspaceId, String srcProjectPath, String newNameForProject,
                                               AsyncRequestCallback<String> callback);

    /**
     * Get project.
     *
     * @param path
     *         path to the project to get
     * @param callback
     *         the callback to use for the response
     */
    public void getProject(String path, AsyncRequestCallback<ProjectDescriptor> callback);

    /**
     * Create project.
     *
     * @param name
     *         name of the project to create
     * @param newProject
     *         descriptor of the project to create
     * @param callback
     *         the callback to use for the response
     */
    public void createProject(String name, NewProject newProject, AsyncRequestCallback<ProjectDescriptor> callback);


    /**
     * Estimates if the folder supposed to be project of certain type.
     *
     * @param path
     *         path of the project to estimate
     * @param projectType
     *         Project Type ID to estimate against
     * @param callback
     *         the callback to use for the response
     */
    public void estimateProject(String path, String projectType, AsyncRequestCallback<Map<String, List<String>>> callback);

    /**
     * Get sub-project.
     *
     * @param path
     *         path to the parent project
     * @param callback
     *         the callback to use for the response
     */
    public void getModules(String path, AsyncRequestCallback<Array<ProjectDescriptor>> callback);

    /**
     * Create sub-project.
     *
     * @param parentProjectPath
     *         path to the parent project
     * @param name
     *         name of the module to create
     * @param newProject
     *         descriptor of the project to create
     * @param callback
     *         the callback to use for the response
     */
    public void createModule(String parentProjectPath, String name, NewProject newProject,
                             AsyncRequestCallback<ProjectDescriptor> callback);

    /**
     * Update project.
     *
     * @param path
     *         path to the project to get
     * @param descriptor
     *         descriptor of the project to update
     * @param callback
     *         the callback to use for the response
     * @deprecated use {@link #updateProject(String, ProjectUpdate, AsyncRequestCallback)} instead.
     */
    public void updateProject(String path, ProjectDescriptor descriptor, AsyncRequestCallback<ProjectDescriptor> callback);

    /**
     * Update project.
     *
     * @param path
     *         path to the project to get
     * @param descriptor
     *         descriptor of the project to update
     * @param callback
     *         the callback to use for the response
     */
    public void updateProject(String path, ProjectUpdate descriptor, AsyncRequestCallback<ProjectDescriptor> callback);

    /**
     * Create new file in the specified folder.
     *
     * @param parentPath
     *         path to parent for new file
     * @param name
     *         file name
     * @param content
     *         file content
     * @param contentType
     *         media type of file content
     * @param callback
     *         the callback to use for the response
     */
    public void createFile(String parentPath, String name, String content, String contentType,
                           AsyncRequestCallback<ItemReference> callback);

    /**
     * Get file content.
     *
     * @param path
     *         path to file
     * @param callback
     *         the callback to use for the response
     */
    public void getFileContent(String path, AsyncRequestCallback<String> callback);

    /**
     * Update file content.
     *
     * @param path
     *         path to file
     * @param content
     *         new content of file
     * @param contentType
     *         content media type
     * @param callback
     *         the callback to use for the response
     */
    public void updateFile(String path, String content, String contentType, AsyncRequestCallback<Void> callback);

    /**
     * Create new folder in the specified folder.
     *
     * @param path
     *         path to parent for new folder
     * @param callback
     *         the callback to use for the response
     */
    public void createFolder(String path, AsyncRequestCallback<ItemReference> callback);

    /**
     * Delete item.
     *
     * @param path
     *         path to item to delete
     * @param callback
     *         the callback to use for the response
     */
    public void delete(String path, AsyncRequestCallback<Void> callback);

    /**
     * Delete module.
     *
     * @param path
     *         path to module's parent
     * @param modulePath
     *         path to module to delete
     * @param callback
     *         the callback to use for the response
     */
    public void deleteModule(String path, String modulePath, AsyncRequestCallback<Void> callback);

    /**
     * Copy an item to the specified target path.
     *
     * @param path
     *         path to the item to copy
     * @param newParentPath
     *         path to the target item
     * @param callback
     *         the callback to use for the response
     */
    public void copy(String path, String newParentPath, AsyncRequestCallback<Void> callback);

    /**
     * Move an item to the specified target path.
     *
     * @param path
     *         path to the item to move
     * @param newParentPath
     *         path to the target item
     * @param callback
     *         the callback to use for the response
     */
    public void move(String path, String newParentPath, AsyncRequestCallback<Void> callback);

    /**
     * Rename and/or set new media type for item.
     *
     * @param path
     *         path to the item to rename
     * @param newName
     *         new name
     * @param newMediaType
     *         new media type. May be <code>null</code>
     * @param callback
     *         the callback to use for the response
     */
    public void rename(String path, String newName, @Nullable String newMediaType, AsyncRequestCallback<Void> callback);

    /**
     * Import sources into project.
     *
     * @param path
     *         path to the project to import sources
     * @param force
     *         set true for force rewrite existed project
     * @param importProject
     *         {@link ImportProject}
     * @param callback
     *         the callback to use for the response
     */
    public void importProject(String path, boolean force, ImportProject importProject, AsyncRequestCallback<ImportResponse> callback);

    /**
     * Get children for the specified path.
     *
     * @param path
     *         path to get its children
     * @param callback
     *         the callback to use for the response
     */
    public void getChildren(String path, AsyncRequestCallback<Array<ItemReference>> callback);

    /**
     * Get folders tree starts from the specified path.
     *
     * @param path
     *         path to get its folder tree
     * @param depth
     *         depth for discover children
     * @param callback
     *         the callback to use for the response
     */
    public void getTree(String path, int depth, AsyncRequestCallback<TreeElement> callback);

    /**
     * Search an item(s) by the specified criteria.
     *
     * @param expression
     *         search query expression
     * @param callback
     *         the callback to use for the response
     */
    public void search(QueryExpression expression, AsyncRequestCallback<Array<ItemReference>> callback);

    /**
     * Switch visibility(public/private) of the project represented by it's path.
     *
     * @param path
     *         path of the project to change visibility
     * @param visibility
     *         visibility to set
     * @param callback
     *         the callback to use for the response
     */
    public void switchVisibility(String path, String visibility, AsyncRequestCallback<Void> callback);

    /**
     * Get available project-scoped runner environments.
     *
     * @param path
     *         path to the project
     * @param callback
     *         the callback to use for the response
     */
    public void getRunnerEnvironments(String path, AsyncRequestCallback<RunnerEnvironmentTree> callback);
}
