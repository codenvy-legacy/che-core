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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import org.eclipse.che.api.core.ApiException;
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
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.google.common.base.MoreObjects.firstNonNull;
import static org.eclipse.che.api.core.model.workspace.WorkspaceStatus.RUNNING;
import static org.eclipse.che.api.core.model.workspace.WorkspaceStatus.STARTING;
import static org.eclipse.che.api.core.model.workspace.WorkspaceStatus.STOPPING;

/**
 * Defines {@link RuntimeWorkspace} internal API.
 *
 * @author Eugene Voevodin
 * @author Alexander Garagatyi
 */
@Singleton
public class RuntimeWorkspaceRegistry {

    //TODO add LOCK cache

    private static final Logger LOG = LoggerFactory.getLogger(RuntimeWorkspaceRegistry.class);

    private final Map<String, RuntimeWorkspaceImpl>          idToWorkspaces;
    private final ListMultimap<String, RuntimeWorkspaceImpl> ownerToWorkspaces;
    private final ReadWriteLock                              lock;
    private final MachineClient                              machineClient;

    private volatile boolean isStopped;

    @Inject
    public RuntimeWorkspaceRegistry(MachineClient machineClient) {
        this.machineClient = machineClient;
        this.idToWorkspaces = new HashMap<>();
        this.ownerToWorkspaces = ArrayListMultimap.create();
        this.lock = new ReentrantReadWriteLock();
    }

    public RuntimeWorkspace start(UsersWorkspace usersWs, String envName)
            throws ConflictException, ServerException, BadRequestException, NotFoundException {
        final RuntimeWorkspaceImpl runtimeWs = new RuntimeWorkspaceImpl(usersWs, null, envName);
        runtimeWs.setStatus(STARTING);

        add(runtimeWs);

        final Environment environment = runtimeWs.getEnvironments().get(firstNonNull(envName, runtimeWs.getDefaultEnvName()));

        final List<Machine> machines = startEnvironment(environment, runtimeWs.getId());
        runtimeWs.setDevMachine(findDev(machines));
        runtimeWs.setMachines(machines);
        runtimeWs.setStatus(RUNNING);
        return runtimeWs;
    }

    public void stop(String workspaceId) throws ForbiddenException, NotFoundException, ServerException {
        final RuntimeWorkspaceImpl runtimeWorkspace = get(workspaceId);

        runtimeWorkspace.setStatus(STOPPING);

        for (Machine machine : runtimeWorkspace.getMachines()) {
            machineClient.destroy(machine.getId());
        }

        remove(runtimeWorkspace);
    }

    public boolean isRunning(String workspaceId) {
        lock.readLock().lock();
        try {
            return idToWorkspaces.containsKey(workspaceId);
        } finally {
            lock.readLock().unlock();
        }
    }

    public RuntimeWorkspaceImpl get(String workspaceId) throws NotFoundException {
        lock.readLock().lock();
        final RuntimeWorkspaceImpl runtimeWorkspace;
        try {
            runtimeWorkspace = idToWorkspaces.get(workspaceId);
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
        try {
            return ownerToWorkspaces.get(ownerId);
        } finally {
            lock.readLock().unlock();
        }
    }

    List<Machine> startEnvironment(Environment environment, String workspaceId)
            throws BadRequestException, ServerException, NotFoundException, ConflictException {
        /* todo replace with environments management
           for now we consider environment is:
           - recipe with type "docker"
           - recipe script will be ignored
           - list of docker machines configs
           - one of machines in this list is dev machine
        */
        //FIXME "docker"
        String envRecipeType = environment.getRecipe() == null ? "docker" : environment.getRecipe().getType();
        if (!"docker".equals(envRecipeType)) {
            throw new BadRequestException("Invalid environment recipe type " + envRecipeType);
        }

        final List<Machine> machines = new ArrayList<>();

        MachineConfig devMachine = findDev(environment.getMachineConfigs());
        if (devMachine == null) {
            throw new BadRequestException("Dev machine was not found in workspace environment " + environment.getName());
        }
        machines.add(machineClient.start(devMachine, workspaceId));

        for (MachineConfig machineConfig : environment.getMachineConfigs()) {
            if (!machineConfig.isDev()) {
                try {
                    machines.add(machineClient.start(machineConfig, workspaceId));
                } catch (ApiException apiEx) {
                    //TODO should it be error?
                    LOG.error(apiEx.getMessage(), apiEx);
                }
            }
        }

        return machines;
    }

    private void add(RuntimeWorkspaceImpl runtimeWorkspace) throws ConflictException, ServerException {
        final String wsId = runtimeWorkspace.getId();
        final String owner = runtimeWorkspace.getOwner();
        lock.writeLock().lock();
        try {
            if (isStopped) {
                throw new ServerException("Server is stopping. Can't start workspace.");
            }
            if (idToWorkspaces.containsKey(wsId)) {
                throw new ConflictException("Workspace is not stopped to perform start operation.");
            }
            idToWorkspaces.put(wsId, runtimeWorkspace);
            ownerToWorkspaces.get(owner).add(runtimeWorkspace);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private <T extends MachineConfig> T findDev(List<T> machines) {
        for (T machine : machines) {
            if (machine.isDev()) {
                return machine;
            }
        }
        return null;
    }

    private void remove(RuntimeWorkspaceImpl runtimeWorkspace) {
        lock.writeLock().lock();
        try {
            idToWorkspaces.remove(runtimeWorkspace.getId());
            ownerToWorkspaces.get(runtimeWorkspace.getOwner()).removeIf(ws -> ws.getId().equals(runtimeWorkspace.getId()));
        } finally {
            lock.writeLock().unlock();
        }
    }

    @PreDestroy
    private void stopWorkspaces() {
        isStopped = true;

        lock.writeLock().lock();
        try {
            for (RuntimeWorkspaceImpl runtimeWorkspace : idToWorkspaces.values()) {
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
