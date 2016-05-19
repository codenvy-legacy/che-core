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
 * Provides info for registration new {@code SlaveRunnerService}.
 *
 * @author andrew00x
 * @see org.eclipse.che.api.runner.RunnerAdminService#registerRunnerServer(RunnerServerRegistration)
 */
@DTO
public interface RunnerServerRegistration {
    RunnerServerLocation getRunnerServerLocation();

    RunnerServerRegistration withRunnerServerLocation(RunnerServerLocation runnerServiceLocation);

    void setRunnerServerLocation(RunnerServerLocation runnerServiceLocation);

    RunnerServerAccessCriteria getRunnerServerAccessCriteria();

    RunnerServerRegistration withRunnerServerAccessCriteria(RunnerServerAccessCriteria runnerServerAccessCriteria);

    void setRunnerServerAccessCriteria(RunnerServerAccessCriteria runnerServerAccessCriteria);
}
