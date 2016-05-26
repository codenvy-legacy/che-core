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
 * Describes parameters of the workspace that should be used for factory
 *
 * @author Alexander Garagatyi
 * @author Sergii Leschenko
 */
@DTO
public interface Workspace {
    @FactoryParameter(obligation = OPTIONAL)
    String getType();

    void setType(String type);

    Workspace withType(String type);

    @FactoryParameter(obligation = OPTIONAL)
    String getLocation();

    void setLocation(String location);

    Workspace withLocation(String location);


    @FactoryParameter(obligation = OPTIONAL)
    WorkspaceResources getResources();

    void setResources(WorkspaceResources resources);

    Workspace withResources(WorkspaceResources resources);

}

