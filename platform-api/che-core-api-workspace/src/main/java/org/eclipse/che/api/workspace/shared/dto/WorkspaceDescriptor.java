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
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import java.util.List;
import java.util.Map;

/**
 * @author andrew00x
 */
@DTO
@ApiModel(value = "Information about workspace")
public interface WorkspaceDescriptor {
    @ApiModelProperty(value = "Identifier of a workspace in a system", required = true)
    String getId();

    void setId(String id);

    WorkspaceDescriptor withId(String id);

    @ApiModelProperty(value = "Workspace name. It comes just after 'ws' in the workspace URL - http://codenvy.com/ws/{ws-name}", required = true)
    String getName();

    void setName(String name);

    WorkspaceDescriptor withName(String name);

    void setTemporary(boolean temporary);

    @ApiModelProperty(value = "Information on whether or not the workspace is temporary", required = true, allowableValues = "true,false")
    boolean isTemporary();

    WorkspaceDescriptor withTemporary(boolean temporary);

    @ApiModelProperty(value ="ID of an account", required = true)
    String getAccountId();

    void setAccountId(String accountId);

    WorkspaceDescriptor withAccountId(String accountId);

    @ApiModelProperty(value = "Workspace attributes, such as runner and builder life time, RAM allocation")
    Map<String, String> getAttributes();

    void setAttributes(Map<String, String> attributes);

    WorkspaceDescriptor withAttributes(Map<String, String> attributes);

    List<Link> getLinks();

    void setLinks(List<Link> links);

    WorkspaceDescriptor withLinks(List<Link> links);
}
