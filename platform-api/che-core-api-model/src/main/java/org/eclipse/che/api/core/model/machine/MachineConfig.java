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
package org.eclipse.che.api.core.model.machine;

import java.util.List;
import java.util.Map;

/**
 * @author gazarenkov
 * @author Alexander Garagatyi
 */
public interface MachineConfig {

    /**
     * Display name.
     */
    String getName();

    /**
     * From where to create this Machine (Recipe/Snapshot).
     */
    MachineSource getSource();

    /**
     * Is workspace bound to machine or not.
     */
    boolean isDev();

    /**
     * Machine type (i.e. "docker").
     */
    String getType();

    /**
     * Machine limits such as RAM size.
     */
    Limits getLimits();

    /**
     * Servers that can be started in machine
     */
    List<ServerConf> getServers();

    /**
     * Environment variables that will be injected in machine at runtime
     * <p>
     * To inject host and port of server use {SERVER_{server-port}_ADDRESS} as value of env variable.
     * Example:
     * <br>Entry < MY_APP_ADDRESS, {SERVER_8080_ADDRESS}> will appear at runtime as something like
     * <br>MY_APP_ADDRESS=machine.hostname:35012 at runtime
     */
    Map<String, String> getEnvVariables();
}
