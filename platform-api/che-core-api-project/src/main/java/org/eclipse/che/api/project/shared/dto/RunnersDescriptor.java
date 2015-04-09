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
package org.eclipse.che.api.project.shared.dto;

import org.eclipse.che.api.core.factory.FactoryParameter;
import org.eclipse.che.dto.shared.DTO;

import java.util.Map;

import static org.eclipse.che.api.core.factory.FactoryParameter.Obligation.OPTIONAL;
/**
 * @author andrew00x
 */
@DTO
public interface RunnersDescriptor {
    @FactoryParameter(obligation = OPTIONAL)
    /** Gets default runner identifier. */
    String getDefault();

    /** Sets default runner identifier. */
    void setDefault(String _default);

    RunnersDescriptor withDefault(String _default);

    @FactoryParameter(obligation = OPTIONAL)
    /** Gets all available runner configurations. */
    Map<String, RunnerConfiguration> getConfigs();

    /** Sets new runner configurations. */
    void setConfigs(Map<String, RunnerConfiguration> configs);

    RunnersDescriptor withConfigs(Map<String, RunnerConfiguration> configs);
}
