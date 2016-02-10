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
package org.eclipse.che.api.machine.gwt.client;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.machine.gwt.client.events.ExtServerStateEvent;
import org.eclipse.che.api.machine.gwt.client.events.ExtServerReadyEvent;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.callback.AsyncPromiseHelper;
import org.eclipse.che.ide.ui.loaders.initialization.InitialLoadingInfo;
import org.eclipse.che.ide.ui.loaders.initialization.LoaderPresenter;
import org.eclipse.che.ide.util.loging.Log;
import org.eclipse.che.ide.websocket.MessageBus;
import org.eclipse.che.ide.websocket.MessageBusProvider;
import org.eclipse.che.ide.websocket.WebSocket;
import org.eclipse.che.ide.websocket.events.ConnectionClosedHandler;
import org.eclipse.che.ide.websocket.events.ConnectionErrorHandler;
import org.eclipse.che.ide.websocket.events.ConnectionOpenedHandler;
import org.eclipse.che.ide.websocket.events.WebSocketClosedEvent;

import static org.eclipse.che.ide.ui.loaders.initialization.InitialLoadingInfo.Operations.EXTENSION_SERVER_BOOTING;
import static org.eclipse.che.ide.ui.loaders.initialization.OperationInfo.Status.ERROR;
import static org.eclipse.che.ide.ui.loaders.initialization.OperationInfo.Status.IN_PROGRESS;
import static org.eclipse.che.ide.ui.loaders.initialization.OperationInfo.Status.SUCCESS;

/**
 * @author Roman Nikitenko
 * @author Valeriy Svydenko
 */
@Singleton
public class ExtServerStateController implements ConnectionOpenedHandler, ConnectionClosedHandler, ConnectionErrorHandler {

    private final Timer              retryConnectionTimer;
    private final EventBus           eventBus;
    private final MessageBusProvider messageBusProvider;
    private final InitialLoadingInfo initialLoadingInfo;
    private final LoaderPresenter    loader;

    private MessageBus                messageBus;
    private ExtServerState            state;
    private String                    wsUrl;
    private int                       countRetry;
    private AsyncCallback<MessageBus> messageBusCallback;
    private WebSocket                 testConnection; //use it only for testing state of ext-service

    @Inject
    public ExtServerStateController(EventBus eventBus,
                                    LoaderPresenter loader,
                                    MessageBusProvider messageBusProvider,
                                    InitialLoadingInfo initialLoadingInfo) {
        this.loader = loader;
        this.eventBus = eventBus;
        this.messageBusProvider = messageBusProvider;
        this.initialLoadingInfo = initialLoadingInfo;

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
        this.countRetry = 50;
        this.state = ExtServerState.STOPPED;

        initialLoadingInfo.setOperationStatus(EXTENSION_SERVER_BOOTING.getValue(), IN_PROGRESS);
        connect();
    }

    @Override
    public void onClose(WebSocketClosedEvent event) {
        Log.info(getClass(), "Test WS connection closed with code " + event.getCode() + " reason: " + event.getReason() +
                             " workspace agent started well ");
    }

    @Override
    public void onError() {
        if (countRetry > 0) {
            retryConnectionTimer.schedule(1000);
        } else {
            state = ExtServerState.STOPPED;
            initialLoadingInfo.setOperationStatus(EXTENSION_SERVER_BOOTING.getValue(), ERROR);
            loader.hide();
            eventBus.fireEvent(ExtServerStateEvent.createExtServerStoppedEvent());
        }
    }

    @Override
    public void onOpen() {
        testConnection.close(); //close testing connection now we know ext-server already start
        state = ExtServerState.STARTED;
        initialLoadingInfo.setOperationStatus(EXTENSION_SERVER_BOOTING.getValue(), SUCCESS);
        loader.hide();

        messageBus = messageBusProvider.createMachineMessageBus(wsUrl);
        messageBus.addOnOpenHandler(new ConnectionOpenedHandler() {
            @Override
            public void onOpen() {
                messageBus.removeOnOpenHandler(this);
//                eventBus.fireEvent(ExtServerStateEvent.createExtServerStartedEvent());
                // TODO: think about better event name
                eventBus.fireEvent(new ExtServerReadyEvent());
            }
        });

        if (messageBusCallback != null) {
            messageBusCallback.onSuccess(messageBus);
        }
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
        testConnection = WebSocket.create(wsUrl);
        testConnection.setOnOpenHandler(this);
        testConnection.setOnCloseHandler(this);
        testConnection.setOnErrorHandler(this);
    }
}
