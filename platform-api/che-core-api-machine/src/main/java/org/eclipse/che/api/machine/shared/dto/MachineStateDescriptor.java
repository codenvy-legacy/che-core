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

/**
 * Describes created machine
 *
 * @author andrew00x
 * @author Alexander Garagatyi
 */
@DTO
public interface MachineStateDescriptor extends Machine, Hyperlinks {
    void setId(String id);

    MachineStateDescriptor withId(String id);

    void setType(String type);

    MachineStateDescriptor withType(String type);

    void setStatus(MachineStatus status);

    MachineStateDescriptor withStatus(MachineStatus status);

    void setOwner(String owner);

    MachineStateDescriptor withOwner(String owner);

    void setWorkspaceId(String workspaceId);

    MachineStateDescriptor withWorkspaceId(String workspaceId);

    /**
     * List of the project which are bound to machine
     */
    @Override
    List<ProjectBindingDescriptor> getProjects();

    void setProjects(List<ProjectBindingDescriptor> projects);

    MachineStateDescriptor withProjects(List<ProjectBindingDescriptor> projects);

    void setWorkspaceBound(boolean isWorkspaceBound);

    MachineStateDescriptor withWorkspaceBound(boolean isWorkspaceBound);

    void setDisplayName(String displayName);

    MachineStateDescriptor withDisplayName(String displayName);

    void setMemorySize(int memorySize);

    MachineStateDescriptor withMemorySize(int memorySize);

    @Override
    MachineStateDescriptor withLinks(List<Link> links);
}
