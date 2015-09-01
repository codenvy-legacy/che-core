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
import org.eclipse.che.api.core.rest.shared.dto.Hyperlinks;
import org.eclipse.che.api.core.rest.shared.dto.Link;

import java.util.List;
import java.util.Map;

/**
 * @author Alexander Garagatyi
 */
public interface MachineDto extends MachineConfigDto, Machine {
    @Override
    String getId();

    MachineDto withId(String id);

    @Override
    MachineDto withName(String name);

    @Override
    MachineDto withDev(boolean dev);

    @Override
    MachineDto withSource(MachineSourceDto source);

    @Override
    MachineDto withType(String type);

    @Override
    MachineSourceDto getSource();

    @Override
    String getOutputChannel();

    MachineDto withOutputChannel(String outputChannel);

    MachineDto withStatusChannel(String statusChannel);

    @Override
    Map<String, String> getProperties();

    MachineDto withProperties(Map<String, String> properties);

    @Override
    Map<String, ServerDto> getServers();

    MachineDto withServers(Map<String, ServerDto> servers);

    @Override
    MachineDto withMemorySize(int memorySize);

    List<Link> getLinks();

    MachineDto withLinks(List<Link> links);
}
