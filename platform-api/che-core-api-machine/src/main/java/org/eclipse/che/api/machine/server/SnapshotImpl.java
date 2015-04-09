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
    private String               id;
    private String               type;
    private ImageKey             imageKey;
    private String               owner;
    private long                 creationDate;
    private String               workspaceId;
    private List<ProjectBinding> projects;
    private String               description;
    private String               label;

    public SnapshotImpl() {
    }

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

    public void setId(String id) {
        this.id = id;
    }

    public SnapshotImpl withId(String id) {
        this.id = id;
        return this;
    }

    @Override
    public String getImageType() {
        return type;
    }

    public void setImageType(String imageType) {
        this.type = imageType;
    }

    public SnapshotImpl withImageType(String imageType) {
        this.type = imageType;
        return this;
    }

    public ImageKey getImageKey() {
        return imageKey;
    }

    public void setImageKey(ImageKey imageKey) {
        this.imageKey = imageKey;
    }

    public SnapshotImpl withImageKey(ImageKey imageKey) {
        this.imageKey = imageKey;
        return this;
    }

    @Override
    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public SnapshotImpl withOwner(String owner) {
        this.owner = owner;
        return this;
    }

    @Override
    public long getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(long creationDate) {
        this.creationDate = creationDate;
    }

    public SnapshotImpl withCreationDate(long creationDate) {
        this.creationDate = creationDate;
        return this;
    }

    @Override
    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public SnapshotImpl withWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
        return this;
    }

    @Override
    public List<ProjectBinding> getProjects() {
        return projects;
    }

    public void setProjects(List<ProjectBinding> projects) {
        this.projects = projects;
    }

    public SnapshotImpl withProjects(List<ProjectBinding> projects) {
        this.projects = projects;
        return this;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public SnapshotImpl withDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public SnapshotImpl withLabel(String label) {
        this.label = label;
        return this;
    }
}
