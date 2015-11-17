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
import org.eclipse.che.api.core.model.workspace.ModuleConfig;
import org.eclipse.che.dto.shared.DTO;

import java.util.List;
import java.util.Map;

import static org.eclipse.che.api.core.factory.FactoryParameter.Obligation.MANDATORY;
import static org.eclipse.che.api.core.factory.FactoryParameter.Obligation.OPTIONAL;

/**
 *  @author  Vitalii Parfonov
 */
@DTO
public interface ModuleConfigDto extends ModuleConfig {

    @Override
    @FactoryParameter(obligation = MANDATORY)
    String getName();

    void setName(String name);

    ModuleConfigDto withName(String name);

    @Override
    @FactoryParameter(obligation = MANDATORY)
    String getPath();

    void setPath(String path);

    ModuleConfigDto withPath(String path);

    @Override
    @FactoryParameter(obligation = OPTIONAL)
    String getDescription();

    void setDescription(String description);

    ModuleConfigDto withDescription(String description);

    @Override
    @FactoryParameter(obligation = MANDATORY)
    String getType();

    void setType(String type);

    ModuleConfigDto withType(String type);

    @Override
    @FactoryParameter(obligation = OPTIONAL)
    List<String> getMixinTypes();

    void setMixinTypes(List<String> mixinTypes);

    ModuleConfigDto withMixinTypes(List<String> mixinTypes);

    @Override
    @FactoryParameter(obligation = OPTIONAL)
    Map<String, List<String>> getAttributes();

    void setAttributes(Map<String, List<String>> attributes);

    ModuleConfigDto withAttributes(Map<String, List<String>> attributes);

    @Override
    List<ModuleConfigDto> getModules();

    void setModules(List<ModuleConfigDto> modules);

    ModuleConfigDto withModules(List<ModuleConfigDto> modules);
}
