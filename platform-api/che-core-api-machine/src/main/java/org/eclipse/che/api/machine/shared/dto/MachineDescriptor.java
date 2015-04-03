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
 * @author andrew00x
 */
@DTO
public interface MachineDescriptor extends Hyperlinks {
    String getId();

    void setId(String id);

    MachineDescriptor withId(String id);

    String getType();

    void setType(String type);

    MachineDescriptor withType(String type);

    MachineState getState();

    void setState(MachineState state);

    MachineDescriptor withState(MachineState state);

    String getOwner();

    void setOwner(String owner);

    MachineDescriptor withOwner(String owner);

    String getWorkspaceId();

    void setWorkspaceId(String workspaceId);

    MachineDescriptor withWorkspaceId(String workspaceId);

    List<ProjectBindingDescriptor> getProjects();

    void setProjects(List<ProjectBindingDescriptor> projects);

    MachineDescriptor withProjects(List<ProjectBindingDescriptor> projects);

    Map<String, String> getMetadata();

    void setMetadata(Map<String, String> metadata);

    MachineDescriptor withMetadata(Map<String, String> metadata);

    @Override
    MachineDescriptor withLinks(List<Link> links);
}
