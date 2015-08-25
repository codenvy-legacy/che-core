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
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.workspace.server.model.impl.UsersWorkspaceImpl;
import org.eclipse.che.api.workspace.server.spi.WorkspaceDao;

import javax.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

/**
 * TODO add local storage
 *
 * @author Eugene Voevodin
 */
@Singleton
public class LocalWorkspaceDaoImpl implements WorkspaceDao {

    private final Map<String, UsersWorkspaceImpl> workspaces = new HashMap<>();

    @Override
    public synchronized UsersWorkspaceImpl create(UsersWorkspaceImpl workspace) throws ConflictException, ServerException {
        if (workspaces.containsKey(workspace.getId())) {
            throw new ConflictException("Workspace with id " + workspace.getId() + " already exists");
        }
        if (find(workspace.getName(), workspace.getOwner()).isPresent()) {
            throw new ConflictException(format("Workspace with name %s and owner %s already exists",
                                               workspace.getName(),
                                               workspace.getOwner()));
        }
        workspace.setStatus(null);
        workspaces.put(workspace.getId(), workspace);
        return workspace;
    }

    @Override
    public synchronized UsersWorkspaceImpl update(UsersWorkspaceImpl workspace)
            throws NotFoundException, ConflictException, ServerException {
        if (!workspaces.containsKey(workspace.getId())) {
            throw new NotFoundException("Workspace with id " + workspace.getId() + " was not found");
        }
        workspace.setStatus(null);
        workspaces.put(workspace.getId(), workspace);
        return workspace;
    }

    @Override
    public synchronized void remove(String id) throws ConflictException, NotFoundException, ServerException {
        workspaces.remove(id);
    }

    @Override
    public synchronized UsersWorkspaceImpl get(String id) throws NotFoundException, ServerException {
        final UsersWorkspaceImpl workspace = workspaces.get(id);
        if (workspace == null) {
            throw new NotFoundException("Workspace with id " + id + " was not found");
        }
        return workspace;
    }

    @Override
    public synchronized UsersWorkspaceImpl get(String name, String owner) throws NotFoundException, ServerException {
        final Optional<UsersWorkspaceImpl> wsOpt = find(name, owner);
        if (!wsOpt.isPresent()) {
            throw new NotFoundException(format("Workspace with name %s and owner %s was not found", name, owner));
        }
        return wsOpt.get();
    }

    @Override
    public synchronized List<UsersWorkspaceImpl> getList(String owner) throws ServerException {
        return workspaces.values()
                         .stream()
                         .filter(ws -> ws.getOwner().equals(owner))
                         .collect(toList());
    }

    private Optional<UsersWorkspaceImpl> find(String name, String owner) {
        return workspaces.values()
                         .stream()
                         .filter(ws -> ws.getName().equals(name) && ws.getOwner().equals(owner))
                         .findFirst();
    }
}