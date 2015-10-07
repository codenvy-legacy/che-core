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
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.machine.MachineConfig;
import org.eclipse.che.api.core.model.workspace.Environment;
import org.eclipse.che.api.core.model.workspace.Machine;
import org.eclipse.che.api.core.model.workspace.RuntimeWorkspace;
import org.eclipse.che.api.core.model.workspace.WorkspaceStatus;
import org.eclipse.che.api.core.model.workspace.UsersWorkspace;
import org.eclipse.che.api.workspace.server.model.impl.MachineImpl;
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
 * @implNote
 * Workspaces are stored in memory - in 2 Maps.
 * First for <i>identifier -> workspace</i> mapping, second for <i>owner -> list of workspaces</i> mapping.
 * Maps are guarded by {@link ReentrantReadWriteLock}.
 *
 * <p>It is thread-safe!
 *
 * @author Eugene Voevodin
 * @author Alexander Garagatyi
 */
@Singleton
public class RuntimeWorkspaceRegistry {

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

    /**
     * Starts {@link UsersWorkspace workspace} with specified environment.
     *
     * <p>Actually starts all machines in certain environment starting from dev-machine.
     * When environment is not specified - default one is going to be used.
     * During workspace starting workspace is visible with {@link WorkspaceStatus#STARTING starting} status,
     * until all machines in workspace is not started, after that status will be changed to {@link WorkspaceStatus#RUNNING running}.
     *
     * <p>Note that it doesn't provide any events for machines start, Machine API is responsible for it.
     *
     * @param usersWorkspace
     *         workspace which should be started
     * @param envName
     *         name of environment or null when default environment should be used
     * @return runtime view of {@code usersWorkspace} with status {@link WorkspaceStatus#RUNNING}
     * @throws ConflictException
     *         when workspace is already running or any other conflict error occurs during environment start
     * @throws BadRequestException
     *         when active environment is in inconsistent state
     * @throws NotFoundException
     *         whe any not found exception occurs during environment start
     * @throws ServerException
     *         when registry {@link #isStopped is stopped} other error occurs during environment start
     * @see MachineClient#start(MachineConfig, String, String)
     */
    public RuntimeWorkspaceImpl start(UsersWorkspace usersWorkspace, String envName) throws ConflictException,
                                                                                            ServerException,
                                                                                            BadRequestException,
                                                                                            NotFoundException {
        final String activeEnvName = firstNonNull(envName, usersWorkspace.getDefaultEnvName());
        RuntimeWorkspaceImpl runtimeWorkspace = RuntimeWorkspaceImpl.builder()
                                                                    .fromWorkspace(usersWorkspace)
                                                                    .setActiveEnvName(activeEnvName)
                                                                    .setStatus(STARTING)
                                                                    .build();
        final Environment environment = runtimeWorkspace.getEnvironments().get(activeEnvName);
        save(runtimeWorkspace);

        final List<MachineImpl> machines = startEnvironment(environment, runtimeWorkspace.getId());

        lock.writeLock().lock();
        try {
            runtimeWorkspace = get(runtimeWorkspace.getId());
            runtimeWorkspace.setDevMachine(findDev(machines));
            runtimeWorkspace.setMachines(machines);
            runtimeWorkspace.setStatus(RUNNING);
        } finally {
            lock.writeLock().unlock();
        }
        return runtimeWorkspace;
    }

    /**
     * Stops running workspace.
     *
     * <p>Actually stops all machines related to certain workspace one by one (order is not specified).
     * During workspace stopping workspace still will be accessible with {@link WorkspaceStatus#STOPPING stopping} status,
     * and invoking {@link #start(UsersWorkspace, String)} for the same workspace {@code workspaceId} will throw {@link ConflictException}.
     *
     * <p>Note that it doesn't provide any events for machines stop, Machine API is responsible for it.
     *
     * @param workspaceId
     *         identifier of workspace which should be stopped
     * @throws NotFoundException
     *         when workspace with specified identifier is not running
     * @throws ServerException
     *         when any error occurs during workspace stopping
     * @see MachineClient#destroy(String)
     */
    public void stop(String workspaceId) throws NotFoundException, ServerException, ConflictException {
        lock.writeLock().lock();
        final RuntimeWorkspaceImpl workspace;
        try {
            workspace = get(workspaceId);
            if (workspace.getStatus() != RUNNING) {
                throw new ConflictException("Can't stop " + workspace.getId() + " workspace, it is " + workspace.getStatus());
            }
            workspace.setStatus(STOPPING);
        } finally {
            lock.writeLock().unlock();
        }
        doStop(workspace);
    }

