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
package org.eclipse.che.api.builder.internal;

import org.eclipse.che.api.builder.BuilderException;
import org.eclipse.che.api.core.util.CommandLine;

/**
 * Build task abstraction.
 *
 * @author andrew00x
 */
public interface BuildTask {

    /** Will be notified when processing of {@code BuildTask} is started or done (successfully, failed or cancelled). */
    interface Callback {
        void begin(BuildTask task);

        void done(BuildTask task);
    }

    /**
     * Get unique id of this task.
     *
     * @return unique id of this task
     */
    Long getId();

    /**
     * Get command line which this task runs. Modifications to the returned {@code CommandLine} will not affect the task it it already
     * started. Caller always must check is task is started before use this method.
     *
     * @return command line
     * @see #isStarted()
     */
    CommandLine getCommandLine();

    /**
     * Get name of builder which owns this task.
     *
     * @return name of builder which owns this task
     */
    String getBuilder();

    /**
     * Get build logger.
     *
     * @return build logger
     */
    BuildLogger getBuildLogger();

    /**
     * Reports whether build task is started or not.
     *
     * @return {@code true} if task is started and {@code false} otherwise
     */
    boolean isStarted();

    /**
     * Get time when task was started.
     *
     * @return time when task was started or {@code -1} if task is not started yet
     * @see #isStarted()
     */
    long getStartTime();

    /**
     * Get time when task was done (successfully ends, fails, cancelled).
     *
     * @return time when task was started or {@code -1} if task is not done yet
     * @see #isStarted()
     */
    long getEndTime();

    /**
     * Get running time of this task in milliseconds.
     *
     * @return running time of this tas or {@code 0} if task is not started yet
     */
    long getRunningTime();

    /**
     * Reports whether build task is done (successfully ends, fails, cancelled) or not.
     *
     * @return {@code true} if task is done and {@code false} otherwise
     */
    boolean isDone();

    /**
     * Reports that the process is interrupted.
     *
     * @return {@code true} if task is interrupted and {@code false} otherwise
     */
    boolean isCancelled();

    /**
     * Interrupt build process.
     *
     * @throws BuilderException
     *         if an error occurs when try to interrupt build process
     */
    void cancel() throws BuilderException;

    /**
     * Get build result.
     *
     * @return build result or {@code null} if task is not done yet
     * @throws BuilderException
     *         if an error occurs when try to start build process or get its result.
     *         <p/>
     *         <strong>Note</strong> Throwing of this exception is typically should not be related to failed build process itself. Builder
     *         should always provide result of build process with BuildResult instance. Throwing of this exception means something going
     *         wrong with build system itself and it is not possible to start build process or getting result of a build
     */
    BuildResult getResult() throws BuilderException;

    /**
     * Get configuration of this task.
     *
     * @return configuration of this task
     */
    BuilderConfiguration getConfiguration();
}
