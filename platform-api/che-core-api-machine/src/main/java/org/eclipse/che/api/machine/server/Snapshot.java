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

import java.util.Collections;
import java.util.List;

/**
 * Saved state of {@link org.eclipse.che.api.machine.server.spi.Instance}.
 *
 * @author andrew00x
 */
public class Snapshot {
    private final String               id;
    private final String               type;
    private final ImageKey             imageKey;
    private final String               owner;
    private final long                 creationDate;
    private final String               workspaceId;
    private final List<ProjectBinding> projects;
    private final String               description;

    public Snapshot(String id,
                    String type,
                    ImageKey imageKey,
                    String owner,
                    long creationDate,
                    String workspaceId,
                    List<ProjectBinding> projects,
                    String description) {
        this.id = id;
        this.type = type;
        this.imageKey = imageKey;
        this.owner = owner;
        this.creationDate = creationDate;
        this.workspaceId = workspaceId;
        this.projects = Collections.unmodifiableList(projects);
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getImageType() {
        return type;
    }

    public ImageKey getImageKey() {
        return imageKey;
    }

    public String getOwner() {
        return owner;
    }

    public long getCreationDate() {
        return creationDate;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public List<ProjectBinding> getProjects() {
        return projects;
    }

    public String getDescription() {
        return description;
    }
}
