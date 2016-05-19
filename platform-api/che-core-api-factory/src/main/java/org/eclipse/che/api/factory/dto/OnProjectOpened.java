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

import java.util.List;

import static org.eclipse.che.api.core.factory.FactoryParameter.Obligation.OPTIONAL;

/**
 * Describe IDE look and feel on project opened event.
 *
 * @author Sergii Kabashniuk
 */
@DTO
public interface OnProjectOpened {
    /**
     * @return actions for current event.
     */
    @FactoryParameter(obligation = OPTIONAL)
    List<Action> getActions();

    void setActions(List<Action> actions);

    OnProjectOpened withActions(List<Action> actions);
}
