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
package org.eclipse.che.api.workspace.shared.dto2;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;
import org.eclipse.che.api.core.model.UsersWorkspace;
import org.eclipse.che.api.core.model.workspace.Command;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.core.model.Environment;
import org.eclipse.che.api.core.model.ProjectConfig;
import org.eclipse.che.api.core.model.Workspace;
import org.eclipse.che.dto.shared.DTO;

import java.util.List;
import java.util.Map;

/**
 * @author andrew00x
 */
@DTO
@ApiModel(value = "Information about workspace")
public interface WorkspaceDto extends UsersWorkspace {
    @ApiModelProperty(value = "Identifier of a workspace in a system", required = true, position = 1)
    @Override
    String getId();

    void setId(String id);

    WorkspaceDto withId(String id);

    @ApiModelProperty(value = "Workspace name. It comes just after 'ws' in the workspace URL - http://codenvy.com/ws/{ws-name}", required = true, position = 2)
    @Override
    String getName();

    void setName(String name);

    WorkspaceDto withName(String name);
//
//    void setTemporary(boolean temporary);
//
//    @ApiModelProperty(value = "Information on whether or not the workspace is temporary", required = true, position = 3, allowableValues = "true,false")
//    @Override
//    boolean isTemporary();
//
//    WorkspaceDto withTemporary(boolean temporary);

//    @Deprecated
//    @ApiModelProperty(value ="ID of an account", required = true, position = 4)
//    String getAccountId();
//
//    @Deprecated
//    void setAccountId(String accountId);
//
//    @Deprecated
//    WorkspaceDto withAccountId(String accountId);

    @ApiModelProperty(value = "Workspace attributes, such as runner and builder life time, RAM allocation", position = 5)
    @Override
    Map<String, String> getAttributes();

    void setAttributes(Map<String, String> attributes);

    WorkspaceDto withAttributes(Map<String, String> attributes);

//    @Override
//    Set<Membership> getMembers();

    @Override
    List<Command> getCommands();

    @Override
    List<ProjectConfig> getProjects();

    @Override
    Environment getDefaultEnvironment();

    @Override
    Map<String, ? extends Environment> getEnvironments();

    List<Link> getLinks();

    void setLinks(List<Link> links);

    WorkspaceDto withLinks(List<Link> links);
}
