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


import org.eclipse.che.api.core.model.project.SourceStorage;
import org.eclipse.che.api.core.model.workspace.ProjectConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

//TODO move?

/**
 * Data object for {@link ProjectConfig}.
 *
 * @author Eugene Voevodin
 */
public class ProjectConfigImpl implements ProjectConfig {

    private String                    name;
    private String                    path;
    private String                    description;
    private String                    type;
    private List<String>              mixinTypes;
    private Map<String, List<String>> attributes;
    private SourceStorage             sourceStorage;

    @Override
    public String getName() {
        return name;
    }

    public ProjectConfigImpl setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public String getPath() {
        return path;
    }

    public ProjectConfigImpl setPath(String path) {
        this.path = path;
        return this;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public ProjectConfigImpl setDescription(String description) {
        this.description = description;
        return this;
    }

    @Override
    public String getType() {
        return type;
    }

    public ProjectConfigImpl setType(String type) {
        this.type = type;
        return this;
    }

    @Override
    public List<String> getMixinTypes() {
        if (mixinTypes == null) {
            this.mixinTypes = new ArrayList<>();
        }
        return mixinTypes;
    }

    public ProjectConfigImpl setMixinTypes(List<String> mixinTypes) {
        this.mixinTypes = mixinTypes;
        return this;
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        return attributes;
    }

    public ProjectConfigImpl setAttributes(Map<String, List<String>> attributes) {
        this.attributes = attributes;
        return this;
    }

    @Override
    public SourceStorage getSourceStorage() {
        return sourceStorage;
    }

    public ProjectConfigImpl setSourceStorage(SourceStorage sourceStorage) {
        this.sourceStorage = sourceStorage;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProjectConfigImpl)) return false;
        final ProjectConfigImpl other = (ProjectConfigImpl)o;
        return Objects.equals(name, other.name) &&
               Objects.equals(path, other.path) &&
               Objects.equals(description, other.description) &&
               Objects.equals(type, other.type) &&
               Objects.equals(sourceStorage, other.sourceStorage) &&
               getMixinTypes().equals(other.getMixinTypes()) &&
               getAttributes().equals(other.getAttributes());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = hash * 31 + Objects.hashCode(name);
        hash = hash * 31 + Objects.hashCode(path);
        hash = hash * 31 + Objects.hashCode(description);
        hash = hash * 31 + Objects.hashCode(type);
        hash = hash * 31 + Objects.hashCode(sourceStorage);
        hash = hash * 31 + getMixinTypes().hashCode();
        hash = hash * 31 + getAttributes().hashCode();
        return hash;
    }

    @Override
    public String toString() {
        return "ProjectConfigImpl{" +
               "name='" + name + '\'' +
               ", path='" + path + '\'' +
               ", description='" + description + '\'' +
               ", type='" + type + '\'' +
               ", mixinTypes=" + mixinTypes +
               ", attributes=" + attributes +
               ", sourceStorage=" + sourceStorage +
               '}';
    }
}
