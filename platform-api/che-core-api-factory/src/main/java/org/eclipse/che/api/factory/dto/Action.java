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

import static org.eclipse.che.api.core.factory.FactoryParameter.Obligation.OPTIONAL;

import org.eclipse.che.api.core.factory.FactoryParameter;
import org.eclipse.che.dto.shared.DTO;

import java.util.Map;

/**
 * Describe ide action.
 *
 * @author Sergii Kabashniuk
 */
@DTO
public interface Action {
    /**
     * Action Id
     *
     * @return id of action.
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "id")
    String getId();

    void setId(String id);

    Action withId(String id);

    /***
     *
     * @return Action properties
     */
    @FactoryParameter(obligation = OPTIONAL, queryParameterName = "properties")
    Map<String, String> getProperties();

    void setProperties(Map<String, String> properties);

    Action withProperties(Map<String, String> properties);
}
