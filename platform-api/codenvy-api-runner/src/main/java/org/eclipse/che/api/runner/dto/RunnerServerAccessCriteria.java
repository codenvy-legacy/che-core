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
package org.eclipse.che.api.runner.dto;

import org.eclipse.che.dto.shared.DTO;
import com.wordnik.swagger.annotations.ApiModelProperty;

/**
 * Resource access criteria. Basically resource may be assigned to {@code workspace}, {@code project} in some workspace.
 *
 * @author andrew00x
 */
@DTO
public interface RunnerServerAccessCriteria {
    @ApiModelProperty(value = "Workspace ID")
    String getWorkspace();

    RunnerServerAccessCriteria withWorkspace(String workspace);

    void setWorkspace(String workspace);

    @ApiModelProperty(value = "Project name")
    String getProject();

    RunnerServerAccessCriteria withProject(String project);

    void setProject(String project);

    String getInfra();

    RunnerServerAccessCriteria withInfra(String infra);

    void setInfra(String infra);
}
