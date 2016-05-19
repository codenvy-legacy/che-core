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
package org.eclipse.che.ide.websocket;

import org.eclipse.che.ide.rest.HTTPHeader;
import org.eclipse.che.ide.util.ListenerManager;
import org.eclipse.che.ide.util.loging.Log;
import org.eclipse.che.ide.websocket.events.ConnectionClosedHandler;
import org.eclipse.che.ide.websocket.events.ConnectionErrorHandler;
import org.eclipse.che.ide.websocket.events.ConnectionOpenedHandler;
import org.eclipse.che.ide.websocket.events.MessageHandler;
import org.eclipse.che.ide.websocket.events.MessageReceivedEvent;
import org.eclipse.che.ide.websocket.events.ReplyHandler;
import org.eclipse.che.ide.websocket.events.WebSocketClosedEvent;
import org.eclipse.che.ide.websocket.rest.Pair;
import org.eclipse.che.ide.websocket.rest.RequestCallback;
import org.eclipse.che.ide.websocket.rest.SubscriptionHandler;
import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.user.client.Timer;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The implementation of {@link MessageBus}.
 *
 * @author Artem Zatsarynnyy
 */
@Singleton
public class MessageBusImpl implements MessageBus {
    /** Period (in milliseconds) to send heartbeat pings. */
    private static final int    HEARTBEAT_PERIOD                     = 50 * 1000;
    /** Period (in milliseconds) between reconnection attempts after connection has been closed. */
    private final static int    FREQUENTLY_RECONNECTION_PERIOD       = 2 * 1000;
    /**
     * Period (in milliseconds) between reconnection attempts after all previous
     * <code>MAX_FREQUENTLY_RECONNECTION_ATTEMPTS</code> attempts is failed.
     */
    private final static int    SELDOM_RECONNECTION_PERIOD           = 60 * 1000;
    /** Max. number of attempts to reconnect for every <code>FREQUENTLY_RECONNECTION_PERIOD</code> ms. */
    private final static int    MAX_FREQUENTLY_RECONNECTION_ATTEMPTS = 5;
    /** Max. number of attempts to reconnect for every <code>SELDOM_RECONNECTION_PERIOD</code> ms. */
    private final static int    MAX_SELDOM_RECONNECTION_ATTEMPTS     = 5;
    private static final String MESSAGE_TYPE_HEADER_NAME             = "x-everrest-websocket-message-type";
    /** Timer for sending heartbeat pings to prevent autoclosing an idle WebSocket connection. */
    private final        Timer  heartbeatTimer                       = new Timer() {
        @Override
        public void run() {
            Message message = getHeartbeatMessage();
            try {
                send(message, null);
            } catch (WebSocketException e) {
                if (getReadyState() == ReadyState.CLOSED) {
                    wsListener.onClose(new WebSocketClosedEvent());
                } else {
                    Log.error(MessageBus.class, e);
                }
            }
        }
    };
    private final Message heartbeatMessage;
    /** Counter of attempts to reconnect. */
    private       int     frequentlyReconnectionAttemptsCounter;
    /** Timer for reconnecting WebSocket. */
    private Timer frequentlyReconnectionTimer = new Timer() {
        @Override
        public void run() {
            if (frequentlyReconnectionAttemptsCounter == MAX_FREQUENTLY_RECONNECTION_ATTEMPTS) {
                cancel();
                seldomReconnectionTimer.scheduleRepeating(SELDOM_RECONNECTION_PERIOD);
                return;
            }
            frequentlyReconnectionAttemptsCounter++;
            initialize();
        }
    };
    /** Counter of attempts to reconnect. */
    private int seldomReconnectionAttemptsCounter;
    /** Timer for reconnecting WebSocket. */
    private Timer seldomReconnectionTimer = new Timer() {
        @Override
        public void run() {
            if (seldomReconnectionAttemptsCounter == MAX_SELDOM_RECONNECTION_ATTEMPTS) {
                cancel();
                return;
            }
            seldomReconnectionAttemptsCounter++;
            initialize();
        }
    };
    /** Internal {@link WebSocket} instance. */
    private WebSocket ws;
    /** WebSocket server URL. */
    private String    url;
    /** Map of the message identifier to the {@link org.eclipse.che.ide.websocket.events.ReplyHandler}. */
    private Map<String, RequestCallback>             requestCallbackMap       = new HashMap<>();
    private Map<String, ReplyHandler>                replyCallbackMap         = new HashMap<>();
    /** Map of the channel to the subscribers. */
    private Map<String, List<MessageHandler>>        channelToSubscribersMap  = new HashMap<>();
    private ListenerManager<ConnectionOpenedHandler> connectionOpenedHandlers = ListenerManager.create();
    private ListenerManager<ConnectionClosedHandler> connectionClosedHandlers = ListenerManager.create();
    private ListenerManager<ConnectionErrorHandler>  connectionErrorHandlers  = ListenerManager.create();
    private WsListener wsListener;
    private List<String> messages2send = new ArrayList<>();

