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

import org.eclipse.che.api.core.model.workspace.Machine;

import java.util.Map;

/**
 * @author Alexander Garagatyi
 */
public interface MachineDto extends MachineConfigDto, Machine {
    @Override
    String getId();

    void setId(String id);

    MachineDto withId(String id);

    @Override
    MachineSourceDto getSource();

    @Override
    String getOutputChannel();

    void setOutputChannel(String outputChannel);

    MachineDto withOutputChannel(String outputChannel);

    @Override
    Map<String, String> getProperties();

    void setProperties(Map<String, String> properties);

    MachineDto withProperties(Map<String, String> properties);

    @Override
    Map<String, ServerDto> getServers();

    void setServers(Map<String, ServerDto> servers);

    MachineDto withServers(Map<String, ServerDto> servers);
}
