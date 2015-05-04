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

import java.util.List;

/**
 * DTO describes the state of the project.
 *
 * @author Artem Zatsarynnyy
 */
@DTO
public interface ProjectState {

    /** Get the list of the actions that should be performed in order to restore some project's state. */
    List<ActionDescriptor> getActions();

    void setActions(List<ActionDescriptor> actions);

    ProjectState withActions(List<ActionDescriptor> actions);
}
