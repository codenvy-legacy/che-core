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
package org.eclipse.che.api.core.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** @author andrew00x */
public final class StreamPump implements Runnable {

    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private LineConsumer   lineConsumer;

    private Exception exception;
    private boolean   done;

    /**
     * This method should be called if we need write lines of stream to some log file
     * @param logFile file for writing stream
     */
    public synchronized void setLogFile(Path logFile) {
        if (logFile != null) {
            final StandardOpenOption[] openOptions = {StandardOpenOption.APPEND, StandardOpenOption.CREATE};
            try {
                bufferedWriter = Files.newBufferedWriter(logFile, Charset.forName("UTF-8"), openOptions);
            } catch (IOException e) {
                exception = e;
            }
        }
    }

    public synchronized void start(Process process, LineConsumer lineConsumer) {
        this.lineConsumer = lineConsumer;
        bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        final Thread t = new Thread(this, "StreamPump");
        t.setDaemon(true);
        t.start();
    }

    public synchronized void stop() {
        // Not clear do we need close original stream, but since it was wrapped by BufferedReader close it anyway.
        try {
            bufferedReader.close();
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
        } catch (IOException ignored) {
        }
    }

    public synchronized void await() throws InterruptedException {
        while (!done) {
            wait();
        }
    }

    public synchronized boolean isDone() {
        return done;
    }

    public boolean hasError() {
        return null != exception;
    }

    public Exception getException() {
        return exception;
    }

    @Override
    public void run() {
        String line;
        try {
            while ((line = bufferedReader.readLine()) != null) {
                lineConsumer.writeLine(line);
                if (bufferedWriter != null) {
                    bufferedWriter.write(line);
                    bufferedWriter.newLine();
                }
            }
        } catch (IOException e) {
            exception = e;
        } finally {
            synchronized (this) {
                done = true;
                notifyAll();
            }
        }
    }
}
