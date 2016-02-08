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
package org.eclipse.che.api.machine.server.model.impl;

import org.eclipse.che.api.core.model.machine.MachineMetadata;
import org.eclipse.che.api.core.model.machine.MachineRuntimeInfo;

import java.util.Objects;

/**
 * Data object for {@link MachineRuntimeInfo}.
 *
 * @author Alexander Garagatyi
 */
public class MachineRuntimeInfoImpl implements MachineRuntimeInfo {
    private final MachineMetadataImpl metadata;

    public MachineRuntimeInfoImpl(MachineMetadata metadata) {
        this.metadata = new MachineMetadataImpl(metadata);
    }

    public MachineRuntimeInfoImpl(MachineRuntimeInfo machineRuntime) {
        this(machineRuntime.getMetadata());
    }

    @Override
    public MachineMetadataImpl getMetadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MachineRuntimeInfoImpl)) return false;
        MachineRuntimeInfoImpl that = (MachineRuntimeInfoImpl)o;
        return Objects.equals(getMetadata(), that.getMetadata());
    }

    @Override
    public int hashCode() {
        return metadata.hashCode();
    }
}
