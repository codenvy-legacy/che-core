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
import org.eclipse.che.api.machine.shared.Snapshot;
import org.eclipse.che.dto.shared.DTO;

import java.util.List;

/**
 * @author Alexander Garagatyi
 */
@DTO
public interface SnapshotDescriptor extends Hyperlinks {
    String getId();

    void setId(String id);

    SnapshotDescriptor withId(String id);

    String getOwner();

    void setOwner(String owner);

    SnapshotDescriptor withOwner(String owner);

    String getType();

    void setType(String type);

    SnapshotDescriptor withType(String type);

    String getDescription();

    void setDescription(String description);

    SnapshotDescriptor withDescription(String description);

    String getLabel();

    void setLabel(String label);

    SnapshotDescriptor withLabel(String label);

    long getCreationDate();

    void setCreationDate(long creationDate);

    SnapshotDescriptor withCreationDate(long creationDate);

    String getWorkspaceId();

    void setWorkspaceId(String workspaceId);

    SnapshotDescriptor withWorkspaceId(String workspaceId);

    List<ProjectBindingDescriptor> getProjects();

    void setProjects(List<ProjectBindingDescriptor> projects);

    SnapshotDescriptor withProjects(List<ProjectBindingDescriptor> projects);

    boolean isWorkspaceBound();

    void setWorkspaceBound(boolean workspaceBound);

    SnapshotDescriptor withWorkspaceBound(boolean workspaceBound);

    @Override
    SnapshotDescriptor withLinks(List<Link> links);
}
