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
package org.eclipse.che.api.builder;

/**
 * @author tareq.sha@gmail.com
 */
public class BuilderAdminEvent {

    public enum EventType {
        // The list of build servers has changes
        BUILD_SERVERS_CHANGED;
    }

    public static BuilderAdminEvent buildServersChangedEvent() {
        return new BuilderAdminEvent(EventType.BUILD_SERVERS_CHANGED);
    }

    /** Event type. */
    private EventType type;

    BuilderAdminEvent(EventType type) {
        this.type = type;
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + "type=" + type + '}';
    }
}
