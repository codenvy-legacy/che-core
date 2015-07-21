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
package org.eclipse.che.api.machine.server.impl;

import org.eclipse.che.api.machine.server.spi.InstanceKey;
import org.eclipse.che.api.machine.shared.ProjectBinding;
import org.eclipse.che.api.machine.shared.Snapshot;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Saved state of {@link org.eclipse.che.api.machine.server.spi.Instance}.
 *
 * @author andrew00x
 */
public class SnapshotImpl implements Snapshot {
    private String               id;
    private String               type;
    private String               script;
    private InstanceKey          instanceKey;
    private String               owner;
    private long                 creationDate;
    private String               workspaceId;
    private boolean              isWorkspaceBound;
    private List<ProjectBinding> projects;
    private String               description;

    public SnapshotImpl() {
    }

    public SnapshotImpl(String id,
                        String type,
                        String script,
                        InstanceKey instanceKey,
                        String owner,
                        long creationDate,
                        String workspaceId,
                        List<ProjectBinding> projects,
                        String description,
                        boolean isWorkspaceBound) {
        this.id = id;
        this.type = type;
        this.script = script;
        this.instanceKey = instanceKey;
        this.owner = owner;
        this.creationDate = creationDate;
        this.workspaceId = workspaceId;
        this.isWorkspaceBound = isWorkspaceBound;
        this.projects = Collections.unmodifiableList(projects);
        this.description = description;
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
    public String getType() {
        return type;
    }

    @Override
    public String getScript() {
        return script;
    }

    public void setType(String type) {
        this.type = type;
    }

    public SnapshotImpl withType(String type) {
        this.type = type;
        return this;
    }

    public InstanceKey getInstanceKey() {
        return instanceKey;
    }

    public void setInstanceKey(InstanceKey instanceKey) {
        this.instanceKey = instanceKey;
    }

    public SnapshotImpl withInstanceKey(InstanceKey instanceKey) {
        this.instanceKey = instanceKey;
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
    public boolean isWorkspaceBound() {
        return this.isWorkspaceBound;
    }

    public void setWorkspaceBound(boolean isWorkspaceBound) {
        this.isWorkspaceBound = isWorkspaceBound;
    }

    public SnapshotImpl withWorkspaceBound(boolean isWorkspaceBound) {
        this.isWorkspaceBound = isWorkspaceBound;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SnapshotImpl)) return false;
        SnapshotImpl snapshot = (SnapshotImpl)o;
        return Objects.equals(creationDate, snapshot.creationDate) &&
               Objects.equals(isWorkspaceBound, snapshot.isWorkspaceBound) &&
               Objects.equals(id, snapshot.id) &&
               Objects.equals(type, snapshot.type) &&
               Objects.equals(script, snapshot.script) &&
               Objects.equals(instanceKey, snapshot.instanceKey) &&
               Objects.equals(owner, snapshot.owner) &&
               Objects.equals(workspaceId, snapshot.workspaceId) &&
               Objects.equals(projects, snapshot.projects) &&
               Objects.equals(description, snapshot.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, script, instanceKey, owner, creationDate, workspaceId, isWorkspaceBound, projects, description);
    }
}
