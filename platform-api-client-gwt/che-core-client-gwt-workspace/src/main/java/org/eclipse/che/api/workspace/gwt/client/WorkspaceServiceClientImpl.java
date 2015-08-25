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

import org.eclipse.che.api.core.model.workspace.WorkspaceConfig;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.workspace.shared.dto.CommandDto;
import org.eclipse.che.api.workspace.shared.dto.EnvironmentDto;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.api.workspace.shared.dto.RuntimeWorkspaceDto;
import org.eclipse.che.api.workspace.shared.dto.UsersWorkspaceDto;

import java.util.List;

//TODO

/**
 * Implementation for {@link WorkspaceServiceClient}.
 *
 * @author Roman Nikitenko
 */
public class WorkspaceServiceClientImpl implements WorkspaceServiceClient {

    @Override
    public Promise<UsersWorkspaceDto> create(UsersWorkspaceDto newWorkspace, String account) {
        return null;
    }

    @Override
    public Promise<UsersWorkspaceDto> getUsersWorkspace(String wsId) {
        return null;
    }

    @Override
    public Promise<RuntimeWorkspaceDto> getRuntimeWorkspace(String wsId) {
        return null;
    }

    @Override
    public Promise<List<UsersWorkspaceDto>> getWorkspaces(Integer skip, Integer limit) {
        return null;
    }

    @Override
    public Promise<List<RuntimeWorkspaceDto>> getRuntimeWorkspaces(Integer skip, Integer limit) {
        return null;
    }

    @Override
    public Promise<UsersWorkspaceDto> update(String wsId, WorkspaceConfig newCfg) {
        return null;
    }

    @Override
    public Promise<Void> delete(String wsId) {
        return null;
    }

    @Override
    public Promise<UsersWorkspaceDto> startTemporary(WorkspaceConfig cfg, String accountId) {
        return null;
    }

    @Override
    public Promise<UsersWorkspaceDto> startById(String id, String envName) {
        return null;
    }

    @Override
    public Promise<UsersWorkspaceDto> startByName(String name, String envName) {
        return null;
    }

    @Override
    public Promise<Void> stop(String wsId) {
        return null;
    }

    @Override
    public Promise<UsersWorkspaceDto> addCommand(String wsId, CommandDto newCommand) {
        return null;
    }

    @Override
    public Promise<UsersWorkspaceDto> updateCommand(String wsId, CommandDto commandUpdate) {
        return null;
    }

    @Override
    public Promise<UsersWorkspaceDto> deleteCommand(String wsId, String commandName) {
        return null;
    }

    @Override
    public Promise<UsersWorkspaceDto> addEnvironment(String wsId, EnvironmentDto newEnv) {
        return null;
    }

    @Override
    public Promise<UsersWorkspaceDto> updateEnvironment(String wsId, EnvironmentDto environmentUpdate) {
        return null;
    }

    @Override
    public Promise<UsersWorkspaceDto> addEnvironment(String wsId, String envName) {
        return null;
    }

    @Override
    public Promise<UsersWorkspaceDto> addProject(String wsId, ProjectConfigDto newProject) {
        return null;
    }

    @Override
    public Promise<UsersWorkspaceDto> updateProject(String wsId, ProjectConfigDto newEnv) {
        return null;
    }

    @Override
    public Promise<UsersWorkspaceDto> deleteProject(String wsId, String projectName) {
        return null;
    }
}
