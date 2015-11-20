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
package org.eclipse.che.api.workspace.server.model.impl;


import org.eclipse.che.api.core.model.workspace.ModuleConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.stream.Collectors.toMap;

//TODO move?

/**
 * Data object for {@link ModuleConfig}.
 *
 * @author Vitalii Parfonov
 * @author Dmitry Shnurenko
 */
public class ModuleConfigImpl implements ModuleConfig {

    private String                    name;
    private String                    path;
    private String                    description;
    private String                    type;
    private List<String>              mixins;
    private Map<String, List<String>> attributes;
    private List<ModuleConfig>        modules;

    public ModuleConfigImpl() {
    }

    public ModuleConfigImpl(ModuleConfig moduleConfig) {
        name = moduleConfig.getName();
        path = moduleConfig.getPath();
        description = moduleConfig.getDescription();
        type = moduleConfig.getType();
        mixins = new ArrayList<>(moduleConfig.getMixins());
        modules = new ArrayList<>(moduleConfig.getModules() != null ? moduleConfig.getModules() : Collections.<ModuleConfig>emptyList());
        attributes = moduleConfig.getAttributes()
                                 .entrySet()
                                 .stream()
                                 .collect(toMap(Map.Entry::getKey, e -> new ArrayList<>(e.getValue())));
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public List<String> getMixins() {
        if (mixins == null) {
            mixins = new ArrayList<>();
        }
        return mixins;
    }

    public void setMixins(List<String> mixins) {
        this.mixins = mixins;
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        return attributes;
    }

    @Override
    public List<ModuleConfig> getModules() {
        if (modules == null) {
            modules = new ArrayList<>();
        }
        return modules;
    }

    public void setModules(List<ModuleConfig> modules) {
        this.modules = modules;
    }

    public void setAttributes(Map<String, List<String>> attributes) {
        this.attributes = attributes;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ModuleConfigImpl)) return false;
        final ModuleConfigImpl other = (ModuleConfigImpl)o;
        return Objects.equals(name, other.name) &&
               Objects.equals(path, other.path) &&
               Objects.equals(description, other.description) &&
               Objects.equals(type, other.type) &&
               getMixins().equals(other.getMixins()) &&
               getAttributes().equals(other.getAttributes()) &&
               getModules().equals(other.getModules());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = hash * 31 + Objects.hashCode(name);
        hash = hash * 31 + Objects.hashCode(path);
        hash = hash * 31 + Objects.hashCode(description);
        hash = hash * 31 + Objects.hashCode(type);
        hash = hash * 31 + getMixins().hashCode();
        hash = hash * 31 + getAttributes().hashCode();
        hash = hash * 31 + getModules().hashCode();
        return hash;
    }

    @Override
    public String toString() {
        return "ModuleConfigImpl{" +
               "name='" + name + '\'' +
               ", path='" + path + '\'' +
               ", description='" + description + '\'' +
               ", type='" + type + '\'' +
               ", mixins=" + mixins +
               ", attributes=" + attributes +
               ", modules=" + modules +
               '}';
    }
}
