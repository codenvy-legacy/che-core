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
package org.eclipse.che.api.local;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.workspace.server.dao.Workspace;
import org.eclipse.che.api.workspace.server.dao.WorkspaceDao;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

@Singleton
public class LocalWorkspaceDaoImpl implements WorkspaceDao {
    private static final Pattern WS_NAME = Pattern.compile("[\\w][\\w\\.\\-]{1,18}[\\w]");

    private final List<Workspace> workspaces;
    private final ReadWriteLock   lock;

    @Inject
    public LocalWorkspaceDaoImpl(@Named("codenvy.local.infrastructure.workspaces") Set<Workspace> workspaces) {
        this.workspaces = new LinkedList<>();
        lock = new ReentrantReadWriteLock();
        try {
            for (Workspace workspace : workspaces) {
                create(workspace);
            }
        } catch (Exception e) {
            // fail if can't validate this instance properly
            throw new RuntimeException(e);
        }
    }

    @Override
    public void create(Workspace workspace) throws ConflictException {
        validateWorkspaceName(workspace.getName());
        lock.writeLock().lock();
        try {
            for (Workspace w : workspaces) {
                if (w.getId().equals(workspace.getId())) {
                    throw new ConflictException(String.format("Workspace with id %s already exists.", workspace.getId()));
                }
                if (w.getName().equals(workspace.getName())) {
                    throw new ConflictException(String.format("Workspace with name %s already exists.", workspace.getName()));
                }
            }
            workspaces.add(new Workspace().withId(workspace.getId()).withName(workspace.getName()).withAccountId(workspace.getAccountId())
                                          .withAttributes(new LinkedHashMap<>(workspace.getAttributes()))
                                          .withTemporary(workspace.isTemporary()));
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void update(Workspace workspace) throws NotFoundException, ConflictException {
        validateWorkspaceName(workspace.getName());
        lock.writeLock().lock();
        try {
            Workspace myWorkspace = null;
            for (int i = 0, size = workspaces.size(); i < size && myWorkspace == null; i++) {
                if (workspaces.get(i).getId().equals(workspace.getId())) {
                    myWorkspace = workspaces.get(i);
                }
            }
            if (myWorkspace == null) {
                throw new NotFoundException(String.format("Workspace not found %s", workspace.getId()));
            }
            myWorkspace.setName(workspace.getName());
            myWorkspace.getAttributes().clear();
            myWorkspace.getAttributes().putAll(workspace.getAttributes());
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void remove(String id) throws NotFoundException {
        lock.writeLock().lock();
        try {
            Workspace myWorkspace = null;
            for (int i = 0, size = workspaces.size(); i < size && myWorkspace == null; i++) {
                if (workspaces.get(i).getId().equals(id)) {
                    myWorkspace = workspaces.get(i);
                }
            }
            if (myWorkspace == null) {
                throw new NotFoundException(String.format("Workspace not found %s", id));
            }
            workspaces.remove(myWorkspace);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Workspace getById(String id) throws NotFoundException {
        lock.readLock().lock();
        try {
            for (Workspace workspace : workspaces) {
                if (workspace.getId().equals(id)) {
                    return new Workspace().withId(workspace.getId()).withName(workspace.getName()).withAccountId(workspace.getAccountId())
                                          .withAttributes(new LinkedHashMap<>(workspace.getAttributes()))
                                          .withTemporary(workspace.isTemporary());
                }
            }
        } finally {
            lock.readLock().unlock();
        }
        throw new NotFoundException(String.format("Workspace not found %s", id));
    }

    @Override
    public Workspace getByName(String name) throws NotFoundException {
        lock.readLock().lock();
        try {
            for (Workspace workspace : workspaces) {
                if (workspace.getName().equals(name)) {
                    return new Workspace().withId(workspace.getId()).withName(workspace.getName()).withAccountId(workspace.getAccountId())
                                          .withAttributes(new LinkedHashMap<>(workspace.getAttributes()))
                                          .withTemporary(workspace.isTemporary());
                }
            }
        } finally {
            lock.readLock().unlock();
        }
        throw new NotFoundException(String.format("Workspace not found %s", name));
    }

    @Override
    public List<Workspace> getByAccount(String accountId) {
        final List<Workspace> result = new LinkedList<>();
        lock.readLock().lock();
        try {
            for (Workspace workspace : workspaces) {
                if (workspace.getAccountId().equals(accountId)) {
                    result.add(new Workspace().withId(workspace.getId()).withName(workspace.getName())
                                              .withAccountId(workspace.getAccountId())
                                              .withAttributes(new LinkedHashMap<>(workspace.getAttributes()))
                                              .withTemporary(workspace.isTemporary()));
                }
            }
        } finally {
            lock.readLock().unlock();
        }
        return result;
    }

    private void validateWorkspaceName(String workspaceName) throws ConflictException {
        if (workspaceName == null) {
            throw new ConflictException("Workspace name required");
        }
        if (!WS_NAME.matcher(workspaceName).matches()) {
            throw new ConflictException("Incorrect workspace name");
        }
    }
}