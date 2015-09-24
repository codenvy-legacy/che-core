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
    private String            outputChannel;
    private String            statusChannel;

    public MachineConfigImpl() {
    }

    public MachineConfigImpl(boolean isDev,
                             String name,
                             String type,
                             MachineSource source,
                             int memorySize,
                             String outputChannel,
                             String statusChannel) {
        this.isDev = isDev;
        this.name = name;
        this.type = type;
        if (source != null) {
            this.source = new MachineSourceImpl(source.getType(), source.getLocation());
        }
        this.memorySize = memorySize;
        this.outputChannel = outputChannel;
        this.statusChannel = statusChannel;
    }

    public MachineConfigImpl(MachineConfig machineCfg) {
        this(machineCfg.isDev(),
             machineCfg.getName(),
             machineCfg.getType(),
             machineCfg.getSource(),
             machineCfg.getMemorySize(),
             machineCfg.getOutputChannel(),
             machineCfg.getStatusChannel());
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public MachineSource getSource() {
        return source;
    }

    public void setSource(MachineSourceImpl source) {
        this.source = source;
    }

    @Override
    public boolean isDev() {
        return isDev;
    }

    public void setDev(boolean isDev) {
        this.isDev = isDev;
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

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String getOutputChannel() {
        return outputChannel;
    }

    public void setOutputChannel(String outputChannel) {
        this.outputChannel = outputChannel;
    }

    @Override
    public String getStatusChannel() {
        return statusChannel;
    }

    public void setStatusChannel(String statusChannel) {
        this.statusChannel = statusChannel;
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
               Objects.equals(type, other.type) &&
               Objects.equals(outputChannel, other.outputChannel) &&
               Objects.equals(statusChannel, other.statusChannel);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = hash * 31 + Boolean.hashCode(isDev);
        hash = hash * 31 + Objects.hashCode(name);
        hash = hash * 31 + Objects.hashCode(type);
        hash = hash * 31 + Objects.hashCode(source);
        hash = hash * 31 + Objects.hashCode(memorySize);
        hash = hash * 31 + Objects.hashCode(outputChannel);
        hash = hash * 31 + Objects.hashCode(statusChannel);
        return hash;
    }

    @Override
    public String toString() {
        return "MachineConfigImpl{" +
               "isDev=" + isDev +
               ", name='" + name + '\'' +
               ", type='" + type + '\'' +
               ", source=" + source +
               ", memorySize=" + memorySize +
               ", outputChannel='" + outputChannel + '\'' +
               ", statusChannel='" + statusChannel + '\'' +
               '}';
    }
}
