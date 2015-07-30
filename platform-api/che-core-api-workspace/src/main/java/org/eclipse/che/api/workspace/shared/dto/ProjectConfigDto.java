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

import org.eclipse.che.api.core.model.workspace.ProjectConfig;
import org.eclipse.che.dto.shared.DTO;

import java.util.List;
import java.util.Map;

/**
 * @author Alexander Garagatyi
 */
@DTO
public interface ProjectConfigDto extends ProjectConfig {
    void setName(String name);

    ProjectConfigDto withName(String name);

    void setPath(String path);

    ProjectConfigDto withPath(String path);

    void setDescription(String description);

    ProjectConfigDto withDescription(String description);

    void setType(ProjectTypeDto type);

    ProjectConfigDto withType(ProjectTypeDto type);

    void setMixinTypes(List<ProjectTypeDto> mixinTypes);

    ProjectConfigDto withMixinTypes(List<ProjectTypeDto> mixinTypes);

    void setAttributes(Map<String, List<AttributeDto>> attributes);

    ProjectConfigDto withAttributes(Map<String, List<AttributeDto>> attributes);

    void setSourceStorage(SourceStorageDto sourceStorage);

    ProjectConfigDto withSourceStorage(SourceStorageDto sourceStorage);
}
