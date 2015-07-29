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

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.machine.server.exception.MachineException;
import org.eclipse.che.api.machine.server.impl.MachineImpl;
import org.eclipse.che.api.machine.server.spi.Instance;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Holds active machines
 *
 * @author Alexander Garagatyi
 */
@Singleton
public class MachineRegistry {
    private final HashMap<String, Instance>    instances;
    private final HashMap<String, MachineImpl> states;

    public MachineRegistry() {
        instances = new HashMap<>();
        states = new HashMap<>();
    }

    //TODO return unmodifiable lists

    /**
     * Get all active machines
     */
    public synchronized List<MachineImpl> getStates() throws MachineException {
        final List<MachineImpl> list = new ArrayList<>(states.size() + instances.size());
        list.addAll(states.values());
        for (Instance instance : instances.values()) {
            list.add(getState(instance));
        }
        return list;
    }

    public synchronized MachineImpl getState(String machineId) throws NotFoundException, MachineException {
        MachineImpl state = states.get(machineId);
        if (state == null) {
            if (instances.get(machineId) == null) {
                throw new NotFoundException("Machine " + machineId + " is not found");
            }
            state = getState(instances.get(machineId));
        }

        return state;
    }

    /**
     * Get all machines that already were launched
     */
    public synchronized List<Instance> getMachines() throws MachineException {
        return new ArrayList<>(instances.values());
    }

    /**
     * Get machine by id
     *
     * @param machineId
     *         id of machine
     * @return machine with specified id
     * @throws NotFoundException
     *         if machine with specified id not found
     */
    public synchronized Instance get(String machineId) throws NotFoundException, MachineException {
        final Instance instance = instances.get(machineId);
        if (instance == null) {
            throw new NotFoundException("Machine " + machineId + " is not found");
        } else {
            return instance;
        }
    }

    public synchronized void add(Instance instance) throws MachineException, ConflictException {
        final Instance nullOrArgument = instances.put(instance.getId(), instance);
        if (nullOrArgument != null) {
            throw new ConflictException("Machine with id " + instance.getId() + " is already exist");
        }
    }

    public synchronized void add(MachineImpl machineState) throws MachineException, ConflictException {
        if (states.containsKey(machineState.getId())) {
            throw new ConflictException("Machine with id " + machineState.getId() + " is already exist");
        }
        states.put(machineState.getId(), machineState);
    }

    public synchronized void update(MachineImpl state) throws ConflictException, MachineException {
        if (!states.containsKey(state.getId())) {
            throw new ConflictException("Machine state " + state.getId() + " not found");
        } else {
            states.put(state.getId(), state);
        }
    }

    public synchronized void update(Instance instance) throws ConflictException, MachineException {
        if (!instances.containsKey(instance.getId()) && !states.containsKey(instance.getId())) {
            throw new ConflictException("Machine " + instance.getId() + " not found");
        } else {
            instances.put(instance.getId(), instance);
            states.remove(instance.getId());
        }
    }

    /**
     * Remove machine by id
     *
     * @param machineId
     *         id of machine that should be removed
     * @throws NotFoundException
     *         if machine with specified id not found
     */
    public synchronized void remove(String machineId) throws NotFoundException {
        final Instance instance = instances.remove(machineId);
        final MachineImpl state = states.remove(machineId);
        if (null == instance && null == state) {
            throw new NotFoundException("Machine " + machineId + " is not found");
        }
    }

    private MachineImpl getState(Instance instance) {
        return new MachineImpl(instance.getId(), instance.getType(), instance.getRecipe(), instance.getWorkspaceId(), instance.getOwner(),
                                instance.isWorkspaceBound(), instance.getDisplayName(), instance.getMemorySize(), instance.getStatus());
    }
}