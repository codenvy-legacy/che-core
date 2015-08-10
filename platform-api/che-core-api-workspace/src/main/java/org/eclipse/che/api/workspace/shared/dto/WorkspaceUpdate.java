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
package org.eclipse.che.api.workspace.shared.dto;

import com.wordnik.swagger.annotations.ApiModelProperty;
import org.eclipse.che.dto.shared.DTO;

import java.util.Map;

/**
 * @author andrew00x
 */
@DTO
public interface WorkspaceUpdate {
    @ApiModelProperty(value = "Workspace name", required = true)
    String getName();

    void setName(String name);

    WorkspaceUpdate withName(String name);

    @ApiModelProperty(value = "Workspace attributes are used to store random information about a workspace")
    Map<String, String> getAttributes();

    void setAttributes(Map<String, String> attributes);

    WorkspaceUpdate withAttributes(Map<String, String> attributes);
}
