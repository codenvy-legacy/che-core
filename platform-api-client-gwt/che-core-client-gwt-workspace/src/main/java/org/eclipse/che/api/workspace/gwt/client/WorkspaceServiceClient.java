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
package org.eclipse.che.api.workspace.gwt.client;

import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.workspace.server.WorkspaceService;
import org.eclipse.che.api.workspace.shared.dto.CommandDto;
import org.eclipse.che.api.workspace.shared.dto.EnvironmentDto;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.api.workspace.shared.dto.RuntimeWorkspaceDto;
import org.eclipse.che.api.workspace.shared.dto.UsersWorkspaceDto;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceConfigDto;

import java.util.List;

/**
 * GWT Client for Workspace Service.
 *
 * @author Eugene Voevodin
 */
public interface WorkspaceServiceClient {

    /**
     * Creates new workspace.
     *
     * @see WorkspaceService#create(UsersWorkspaceDto, String)
     */
    Promise<UsersWorkspaceDto> create(UsersWorkspaceDto newWorkspace, String account);

    /**
     * Gets users workspace by id.
     *
     * @see WorkspaceService#getById(String)
     */
    Promise<UsersWorkspaceDto> getUsersWorkspace(String wsId);

    /**
     * Gets runtime workspace by id.
     *
     * @see WorkspaceService#getRuntimeWorkspaceById(String)
     */
    Promise<RuntimeWorkspaceDto> getRuntimeWorkspace(String wsId);

    /**
     * Gets all workspaces of current user.
     *
     * @see WorkspaceService#getWorkspaces(Integer, Integer)
     */
    Promise<List<UsersWorkspaceDto>> getWorkspaces(int skip, int limit);

    /**
     * Gets all runtime workspaces of current user.
     *
     * @see WorkspaceService#getRuntimeWorkspaces(Integer, Integer)
     */
    Promise<List<RuntimeWorkspaceDto>> getRuntimeWorkspaces(int skip, int limit);

    /**
     * Updates workspace.
     *
     * @see WorkspaceService#update(String, WorkspaceConfigDto)
     */
    Promise<UsersWorkspaceDto> update(String wsId, WorkspaceConfigDto newCfg);

    /**
     * Removes workspace.
     *
     * @see WorkspaceService#delete(String)
     */
    Promise<Void> delete(String wsId);

    /**
     * Starts temporary workspace based on given workspace configuration.
     *
     * @see WorkspaceService#startTemporary(WorkspaceConfigDto, String)
     */
    Promise<UsersWorkspaceDto> startTemporary(WorkspaceConfigDto cfg, String accountId);

    /**
     * Starts workspace based on workspace id and environment.
     *
     * @see WorkspaceService#startById(String, String)
     */
    Promise<UsersWorkspaceDto> startById(String id, String envName);

    /**
     * Starts workspace based on workspace name and environment.
     *
     * @see WorkspaceService#startByName(String, String)
     */
    Promise<UsersWorkspaceDto> startByName(String name, String envName);

    /**
     * Stops running workspace.
     *
     * @see WorkspaceService#stop(String)
     */
    Promise<Void> stop(String wsId);

    /**
     * Adds command to workspace
     *
     * @see WorkspaceService#addCommand(String, CommandDto)
     */
    Promise<UsersWorkspaceDto> addCommand(String wsId, CommandDto newCommand);

    /**
     * Updates command.
     *
     * @see WorkspaceService#updateCommand(String, CommandDto)
     */
    Promise<UsersWorkspaceDto> updateCommand(String wsId, CommandDto commandUpdate);

    /**
     * Removes command from workspace.
     *
     * @see WorkspaceService#deleteCommand(String, String)
     */
    Promise<UsersWorkspaceDto> deleteCommand(String wsId, String commandName);

    /**
     * Adds environment to workspace.
     *
     * @see WorkspaceService#addEnvironment(String, EnvironmentDto)
     */
    Promise<UsersWorkspaceDto> addEnvironment(String wsId, EnvironmentDto newEnv);

    /**
     * Updates environment.
     *
     * @see WorkspaceService#updateEnvironment(String, EnvironmentDto)
     */
    Promise<UsersWorkspaceDto> updateEnvironment(String wsId, EnvironmentDto environmentUpdate);

    /**
     * Removes environment.
     *
     * @see WorkspaceService#deleteEnvironment(String, String)
     */
    Promise<UsersWorkspaceDto> addEnvironment(String wsId, String envName);

    /**
     * Adds project configuration to workspace.
     *
     * @see WorkspaceService#addProject(String, ProjectConfigDto)
     */
    Promise<UsersWorkspaceDto> addProject(String wsId, ProjectConfigDto newProject);

    /**
     * Updates project configuration.
     *
     * @see WorkspaceService#updateProject(String wsId, ProjectConfigDto projectUpdate);
     */
    Promise<UsersWorkspaceDto> updateProject(String wsId, ProjectConfigDto newEnv);

    /**
     * Removes project from workspace.
     *
     * @see WorkspaceService#deleteProject(String, String)
     */
    Promise<UsersWorkspaceDto> deleteProject(String wsId, String projectName);
}
