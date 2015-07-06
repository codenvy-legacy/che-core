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
public interface MachineStateDescriptor extends Hyperlinks {
    /**
     * Machine id
     */
    String getId();

    void setId(String id);

    MachineStateDescriptor withId(String id);

    /**
     * Type of machine implementation
     */
    String getType();

    void setType(String type);

    MachineStateDescriptor withType(String type);

    /**
     * Machine state
     */
    MachineStatus getStatus();

    void setStatus(MachineStatus status);

    MachineStateDescriptor withStatus(MachineStatus status);

    /**
     * Id of user that is owner of machine
     */
    String getOwner();

    void setOwner(String owner);

    MachineStateDescriptor withOwner(String owner);

    /**
     * Id of a workspace machine is bound to
     */
    String getWorkspaceId();

    void setWorkspaceId(String workspaceId);

    MachineStateDescriptor withWorkspaceId(String workspaceId);

    /**
     * List of the project which are bound to machine
     */
    List<ProjectBindingDescriptor> getProjects();

    void setProjects(List<ProjectBindingDescriptor> projects);

    MachineStateDescriptor withProjects(List<ProjectBindingDescriptor> projects);

    boolean isWorkspaceBound();

    void setWorkspaceBound(boolean isWorkspaceBound);

    String getDisplayName();

    void setDisplayName(String displayName);

    MachineStateDescriptor withDisplayName(String displayName);

    MachineStateDescriptor withWorkspaceBound(boolean isWorkspaceBound);

    @Override
    MachineStateDescriptor withLinks(List<Link> links);
}
