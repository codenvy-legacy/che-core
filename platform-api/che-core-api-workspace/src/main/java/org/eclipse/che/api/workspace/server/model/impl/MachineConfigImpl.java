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

import org.eclipse.che.api.core.model.machine.MachineConfig;
import org.eclipse.che.api.core.model.machine.MachineSource;

import java.util.Objects;

//TODO move?

/**
 * Data object for {@link MachineConfig}.
 *
 * @author Eugene Voevodin
 */
public class MachineConfigImpl implements MachineConfig {

    private boolean           isDev;
    private String            name;
    private String            type;
    private MachineSourceImpl source;
    private int               memorySize;

    @Override
    public String getName() {
        return name;
    }

    public MachineConfigImpl setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public MachineSource getSource() {
        return source;
    }

    public MachineConfigImpl setSource(MachineSourceImpl source) {
        this.source = source;
        return this;
    }

    @Override
    public boolean isDev() {
        return isDev;
    }

    public MachineConfigImpl setDev(boolean isDev) {
        this.isDev = isDev;
        return this;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public int getMemorySize() {
        return memorySize;
    }

    public void setMemorySize(int memorySize) {
        this.memorySize = memorySize;
    }

    public MachineConfigImpl setType(String type) {
        this.type = type;
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof MachineConfigImpl)) return false;
        final MachineConfigImpl other = (MachineConfigImpl)obj;
        return isDev == other.isDev &&
               Objects.equals(name, other.name) &&
               Objects.equals(source, other.source) &&
               Objects.equals(memorySize, other.memorySize) &&
               Objects.equals(type, other.type);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = hash * 31 + (isDev ? 1231 : 1237);
        hash = hash * 31 + Objects.hashCode(name);
        hash = hash * 31 + Objects.hashCode(type);
        hash = hash * 31 + Objects.hashCode(source);
        hash = hash * 31 + Objects.hashCode(memorySize);
        return hash;
    }

    @Override
    public String toString() {
        return "MachineConfigImpl{" +
               "isDev=" + isDev +
               ", name='" + name + '\'' +
               ", type='" + type + '\'' +
               ", source=" + source + '\'' +
               ", memorySize=" + memorySize +
               '}';
    }
}
