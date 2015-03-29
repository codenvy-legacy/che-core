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
package org.eclipse.che.api.factory.dto;

import org.eclipse.che.dto.shared.DTO;

/**
 * @author Max Shaposhnik
 */
@DTO
public interface WorkspaceResources {
    void setRunnerRam(Integer runnerRam);

    Integer getRunnerRam();

    WorkspaceResources withRunnerRam(Integer runnerRam);

    void setRunnerTimeout(Integer runnerTimeout);

    Integer getRunnerTimeout();

    WorkspaceResources withRunnerTimeout(Integer runnerTimeout);

    void setBuilderTimeout(Integer builderTimeout);

    Integer getBuilderTimeout();

    WorkspaceResources withBuilderTimeout(Integer builderTimeout);
}
