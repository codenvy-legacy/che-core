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
import org.eclipse.che.api.machine.shared.MachineState;
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
public interface MachineDescriptor extends Hyperlinks {
    /**
     * Machine id
     */
    String getId();

    void setId(String id);

    MachineDescriptor withId(String id);

    /**
     * Type of machine implementation
     */
    String getType();

    void setType(String type);

    MachineDescriptor withType(String type);

    /**
     * Machine state
     */
    MachineState getState();

    void setState(MachineState state);

    MachineDescriptor withState(MachineState state);

    /**
     * Id of user that is owner of machine
     */
    String getOwner();

    void setOwner(String owner);

    MachineDescriptor withOwner(String owner);

    /**
     * Id of a workspace machine is bound to
     */
    String getWorkspaceId();

    void setWorkspaceId(String workspaceId);

    MachineDescriptor withWorkspaceId(String workspaceId);

    /**
     * List of the project which are bound to machine
     */
    List<ProjectBindingDescriptor> getProjects();

    void setProjects(List<ProjectBindingDescriptor> projects);

    MachineDescriptor withProjects(List<ProjectBindingDescriptor> projects);

    /**
     * Implementation specific information about machine
     */
    Map<String, String> getProperties();

    void setProperties(Map<String, String> metadata);

    MachineDescriptor withProperties(Map<String, String> metadata);

    /**
     * Port mapping for machine
     */
    Map<String, ServerDescriptor> getServers();

    void setServers(Map<String, ServerDescriptor> exposedPorts);

    MachineDescriptor withServers(Map<String, ServerDescriptor> exposedPorts);

    boolean isWorkspaceBound();

    void setWorkspaceBound(boolean isWorkspaceBound);

    String getDisplayName();

    void setDisplayName(String displayName);

    MachineDescriptor withDisplayName(String displayName);

    MachineDescriptor withWorkspaceBound(boolean isWorkspaceBound);

    @Override
    MachineDescriptor withLinks(List<Link> links);
}
