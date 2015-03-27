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
package org.eclipse.che.ide.websocket.events;

/**
 * Event is fired, when WebSocket connection is closed.
 *
 * @author <a href="mailto:azatsarynnyy@exoplatform.org">Artem Zatsarynnyy</a>
 * @version $Id: WebSocketClosedEvent.java Jun 18, 2012 14:33:50 PM azatsarynnyy $
 */
public class WebSocketClosedEvent {

    /**
     * Normal closure; the connection successfully completed whatever purpose for which it was created.
     */
    public static final int CLOSE_NORMAL = 1000;

    /**
     * The endpoint is going away, either because of a server failure or because the browser is navigating away from the page that opened
     * the connection.
     */
    public static final int CLOSE_GOING_AWAY = 1001;

    /**
     * The endpoint is terminating the connection due to a protocol error.
     */
    public static final int CLOSE_PROTOCOL_ERROR = 1002;
    /**
     * The connection is being terminated because the endpoint received data of a type it cannot accept (for example, a text-only endpoint
     * received binary data).
     */
    public static final int CLOSE_UNSUPPORTED    = 1003;
    /**
     * Indicates that no status code was provided even though one was expected.
     */
    public static final int CLOSE_NO_STATUS      = 1005;
    /**
     * Reserved. Used to indicate that a connection was closed abnormally (that is, with no close frame being sent) when a status code is
     * expected.
     */
    public static final int CLOSE_ABNORMAL       = 1006;

    /**
     * The WebSocket connection close code provided by the server.
     */
    private int code;

    /** A string indicating the reason the server closed the connection. This is specific to the particular server and sub-protocol. */
    private String reason;

    /** Indicates whether or not the connection was cleanly closed. */
    private boolean wasClean;

    public WebSocketClosedEvent() {
    }

    public WebSocketClosedEvent(int code, String reason, boolean wasClean) {
        this.code = code;
        this.reason = reason;
        this.wasClean = wasClean;
    }

    /**
     * Returns close code.
     *
     * @return close code
     */
    public int getCode() {
        return code;
    }

    /**
     * Returns the reason closed the connection.
     *
     * @return reason
     */
    public String getReason() {
        return reason;
    }

    /**
     * Checks weather the connection was cleanly closed.
     *
     * @return <code>true</code> when WebSocket connection was cleanly closed;
     * <code>false</code> when WebSocket connection was not cleanly closed
     */
    public boolean wasClean() {
        return wasClean;
    }
}