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
package org.eclipse.che.api.machine.server;

import org.eclipse.che.api.machine.server.spi.ImageKey;
import org.eclipse.che.api.machine.shared.ProjectBinding;
import org.eclipse.che.api.machine.shared.Snapshot;

import java.util.Collections;
import java.util.List;

/**
 * Saved state of {@link org.eclipse.che.api.machine.server.spi.Instance}.
 *
 * @author andrew00x
 */
public class SnapshotImpl implements Snapshot {
    private final String               id;
    private final String               type;
    private final ImageKey             imageKey;
    private final String               owner;
    private final long                 creationDate;
    private final String               workspaceId;
    private final List<ProjectBinding> projects;
    private final String               description;
    private final String               label;

    public SnapshotImpl(String id,
                        String type,
                        ImageKey imageKey,
                        String owner,
                        long creationDate,
                        String workspaceId,
                        List<ProjectBinding> projects,
                        String description,
                        String label) {
        this.id = id;
        this.type = type;
        this.imageKey = imageKey;
        this.owner = owner;
        this.creationDate = creationDate;
        this.workspaceId = workspaceId;
        this.projects = Collections.unmodifiableList(projects);
        this.description = description;
        this.label = label;
    }
    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getImageType() {
        return type;
    }

    public ImageKey getImageKey() {
        return imageKey;
    }

    @Override
    public String getOwner() {
        return owner;
    }

    @Override
    public long getCreationDate() {
        return creationDate;
    }

    @Override
    public String getWorkspaceId() {
        return workspaceId;
    }

    @Override
    public List<ProjectBinding> getProjects() {
        return projects;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
