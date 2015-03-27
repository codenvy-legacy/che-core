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
package org.eclipse.che.api.builder.dto;

import org.eclipse.che.dto.shared.DTO;
import com.wordnik.swagger.annotations.ApiModelProperty;

import java.util.List;
import java.util.Map;

/**
 * Options to configure build process.
 *
 * @author Eugene Voevodin
 */
@DTO
public interface BuildOptions {
    /** Get name of builder. This parameter has preference over builder name that is configured in properties of project. */
    @ApiModelProperty(value = "Builder name. Any registered builder name: maven, ant, npm", position = 1)
    String getBuilderName();

    void setBuilderName(String builderName);

    BuildOptions withBuilderName(String builderName);

    /**
     * Build targets, e.g. "clean", "compile", ... . Supported targets depend on builder implementation. Builder uses default targets if
     * this parameter is not provided by client.
     */
    @ApiModelProperty(value = "Build targets. Build targets, e.g. \"clean\", \"compile\". Supported targets depend on builder implementation", position = 2)
    List<String> getTargets();

    BuildOptions withTargets(List<String> targets);

    void setTargets(List<String> targets);

    /**
     * Optional parameters for builder. Supported options depend on builder implementation. Builder may provide own set of options. User
     * specified options have preference over builder's default options.
     */
    Map<String, String> getOptions();

    BuildOptions withOptions(Map<String, String> options);

    void setOptions(Map<String, String> options);

    @ApiModelProperty(value = "Skit tests", allowableValues = "true,false", dataType = "boolean", position = 3)
    boolean isSkipTest();

    void setSkipTest(boolean skip);

    BuildOptions withSkipTest(boolean skip);

    @ApiModelProperty(value = "Include deps", allowableValues = "true,false", dataType = "boolean", position = 4)
    boolean isIncludeDependencies();

    void setIncludeDependencies(boolean includeDependencies);

    BuildOptions withIncludeDependencies(boolean includeDependencies);
}
