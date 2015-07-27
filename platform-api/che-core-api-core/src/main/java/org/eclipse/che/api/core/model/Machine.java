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
package org.eclipse.che.api.core.model;

import org.eclipse.che.api.core.model.machine.MachineConfig;
import org.eclipse.che.api.core.model.machine.Server;

import java.util.Map;

/**
 * Describes machine
 *
 * @author gazarenkov
 */
public interface Machine extends MachineConfig {

    /**
     * Unique ID of this machine
     */
    String getId();

    /**
     * Machine type (i.e. "docker")
     */
    //String getType();

    /**
     * Identifier of user who launched this machine.
     */
    User getOwner();

    //MachineStatus getStatus();

    /**
     * List of projects bound to this machine
     */
    //Set<? extends ProjectBinding> getProjects();

    /**
     * Id of workspace this machine belongs to
     */
    //String getWorkspaceId();

    /**
     * Is workspace bound to machine or not
     */
    //boolean isDev();

    /**
     * Channel of websocket where machine logs should be put
     */
    String getOutputChannel();

    /**
     * Machine specific metadata
     */
    //InstanceMetadata getMetadata() throws MachineException;
    Map<String, String> getProperties();

    /**
     * Returns mapping of exposed ports to {link Server}
     */
    Map<String, Server> getServers();// throws MachineException;

}
