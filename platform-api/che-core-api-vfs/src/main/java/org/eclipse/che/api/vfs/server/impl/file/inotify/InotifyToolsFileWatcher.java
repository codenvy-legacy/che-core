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
package org.eclipse.che.api.vfs.server.impl.file.inotify;

import com.google.common.base.Joiner;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.eclipse.che.api.core.util.CommandLine;
import org.eclipse.che.api.core.util.LineConsumer;
import org.eclipse.che.api.core.util.ProcessUtil;
import org.eclipse.che.api.vfs.server.impl.file.FileWatcherNotificationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Iterables.getLast;

public class InotifyToolsFileWatcher {
    protected enum Event {
        CREATE("create"),
        DELETE("delete"),
        MODIFY("modify");

        final String value;

        Event(String value) {
            this.value = value;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(InotifyToolsFileWatcher.class);

    private static final String EVENT_LINE_FORMAT         = "%w%f,%,e";
    private static final int    MAX_RESTART_ATTEMPT_COUNT = 10;

    private final List<Pattern>                   excludePatterns;
    private final FileWatcherNotificationListener fileWatcherNotificationListener;
    private final File                            watchRoot;
    private final boolean                         recursive;
    private final String                          watchRootPath;
    private final Executor                        executor;
    private final AtomicInteger                   startAttemptCounter;
    private final AtomicBoolean                   running;

    private Process watcherProcess;

    public InotifyToolsFileWatcher(File watchRoot,
                                   List<String> excludeRegExs,
                                   FileWatcherNotificationListener fileWatcherNotificationListener,
                                   boolean recursive) {
        this.watchRoot = toCanonicalFile(watchRoot);
        this.excludePatterns = buildPatterns(excludeRegExs);
        this.fileWatcherNotificationListener = fileWatcherNotificationListener;
        this.recursive = recursive;

        String watchRootPath = this.watchRoot.getAbsolutePath();
        if (watchRootPath.endsWith("/")) {
            this.watchRootPath = watchRootPath;
        } else {
            this.watchRootPath = watchRootPath + "/";
        }

        ThreadFactory threadFactory = new ThreadFactoryBuilder().setDaemon(true).setNameFormat("InotifyToolsFileWatcher-%d").build();
        executor = Executors.newSingleThreadExecutor(threadFactory);
        startAttemptCounter = new AtomicInteger();
        running = new AtomicBoolean();
    }

    private static List<Pattern> buildPatterns(List<String> regExs) {
        return regExs.stream().map(Pattern::compile).collect(Collectors.toList());
    }

    private static File toCanonicalFile(File file) {
        try {
            return file.getCanonicalFile();
        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    public void startup() throws IOException {
        running.set(true);
        startAttemptCounter.set(0);
        startWatcherProcess(createCommandLine());
    }

    private CommandLine createCommandLine() {
        CommandLine commandLine = new CommandLine("inotifywait", "--monitor", "--quiet");
        commandLine.add("--format", EVENT_LINE_FORMAT);
        if (recursive) {
            commandLine.add("--recursive");
        }
        commandLine.add("--event", Joiner.on(',').join(Arrays.stream(Event.values()).map(e -> e.value).toArray()));
        commandLine.add(watchRootPath);
        return commandLine;
    }

    public synchronized void shutdown() {
        running.set(false);
        if (watcherProcess != null) {
            ProcessUtil.kill(watcherProcess);
        }
    }

    private synchronized void startWatcherProcess(CommandLine watcherCommand) throws IOException {
        if (watcherProcess != null) {
            ProcessUtil.kill(watcherProcess);
        }

        watcherProcess = new ProcessBuilder(watcherCommand.toShellCommand()).start();
        LOG.info("Started with command: {}", watcherCommand.toString());
        fileWatcherNotificationListener.started(watchRoot);

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessUtil.process(watcherProcess, new LineConsumer() {
                        @Override
                        public void writeLine(String inotifyEventLine) throws IOException {
                            if (isNullOrEmpty(inotifyEventLine)) {
                                return;
                            }
                            parseAndFireWatcherEvent(inotifyEventLine);
                        }

                        @Override
                        public void close() throws IOException {
                        }
                    });

                    if (shouldRestartWatcherProcess()) {
                        startWatcherProcess(watcherCommand);
                    }
                } catch (Throwable e) {
                    fileWatcherNotificationListener.errorOccurred(watchRoot, e);
                }
            }
        });
    }

    private boolean shouldRestartWatcherProcess() {
        return running.get() && startAttemptCounter.incrementAndGet() < MAX_RESTART_ATTEMPT_COUNT;
    }

    private void parseAndFireWatcherEvent(String inotifyEventLine) {
        List<String> tokens = parseEventLine(inotifyEventLine);
        if (tokens.size() >= 2) {
            String subPath = tokens.get(0).substring(watchRootPath.length());
            if (isIgnored(subPath)) {
                return;
            }
            boolean isDirectory = tokens.size() > 2 && "ISDIR".equals(getLast(tokens));
            for (String token : tokens) {
                if (token.equals(Event.CREATE.name())) {
                    fileWatcherNotificationListener.pathCreated(watchRoot, subPath, isDirectory);
                } else if (token.equals(Event.DELETE.name())) {
                    fileWatcherNotificationListener.pathDeleted(watchRoot, subPath, isDirectory);
                } else if (token.equals(Event.MODIFY.name())) {
                    fileWatcherNotificationListener.pathUpdated(watchRoot, subPath, isDirectory);
                }
            }
        }
    }

    protected boolean isIgnored(String subPath) {
        for (Pattern excludePattern : excludePatterns) {
            if (excludePattern.matcher(subPath).matches()) {
                return true;
            }
        }
        return false;
    }

    private List<String> parseEventLine(String inotifyEventLine) {
        List<String> tokens = new ArrayList<>(4);
        int stopAt = inotifyEventLine.length() - 1;
        int p = 0;

        for (int i = 0; i < stopAt; i++) {
            if (inotifyEventLine.charAt(i) == ',') {
                tokens.add(inotifyEventLine.substring(p, i));
                p = i + 1;
            }
        }
        tokens.add(inotifyEventLine.substring(p));

        return tokens;
    }

    Process getWatcherProcess() {
        return watcherProcess;
    }
}
