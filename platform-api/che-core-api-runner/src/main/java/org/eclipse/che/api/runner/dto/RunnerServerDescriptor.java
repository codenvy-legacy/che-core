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
package org.eclipse.che.api.runner.dto;

import org.eclipse.che.api.core.rest.shared.dto.ServiceDescriptor;
import org.eclipse.che.dto.shared.DTO;

/**
 * @author andrew00x
 */
@DTO
public interface RunnerServerDescriptor extends ServiceDescriptor {
    String getAssignedWorkspace();

    void setAssignedWorkspace(String assignedWorkspace);

    RunnerServerDescriptor withAssignedWorkspace(String assignedWorkspace);

    String getAssignedProject();

    void setAssignedProject(String assignedProject);

    RunnerServerDescriptor withAssignedProject(String assignedProject);
}
