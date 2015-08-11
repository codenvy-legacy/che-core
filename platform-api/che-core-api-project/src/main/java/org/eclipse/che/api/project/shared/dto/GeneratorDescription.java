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

import org.eclipse.che.dto.shared.DTO;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import java.util.Map;

/**
 * Data transfer object (DTO) for generate project.
 *
 * @author Vladyslav Zhukovskiy
 */
@DTO
@ApiModel(description = "Generate new project")
public interface GeneratorDescription {
//    /** Get name of project generator. */
//    @ApiModelProperty(value = "Name of project generator", position = 1, required = true)
//    String getName();
//
//    /** Set name of project generator. */
//    void setName(String generatorName);
//
//    GeneratorDescription withName(String generatorName);

    /** Get options needed for generator. */
    @ApiModelProperty("Options needed for generator")
    Map<String, String> getOptions();

    /** Set options needed for generator. */
    void setOptions(Map<String, String> options);

    GeneratorDescription withOptions(Map<String, String> options);
}
