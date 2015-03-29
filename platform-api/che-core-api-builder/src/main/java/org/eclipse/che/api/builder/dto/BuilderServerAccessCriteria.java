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
package org.eclipse.che.api.builder.dto;

import org.eclipse.che.dto.shared.DTO;
import com.wordnik.swagger.annotations.ApiModelProperty;

/**
 * Resource access criteria. Basically resource may be assigned to {@code workspace}, {@code project} in some workspace or {@code
 * username}.
 *
 * @author andrew00x
 */
@DTO
public interface BuilderServerAccessCriteria {
    @ApiModelProperty(value = "Workspace ID", notes = "Optional. Used only when a builder should accept requests from a particular workspace")
    String getWorkspace();

    BuilderServerAccessCriteria withWorkspace(String workspace);

    void setWorkspace(String workspace);

    @ApiModelProperty(value = "Project name", notes = "Optional. If specified, the builder will accept requests from a particular project only")
    String getProject();

    BuilderServerAccessCriteria withProject(String project);

    void setProject(String project);
}
