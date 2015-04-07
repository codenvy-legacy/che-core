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
package org.eclipse.che.ide.statepersistance.dto;

import org.eclipse.che.dto.shared.DTO;

import java.util.Map;

/**
 * DTO for {@link org.eclipse.che.ide.api.action.Action}.
 *
 * @author Artem Zatsarynnyy
 */
@DTO
public interface ActionDescriptor {

    /** get action's ID. */
    String getId();

    void setId(String id);

    ActionDescriptor withId(String id);


    /** Get parameters for performing action. */
    Map<String, String> getParameters();

    void setParameters(Map<String, String> parameters);

    ActionDescriptor withParameters(Map<String, String> parameters);
}
