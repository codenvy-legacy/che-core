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

import org.eclipse.che.api.core.model.workspace.MachineMetadata;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Data object for {@link MachineMetadata}
 *
 * @author Alexander Garagatyi
 */
public class MachineMetadataImpl implements MachineMetadata {
    private Map<String, String> envVariables;

    public MachineMetadataImpl() {
    }

    public MachineMetadataImpl(MachineMetadata metadata) {
        if (metadata != null && metadata.getEnvVariables() != null) {
            this.envVariables = new HashMap<>(metadata.getEnvVariables());
        }
    }

    @Override
    public Map<String, String> getEnvVariables() {
        if (envVariables == null) {
            envVariables = new HashMap<>();
        }
        return envVariables;
    }

    @Override
    public String projectsRoot() {
        return getEnvVariables().get("CHE_PROJECTS_ROOT");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MachineMetadataImpl)) return false;
        MachineMetadataImpl that = (MachineMetadataImpl)o;
        return Objects.equals(getEnvVariables(), that.getEnvVariables());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getEnvVariables());
    }
}
