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
package org.eclipse.che.api.machine.shared.dto;

import org.eclipse.che.api.core.rest.shared.dto.Hyperlinks;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.machine.shared.Machine;
import org.eclipse.che.api.machine.shared.MachineStatus;
import org.eclipse.che.dto.shared.DTO;

import java.util.List;
import java.util.Map;

/**
 * Describes created machine
 *
 * @author andrew00x
 * @author Alexander Garagatyi
 */
@DTO
public interface MachineDescriptor extends Machine, Hyperlinks {
    void setId(String id);

    MachineDescriptor withId(String id);

    void setType(String type);

    MachineDescriptor withType(String type);

    void setStatus(MachineStatus status);

    MachineDescriptor withStatus(MachineStatus status);

    void setOwner(String owner);

    MachineDescriptor withOwner(String owner);

    void setWorkspaceId(String workspaceId);

    MachineDescriptor withWorkspaceId(String workspaceId);

    @Override
    List<ProjectBindingDescriptor> getProjects();

    void setProjects(List<ProjectBindingDescriptor> projects);

    MachineDescriptor withProjects(List<ProjectBindingDescriptor> projects);

    /**
     * Implementation specific information about machine
     */
    Map<String, String> getMetadata();

    void setMetadata(Map<String, String> metadata);

    MachineDescriptor withMetadata(Map<String, String> metadata);

    void setServers(Map<String, ServerDescriptor> exposedPorts);

    MachineDescriptor withServers(Map<String, ServerDescriptor> exposedPorts);

    void setWorkspaceBound(boolean isWorkspaceBound);

    MachineDescriptor withWorkspaceBound(boolean isWorkspaceBound);

    void setDisplayName(String displayName);

    MachineDescriptor withDisplayName(String displayName);

    void setMemorySize(int mem);

    MachineDescriptor withMemorySize(int mem);

    @Override
    MachineDescriptor withLinks(List<Link> links);
}
