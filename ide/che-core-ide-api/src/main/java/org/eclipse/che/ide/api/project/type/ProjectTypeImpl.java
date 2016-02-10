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
package org.eclipse.che.ide.api.project.type;

import org.eclipse.che.api.core.model.project.type.Attribute;
import org.eclipse.che.api.core.model.project.type.ProjectType;
import org.eclipse.che.api.project.shared.dto.ProjectTypeDto;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable implementation of Project Type interface
 * intended to use in client side
 *
 * @author gazarenkov
 */
public class ProjectTypeImpl implements ProjectType {

    protected final String       id;
    protected final boolean      persisted;
    protected final boolean      mixable;
    protected final boolean      primaryable;
    protected final String       displayName;
    protected final List<String> parents;
    protected final List<String> ancestors;

    protected List<? extends Attribute> attributes;

    public ProjectTypeImpl(String id,
                           boolean persisted,
                           boolean mixable,
                           boolean primaryable,
                           String displayName,
                           List<? extends Attribute> attributes,
                           List<String> parents) {
        ancestors = new ArrayList<>();

        this.id = id;
        this.persisted = persisted;
        this.mixable = mixable;
        this.primaryable = primaryable;
        this.displayName = displayName;
        this.attributes = attributes;
        this.parents = parents;
    }

    public ProjectTypeImpl(ProjectTypeDto dto) {
        this(dto.getId(), dto.isPersisted(), dto.isMixable(), dto.isPrimaryable(), dto.getDisplayName(), null, dto.getParents());
        attributes = dto.getAttributes();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isPersisted() {
        return persisted;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public List<? extends Attribute> getAttributes() {
        return attributes;
    }

    @Override
    public List<String> getParents() {
        return parents;
    }

    @Override
    public boolean isMixable() {
        return mixable;
    }

    @Override
    public boolean isPrimaryable() {
        return primaryable;
    }

    /**
     * @return ids of ancestors
     */
    public List<String> getAncestors() {
        return ancestors;
    }

    /**
     * whether this type is subtype of typeId
     *
     * @param typeId
     * @return true if it is a subtype
     */
    public boolean isTypeOf(String typeId) {
        return this.id.equals(typeId) || ancestors.contains(typeId);
    }

    /**
     * @param name
     * @return attribute by name
     */
    public Attribute getAttribute(String name) {
        for (Attribute attr : attributes) {
            if (attr.getName().equals(name))
                return attr;
        }
        return null;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        return id.equals(((ProjectTypeImpl)obj).getId());
    }
}
