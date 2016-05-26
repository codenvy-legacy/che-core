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
import java.util.Map;

/**
 * Describes runner configuration for project.
 *
 * @author andrew00x
 */
public class Runners {
    /** Default runner identifier. */
    private String              _default;
    /** Runner configurations, {@link #_default} must point to the one configuration in this {@code Map}. */
    private Map<String, Config> configs;

    public Runners() {
    }

    public Runners(String _default) {
        this._default = _default;
    }

    public Runners(String _default, Map<String, Config> configs) {
        this._default = _default;
        setConfigs(new LinkedHashMap<>(configs));
    }

    /** Copy constructor. */
    public Runners(Runners other) {
        this._default = other._default;
    }

    /** Gets default runner identifier. */
    public String getDefault() {
        return _default;
    }

    /** Sets default runner identifier. */
    public void setDefault(String _default) {
        this._default = _default;
    }

    public Runners withDefault(String _default) {
        this._default = _default;
        return this;
    }

    /** Gets all available runner configurations. Modifications to the returned {@code Map} will affect the internal state. */
    public Map<String, Config> getConfigs() {
        if (configs == null) {
            configs = new LinkedHashMap<>();
        }
        return configs;
    }

    /** Gets runner configurations by its identifier. */
    public Config getConfig(String config) {
        if (configs == null) {
            return null;
        }
        return configs.get(config);
    }

    /**
     * Sets new runner configurations.
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

    public Runners withConfigs(Map<String, Config> configs) {
        setConfigs(configs);
        return this;
    }

    public static class Config {
        /** Amount of RAM for this configuration in megabytes */
        private int                 ram;
        /** Runtime options (runner type/receipt specific). */
        private Map<String, String> options;
        /** Environment variables (runner type/receipt specific). */
        private Map<String, String> variables;

        public Config() {
        }

        public Config(int ram, Map<String, String> options, Map<String, String> variables) {
            this.ram = ram;
            setOptions(options);
            setVariables(variables);
        }

        /** Copy constructor. */
        public Config(int ram) {
            this.ram = ram;
        }

        /** Copy constructor. */
        public Config(Config other) {
            this.ram = other.ram;
            setOptions(other.options);
            setVariables(other.variables);
        }

        /** Gets amount of RAM for this configuration in megabytes. */
        public int getRam() {
            return ram;
        }

        /** Sets amount of RAM for this configuration in megabytes. */
        public void setRam(int ram) {
            this.ram = ram;
        }

        public Config withRam(int ram) {
            this.ram = ram;
            return this;
        }

        /**
         * Gets runtime options (runner type and(or) receipt specific). Modifications to the returned {@code Map} will affect the internal
         * state.
         */
        public Map<String, String> getOptions() {
            if (options == null) {
                options = new LinkedHashMap<>();
            }
            return options;
        }

        /**
         * Sets runtime options (runner type and(or) receipt specific).
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
         * Gets environment variables (runner type and(or) receipt specific). Modifications to the returned {@code Map} will affect the
         * internal state.
         */
        public Map<String, String> getVariables() {
            if (variables == null) {
                variables = new LinkedHashMap<>();
            }
            return variables;
        }

        /**
         * Sets environment variables (runner type and(or) receipt specific).
         *
         * @see #getVariables()
         */
        public void setVariables(Map<String, String> variables) {
            final Map<String, String> myVariables = getVariables();
            myVariables.clear();
            if (variables != null) {
                myVariables.putAll(variables);
            }
        }

        public Config withVariables(Map<String, String> variables) {
            setVariables(variables);
            return this;
        }
    }
}
