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
package org.eclipse.che.api.runner.internal;

import org.eclipse.che.api.runner.RunnerException;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Facade for application process.
 *
 * @author andrew00x
 */
public abstract class ApplicationProcess {
    public static interface Callback {
        void started();

        void stopped();
    }

    private static final AtomicLong sequence = new AtomicLong(1);

    private final Long id;

    public ApplicationProcess() {
        this.id = sequence.getAndIncrement();
    }

    /**
     * Get unique id of this process.
     *
     * @return unique id of this process
     */
    public final Long getId() {
        return id;
    }

    /**
     * Starts application process.
     *
     * @throws RunnerException
     *         if an error occurs when start process
     * @throws IllegalStateException
     *         if process is already started
     */
    public abstract void start() throws RunnerException;

    /**
     * Stops application process.
     *
     * @throws RunnerException
     *         if an error occurs when stop process
     * @throws IllegalStateException
     *         if process isn't started yet
     */
    public abstract void stop() throws RunnerException;

    /**
     * Wait, if necessary, until this process stops, then returns exit code.
     *
     * @throws IllegalStateException
     *         if process isn't started yet
     * @throws RunnerException
     *         if any other error occurs
     */
    public abstract int waitFor() throws RunnerException;

    /**
     * Get exit code of application process. Returns {@code -1} if application is not started or still running.
     *
     * @throws RunnerException
     *         if an error occurs when try getting process' exit code
     */
    public abstract int exitCode() throws RunnerException;

    /**
     * Reports whether application process is running or not.
     *
     * @throws RunnerException
     *         if an error occurs when try getting process' status
     */
    public abstract boolean isRunning() throws RunnerException;

    /** Get application logger. */
    public abstract ApplicationLogger getLogger() throws RunnerException;
}
