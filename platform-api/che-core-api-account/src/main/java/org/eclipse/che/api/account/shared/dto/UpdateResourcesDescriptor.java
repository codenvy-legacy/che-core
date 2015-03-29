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
package org.eclipse.che.api.account.shared.dto;

import org.eclipse.che.dto.shared.DTO;

/**
 * @author Sergii Leschenko
 */
@DTO
public interface UpdateResourcesDescriptor {
    void setWorkspaceId(String workspaceId);

    String getWorkspaceId();

    UpdateResourcesDescriptor withWorkspaceId(String workspaceId);

    void setRunnerRam(Integer runnerRam);

    Integer getRunnerRam();

    UpdateResourcesDescriptor withRunnerRam(Integer runnerRam);

    void setRunnerTimeout(Integer runnerTimeout);

    Integer getRunnerTimeout();

    UpdateResourcesDescriptor withRunnerTimeout(Integer runnerTimeout);

    void setBuilderTimeout(Integer builderTimeout);

    Integer getBuilderTimeout();

    UpdateResourcesDescriptor withBuilderTimeout(Integer builderTimeout);
}
