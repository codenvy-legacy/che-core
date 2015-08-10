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

import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.dto.shared.DTO;
import com.wordnik.swagger.annotations.ApiModelProperty;

import java.util.List;

/**
 * @author Eugene Voevodin
 */
@DTO
public interface MemberDescriptor {

    @ApiModelProperty(value = "ID of a user in the system", required = true)
    String getUserId();

    void setUserId(String userId);

    MemberDescriptor withUserId(String userId);

    @ApiModelProperty(value = "Workspace information", required = true)
    WorkspaceReference getWorkspaceReference();

    void setWorkspaceReference(WorkspaceReference wsRef);

    MemberDescriptor withWorkspaceReference(WorkspaceReference wsRef);

    @ApiModelProperty(value = "Roles in a specified workspace", required = true, allowableValues = "workspace/admin, workspace/developer")
    List<String> getRoles();

    void setRoles(List<String> roles);

    MemberDescriptor withRoles(List<String> roles);

    List<Link> getLinks();

    void setLinks(List<Link> links);

    MemberDescriptor withLinks(List<Link> links);
}
