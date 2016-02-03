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
package org.eclipse.che.api.workspace.server.model.impl.stack;

import org.eclipse.che.api.core.model.workspace.WorkspaceConfig;
import org.eclipse.che.api.machine.shared.Permissions;
import org.eclipse.che.api.workspace.server.stack.image.StackIcon;
import org.eclipse.che.commons.lang.NameGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

/**
 * Server implementation of {@link Stack}
 *
 * @author Alexander Andrienko
 */
public class DecoratedStackImpl implements DecoratedStack {

    public static StackBuilder builder() {
        return new StackBuilder();
    }

    public DecoratedStackImpl(Stack stack, StackIcon stackIcon) {
        this(stack.getId(),
             stack.getName(),
             stack.getDescription(),
             stack.getScope(),
             stack.getCreator(),
             stack.getTags(),
             stack.getWorkspaceConfig(),
             stack.getSource(),
             stack.getComponents(),
             stackIcon,
             stack.getPermissions());
    }

    public DecoratedStackImpl(String id,
                              String name,
                              String description,
                              String scope,
                              String creator,
                              List<String> tags,
                              WorkspaceConfig workspaceConfig,
                              StackSource source,
                              List<? extends StackComponent> components,
                              StackIcon icon,
                              Permissions permissions) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.scope = scope;
        this.creator = creator;
        this.tags = tags;
        this.workspaceConfig = workspaceConfig;
        this.source = source;
        this.components = components == null ? emptyList() : components.stream()
                                                                       .map(component -> new StackComponentImpl(component.getName(),
                                                                                                                component.getVersion()))
                                                                       .collect(toList());
        this.icon = icon;
        this.permissions = permissions;
    }

    private String               id;
    private String               name;
    private String               description;
    private String               scope;
    private String               creator;
    private List<String>         tags;
    private WorkspaceConfig      workspaceConfig;
    private StackSource          source;
    private List<StackComponent> components;
    private StackIcon            icon;
    private Permissions          permissions;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    @Override
    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    @Override
    public List<String> getTags() {
        if (tags == null) {
            return new ArrayList<>();
        }
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    @Override
    public WorkspaceConfig getWorkspaceConfig() {
        return workspaceConfig;
    }

    public void setWorkspaceConfig(WorkspaceConfig workspaceConfig) {
        this.workspaceConfig = workspaceConfig;
    }

    public void setSource(StackSource source) {
        this.source = source;
    }

    @Override
    public StackSource getSource() {
        return source;
    }

    @Override
    public List<StackComponent> getComponents() {
        if (components == null) {
            return new ArrayList<>();
        }
        return components;
    }

    public void setComponents(List<StackComponent> components) {
        this.components = components;
    }

    public StackIcon getIcon() {
        return icon;
    }

    public void setIcon(StackIcon icon) {
        this.icon = icon;
    }

    public void setPermissions(Permissions permissions) {
        this.permissions = permissions;
    }

    @Override
    public Permissions getPermissions() {
        return permissions;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DecoratedStackImpl)) {
            return false;
        }
        DecoratedStackImpl other = (DecoratedStackImpl)obj;
        return Objects.equals(id, other.id) &&
               Objects.equals(name, other.name) &&
               Objects.equals(description, other.description) &&
               Objects.equals(creator, other.creator) &&
               Objects.equals(scope, other.getScope()) &&
               Objects.equals(getTags(), other.getTags()) &&
               Objects.equals(getWorkspaceConfig(), other.getWorkspaceConfig()) &&
               Objects.equals(getSource(), other.getSource()) &&
               Objects.equals(getComponents(), other.getComponents()) &&
               Objects.equals(getIcon(), other.getIcon()) &&
               Objects.equals(permissions, other.permissions);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + Objects.hashCode(id);
        hash = 31 * hash + Objects.hashCode(name);
        hash = 31 * hash + Objects.hashCode(description);
        hash = 31 * hash + Objects.hashCode(scope);
        hash = 31 * hash + Objects.hashCode(creator);
        hash = 31 * hash + Objects.hashCode(getTags());
        hash = 31 * hash + Objects.hashCode(getWorkspaceConfig());
        hash = 31 * hash + Objects.hashCode(getSource());
        hash = 31 * hash + Objects.hashCode(getComponents());
        hash = 31 * hash + Objects.hashCode(getIcon());
        hash = 31 * hash + Objects.hashCode(permissions);
        return hash;
    }

    @Override
    public String toString() {
        return "DecoratedStackImpl{id='" + id +
               "', name='" + name +
               "', description='" + description +
               "', scope='" + scope +
               "', creator='" + creator +
               "', tags='" + tags + "'" +
               ", workspaceConfig=" + workspaceConfig +
               ", stackSource=" + source +
               ", components=" + components +
               ", iconContent=" + icon +
               ", permission=" + permissions +
               "}";
    }

    public static class StackBuilder {

        private String               id;
        private String               name;
        private String               description;
        private String               scope;
        private String               creator;
        private List<String>         tags;
        private WorkspaceConfig      workspaceConfig;
        private StackSource          source;
        private List<StackComponent> components;
        private StackIcon            icon;
        private Permissions          permissions;

        public StackBuilder generateId() {
            id = NameGenerator.generate("stack", 16);
            return this;
        }

        public StackBuilder setId(String id) {
            if (id == null) {
                return generateId();
            }
            this.id = id;
            return this;
        }

        public StackBuilder setName(String name) {
            this.name = name;
            return this;
        }

        public StackBuilder setDescription(String description) {
            this.description = description;
            return this;
        }

        public StackBuilder setScope(String scope) {
            this.scope = scope;
            return this;
        }

        public StackBuilder setCreator(String creator) {
            this.creator = creator;
            return this;
        }

        public StackBuilder setTags(List<String> tags) {
            this.tags = (tags == null) ? new ArrayList<>() : tags;
            return this;
        }

        public StackBuilder setWorkspaceConfig(WorkspaceConfig workspaceConfig) {
            this.workspaceConfig = workspaceConfig;
            return this;
        }

        public StackBuilder setSource(StackSource source) {
            this.source = source;
            return this;
        }

        public StackBuilder setComponents(List<StackComponent> components) {
            this.components = (components == null) ? new ArrayList<>() : components;
            return this;
        }

        public StackBuilder setIcon(StackIcon icon) {
            this.icon = icon;
            return this;
        }

        public StackBuilder setPermissions(Permissions permissions) {
            this.permissions = permissions;
            return this;
        }

        public DecoratedStackImpl build() {
            return new DecoratedStackImpl(id,
                                          name,
                                          description,
                                          scope,
                                          creator,
                                          tags,
                                          workspaceConfig,
                                          source,
                                          components,
                                          icon,
                                          permissions);
        }
    }
}
