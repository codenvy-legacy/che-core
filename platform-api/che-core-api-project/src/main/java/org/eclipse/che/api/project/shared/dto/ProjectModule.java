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

    /** Gets path of project. */
    @ApiModelProperty(value = "Module path", position = 1)
    String getPath();

    /** Sets path of project. */
    void setPath(String path);

    ProjectModule withPath(String path);


    /** Gets unique id of type of project. */
    @ApiModelProperty(value = "Module type ID", position = 2)
    String getType();

    /** Sets unique id of type of project. */
    void setType(String type);

    ProjectModule withType(String type);

    /** Gets builder configurations. */
    @ApiModelProperty(value = "Builders configuration for the module", position = 3)
    BuildersDescriptor getBuilders();

    /** Sets builder configurations. */
    void setBuilders(BuildersDescriptor builders);

    ProjectModule withBuilders(BuildersDescriptor builders);

    /** Gets runner configurations. */
    @ApiModelProperty(value = "Runners configuration for the module", position = 4)
    RunnersDescriptor getRunners();

    /** Sets runner configurations. */
    void setRunners(RunnersDescriptor runners);

    ProjectModule withRunners(RunnersDescriptor runners);

    /** Gets optional description of project. */
    @ApiModelProperty(value = "Module description", position = 5)
    String getDescription();

    /** Sets optional description of project. */
    void setDescription(String description);

    ProjectModule withDescription(String description);


    /** Gets attributes of this project. */
    @ApiModelProperty(value = "Module attributes", position = 6)
    Map<String, List<String>> getAttributes();

    /** Sets attributes of this project. */
    void setAttributes(Map<String, List<String>> attributes);

    ProjectModule withAttributes(Map<String, List<String>> attributes);


    @ApiModelProperty(value = "Mixins of current module", position = 7)
    List<String> getMixins();

    /** Sets permissions of current user on this project. */
    void setMixins(List<String> mixins);

    ProjectModule withMixins(List<String> mixins);
}
