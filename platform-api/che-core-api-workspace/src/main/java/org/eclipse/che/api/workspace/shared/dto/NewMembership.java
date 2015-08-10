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

import java.util.List;

/**
 *
 *
 * @author andrew00x
 */
@DTO
public interface NewMembership {
    @ApiModelProperty(value = "User ID", required = true)
    String getUserId();

    void setUserId(String id);

    NewMembership withUserId(String id);

    @ApiModelProperty(value = "Roles in a specified workspace", required = true, allowableValues = "workspace/admin, workspace/developer")
    List<String> getRoles();

    void setRoles(List<String> roles);

    NewMembership withRoles(List<String> roles);
}
