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
package org.eclipse.che.api.machine.gwt.client;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.callback.AsyncPromiseHelper;
import org.eclipse.che.ide.websocket.MessageBus;
import org.eclipse.che.ide.websocket.MessageBusImpl;
import org.eclipse.che.ide.websocket.WebSocket;
import org.eclipse.che.ide.websocket.events.ConnectionClosedHandler;
import org.eclipse.che.ide.websocket.events.ConnectionErrorHandler;
import org.eclipse.che.ide.websocket.events.ConnectionOpenedHandler;
import org.eclipse.che.ide.websocket.events.WebSocketClosedEvent;

/**
 * @author Roman Nikitenko
 * @author Valeriy Svydenko
 */
@Singleton
public class ExtServerStateController implements ConnectionOpenedHandler, ConnectionClosedHandler, ConnectionErrorHandler {

    private final Timer               retryConnectionTimer;
    private final EventBus            eventBus;

    private ExtServerState            state;
    private String                    wsUrl;
    private int                       countRetry;
    private MessageBus                messageBus;
    private AsyncCallback<MessageBus> messageBusCallback;

    @Inject
    public ExtServerStateController(EventBus eventBus) {
        this.eventBus = eventBus;
        retryConnectionTimer = new Timer() {
            @Override
            public void run() {
                connect();
                countRetry--;
            }
        };
    }

    public void initialize(String wsUrl) {
        this.wsUrl = wsUrl;
        this.countRetry = 5;
        this.state = ExtServerState.STOPPED;

        connect();
    }

    @Override
    public void onClose(WebSocketClosedEvent event) {
        if (state == ExtServerState.STARTED) {
            state = ExtServerState.STOPPED;
            eventBus.fireEvent(ExtServerStateEvent.createExtServerStoppedEvent());
        }
    }

    @Override
    public void onError() {
        if (countRetry > 0) {
            retryConnectionTimer.schedule(1000);
        } else {
            state = ExtServerState.STOPPED;
            eventBus.fireEvent(ExtServerStateEvent.createExtServerStoppedEvent());
        }
    }

    @Override
    public void onOpen() {
        state = ExtServerState.STARTED;
        eventBus.fireEvent(ExtServerStateEvent.createExtServerStartedEvent());
        messageBus = new MessageBusImpl(wsUrl);
        messageBusCallback.onSuccess(messageBus);
    }

    public ExtServerState getState() {
        return state;
    }

    public Promise<MessageBus> getMessageBus() {
        return AsyncPromiseHelper.createFromAsyncRequest(new AsyncPromiseHelper.RequestCall<MessageBus>() {
            @Override
            public void makeCall(AsyncCallback<MessageBus> callback) {
                if (messageBus != null) {
                    callback.onSuccess(messageBus);
                } else {
                    ExtServerStateController.this.messageBusCallback = callback;
                }
            }
        });
    }

    private void connect() {
        WebSocket socket = WebSocket.create(wsUrl);
        socket.setOnOpenHandler(this);
        socket.setOnCloseHandler(this);
        socket.setOnErrorHandler(this);
    }
}
