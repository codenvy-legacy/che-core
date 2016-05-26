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

import java.util.List;

/**
 * Describes current state of {@link org.eclipse.che.api.runner.internal.Runner}.
 *
 * @author andrew00x
 */
@DTO
public interface RunnerState {
    String getName();

    RunnerState withName(String name);

    void setName(String name);

    List<RunnerMetric> getStats();

    RunnerState withStats(List<RunnerMetric> stats);

    void setStats(List<RunnerMetric> stats);

    ServerState getServerState();

    RunnerState withServerState(ServerState serverState);

    void setServerState(ServerState serverState);
}
