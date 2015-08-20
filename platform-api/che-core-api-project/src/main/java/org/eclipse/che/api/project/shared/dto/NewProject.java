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
package org.eclipse.che.api.project.shared.dto;

import org.eclipse.che.api.core.factory.FactoryParameter;
import org.eclipse.che.dto.shared.DTO;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import java.util.List;
import java.util.Map;

import static org.eclipse.che.api.core.factory.FactoryParameter.Obligation.OPTIONAL;

/**
 * Data transfer object (DTO) for create project.
 *
 * @author andrew00x
 */
@DTO
@ApiModel(description = "New project")
public interface NewProject extends ProjectUpdate {
    /** Gets name of project. */
    @ApiModelProperty(value = "Project name", position = 1)
    @FactoryParameter(obligation = OPTIONAL)
    String getName();

    /** Sets name of project. */
    void setName(String name);

    /** Gets generator description. */
    @ApiModelProperty(value = "Project generator descriptior provides details on the project being created", position = 2)
    GeneratorDescription getGeneratorDescription();

    /** Sets generator description. */
    void setGeneratorDescription(GeneratorDescription generatorDescription);


    @ApiModelProperty(value = "Descriptions for project modules", position = 3)
    List<ProjectModule>  getModules();

    void setModules(List<ProjectModule> modules);

    // For method call chain


    NewProject withName(String name);

    NewProject withGeneratorDescription(GeneratorDescription generatorDescription);

    NewProject withModules(List<ProjectModule> modules);

    NewProject withType(String type);

    NewProject withBuilders(BuildersDescriptor builders);

    NewProject withRunners(RunnersDescriptor runners);

    NewProject withDescription(String description);

    NewProject withAttributes(Map<String, List<String>> attributes);

    NewProject withVisibility(String visibility);
}
