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
package org.eclipse.che.api.workspace.server.model.impl.stack;

import java.util.Objects;

/**
 * Implementation of {@link StackSource}
 *
 * @author Alexander Andrienko
 */
public class StackSourceImpl implements StackSource {

    private String type;
    private String origin;

    public StackSourceImpl(String type, String origin) {
        this.type = type;
        this.origin = origin;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getOrigin() {
        return origin;
    }

    @Override
    public String toString() {
        return "StackSourceImpl{" +
               "type='" + type + '\'' +
               ", origin='" + origin + '\'' +
               '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof StackSourceImpl)) {
            return false;
        }
        StackSourceImpl another = (StackSourceImpl)obj;
        return Objects.equals(type, another.type) && Objects.equals(origin, another.origin);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + Objects.hashCode(type);
        hash = 31 * hash + Objects.hashCode(origin);
        return hash;
    }
}
