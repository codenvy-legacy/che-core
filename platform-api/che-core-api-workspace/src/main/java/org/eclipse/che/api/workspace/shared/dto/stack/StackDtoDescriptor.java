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
package org.eclipse.che.api.workspace.shared.dto.stack;

import org.eclipse.che.api.machine.shared.dto.recipe.PermissionsDescriptor;
import org.eclipse.che.api.workspace.server.model.impl.stack.Stack;
import org.eclipse.che.api.core.rest.shared.dto.Hyperlinks;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceConfigDto;
import org.eclipse.che.dto.shared.DTO;

import java.util.List;

/**
 * @author Alexander Andrienko
 */
@DTO
public interface StackDtoDescriptor extends Stack, Hyperlinks {

    void setId(String id);

    StackDtoDescriptor withId(String id);

    void setName(String name);

    StackDtoDescriptor withName(String name);

    void setDescription(String description);

    StackDtoDescriptor withDescription(String description);

    void setScope(String scope);

    StackDtoDescriptor withScope(String scope);

    void setCreator(String creator);

    StackDtoDescriptor withCreator(String creator);

    void setTags(List<String> tags);

    StackDtoDescriptor withTags(List<String> tags);
    
    @Override
    WorkspaceConfigDto getWorkspaceConfig();

    void setWorkspaceConfig(WorkspaceConfigDto workspaceConfig);

    StackDtoDescriptor withWorkspaceConfig(WorkspaceConfigDto workspaceConfig);

    StackSourceDto getSource();

    void setSource(StackSourceDto source);

    StackDtoDescriptor withSource(StackSourceDto source);

    @Override
    List<StackComponentDto> getComponents();

    void setComponents(List<StackComponentDto> components);

    StackDtoDescriptor withComponents(List<StackComponentDto> components);

    @Override
    PermissionsDescriptor getPermissions();

    void setPermissions(PermissionsDescriptor permissions);

    StackDtoDescriptor withPermissions(PermissionsDescriptor permissions);

    StackDtoDescriptor withLinks(List<Link> links);
}

