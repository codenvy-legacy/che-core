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
package org.eclipse.che.api.core.notification;

import org.eclipse.che.commons.lang.Pair;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.everrest.websockets.client.BaseClientMessageListener;
import org.everrest.websockets.client.WSClient;
import org.everrest.websockets.message.JsonMessageConverter;
import org.everrest.websockets.message.MessageConversionException;
import org.everrest.websockets.message.MessageConverter;
import org.everrest.websockets.message.RESTfulOutputMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Receives event over websocket and publish them to the local EventsService.
 *
 * @author andrew00x
 */
@Singleton
public final class WSocketEventBusClient {
    private static final Logger LOG = LoggerFactory.getLogger(WSocketEventBusClient.class);

    private static final long wsConnectionTimeout = 2000;

    private final EventService                         eventService;
    private final Pair<String, String>[]               eventSubscriptions;
    private final ClientEventPropagationPolicy         policy;
    private final MessageConverter                     messageConverter;
    private final ConcurrentMap<URI, Future<WSClient>> connections;
    private final AtomicBoolean                        start;

    private ExecutorService executor;

    @Inject
    public WSocketEventBusClient(EventService eventService,
                                 @Nullable @Named("notification.client.event_subscriptions") Pair<String, String>[] eventSubscriptions,
                                 @Nullable ClientEventPropagationPolicy policy) {
        this.eventService = eventService;
        this.eventSubscriptions = eventSubscriptions;
        this.policy = policy;

        messageConverter = new JsonMessageConverter();
        connections = new ConcurrentHashMap<>();
        start = new AtomicBoolean(false);
    }

    @PostConstruct
    void start() {
        if (start.compareAndSet(false, true)) {
            if (policy != null) {
                eventService.subscribe(new EventSubscriber<Object>() {
                    @Override
                    public void onEvent(Object event) {
                        propagate(event);
                    }
                });
            }
            if (eventSubscriptions != null) {
                final Map<URI, Set<String>> cfg = new HashMap<>();
                for (Pair<String, String> service : eventSubscriptions) {
                    try {
                        final URI key = new URI(service.first);
                        Set<String> values = cfg.get(key);
                        if (values == null) {
                            cfg.put(key, values = new LinkedHashSet<>());
                        }
                        if (service.second != null) {
                            values.add(service.second);
                        }
                    } catch (URISyntaxException e) {
                        LOG.error(e.getMessage(), e);
                    }
                }
                if (!cfg.isEmpty()) {
                    executor = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("WSocketEventBusClient")
                                                                                       .setDaemon(true).build());
                    for (Map.Entry<URI, Set<String>> entry : cfg.entrySet()) {
                        executor.execute(new ConnectTask(entry.getKey(), entry.getValue()));
                    }
                }
            }
        }
    }

    protected void propagate(Object event) {
        for (Future<WSClient> future : connections.values()) {
            if (future.isDone()) {
                try {
                    final WSClient client = future.get();
                    if (policy.shouldPropagated(client.getUri(), event)) {
                        client.send(messageConverter.toString(Messages.clientMessage(event)));
                    }
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        }
    }

    @PreDestroy
    void stop() {
        if (start.compareAndSet(true, false) && executor != null) {
            executor.shutdownNow();
        }
    }

    private void connect(final URI wsUri, final Collection<String> channels) throws IOException {
        Future<WSClient> clientFuture = connections.get(wsUri);
        if (clientFuture == null) {
            FutureTask<WSClient> newFuture = new FutureTask<>(new Callable<WSClient>() {
                @Override
                public WSClient call() throws IOException, MessageConversionException {
                    WSClient wsClient = new WSClient(wsUri, new WSocketListener(wsUri, channels));
                    wsClient.connect(wsConnectionTimeout);
                    return wsClient;
                }
            });
            clientFuture = connections.putIfAbsent(wsUri, newFuture);
            if (clientFuture == null) {
                clientFuture = newFuture;
                newFuture.run();
            }
        }
        boolean connected = false;
        try {
            clientFuture.get(); // wait for connection
            connected = true;
        } catch (ExecutionException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof Error) {
                throw (Error)cause;
            } else if (cause instanceof RuntimeException) {
                throw (RuntimeException)cause;
            } else if (cause instanceof IOException) {
                throw (IOException)cause;
            }
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            if (!connected) {
                connections.remove(wsUri);
            }
        }
    }

    private class WSocketListener extends BaseClientMessageListener {
        final URI         wsUri;
        final Set<String> channels;

        WSocketListener(URI wsUri, Collection<String> channels) {
            this.wsUri = wsUri;
            this.channels = new HashSet<>(channels);
        }

        @Override
        public void onClose(int status, String message) {
            connections.remove(wsUri);
            LOG.debug("Close connection to {}. ", wsUri);
            if (start.get()) {
                executor.execute(new ConnectTask(wsUri, channels));
            }
        }

        @Override
        public void onMessage(String data) {
            try {
                final RESTfulOutputMessage message = messageConverter.fromString(data, RESTfulOutputMessage.class);
                if (message != null && message.getHeaders() != null) {
                    for (org.everrest.websockets.message.Pair header : message.getHeaders()) {
                        if ("x-everrest-websocket-channel".equals(header.getName())) {
                            final String channel = header.getValue();
                            if (channel != null && channels.contains(channel)) {
                                final Object event = Messages.restoreEventFromBroadcastMessage(message);
                                if (event != null) {
                                    eventService.publish(event);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }

        @Override
        public void onOpen(WSClient client) {
            LOG.debug("Open connection to {}. ", wsUri);
            for (String channel : channels) {
                try {
                    client.send(messageConverter.toString(Messages.subscribeChannelMessage(channel)));
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        }
    }

    private class ConnectTask implements Runnable {
        final URI                wsUri;
        final Collection<String> channels;

        ConnectTask(URI wsUri, Collection<String> channels) {
            this.wsUri = wsUri;
            this.channels = channels;
        }

        @Override
        public void run() {
            for (; ; ) {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                try {
                    connect(wsUri, channels);
                    return;
                } catch (IOException e) {
                    LOG.error(String.format("Failed connect to %s", wsUri), e);
                    synchronized (this) {
                        try {
                            wait(wsConnectionTimeout * 2);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }
            }
        }
    }
}
