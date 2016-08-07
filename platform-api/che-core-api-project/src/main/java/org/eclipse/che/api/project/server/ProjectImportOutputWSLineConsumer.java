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
package org.eclipse.che.api.project.server;

import org.eclipse.che.api.core.util.LineConsumer;
import org.eclipse.che.commons.lang.concurrent.ThreadLocalPropagateContext;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.everrest.core.impl.provider.json.JsonUtils;
import org.everrest.websockets.WSConnectionContext;
import org.everrest.websockets.message.ChannelBroadcastMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Send project import output to WS by skipping output messages written below the delay specified.
 */
public class ProjectImportOutputWSLineConsumer implements LineConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(ProjectImportOutputWSLineConsumer.class);

    protected final AtomicInteger            lineCounter;
    protected final String                   fPath;
    protected final String                   fWorkspace;
    protected final BlockingQueue<String>    lineToSendQueue;
    protected final ScheduledExecutorService executor;

    public ProjectImportOutputWSLineConsumer(String fPath, String fWorkspace, int delayBetweenMessages) {
        this.fPath = fPath;
        this.fWorkspace = fWorkspace;
        lineToSendQueue = new ArrayBlockingQueue<>(1024);
        executor = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder().setNameFormat(ProjectImportOutputWSLineConsumer.class.getSimpleName()+"-%d").setDaemon(true).build());
        executor.scheduleAtFixedRate(ThreadLocalPropagateContext.wrap(new Runnable() {
            @Override
            public void run() {
                String lineToSend = null;
                while (!lineToSendQueue.isEmpty()) {
                    lineToSend = lineToSendQueue.poll();
                }
                if (lineToSend == null) {
                    return;
                }
                sendMessage(lineToSend);
            }
        }), 0, delayBetweenMessages, TimeUnit.MILLISECONDS);
        lineCounter = new AtomicInteger(1);
    }

    @Override
    public void close() throws IOException {
        executor.shutdown();
    }

    @Override
    public void writeLine(String line) throws IOException {
        try {
            lineToSendQueue.put(line);
        } catch (InterruptedException ignored) {
            // ignore if interrupted
        }
    }

    protected void sendMessage(String line) {
        final ChannelBroadcastMessage bm = new ChannelBroadcastMessage();
        bm.setChannel("importProject:output:" + fWorkspace + ":" + fPath);
        bm.setBody(String.format("{\"num\":%d, \"line\":%s}",
                                 lineCounter.getAndIncrement(), JsonUtils.getJsonString(line)));
        sendMessageToWS(bm);
    }

    protected void sendMessageToWS(final ChannelBroadcastMessage bm) {
        try {
            WSConnectionContext.sendMessage(bm);
        } catch (Exception e) {
            LOG.error("A problem occurred while sending websocket message", e);
        }
    }
}
