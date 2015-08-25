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
    String getDefaultEnvName();

    WorkspaceConfigDto withDefaultEnvName(String defaultEnvironment);

    @Override
    List<CommandDto> getCommands();

    WorkspaceConfigDto withCommands(List<CommandDto> commands);

    @Override
    List<ProjectConfigDto> getProjects();

    WorkspaceConfigDto withProjects(List<ProjectConfigDto> projects);

    @Override
    Map<String, EnvironmentDto> getEnvironments();

    WorkspaceConfigDto withEnvironments(Map<String, EnvironmentDto> environments);

    @Override
    Map<String, String> getAttributes();

    WorkspaceConfigDto withAttributes(Map<String, String> attributes);

    @Override
    WorkspaceConfigDto withLinks(List<Link> links);
}
