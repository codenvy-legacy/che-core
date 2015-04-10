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
 * Describe docker runner configuration source
 *
 * @author Alexander Garagatyi
 * @deprecated
 */
@DTO
public interface RunnerSource {
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "location")
    String getLocation();

    void setLocation(String location);

    RunnerSource withLocation(String location);

    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "parameters")
    Map<String, String> getParameters();

    void setParameters(Map<String, String> parameters);

    RunnerSource withParameters(Map<String, String> parameters);
}

