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

import org.eclipse.che.api.core.model.machine.MachineConfig;
import org.eclipse.che.dto.shared.DTO;

/**
 * @author Alexander Garagatyi
 */
@DTO
public interface MachineConfigDto extends MachineConfig {
    @Override
    String getName();

    void setName(String name);

    MachineConfigDto withName(String name);

    @Override
    MachineSourceDto getSource();

    void setSource(MachineSourceDto source);

    MachineConfigDto withSource(MachineSourceDto source);

    @Override
    boolean isDev();

    void setDev(boolean dev);

    MachineConfigDto withDev(boolean dev);

    @Override
    String getType();

    void setType(String type);

    MachineConfigDto withType(String type);

    @Override
    int getMemorySize();

    void setMemorySize(int memorySize);

    MachineConfigDto withMemorySize(int memorySize);

    @Override
    String getOutputChannel();

    MachineConfigDto withOutputChannel(String outputChannel);
}
