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

import org.eclipse.che.api.core.factory.FactoryParameter;
import org.eclipse.che.api.core.model.workspace.ProjectConfig;
import org.eclipse.che.dto.shared.DTO;

import java.util.List;
import java.util.Map;

import static org.eclipse.che.api.core.factory.FactoryParameter.Obligation.MANDATORY;
import static org.eclipse.che.api.core.factory.FactoryParameter.Obligation.OPTIONAL;

/**
 * @author Alexander Garagatyi
 */
@DTO
public interface ProjectConfigDto extends ModuleConfigDto, ProjectConfig {
    @Override
    @FactoryParameter(obligation = MANDATORY)
    String getName();

    void setName(String name);

    ProjectConfigDto withName(String name);

    @Override
    @FactoryParameter(obligation = MANDATORY)
    String getPath();

    void setPath(String path);

    ProjectConfigDto withPath(String path);

    @Override
    @FactoryParameter(obligation = OPTIONAL)
    String getDescription();

    void setDescription(String description);

    ProjectConfigDto withDescription(String description);

    @Override
    @FactoryParameter(obligation = MANDATORY)
    String getType();

    void setType(String type);

    ProjectConfigDto withType(String type);

    @Override
    @FactoryParameter(obligation = OPTIONAL)
    List<String> getMixins();

    void setMixins(List<String> mixins);

    ProjectConfigDto withMixins(List<String> mixins);

    @Override
    @FactoryParameter(obligation = OPTIONAL)
    Map<String, List<String>> getAttributes();

    void setAttributes(Map<String, List<String>> attributes);

    ProjectConfigDto withAttributes(Map<String, List<String>> attributes);

    @Override
    List<ModuleConfigDto> getModules();

    void setModules(List<ModuleConfigDto> modules);

    ProjectConfigDto withModules(List<ModuleConfigDto> modules);

    @Override
    @FactoryParameter(obligation = MANDATORY)
    SourceStorageDto getSource();

    void setSource(SourceStorageDto source);

    ProjectConfigDto withSource(SourceStorageDto source);
}
