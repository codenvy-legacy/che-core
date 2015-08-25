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

import org.eclipse.che.api.core.model.workspace.RuntimeWorkspace;
import org.eclipse.che.api.core.model.workspace.WorkspaceStatus;
import org.eclipse.che.api.core.rest.shared.dto.Hyperlinks;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.dto.shared.DTO;

import java.util.List;
import java.util.Map;

/**
 * @author Alexander Garagatyi
 */
@DTO
public interface RuntimeWorkspaceDto extends UsersWorkspaceDto, RuntimeWorkspace, Hyperlinks {
    @Override
    MachineDto getDevMachine();

    RuntimeWorkspaceDto withDevMachine(MachineDto devMachine);

    RuntimeWorkspaceDto withStatus(WorkspaceStatus status);

    @Override
    List<MachineDto> getMachines();

    void setMachines(List<MachineDto> machines);

    RuntimeWorkspaceDto withMachines(List<MachineDto> machines);

    @Override
    String getRootFolder();

    RuntimeWorkspaceDto withRootFolder(String rootFolder);

    @Override
    boolean isTemporary();

    RuntimeWorkspaceDto withTemporary(boolean temporary);

    @Override
    String getActiveEnvName();

    RuntimeWorkspaceDto withActiveEnvName(String currentEnvironment);

    @Override
    RuntimeWorkspaceDto withId(String id);

    @Override
    RuntimeWorkspaceDto withOwner(String owner);

    @Override
    RuntimeWorkspaceDto withName(String name);

    @Override
    RuntimeWorkspaceDto withDefaultEnvName(String defaultEnvironment);

    @Override
    List<CommandDto> getCommands();

    @Override
    RuntimeWorkspaceDto withCommands(List<CommandDto> commands);

    @Override
    List<ProjectConfigDto> getProjects();

    @Override
    RuntimeWorkspaceDto withProjects(List<ProjectConfigDto> projects);

    @Override
    Map<String, EnvironmentDto> getEnvironments();

    @Override
    RuntimeWorkspaceDto withEnvironments(Map<String, EnvironmentDto> environments);

    @Override
    RuntimeWorkspaceDto withAttributes(Map<String, String> attributes);

    @Override
    RuntimeWorkspaceDto withLinks(List<Link> links);


}
