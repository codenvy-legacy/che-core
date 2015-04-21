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
package org.eclipse.che.api.machine.server;

import org.eclipse.che.api.machine.shared.Group;
import org.eclipse.che.api.machine.shared.dto.GroupDescriptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GroupImpl implements Group {

    public static GroupImpl fromDescriptor(GroupDescriptor descriptor) {
        return new GroupImpl(descriptor.getName(), descriptor.getUnit(), descriptor.getAcl());
    }

    private String       name;
    private String       unit;
    private List<String> acl;

    public GroupImpl(String name, String unit, List<String> acl) {
        this.name = name;
        this.unit = unit;
        this.acl = acl;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public GroupImpl withName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public String getUnit() {
        return unit;
    }

    @Override
    public void setUnit(String unit) {
        this.unit = unit;
    }

    @Override
    public Group withUnit(String unit) {
        this.unit = unit;
        return this;
    }

    @Override
    public List<String> getAcl() {
        if (acl == null) {
            acl = new ArrayList<>();
        }
        return acl;
    }

    @Override
    public void setAcl(List<String> acl) {
        this.acl = acl;
    }

    @Override
    public Group withAcl(List<String> acl) {
        this.acl = acl;
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof GroupImpl)) {
            return false;
        }
        final GroupImpl other = (GroupImpl)obj;
        return Objects.equals(name, other.name) &&
               Objects.equals(unit, other.unit) &&
               getAcl().equals(other.getAcl());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + Objects.hashCode(name);
        hash = 31 * hash + Objects.hashCode(unit);
        hash = 31 * hash + getAcl().hashCode();
        return hash;
    }
}
