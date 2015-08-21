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

import com.wordnik.swagger.annotations.ApiModelProperty;

import org.eclipse.che.dto.shared.DTO;

import java.util.List;
import java.util.Map;

/**
 * Module configuration DTO.
 *
 * @author Max Shaposhnik
 */
@DTO
public interface ProjectModule {

    /** Gets path of module. */
    @ApiModelProperty(value = "Module path", position = 1)
    String getPath();

    /** Sets path of module. */
    void setPath(String path);

    ProjectModule withPath(String path);


    /** Gets unique id of type of module. */
    @ApiModelProperty(value = "Module type ID", position = 2)
    String getType();

    /** Sets unique id of type of project. */
    void setType(String type);

    ProjectModule withType(String type);

    /** Gets recipe for this module */
    @ApiModelProperty(value = "Module type ID", position = 3)
    String getRecipe();

    void setRecipe(String recipe);

    ProjectModule withRecipe(String recipe);

    /** Gets optional description of project. */
    @ApiModelProperty(value = "Module description", position = 4)
    String getDescription();

    /** Sets optional description of project. */
    void setDescription(String description);

    ProjectModule withDescription(String description);


    /** Gets attributes of this project. */
    @ApiModelProperty(value = "Module attributes", position = 5)
    Map<String, List<String>> getAttributes();

    /** Sets attributes of this project. */
    void setAttributes(Map<String, List<String>> attributes);

    ProjectModule withAttributes(Map<String, List<String>> attributes);


    @ApiModelProperty(value = "Mixins of current module", position = 6)
    List<String> getMixins();

    /** Sets permissions of current user on this project. */
    void setMixins(List<String> mixins);

    ProjectModule withMixins(List<String> mixins);
}
