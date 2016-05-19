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
package org.eclipse.che.api.core.util;

import com.sun.jna.Library;
import com.sun.jna.Native;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Process manager for *nix like system.
 *
 * @author andrew00x
 */
class UnixProcessManager extends ProcessManager {
    /*
    At the moment tested on linux only.
     */

    private static final Logger LOG = LoggerFactory.getLogger(UnixProcessManager.class);

    private static final CLibrary C_LIBRARY;

    private static final Field PID_FIELD;

    static {
        CLibrary lib = null;
        Field pidField = null;
        if (SystemInfo.isUnix()) {
            try {
                lib = ((CLibrary)Native.loadLibrary("c", CLibrary.class));
            } catch (Exception e) {
                LOG.error("Cannot load native library", e);
            }
            try {
                pidField = Thread.currentThread().getContextClassLoader().loadClass("java.lang.UNIXProcess").getDeclaredField("pid");
                pidField.setAccessible(true);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
        C_LIBRARY = lib;
        PID_FIELD = pidField;
    }

    private static interface CLibrary extends Library {
        // kill -l
        int SIGKILL = 9;
        int SIGTERM = 15;

        int kill(int pid, int signal);

        String strerror(int errno);

        int system(String cmd);
    }

    private static final Pattern UNIX_PS_TABLE_PATTERN = Pattern.compile("\\s+");

    @Override
    public void kill(Process process) {
        if (C_LIBRARY != null) {
            killTree(getPid(process));
        } else {
            throw new IllegalStateException("Can't kill process. Not unix system?");
        }
    }

    private void killTree(int pid) {
        final int[] children = getChildProcesses(pid);
        LOG.debug("PID: {}, child PIDs: {}", pid, children);
        if (children.length > 0) {
            for (int cpid : children) {
                killTree(cpid); // kill process tree recursively
            }
        }
        int r = C_LIBRARY.kill(pid, CLibrary.SIGKILL); // kill origin process
        LOG.debug("kill {}", pid);
        if (r != 0) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("kill for {} returns {}, strerror '{}'", pid, r, C_LIBRARY.strerror(r));
            }
        }
    }

    private int[] getChildProcesses(final int myPid) {
        final String ps = "ps -e -o ppid,pid,comm"; /* PPID, PID, COMMAND */
        final List<Integer> children = new ArrayList<>();
        final StringBuilder error = new StringBuilder();
        final LineConsumer stdout = new LineConsumer() {
            @Override
            public void writeLine(String line) throws IOException {
                if (line != null && !line.isEmpty()) {
                    final String[] tokens = UNIX_PS_TABLE_PATTERN.split(line.trim());
                    if (tokens.length == 3 /* PPID, PID, COMMAND */) {
                        int ppid;
                        try {
                            ppid = Integer.parseInt(tokens[0]);
                        } catch (NumberFormatException e) {
                            // May be first line from process table: 'PPID PID COMMAND'. Skip it.
                            return;
                        }
                        if (ppid == myPid) {
                            int pid = Integer.parseInt(tokens[1]);
                            children.add(pid);
                        }
                    }
                }
            }

            @Override
            public void close() throws IOException {
            }
        };

        final LineConsumer stderr = new LineConsumer() {
            @Override
            public void writeLine(String line) throws IOException {
                if (error.length() > 0) {
                    error.append('\n');
                }
                error.append(line);
            }

            @Override
            public void close() throws IOException {
            }
        };

        try {
            ProcessUtil.process(Runtime.getRuntime().exec(ps), stdout, stderr);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        if (error.length() > 0) {
            throw new IllegalStateException("can't get child processes: " + error.toString());
        }
        final int size = children.size();
        final int[] result = new int[size];
        for (int i = 0; i < size; i++) {
            result[i] = children.get(i);
        }
        return result;
    }

    @Override
    public boolean isAlive(Process process) {
        return process.isAlive();
    }

    int getPid(Process process) {
        if (PID_FIELD != null) {
            try {
                return ((Number)PID_FIELD.get(process)).intValue();
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Can't get process' pid. Not unix system?", e);
            }
        } else {
            throw new IllegalStateException("Can't get process' pid. Not unix system?");
        }
    }

    @Override
    int system(String command) {
        return C_LIBRARY.system(command);
    }
}
