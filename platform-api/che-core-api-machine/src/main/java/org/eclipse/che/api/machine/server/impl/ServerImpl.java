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
package org.eclipse.che.api.machine.server.impl;

import com.google.common.base.Objects;

import org.eclipse.che.api.machine.shared.Server;

/**
 * @author Alexander Garagatyi
 */
public class ServerImpl implements Server {
    private String address;

    public ServerImpl() {
    }

    public ServerImpl(String address) {
        this.address = address;
    }

    @Override
    public String getAddress() {
        return address;
    }

    public ServerImpl setAddress(String address) {
        this.address = address;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServerImpl)) return false;
        ServerImpl server = (ServerImpl)o;
        return Objects.equal(address, server.address);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(address);
    }
}
