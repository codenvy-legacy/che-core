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
package org.eclipse.che.api.account.server.dao;

import org.eclipse.che.api.core.model.workspace.UsersWorkspace;
import org.eclipse.che.api.workspace.server.model.impl.UsersWorkspaceImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Eugene Voevodin
 */
public class Account {

    private String               id;
    private String               name;
    private List<UsersWorkspace> workspaces;
    private Map<String, String>  attributes;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Account withId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Account withName(String name) {
        this.name = name;
        return this;
    }

    public Map<String, String> getAttributes() {
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        return attributes;
    }

    public void setWorkspaces(List<UsersWorkspace> workspaces) {
        this.workspaces = workspaces;
    }

    public Account withWorkspaces(List<UsersWorkspace> workspaces) {
        this.workspaces = workspaces;
        return this;
    }

    public List<UsersWorkspace> getWorkspaces() {
        if (workspaces == null) {
            workspaces = new ArrayList<>();
        }
        return workspaces;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public Account withAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Account)) {
            return false;
        }
        final Account other = (Account)obj;
        return Objects.equals(id, other.id)
               && Objects.equals(name, other.name)
               && getAttributes().equals(other.getAttributes())
               && getWorkspaces().equals(other.getWorkspaces());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + Objects.hashCode(id);
        hash = 31 * hash + Objects.hashCode(name);
        hash = 31 * hash + getAttributes().hashCode();
        hash = 31 * hash + getWorkspaces().hashCode();
        return hash;
    }

    @Override
    public String toString() {
        return "Account{" +
               "id='" + id + '\'' +
               ", name='" + name + '\'' +
               ", workspaces=" + workspaces +
               ", attributes=" + attributes +
               '}';
    }
}
