/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.builder.internal;

import javax.inject.Singleton;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores available builders.
 *
 * @author andrew00x
 */
@Singleton
public class BuilderRegistry {
    private final Map<String, Builder> builders;

    public BuilderRegistry() {
        builders = new ConcurrentHashMap<>();
    }

    /**
     * Add {@code Builder}. Uses {@code String} returned by method {@code Builder.getName()} as builder's identifier. If {@code Builder}
     * with the same name already registered it is replaced by new one.
     *
     * @param builder
     *         Builder
     */
    public void add(Builder builder) {
        builders.put(builder.getName(), builder);
    }

    /**
     * Get {@code Builder} by its name.
     *
     * @param name
     *         name
     * @return {@code Builder} or {@code null} if there is no such {@code Builder}
     */
    public Builder get(String name) {
        if (name == null) {
            return null;
        }
        return builders.get(name);
    }

    /**
     * Remove {@code Builder} by its name.
     *
     * @param name
     *         name
     * @return {@code Builder} or {@code null} if there is no such {@code Builder}
     */
    public Builder remove(String name) {
        if (name == null) {
            return null;
        }
        return builders.remove(name);
    }

    /**
     * Get all available builders. Modifications to the returned {@code Set} will not affect the internal state of {@code BuilderRegistry}.
     *
     * @return all available builders
     */
    public Set<Builder> getAll() {
        return new LinkedHashSet<>(builders.values());
    }

    public void clear() {
        builders.clear();
    }
}
