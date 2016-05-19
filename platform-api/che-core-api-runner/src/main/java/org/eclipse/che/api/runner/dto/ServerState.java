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
package org.eclipse.che.api.runner.dto;

import org.eclipse.che.dto.shared.DTO;

/**
 * Describes state of computer.
 *
 * @author andrew00x
 */
@DTO
public interface ServerState {
    /** Gets CPU load in percents (1-100). Return -1 when fails getting CPU load. */
    int getCpuPercentUsage();

    ServerState withCpuPercentUsage(int cpuPercentUsage);

    void setCpuPercentUsage(int cpuPercentUsage);

    /** Gets total amount of memory (in megabytes) allocated to launching applications. */
    long getTotalMemory();

    ServerState withTotalMemory(long totalMemory);

    void setTotalMemory(long totalMemory);

    /** Gets amount of memory (in megabytes) available for launching applications. */
    long getFreeMemory();

    ServerState withFreeMemory(long freeMemory);

    void setFreeMemory(long freeMemory);
}
