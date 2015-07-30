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
package org.eclipse.che.api.workspace.shared.dto;

import org.eclipse.che.api.core.model.workspace.Environment;
import org.eclipse.che.dto.shared.DTO;

import java.util.List;

/**
 * @author Alexander Garagatyi
 */
@DTO
public interface EnvironmentDto extends Environment {
    @Override
    String getName();

    void setName(String name);

    EnvironmentDto withName(String name);

    @Override
    List<MachineConfigDto> getMachineConfigs();

    void setMachineConfigs(List<MachineConfigDto> machineConfigs);

    EnvironmentDto withMachineConfigs(List<MachineConfigDto> machineConfigs);
}
