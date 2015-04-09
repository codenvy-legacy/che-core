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

import org.eclipse.che.api.core.NotFoundException;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds active machines
 *
 * @author Alexander Garagatyi
 */
@Singleton
public class MachineRegistry {
    private final Map<String, MachineImpl> machines;

    public MachineRegistry() {
        machines = new ConcurrentHashMap<>();
    }

    /**
     * Get all active machine
     */
    public List<MachineImpl> getAll() {
        return new ArrayList<>(machines.values());
    }

    /**
     * Add new machine
     */
    public void put(MachineImpl machine) {
        machines.put(machine.getId(), machine);
    }

    /**
     * Remove machine by id
     *
     * @param machineId
     *         id of machine that should be removed
     * @throws NotFoundException
     *         if machine with specified id not found
     */
    public void remove(String machineId) throws NotFoundException {
        if (machines.containsKey(machineId)) {
            machines.remove(machineId);
        } else {
            throw new NotFoundException("Machine " + machineId + " is not found");
        }
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
    public MachineImpl get(String machineId) throws NotFoundException {
        if (machines.containsKey(machineId)) {
            return machines.get(machineId);
        }
        throw new NotFoundException("Machine " + machineId + " is not found");
    }
}
