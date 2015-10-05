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
    @Override
    String getName();

    void setName(String name);

    ProjectConfigDto withName(String name);

    @Override
    String getPath();

    void setPath(String path);

    ProjectConfigDto withPath(String path);

    @Override
    String getDescription();

    void setDescription(String description);

    ProjectConfigDto withDescription(String description);

    @Override
    String getType();

    void setType(String type);

    ProjectConfigDto withType(String type);

    @Override
    List<String> getMixinTypes();

    void setMixinTypes(List<String> mixinTypes);

    ProjectConfigDto withMixinTypes(List<String> mixinTypes);

    @Override
    Map<String, List<String>> getAttributes();

    void setAttributes(Map<String, List<String>> attributes);

    ProjectConfigDto withAttributes(Map<String, List<String>> attributes);

    @Override
    SourceStorageDto getStorage();

    void setStorage(SourceStorageDto sourceStorage);

    ProjectConfigDto withStorage(SourceStorageDto sourceStorage);
}