    /**
     * Creates new {@link MessageBus} instance.
     *
     * @param url
     *         WebSocket server URL
     */
    @Inject
    public MessageBusImpl(@WebSocketUrl String url) {
        this.url = url;

        MessageBuilder builder = new MessageBuilder(RequestBuilder.POST, null);
        builder.header("x-everrest-websocket-message-type", "ping");
        heartbeatMessage = builder.build();

        if (isSupported()) {
            initialize();
        }
    }

    /** Initialize the message bus. */
    private void initialize() {
        ws = WebSocket.create(url);
        wsListener = new WsListener();
        ws.setOnMessageHandler(this);
        ws.setOnOpenHandler(wsListener);
        ws.setOnCloseHandler(wsListener);
        ws.setOnErrorHandler(wsListener);
//      callbackMap.clear();
//      channelToSubscribersMap.clear();
    }

    /**
     * Checks if the browser has support for WebSockets.
     *
     * @return <code>true</code> if WebSocket is supported;
     * <code>false</code> if it's not
     */
    private boolean isSupported() {
        return WebSocket.isSupported();
    }

    /** {@inheritDoc} */
    @Override
    public ReadyState getReadyState() {
        if (ws == null) {
            return ReadyState.CLOSED;
        }

        switch (ws.getReadyState()) {
            case 0:
                return ReadyState.CONNECTING;
            case 1:
                return ReadyState.OPEN;
            case 2:
                return ReadyState.CLOSING;
            case 3:
                return ReadyState.CLOSED;
            default:
                return ReadyState.CLOSED;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message message = parseMessage(event.getMessage());

        // http code 202 is "Accepted": The request has been accepted for processing,
        // but the processing has not been completed.
        // At this point, we ignore this code, since the request might or might not eventually be acted upon,
        // as it might be disallowed when processing actually takes place.
        if (message.getResponseCode() == 202) {
            return;
        }

        //TODO Should be revised to remove
        List<Pair> headers = message.getHeaders().toList();
        if (headers != null) {
            for (Pair header : headers) {
                if (HTTPHeader.LOCATION.equals(header.getName()) && header.getValue().contains("async/")) {
                    return;
                }
            }
        }

        if (getChannel(message) != null) {
            // this is a message received by subscription
            processSubscriptionMessage(message);
        } else {
            String uuid = message.getStringField(MessageBuilder.UUID_FIELD);
            ReplyHandler replyCallback = replyCallbackMap.remove(uuid);
            if (replyCallback != null) {
                replyCallback.onReply(message.getBody());
            } else {
                RequestCallback requestCallback = requestCallbackMap.remove(uuid);
                if (requestCallback != null) {
                    requestCallback.onReply(message);
                }
            }
        }
    }

    /**
     * Process the {@link Message} that received by subscription.
     *
     * @param message
     *         {@link Message}
     */
    private void processSubscriptionMessage(Message message) {
        String channel = getChannel(message);
        List<MessageHandler> subscribersSet = channelToSubscribersMap.get(channel);
        if (subscribersSet != null) {
            for (MessageHandler handler : subscribersSet) {
                //TODO this is nasty, need refactor this
                if (handler instanceof SubscriptionHandler) {
                    ((SubscriptionHandler)handler).onMessage(message);
                } else {
                    handler.onMessage(message.getBody());
                }
            }
        }
    }

    /**
     * Parse text message to {@link Message} object.
     *
     * @param message
     *         text message
     * @return {@link Message}
     */
    private Message parseMessage(String message) {
        return Message.deserialize(message);
    }

    /**
     * Get message for heartbeat request
     *
     * @return {@link Message}
     */
    private Message getHeartbeatMessage() {
        return heartbeatMessage;
    }

    /**
     * Get channel from which {@link Message} was received.
     *
     * @param message
     *         {@link Message}
     * @return channel identifier or <code>null</code> if message is invalid.
     */
    private String getChannel(Message message) {
        List<Pair> headers = message.getHeaders().toList();

        if (headers != null) {
            for (Pair header : headers) {
                if ("x-everrest-websocket-channel".equals(header.getName())) {
                    return header.getValue();
                }
            }
        }

        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void send(Message message, RequestCallback callback) throws WebSocketException {
        checkWebSocketConnectionState();
        final String uuid = message.getStringField(MessageBuilder.UUID_FIELD);
        internalSend(uuid, message.serialize(), callback);
        if (callback != null) {
            callback.getLoader().show();
            if (callback.getStatusHandler() != null) {
                callback.getStatusHandler().requestInProgress(uuid);
            }
        }
    }

    /**
     * Send text message.
     *
     * @param uuid
     *         a message identifier
     * @param message
     *         message to send
     * @param callback
     *         callback for receiving reply to message
     * @throws WebSocketException
     *         throws if an any error has occurred while sending data
     */
    private void internalSend(String uuid, String message, RequestCallback callback) throws WebSocketException {
        checkWebSocketConnectionState();

        if (callback != null) {
            requestCallbackMap.put(uuid, callback);
        }

        send(message);
    }

    /**
     * Transmit text data over WebSocket.
     *
     * @param message
     *         text message
     * @throws WebSocketException
     *         throws if an any error has occurred while sending data,
     *         e.g.: WebSocket is not supported by browser, WebSocket connection is not opened
     */
    private void send(String message) throws WebSocketException {
//        checkWebSocketConnectionState();
        if (getReadyState() != ReadyState.OPEN) {
            messages2send.add(message);
            return;
        }
        try {
            ws.send(message);
        } catch (JavaScriptException e) {
            throw new WebSocketException(e.getMessage(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void send(String address, String message) throws WebSocketException {
        send(address, message, null);
    }

    /** {@inheritDoc} */
    @Override
    public void send(String address, String message, ReplyHandler replyHandler) throws WebSocketException {
        checkWebSocketConnectionState();

        MessageBuilder builder = new MessageBuilder(RequestBuilder.POST, address);
        builder.header("content-type", "application/json")
               .data(message);

        Message requestMessage = builder.build();

        String textMessage = requestMessage.serialize();
        String uuid = requestMessage.getStringField(MessageBuilder.UUID_FIELD);
        internalSend(uuid, textMessage, replyHandler);
    }

    /**
     * Send text message.
     *
     * @param uuid
     *         a message identifier
     * @param message
     *         message to send
     * @param callback
     *         callback for receiving reply to message
     * @throws WebSocketException
     *         throws if an any error has occurred while sending data
     */
    private void internalSend(String uuid, String message, ReplyHandler callback) throws WebSocketException {
        checkWebSocketConnectionState();

        if (callback != null) {
            replyCallbackMap.put(uuid, callback);
        }

        send(message);
    }

    /**
     * Send message with subscription info.
     *
     * @param channel
     *         channel identifier
     * @throws WebSocketException
     *         throws if an any error has occurred while sending data
     */
    private void sendSubscribeMessage(String channel) throws WebSocketException {
        MessageBuilder builder = new MessageBuilder(RequestBuilder.POST, null);
        builder.header(MESSAGE_TYPE_HEADER_NAME, "subscribe-channel")
               .data("{\"channel\":\"" + channel + "\"}");

        Message message = builder.build();
        send(message, null);
    }

    /**
     * Send message with unsubscription info.
     *
     * @param channel
     *         channel identifier
     * @throws WebSocketException
     *         throws if an any error has occurred while sending data
     */
    private void sendUnsubscribeMessage(String channel) throws WebSocketException {
        MessageBuilder builder = new MessageBuilder(RequestBuilder.POST, null);
        builder.header(MESSAGE_TYPE_HEADER_NAME, "unsubscribe-channel")
               .data("{\"channel\":\"" + channel + "\"}");

        Message message = builder.build();
        send(message, null);
    }

    /** {@inheritDoc} */
    @Override
    public void addOnOpenHandler(ConnectionOpenedHandler handler) {
        connectionOpenedHandlers.add(handler);
    }

    /** {@inheritDoc} */
    @Override
    public void addOnCloseHandler(ConnectionClosedHandler handler) {
        connectionClosedHandlers.add(handler);
    }

    @Override
    public void removeOnCloseHandler(ConnectionClosedHandler handler) {
        connectionClosedHandlers.remove(handler);
    }

    /** {@inheritDoc} */
    @Override
    public void addOnErrorHandler(ConnectionErrorHandler handler) {
        connectionErrorHandlers.add(handler);
    }

    /** {@inheritDoc} */
    @Override
    public void subscribe(String channel, MessageHandler handler) throws WebSocketException {
        checkWebSocketConnectionState();

        List<MessageHandler> subscribersSet = channelToSubscribersMap.get(channel);
        if (subscribersSet != null) {
            subscribersSet.add(handler);
            return;
        }
        subscribersSet = new ArrayList<>();
        subscribersSet.add(handler);
        channelToSubscribersMap.put(channel, subscribersSet);
        sendSubscribeMessage(channel);
    }

    /** {@inheritDoc} */
    @Override
    public void unsubscribe(String channel, MessageHandler handler) throws WebSocketException {
        checkWebSocketConnectionState();

        List<MessageHandler> subscribersSet = channelToSubscribersMap.get(channel);
        if (subscribersSet == null) {
            throw new IllegalArgumentException("Handler not subscribed to any channel.");
        }

        if (subscribersSet.remove(handler) && subscribersSet.isEmpty()) {
            channelToSubscribersMap.remove(channel);
            sendUnsubscribeMessage(channel);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isHandlerSubscribed(MessageHandler handler, String channel) {
        List<MessageHandler> set = channelToSubscribersMap.get(channel);
        if (set == null) {
            return false;
        }
        return set.contains(handler);
    }

    /**
     * Check WebSocket connection and throws {@link WebSocketException} if WebSocket connection is not ready to use.
     *
     * @throws WebSocketException
     *         throws if WebSocket connection is not ready to use
     */
    private void checkWebSocketConnectionState() throws WebSocketException {
        if (!isSupported()) {
            throw new WebSocketException("WebSocket is not supported.");
        }

        if (getReadyState() != ReadyState.OPEN) {
            throw new WebSocketException("WebSocket is not opened.");
        }
    }

    private class WsListener implements ConnectionOpenedHandler, ConnectionClosedHandler, ConnectionErrorHandler {

        @Override
        public void onClose(final WebSocketClosedEvent event) {
            heartbeatTimer.cancel();
            frequentlyReconnectionTimer.scheduleRepeating(FREQUENTLY_RECONNECTION_PERIOD);
            connectionClosedHandlers.dispatch(new ListenerManager.Dispatcher<ConnectionClosedHandler>() {
                @Override
                public void dispatch(ConnectionClosedHandler listener) {
                    listener.onClose(event);
                }
            });
        }

        @Override
        public void onError() {
            connectionErrorHandlers.dispatch(new ListenerManager.Dispatcher<ConnectionErrorHandler>() {
                @Override
                public void dispatch(ConnectionErrorHandler listener) {
                    listener.onError();
                }
            });
        }

        @Override
        public void onOpen() {
            // If the any timer has been started then stop it.
            if (frequentlyReconnectionAttemptsCounter > 0)
                frequentlyReconnectionTimer.cancel();
            if (seldomReconnectionAttemptsCounter > 0)
                seldomReconnectionTimer.cancel();

            frequentlyReconnectionAttemptsCounter = 0;
            seldomReconnectionAttemptsCounter = 0;
            heartbeatTimer.scheduleRepeating(HEARTBEAT_PERIOD);
            connectionOpenedHandlers.dispatch(new ListenerManager.Dispatcher<ConnectionOpenedHandler>() {
                @Override
                public void dispatch(ConnectionOpenedHandler listener) {
                    listener.onOpen();
                }
            });

            try {
                for (String message : messages2send) {
                    send(message);
                }
                messages2send.clear();
            } catch (WebSocketException e) {
                Log.error(MessageBusImpl.class, e);
            }
        }

    }
}