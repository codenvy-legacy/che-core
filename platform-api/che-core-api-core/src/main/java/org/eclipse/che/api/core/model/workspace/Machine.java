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
package org.eclipse.che.api.core.model.workspace;

import org.eclipse.che.api.core.model.machine.MachineConfig;
import org.eclipse.che.api.core.model.machine.Server;

import java.util.Map;

/**
 * Defines runtime machine.
 *
 * @author gazarenkov
 */
public interface Machine extends MachineConfig {

    /**
     * Returns unique identifier of this machine
     */
    String getId();

    /**
     * Returns channel of websocket where machine logs should be put.
     */
    String getOutputChannel();

    /**
     * Returns machine specific metadata
     */
    Map<String, String> getProperties();

    /**
     * Returns mapping of exposed ports to {@link Server}
     */
    Map<String, ? extends Server> getServers();

    //TODO add links to machines
}
