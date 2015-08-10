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
package org.eclipse.che.api.workspace.server;

import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.machine.MachineConfig;
import org.eclipse.che.api.core.model.workspace.Environment;
import org.eclipse.che.api.core.model.workspace.Machine;
import org.eclipse.che.api.core.model.workspace.RuntimeWorkspace;
import org.eclipse.che.api.core.model.workspace.UsersWorkspace;
import org.eclipse.che.api.workspace.server.model.impl.RuntimeWorkspaceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.eclipse.che.api.core.model.workspace.WorkspaceStatus.RUNNING;
import static org.eclipse.che.api.core.model.workspace.WorkspaceStatus.STARTING;
import static org.eclipse.che.api.core.model.workspace.WorkspaceStatus.STOPPING;

/**
 * @author Alexander Garagatyi
 */
public class RuntimeWorkspaceRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(RuntimeWorkspaceRegistry.class);

    private final HashMap<String, RuntimeWorkspaceImpl>       runtimeWorkspacesById;
    private final HashMap<String, List<RuntimeWorkspaceImpl>> runtimeWorkspacesByOwner;
    private final ReadWriteLock                               lock;
    private final MachineClient                               machineClient;

    private boolean isStopped = false;

    @Inject
    public RuntimeWorkspaceRegistry(MachineClient machineClient) {
        this.machineClient = machineClient;
        this.runtimeWorkspacesById = new HashMap<>();
        this.runtimeWorkspacesByOwner = new HashMap<>();
        this.lock = new ReentrantReadWriteLock();
    }

    public RuntimeWorkspace start(UsersWorkspace userWorkspace, String envName)
            throws ForbiddenException, NotFoundException, ServerException, ConflictException, BadRequestException {

        final RuntimeWorkspaceImpl runtimeWorkspace = new RuntimeWorkspaceImpl(userWorkspace, null, envName);
        runtimeWorkspace.setStatus(STARTING);

        add(runtimeWorkspace);

        if (null != envName) {
            envName = runtimeWorkspace.getDefaultEnvName();
        }
        final Environment environment = runtimeWorkspace.getEnvironments().get(envName);
        final List<Machine> machines = startEnvironment(environment, userWorkspace.getId());

        runtimeWorkspace.setStatus(RUNNING);
        for (Machine machine : machines) {
            if (machine.isDev()) {
                runtimeWorkspace.setDevMachine(machine);
                break;
            }
        }
        runtimeWorkspace.setMachines(machines);

        return runtimeWorkspace;
    }

    private List<Machine> startEnvironment(Environment environment, String workspaceId) throws BadRequestException {
        /* todo replace with environments management
           for now we consider environment is:
           - recipe with type "docker"
           - recipe script will be ignored
           - list of docker machines configs
           - one of machines in this list is dev machine
        */
        String envRecipeType = environment.getRecipe() == null ? null : environment.getRecipe().getType();
        if (!"docker".equals(envRecipeType)) {
            throw new BadRequestException("Invalid environment recipe type " + envRecipeType);
        }

        final List<Machine> machines = new ArrayList<>();

        for (MachineConfig machineConfig : environment.getMachineConfigs()) {
            final Machine machine = machineClient.start(machineConfig, workspaceId);
            machines.add(machine);
        }

        return machines;
    }

    public void stop(String workspaceId) throws ForbiddenException, NotFoundException, ServerException {
        final RuntimeWorkspaceImpl runtimeWorkspace = get(workspaceId);

        runtimeWorkspace.setStatus(STOPPING);

        for (Machine machine : runtimeWorkspace.getMachines()) {
            machineClient.destroy(machine.getId());
        }

        remove(runtimeWorkspace);
    }

    public RuntimeWorkspaceImpl get(String workspaceId) throws NotFoundException {
        lock.readLock().lock();
        final RuntimeWorkspaceImpl runtimeWorkspace;
        try {
            runtimeWorkspace = runtimeWorkspacesById.get(workspaceId);
        } finally {
            lock.readLock().unlock();
        }
        if (null == runtimeWorkspace) {
            throw new NotFoundException("Workspace with id " + workspaceId + " is not running.");
        }
        return runtimeWorkspace;
    }

    public List<RuntimeWorkspaceImpl> getList(String ownerId) {
        lock.readLock().lock();
        List<RuntimeWorkspaceImpl> runtimeWorkspaces;
        try {
            runtimeWorkspaces = runtimeWorkspacesByOwner.get(ownerId);
        } finally {
            lock.readLock().unlock();
        }
        if (null == runtimeWorkspaces) {
            runtimeWorkspaces = Collections.emptyList();
        }
        return runtimeWorkspaces;
    }

    private void add(RuntimeWorkspaceImpl runtimeWorkspace) throws ConflictException, ServerException {
        final String wsId = runtimeWorkspace.getId();
        final String owner = runtimeWorkspace.getOwner();
        lock.writeLock().lock();
        if (isStopped) {
            throw new ServerException("Server is stopping. Can't start workspace.");
        }
        try {
            final RuntimeWorkspace currentEntry = runtimeWorkspacesById.get(wsId);
            if (currentEntry == null) {
                runtimeWorkspacesById.put(wsId, runtimeWorkspace);
                runtimeWorkspacesByOwner.putIfAbsent(owner, new LinkedList<RuntimeWorkspaceImpl>());
                runtimeWorkspacesByOwner.get(owner).add(runtimeWorkspace);
                return;
            }
        } finally {
            lock.writeLock().unlock();
        }
        throw new ConflictException("Workspace is not stopped to perform start operation.");
    }

    private void remove(RuntimeWorkspaceImpl runtimeWorkspace) {
        lock.writeLock().lock();
        try {
            runtimeWorkspacesById.remove(runtimeWorkspace.getId());
            runtimeWorkspacesByOwner.remove(runtimeWorkspace.getOwner());
        } finally {
            lock.writeLock().unlock();
        }
    }

    @PreDestroy
    private void cleanup() {
        isStopped = true;

        lock.writeLock().lock();
        try {
            for (RuntimeWorkspaceImpl runtimeWorkspace : runtimeWorkspacesById.values()) {
                try {
                    stop(runtimeWorkspace.getId());
                } catch (ForbiddenException | NotFoundException | ServerException e) {
                    LOG.error(e.getLocalizedMessage(), e);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
}
