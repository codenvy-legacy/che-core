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
package org.eclipse.che.api.builder.dto;

import org.eclipse.che.dto.shared.DTO;

/**
 * @author andrew00x
 */
@DTO
public interface BuildTaskStats {
    /** Get the time when build task was created. */
    long getCreationTime();

    BuildTaskStats withCreationTime(long created);

    void setCreationTime(long created);


    /** Get the time in milliseconds that this task is waiting for start. */
    long getWaitingTime();

    void setWaitingTime(long waitingTime);

    BuildTaskStats withWaitingTime(long waitingTime);

    /** Get the limit time for task to start. If this task isn't stated before this time, it will be removed from the queue. */
    long getWaitingTimeLimit();

    BuildTaskStats withWaitingTimeLimit(long timeLimit);

    void setWaitingTimeLimit(long timeLimit);


    /** Get the execution time in milliseconds of this task. */
    long getExecutionTime();

    BuildTaskStats withExecutionTime(long endTime);

    void setExecutionTime(long endTime);

    /** Get the limit time for task to complete. If this task isn't completed before this time, if will be terminated forcibly. */
    long getExecutionTimeLimit();

    BuildTaskStats withExecutionTimeLimit(long timeLimit);

    void setExecutionTimeLimit(long timeLimit);
}
