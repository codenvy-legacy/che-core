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
package org.eclipse.che.api.factory.shared.dto;

import org.eclipse.che.api.core.factory.FactoryParameter;
import org.eclipse.che.dto.shared.DTO;

import static org.eclipse.che.api.core.factory.FactoryParameter.Obligation.OPTIONAL;
/**
 * Describe restrictions of the factory
 *
 * @author andrew00x
 * @author Alexander Garagatyi
 */
@DTO
public interface Policies {
    /**
     * Restrict access if referer header doesn't match this field
     */
    // Do not change referer to referrer
    @FactoryParameter(obligation = OPTIONAL)
    String getRefererHostname();

    void setRefererHostname(String refererHostname);

    Policies withRefererHostname(String refererHostname);

    /**
     * Restrict access for factories used earlier then author supposes
     */
    @FactoryParameter(obligation = OPTIONAL)
    Long getValidSince();

    void setValidSince(Long validSince);

    Policies withValidSince(Long validSince);

    /**
     * Restrict access for factories used later then author supposes
     */
    @FactoryParameter(obligation = OPTIONAL)
    Long getValidUntil();

    void setValidUntil(Long validUntil);

    Policies withValidUntil(Long validUntil);

    /**
     * Re-open project on factory 2-nd click
     */
    @FactoryParameter(obligation = OPTIONAL)
    String getMatch();

    void setMatch(String match);

    Policies withMatch(String match);

    /**
     * It's very usefully
     */
    @FactoryParameter(obligation = OPTIONAL)
    String getCreate();

    void setCreate(String create);

    Policies withCreate(String create);
}
