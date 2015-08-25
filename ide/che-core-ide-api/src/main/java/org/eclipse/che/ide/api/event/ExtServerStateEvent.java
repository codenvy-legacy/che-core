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
package org.eclipse.che.ide.api.event;

import com.google.gwt.event.shared.GwtEvent;

import static org.eclipse.che.ide.api.event.ExtServerState.STARTED;
import static org.eclipse.che.ide.api.event.ExtServerState.STOPPED;


/**
 * Event that describes the fact that extension server state has been changed.
 *
 * @author Roman Nikitenko
 */
public class ExtServerStateEvent extends GwtEvent<ExtServerStateHandler> {

    /** Type class used to register this event. */
    public static Type<ExtServerStateHandler> TYPE = new Type<>();
    private final ExtServerState extServerState;

    /**
     * Create new {@link ExtServerStateEvent}.
     *
     * @param state
     *         the type of action
     */
    protected ExtServerStateEvent(ExtServerState state) {
        this.extServerState = state;
    }

    /**
     * Creates a extension server started event.
     */
    public static ExtServerStateEvent createExtServerStartedEvent() {
        return new ExtServerStateEvent(STARTED);
    }

    /**
     * Creates a extension server stopped event.
     */
    public static ExtServerStateEvent createExtServerStoppedEvent() {
        return new ExtServerStateEvent(STOPPED);
    }

    @Override
    public Type<ExtServerStateHandler> getAssociatedType() {
        return TYPE;
    }

    /** @return the state of extension server */
    public ExtServerState getExtServerState() {
        return extServerState;
    }

    @Override
    protected void dispatch(ExtServerStateHandler handler) {
        switch (extServerState) {
            case STARTED:
                handler.onExtServerStarted(this);
                break;
            case STOPPED:
                handler.onExtServerStopped(this);
                break;
            default:
                break;
        }
    }
}
