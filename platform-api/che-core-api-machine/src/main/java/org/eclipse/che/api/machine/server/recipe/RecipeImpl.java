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
package org.eclipse.che.api.machine.server.recipe;

import org.eclipse.che.api.machine.shared.Permissions;
import org.eclipse.che.api.machine.shared.Recipe;
import org.eclipse.che.api.machine.shared.dto.RecipeDescriptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Implementation of {@link Recipe}
 *
 * @author Eugene Voevodin
 */
public class RecipeImpl implements Recipe {

    public static Recipe fromDescriptor(RecipeDescriptor descriptor) {
        final Recipe recipe = new RecipeImpl().withId(descriptor.getId())
                                              .withType(descriptor.getType())
                                              .withScript(descriptor.getScript())
                                              .withTags(descriptor.getTags())
                                              .withCreator(descriptor.getCreator());
        if (descriptor.getPermissions() != null) {
            recipe.setPermissions(PermissionsImpl.fromDescriptor(descriptor.getPermissions()));
        }
        return recipe;
    }

    private String       id;
    private String       creator;
    private String       type;
    private String       script;
    private List<String> tags;
    private Permissions  permissions;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public RecipeImpl withId(String id) {
        this.id = id;
        return this;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public RecipeImpl withType(String type) {
        this.type = type;
        return this;
    }

    @Override
    public String getScript() {
        return script;
    }

    @Override
    public void setScript(String script) {
        this.script = script;
    }

    @Override
    public RecipeImpl withScript(String script) {
        this.script = script;
        return this;
    }

    @Override
    public String getCreator() {
        return creator;
    }

    @Override
    public void setCreator(String creator) {
        this.creator = creator;
    }

    @Override
    public RecipeImpl withCreator(String creator) {
        this.creator = creator;
        return this;
    }

    @Override
    public List<String> getTags() {
        if (tags == null) {
            tags = new ArrayList<>();
        }
        return tags;
    }

    @Override
    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    @Override
    public RecipeImpl withTags(List<String> tags) {
        this.tags = tags;
        return this;
    }

    @Override
    public Permissions getPermissions() {
        return permissions;
    }

    @Override
    public void setPermissions(Permissions permissions) {
        this.permissions = permissions;
    }

    @Override
    public Recipe withPermissions(Permissions permissions) {
        this.permissions = permissions;
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RecipeImpl)) {
            return false;
        }
        final RecipeImpl other = (RecipeImpl)obj;
        return Objects.equals(id, other.id) &&
               Objects.equals(creator, other.creator) &&
               Objects.equals(type, other.type) &&
               Objects.equals(script, other.script) &&
               Objects.equals(permissions, other.permissions) &&
               getTags().equals(other.getTags());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + Objects.hashCode(id);
        hash = 31 * hash + Objects.hashCode(creator);
        hash = 31 * hash + Objects.hashCode(type);
        hash = 31 * hash + Objects.hashCode(script);
        hash = 31 * hash + Objects.hashCode(permissions);
        hash = 31 * hash + getTags().hashCode();
        return hash;
    }
}
