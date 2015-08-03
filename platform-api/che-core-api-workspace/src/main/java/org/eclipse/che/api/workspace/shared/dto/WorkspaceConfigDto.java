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

import org.eclipse.che.api.core.model.workspace.WorkspaceConfig;
import org.eclipse.che.api.core.rest.shared.dto.Hyperlinks;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.dto.shared.DTO;

import java.util.List;
import java.util.Map;

/**
 * @author andrew00x
 */
@DTO
public interface WorkspaceConfigDto extends WorkspaceConfig, Hyperlinks {

    @Override
    String getName();

    void setName(String name);

    WorkspaceConfigDto withName(String name);

    @Override
    String getDefaultEnvironment();

    void setDefaultEnvironment(String defaultEnvironment);

    WorkspaceConfigDto withDefaultEnvironment(String defaultEnvironment);

    @Override
    List<CommandDto> getCommands();

    void setCommands(List<CommandDto> commands);

    WorkspaceConfigDto withCommands(List<CommandDto> commands);

    @Override
    List<ProjectConfigDto> getProjects();

    void setProjects(List<ProjectConfigDto> projects);

    WorkspaceConfigDto withProjects(List<ProjectConfigDto> projects);

    @Override
    Map<String, EnvironmentDto> getEnvironments();

    @Override
    EnvironmentDto getEnvironment(String envId);

    void setEnvironments(Map<String, EnvironmentDto> environments);

    WorkspaceConfigDto withEnvironments(Map<String, EnvironmentDto> environments);

    @Override
    Map<String, String> getAttributes();

    void setAttributes(Map<String, String> attributes);

    WorkspaceConfigDto withAttributes(Map<String, String> attributes);

    @Override
    WorkspaceConfigDto withLinks(List<Link> links);
}
