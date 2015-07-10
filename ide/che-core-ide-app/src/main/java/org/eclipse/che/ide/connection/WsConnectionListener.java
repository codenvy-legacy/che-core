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
package org.eclipse.che.ide.connection;

import org.eclipse.che.ide.api.ConnectionClosedInformer;
import org.eclipse.che.ide.api.event.HttpSessionDestroyedEvent;
import org.eclipse.che.ide.util.loging.Log;
import org.eclipse.che.ide.websocket.MessageBus;
import org.eclipse.che.ide.websocket.events.ConnectionClosedHandler;
import org.eclipse.che.ide.websocket.events.ConnectionOpenedHandler;
import org.eclipse.che.ide.websocket.events.WebSocketClosedEvent;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;

/**
 * @author Evgen Vidolob
 */
public class WsConnectionListener implements ConnectionClosedHandler, ConnectionOpenedHandler {


    private EventBus                 eventBus;
    private MessageBus               messageBus;
    private ConnectionClosedInformer connectionClosedInformer;

    @Inject
    public WsConnectionListener(MessageBus messageBus,
                                EventBus eventBus,
                                ConnectionClosedInformer connectionClosedInformer) {
        this.eventBus = eventBus;
        this.messageBus = messageBus;
        this.connectionClosedInformer = connectionClosedInformer;
        messageBus.addOnCloseHandler(this);
    }

    @Override
    public void onClose(WebSocketClosedEvent event) {
        messageBus.removeOnCloseHandler(this);
        Log.info(getClass(), "WebSocket is closed, the status code is " + event.getCode() + ", the reason is " + event.getReason());

        if (event.getCode() == WebSocketClosedEvent.CLOSE_NORMAL && "Http session destroyed".equals(event.getReason())) {
            eventBus.fireEvent(new HttpSessionDestroyedEvent());
            return;
        }
        connectionClosedInformer.onConnectionClosed(event);
    }

    @Override
    public void onOpen() {
        messageBus.addOnCloseHandler(this);
    }
}
