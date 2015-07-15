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
package org.eclipse.che.api.project.server;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.project.server.handlers.ProjectHandlerRegistry;
import org.eclipse.che.api.project.server.type.AttributeValue;
import org.eclipse.che.api.project.server.type.ProjectTypeRegistry;
import org.eclipse.che.api.project.shared.dto.SourceEstimation;

import org.eclipse.che.api.vfs.server.VirtualFileSystemRegistry;
import com.google.inject.ImplementedBy;

import java.util.List;
import java.util.Map;

/**
 * A manager for codenvy projects.
 *
 * @author andrew00x
 *
 */
@ImplementedBy(DefaultProjectManager.class)
public interface ProjectManager {
    /**
     * Gets the list of projects in {@code workspace}.
     *
     * @param workspace
     *         id of workspace
     * @return the list of projects in specified workspace.
     * @throws ServerException
     *         if an error occurs
     */
    List<Project> getProjects(String workspace) throws ServerException, NotFoundException;

    /**
     * Gets single project by id of workspace and project's path in this workspace.
     *
     * @param workspace
     *         id of workspace
     * @param projectPath
     *         project's path
     * @return requested project or {@code null} if project was not found
     * @throws ForbiddenException
     *         if user which perform operation doesn't have access to the requested project
     * @throws ServerException
     *         if other error occurs
     */
    Project getProject(String workspace, String projectPath) throws ForbiddenException, ServerException, NotFoundException;

    /**
     *
     * Creates new project.
     *
     * @param workspace
     *         id of workspace
     * @param name
     *         project's name
     * @return newly created project
     * @throws ConflictException
     *         if operation causes conflict, e.g. name conflict if project with specified name already exists
     * @throws ForbiddenException
     *         if user which perform operation doesn't have required permissions
     * @throws ServerException
     *         if other error occurs
     */
    Project createProject(String workspace, String name, ProjectConfig projectConfig, Map<String, String> options,
                          String visibility)
            throws ConflictException, ForbiddenException, ServerException, ProjectTypeConstraintException, NotFoundException;

    /**
     * Gets root folder od project tree.
     *
     * @param workspace
     *         id of workspace
     * @return root folder
     * @throws ServerException
     *         if an error occurs
     */
    FolderEntry getProjectsRoot(String workspace) throws ServerException, NotFoundException;

    /**
     * Gets ProjectMisc.
     *
     * @param project
     *         project
     * @return ProjectMisc
     * @throws ServerException
     *         if an error occurs
     * @see ProjectMisc
     */
    ProjectMisc getProjectMisc(Project project) throws ServerException;

    /**
     * Gets ProjectMisc.
     *
     * @param project
     *         project
     * @param misc
     *         ProjectMisc
     * @throws ServerException
     *         if an error occurs
     * @see ProjectMisc
     */
    void saveProjectMisc(Project project, ProjectMisc misc) throws ServerException;


    /**
     *
     * @return VirtualFileSystemRegistry
     */
    VirtualFileSystemRegistry getVirtualFileSystemRegistry();

    /**
     *
     * @return ProjectTypeRegistry
     */
    ProjectTypeRegistry getProjectTypeRegistry();

    /**
     *
     * @return ProjectHandlerRegistry
     */
    ProjectHandlerRegistry getHandlers();


    Map<String, AttributeValue> estimateProject(String workspace, String path, String projectTypeId) throws
            ValueStorageException, ServerException, ForbiddenException, NotFoundException,
            ProjectTypeConstraintException;


    Project addModule(String workspace, String projectPath, String modulePath, ProjectConfig moduleConfig, Map<String,
            String> options, String visibility)
            throws ConflictException, ForbiddenException, ServerException, NotFoundException;

    List<SourceEstimation> resolveSources(String workspace, String path, boolean transientOnly) throws ServerException, ForbiddenException,
            NotFoundException, ValueStorageException, ProjectTypeConstraintException;

    Project convertFolderToProject(String workspace, String path, ProjectConfig projectConfig, String visibility)
            throws ConflictException, ForbiddenException, ServerException, NotFoundException;
}
