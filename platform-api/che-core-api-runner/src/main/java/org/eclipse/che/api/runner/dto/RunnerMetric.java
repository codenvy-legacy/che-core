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
package org.eclipse.che.api.runner.dto;

import org.eclipse.che.dto.shared.DTO;

/**
 * Describes single metric of runner's stats.
 *
 * @author andrew00x
 */
@DTO
public interface RunnerMetric {

    String WAITING_TIME = "waitingTime";

    String WAITING_TIME_LIMIT = "waitingTimeLimit";

    String ALWAYS_ON = "Always On";

    String LIFETIME = "lifetime";

    String TERMINATION_TIME = "terminationTime";

    String START_TIME = "startTime";

    String STOP_TIME = "stopTime";

    String UP_TIME = "uptime";

    String TOTAL_APPS = "totalApps";

    String RUNNING_APPS = "runningApps";

    String MEMORY ="memory";

    String DISK_SPACE_TOTAL = "diskSpaceTotal";

    String DISK_SPACE_USED = "diskSpaceUsed";

    String getName();

    RunnerMetric withName(String name);

    void setName(String name);

    String getValue();

    RunnerMetric withValue(String value);

    void setValue(String value);

    String getDescription();

    RunnerMetric withDescription(String description);

    void setDescription(String description);
}
