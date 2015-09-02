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

import org.eclipse.che.api.core.model.machine.MachineSource;
import org.eclipse.che.api.core.model.machine.Server;
import org.eclipse.che.api.core.model.workspace.Machine;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static java.util.stream.Collectors.toMap;

/**
 * Data object for {@link Machine}.
 *
 * @author Eugene Voevodin
 */
public class MachineImpl extends MachineConfigImpl implements Machine {

    public static MachineImplBuilder builder() {
        return new MachineImplBuilder();
    }

    private String                  id;
    private Map<String, String>     properties;
    private Map<String, ServerImpl> servers;

    public MachineImpl(boolean isDev,
                       String name,
                       String type,
                       MachineSource source,
                       int memorySize,
                       String outputChannel,
                       String statusChannel,
                       String id,
                       Map<String, String> properties,
                       Map<String, ? extends Server> servers) {
        super(isDev, name, type, source, memorySize, outputChannel, statusChannel);
        this.id = id;
        this.properties = properties;
        if (servers != null) {
            this.servers = servers.values()
                                  .stream()
                                  .collect(toMap(Server::getUrl, ServerImpl::new));
        }
    }

    public MachineImpl(Machine machine) {
        this(machine.isDev(),
             machine.getName(),
             machine.getType(),
             machine.getSource(),
             machine.getMemorySize(),
             machine.getOutputChannel(),
             machine.getStatusChannel(),
             machine.getId(),
             machine.getProperties(),
             machine.getServers());
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Map<String, String> getProperties() {
        if (properties == null) {
            properties = new HashMap<>();
        }
        return properties;
    }

    @Override
    public Map<String, ServerImpl> getServers() {
        if (servers == null) {
            servers = new HashMap<>();
        }
        return servers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MachineImpl)) return false;
        if (!super.equals(o)) return false;
        final MachineImpl other = (MachineImpl)o;
        return Objects.equals(id, other.id) &&
               Objects.equals(properties, other.properties) &&
               Objects.equals(servers, other.servers);
    }

    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = hash * 31 + Objects.hashCode(id);
        hash = hash * 31 + getProperties().hashCode();
        hash = hash * 31 + getServers().hashCode();
        return hash;
    }

    /**
     * Helps to build complex {@link MachineImpl machine impl}.
     *
     * @see MachineImpl#builder()
     */
    public static class MachineImplBuilder {

        private boolean                       isDev;
        private int                           memorySize;
        private String                        name;
        private String                        type;
        private String                        id;
        private String                        outputChannel;
        private String                        statusChannel;
        private MachineSource                 source;
        private Map<String, String>           properties;
        private Map<String, ? extends Server> servers;

        public MachineImpl build() {
            return new MachineImpl(isDev,
                                   name,
                                   type,
                                   source,
                                   memorySize,
                                   outputChannel,
                                   statusChannel,
                                   id,
                                   properties,
                                   servers);
        }

        public MachineImplBuilder setDev(boolean isDev) {
            this.isDev = isDev;
            return this;
        }

        public MachineImplBuilder setMemorySize(int memorySize) {
            this.memorySize = memorySize;
            return this;
        }

        public MachineImplBuilder setName(String name) {
            this.name = name;
            return this;
        }

        public MachineImplBuilder setType(String type) {
            this.type = type;
            return this;
        }

        public MachineImplBuilder setId(String id) {
            this.id = id;
            return this;
        }

        public MachineImplBuilder setOutputChannel(String outputChannel) {
            this.outputChannel = outputChannel;
            return this;
        }

        public MachineImplBuilder setSource(MachineSource source) {
            this.source = source;
            return this;
        }

        public MachineImplBuilder setProperties(Map<String, String> properties) {
            this.properties = properties;
            return this;
        }

        public MachineImplBuilder setServers(Map<String, ? extends Server> servers) {
            this.servers = servers;
            return this;
        }

        public MachineImplBuilder setStatusChannel(String statusChannel) {
            this.statusChannel = statusChannel;
            return this;
        }
    }
}
