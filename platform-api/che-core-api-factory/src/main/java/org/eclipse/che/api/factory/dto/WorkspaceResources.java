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
package org.eclipse.che.api.factory.dto;

import org.eclipse.che.api.core.factory.FactoryParameter;
import org.eclipse.che.dto.shared.DTO;

import static org.eclipse.che.api.core.factory.FactoryParameter.Obligation.OPTIONAL;

/**
 * @author Max Shaposhnik
 */
@DTO
public interface WorkspaceResources {

    @FactoryParameter(obligation = OPTIONAL)
    Integer getRunnerRam();

    void setRunnerRam(Integer runnerRam);

    WorkspaceResources withRunnerRam(Integer runnerRam);

    @FactoryParameter(obligation = OPTIONAL)
    Integer getRunnerTimeout();

    void setRunnerTimeout(Integer runnerTimeout);

    WorkspaceResources withRunnerTimeout(Integer runnerTimeout);

    @FactoryParameter(obligation = OPTIONAL)
    Integer getBuilderTimeout();

    void setBuilderTimeout(Integer builderTimeout);

    WorkspaceResources withBuilderTimeout(Integer builderTimeout);
}
