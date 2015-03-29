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
package org.eclipse.che.api.runner.internal;

import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.runner.dto.RunRequest;

import org.eclipse.che.dto.server.DtoFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Runner configuration for particular run process.
 *
 * @author andrew00x
 */
public class RunnerConfiguration {
    private final int        memory;
    private final List<Link> links;
    private final RunRequest request;

    private String              host;
    private Map<String, String> portMapping;
    private String              debugHost;
    private int                 debugPort;
    private java.io.File        recipeFile;

    public RunnerConfiguration(int memory, RunRequest request) {
        this.memory = memory;
        this.request = request;
        this.links = new ArrayList<>(2);
        this.portMapping = new HashMap<>(4);
        this.debugPort = -1;
    }

    public RunnerConfiguration(int memory, RunRequest request, List<Link> links) {
        this.memory = memory;
        this.request = request;
        this.links = new ArrayList<>(links);
        this.portMapping = new HashMap<>(4);
        this.debugPort = -1;
    }

    public int getMemory() {
        return memory;
    }

    /**
     * Get application links. List of links is modifiable.
     *
     * @return application links
     */
    public List<Link> getLinks() {
        return links;
    }

    public RunRequest getRequest() {
        return DtoFactory.getInstance().clone(request);
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Map<String, String> getPortMapping() {
        return portMapping;
    }

    public String getDebugHost() {
        return debugHost;
    }

    public void setDebugHost(String debugHost) {
        this.debugHost = debugHost;
    }

    public int getDebugPort() {
        return debugPort;
    }

    public void setDebugPort(int debugPort) {
        this.debugPort = debugPort;
    }

    public java.io.File getRecipeFile() {
        return recipeFile;
    }

    public void setRecipeFile(java.io.File recipeFile) {
        this.recipeFile = recipeFile;
    }

    @Override
    public String toString() {
        return "RunnerConfiguration{" +
               "memory=" + memory +
               ", links=" + links +
               ", request=" + request +
               ", host='" + host + '\'' +
               ", portMapping=" + portMapping +
               ", debugHost='" + debugHost + '\'' +
               ", debugPort=" + debugPort +
               '}';
    }
}
