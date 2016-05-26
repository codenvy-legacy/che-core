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
package org.eclipse.che.api.runner.internal;

import org.eclipse.che.api.runner.RunnerException;

/** @author andrew00x */
public interface RunnerProcess {
    interface Callback {
        void started(RunnerProcess process);

        void stopped(RunnerProcess process);

        void error(RunnerProcess process, Throwable t);
    }

    /**
     * Get unique id of this process.
     *
     * @return unique id of this process
     */
    Long getId();

    /**
     * Get application process. NOTE Regular user of Runner API is not expected to use this method directly and call any methods of
     * ApplicationProcess.
     *
     * @return ApplicationProcess or {@code null} if application is not started yet.
     */
    ApplicationProcess getApplicationProcess();

    /**
     * Get name of runner which owns this process.
     *
     * @return name of runner which owns this process
     */
    String getRunner();

    /** Get configuration of current process. */
    RunnerConfiguration getConfiguration();

    /** Process error. If process has terminated successfully this method returns {@code null}. */
    Throwable getError();

    /**
     * Reports whether process is started or not.
     *
     * @return {@code true} if process is started and {@code false} otherwise
     */
    boolean isStarted();

    /**
     * Get time when process was started.
     *
     * @return time when process was started or {@code -1} if process is not started yet
     */
    long getStartTime();

    /**
     * Stop this process.
     *
     * @throws org.eclipse.che.api.runner.RunnerException
     *         if an error occurs when try to interrupt this process
     */
    void stop() throws RunnerException;

    /**
     * Reports whether process is stopped (normally or cancelled) or not.
     *
     * @return {@code true} if process is stopped and {@code false} otherwise
     */
    boolean isStopped();

    /**
     * Get time when process was started.
     *
     * @return time when process was stopped or {@code -1} if process is not started yet or still running
     */
    long getStopTime();

    /**
     * Get uptime of application process in milliseconds.
     *
     * @return uptime of application process or {@code 0} if process is not started yet.
     */
    long getUptime();

    /**
     * Reports whether process is stopped abnormally (not by user but by Runner internal mechanism), e.g. if process is running longer than
     * it's allowed.
     *
     * @return {@code true} if is stopped abnormally and {@code false} otherwise
     */
    boolean isCancelled();

    /** Get application logger. */
    ApplicationLogger getLogger() throws RunnerException;
}
