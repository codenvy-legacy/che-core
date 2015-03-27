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
package org.eclipse.che.api.runner.internal;

import javax.inject.Singleton;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores available runners.
 *
 * @author andrew00x
 */
@Singleton
public class RunnerRegistry {
    private final Map<String, Runner> runners;

    public RunnerRegistry() {
        runners = new ConcurrentHashMap<>();
    }

    /**
     * Add {@code Runner}. Uses {@code String} returned by method {@code Runner.getName()} as runner's identifier. If {@code Runner}
     * with the same name already registered it is replaced by new one.
     *
     * @param runner
     *         Runner
     */
    public void add(Runner runner) {
        runners.put(runner.getName(), runner);
    }

    /**
     * Get {@code Runner} by its name.
     *
     * @param name
     *         name
     * @return {@code Runner} or {@code null} if there is no such {@code Runner}
     */
    public Runner get(String name) {
        if (name == null) {
            return null;
        }
        return runners.get(name);
    }

    /**
     * Remove {@code Runner} by its name.
     *
     * @param name
     *         name
     * @return {@code Runner} or {@code null} if there is no such {@code Runner}
     */
    public Runner remove(String name) {
        if (name == null) {
            return null;
        }
        return runners.get(name);
    }

    /**
     * Get all available runners. Modifications to the returned {@code Set} will not affect the internal state of {@code RunnerRegistry}.
     *
     * @return all available runners
     */
    public Set<Runner> getAll() {
        return new LinkedHashSet<>(runners.values());
    }

    public void clear() {
        runners.clear();
    }
}
