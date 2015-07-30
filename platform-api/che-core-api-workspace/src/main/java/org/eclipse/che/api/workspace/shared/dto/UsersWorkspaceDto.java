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
package org.eclipse.che.api.workspace.shared.dto;

import org.eclipse.che.api.core.model.workspace.UsersWorkspace;
import org.eclipse.che.api.core.rest.shared.dto.Hyperlinks;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.dto.shared.DTO;

import java.util.List;
import java.util.Map;

/**
 * @author andrew00x
 */
@DTO
public interface UsersWorkspaceDto extends UsersWorkspace, WorkspaceConfigDto, Hyperlinks {
    void setId(String id);

    UsersWorkspaceDto withId(String id);

    void setOwner(String owner);

    UsersWorkspaceDto withOwner(String owner);

    @Override
    UsersWorkspaceDto withName(String name);

    @Override
    UsersWorkspaceDto  withDefaultEnvironment(String defaultEnvironment);

    @Override
    List<CommandDto> getCommands();

    @Override
    UsersWorkspaceDto  withCommands(List<CommandDto> commands);

    @Override
    List<ProjectConfigDto> getProjects();

    @Override
    UsersWorkspaceDto  withProjects(List<ProjectConfigDto> projects);

    @Override
    Map<String, EnvironmentDto> getEnvironments();

    @Override
    UsersWorkspaceDto withEnvironments(Map<String, EnvironmentDto> environments);

    @Override
    UsersWorkspaceDto withAttributes(Map<String, String> attributes);

    @Override
    UsersWorkspaceDto withLinks(List<Link> links);
}
