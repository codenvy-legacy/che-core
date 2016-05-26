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
package org.eclipse.che.api.project.shared;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Describes builder configuration for project.
 *
 * @author andrew00x
 */
public class Builders {
    /** Default builder identifier. */
    private String _default;

    /** Builder configurations, {@link #_default} must point to the one configuration in this {@code Map}. */
    private Map<String, Config> configs;

    public Builders() {
    }

    public Builders(String _default, Map<String, Config> configs) {
        this._default = _default;
        setConfigs(new LinkedHashMap<>(configs));
    }

    public Builders(String _default) {
        this._default = _default;
    }

    /** Copy constructor. */
    public Builders(Builders other) {
        this._default = other._default;
    }

    /** Gets default builder identifier, e.g. "maven". */
    public String getDefault() {
        return _default;
    }

    /** Sets default builder identifier. e.g. "maven". */
    public void setDefault(String _default) {
        this._default = _default;
    }

    public Builders withDefault(String _default) {
        this._default = _default;
        return this;
    }

    /** Gets all available builders configurations. Modifications to the returned {@code Map} will affect the internal state. */
    public Map<String, Config> getConfigs() {
        if (configs == null) {
            configs = new LinkedHashMap<>();
        }
        return configs;
    }

    /** Gets builder configurations by its identifier. */
    public Config getConfig(String config) {
        if (configs == null) {
            return null;
        }
        return configs.get(config);
    }

    /**
     * Sets new builder configurations.
     *
     * @see #getConfigs()
     */
    public void setConfigs(Map<String, Config> configs) {
        final Map<String, Config> myConfigs = getConfigs();
        myConfigs.clear();
        if (configs != null) {
            myConfigs.putAll(configs);
        }
    }

    public Builders withConfigs(Map<String, Config> configs) {
        setConfigs(configs);
        return this;
    }

    public static class Config {
        /** Runtime options (builder type/receipt specific). */
        private Map<String, String> options;
        /** Build targets (builder type/receipt specific e.g. clean install for maven). */
        private List<String> targets;

        public Config() {
        }

        public Config(Map<String, String> options, List<String> targets) {
            setOptions(options);
            setTargets(targets);
        }


        /** Copy constructor. */
        public Config(Config other) {
            setOptions(other.options);
            setTargets(other.targets);
        }


        /**
         * Gets runtime options (builder type specific). Modifications to the returned {@code Map} will affect the internal
         * state.
         */
        public Map<String, String> getOptions() {
            if (options == null) {
                options = new LinkedHashMap<>();
            }
            return options;
        }

        /**
         * Sets runtime options (builder type specific).
         *
         * @see #getOptions()
         */
        public void setOptions(Map<String, String> options) {
            final Map<String, String> myOptions = getOptions();
            myOptions.clear();
            if (options != null) {
                myOptions.putAll(options);
            }
        }

        public Config withOptions(Map<String, String> options) {
            setOptions(options);
            return this;
        }

        /**
         * Gets build targets (builder type specific). Modifications to the returned {@code Map} will affect the
         * internal state.
         */
        public List<String> getTargets() {
            if (targets == null) {
                targets = new LinkedList<>();
            }
            return targets;
        }

        /**
         * Sets builder targets (runner type specific).
         *
         * @see #getTargets()
         */
        public void setTargets(List<String> targets) {
            final List<String> myTargets = getTargets();
            myTargets.clear();
            if (targets != null) {
                myTargets.addAll(targets);
            }
        }

        public Config withTargets(List<String> targets) {
            setTargets(targets);
            return this;
        }
    }

}