    /**
     * Returns true if workspace was started and it's status is {@link WorkspaceStatus#RUNNING running},
     * {@link WorkspaceStatus#STARTING starting} or {@link WorkspaceStatus#STOPPING stopping} - otherwise returns false.
     *
     * <p> Using of this method is equivalent to {@link #get(String)} + {@code try catch}, see example:
     * <pre>
     *
     *     if (!registry.isRunning("workspace123")) {
     *         doStuff("workspace123");
     *     }
     *
     *     //vs
     *
     *     try {
     *         registry.get("workspace123");
     *     } catch (NotFoundException ex) {
     *         doStuff("workspace123");
     *     }
     *
     * </pre>
     *
     * @param workspaceId
     *         workspace identifier to perform check
     * @return true if workspace is running, otherwise false
     */
    public boolean isRunning(String workspaceId) {
        lock.readLock().lock();
        try {
            return idToWorkspaces.containsKey(workspaceId);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns runtime view of {@link UsersWorkspace} if exists, throws {@link NotFoundException} otherwise.
     *
     * @param workspaceId
     *         workspace identifier to get runtime workspace
     * @return runtime view of {@link UsersWorkspace}
     * @throws NotFoundException
     *         when workspace with specified {@code workspaceId} was not found
     */
    public RuntimeWorkspaceImpl get(String workspaceId) throws NotFoundException {
        lock.readLock().lock();
        final RuntimeWorkspaceImpl runtimeWorkspace;
        try {
            runtimeWorkspace = idToWorkspaces.get(workspaceId);
            if (runtimeWorkspace == null) {
                throw new NotFoundException("Workspace with id " + workspaceId + " is not running.");
            }
        } finally {
            lock.readLock().unlock();
        }
        return runtimeWorkspace;
    }

    /**
     * Gets runtime workspaces owned by certain user.
     *
     * @param ownerId
     *         owner identifier
     * @return list of workspace owned by {@code ownerId} or empty list when user doesn't have any workspaces running
     */
    public List<RuntimeWorkspaceImpl> getByOwner(String ownerId) {
        lock.readLock().lock();
        try {
            return new ArrayList<>(ownerToWorkspaces.get(ownerId));
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Starts all environment machines, starting from dev machine.
     */
    List<MachineImpl> startEnvironment(Environment environment, String workspaceId) throws BadRequestException,
                                                                                           ServerException,
                                                                                           NotFoundException,
                                                                                           ConflictException {
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

        final List<MachineImpl> machines = new ArrayList<>();

        MachineConfig devMachine = findDev(environment.getMachineConfigs());
        if (devMachine == null) {
            throw new BadRequestException("Dev machine was not found in workspace environment " + environment.getName());
        }
        machines.add(machineClient.start(devMachine, workspaceId, environment.getName()));

        for (MachineConfig machineConfig : environment.getMachineConfigs()) {
            if (!machineConfig.isDev()) {
                try {
                    machines.add(machineClient.start(machineConfig, workspaceId, environment.getName()));
                } catch (ApiException apiEx) {
                    //TODO should it be error?
                    LOG.error(apiEx.getMessage(), apiEx);
                }
            }
        }

        return machines;
    }

    private void save(RuntimeWorkspaceImpl runtimeWorkspace) throws ConflictException, ServerException {
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

    /**
     * Stops workspace destroying all its machines and removing it from in memory storage.
     */
    private void doStop(RuntimeWorkspaceImpl workspace) throws NotFoundException, ServerException {
        //destroy all machines
        for (Machine machine : workspace.getMachines()) {
            machineClient.destroy(machine.getId());
        }

        lock.writeLock().lock();
        try {
            idToWorkspaces.remove(workspace.getId());
            ownerToWorkspaces.get(workspace.getOwner()).removeIf(ws -> ws.getId().equals(workspace.getId()));
        } finally {
            lock.writeLock().unlock();
        }
    }

    @PreDestroy
    private void stopWorkspaces() {
        lock.writeLock().lock();
        isStopped = true;
        try {
            for (RuntimeWorkspaceImpl workspace : idToWorkspaces.values()) {
                try {
                    doStop(workspace);
                } catch (NotFoundException | ServerException e) {
                    LOG.error(e.getLocalizedMessage(), e);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
}
