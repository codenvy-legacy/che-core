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

import java.util.List;

/**
 * Describes current state of {@link org.eclipse.che.api.builder.internal.Builder}.
 *
 * @author andrew00x
 */
@DTO
public interface BuilderState {
    String getName();

    BuilderState withName(String name);

    void setName(String name);

    List<BuilderMetric> getStats();

    BuilderState withStats(List<BuilderMetric> stats);

    void setStats(List<BuilderMetric> stats);

    int getFreeWorkers();

    BuilderState withFreeWorkers(int freeWorkers);

    void setFreeWorkers(int freeWorkers);

    ServerState getServerState();

    BuilderState withServerState(ServerState serverState);

    void setServerState(ServerState serverState);
}
