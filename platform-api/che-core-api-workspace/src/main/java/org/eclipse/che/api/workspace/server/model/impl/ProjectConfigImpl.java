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
    private SourceStorageImpl         storage;

    public ProjectConfigImpl() {
    }

    public ProjectConfigImpl(ProjectConfig projectCfg) {
        name = projectCfg.getName();
        path = projectCfg.getPath();
        description = projectCfg.getDescription();
        type = projectCfg.getType();
        mixinTypes = projectCfg.getMixinTypes();
        attributes = projectCfg.getAttributes();
        if (projectCfg.getStorage() != null) {
            storage = new SourceStorageImpl(projectCfg.getStorage()
                                                      .getType(),
                                            projectCfg.getStorage()
                                                      .getLocation(),
                                            projectCfg.getStorage()
                                                      .getParameters());
        }
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
    public List<String> getMixinTypes() {
        if (mixinTypes == null) {
            mixinTypes = new ArrayList<>();
        }
        return mixinTypes;
    }

    public void setMixinTypes(List<String> mixinTypes) {
        this.mixinTypes = mixinTypes;
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        return attributes;
    }

    public void setAttributes(Map<String, List<String>> attributes) {
        this.attributes = attributes;
    }

    @Override
    public SourceStorage getStorage() {
        return storage;
    }

    public void setStorage(SourceStorageImpl sourceStorage) {
        this.storage = sourceStorage;
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
               Objects.equals(storage, other.storage) &&
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
        hash = hash * 31 + Objects.hashCode(storage);
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
               ", storage=" + storage +
               '}';
    }
}
