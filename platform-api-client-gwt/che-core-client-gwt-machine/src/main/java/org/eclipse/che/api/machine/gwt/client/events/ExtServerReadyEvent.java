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
package org.eclipse.che.api.machine.gwt.client.events;

import com.google.gwt.event.shared.GwtEvent;

/**
 * @author Artem Zatsarynnyi
 */
public class ExtServerReadyEvent extends GwtEvent<ExtServerReadyHandler> {

    /** Type class used to register this event. */
    public static Type<ExtServerReadyHandler> TYPE = new Type<>();

    @Override
    public Type<ExtServerReadyHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(ExtServerReadyHandler handler) {
        handler.onExtServerReady(this);
    }
}
