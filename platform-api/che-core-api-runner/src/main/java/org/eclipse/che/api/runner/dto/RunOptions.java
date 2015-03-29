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
package org.eclipse.che.api.runner.dto;

import org.eclipse.che.api.builder.dto.BuildOptions;
import org.eclipse.che.dto.shared.DTO;
import com.wordnik.swagger.annotations.ApiModelProperty;

import java.util.Map;

/**
 * Options to configure run process.
 *
 * @author Eugene Voevodin
 */
@DTO
public interface RunOptions {
    /**
     * Get id of environment that should be used for running an application.
     *
     * @see RunnerDescriptor#getEnvironments()
     */
    @ApiModelProperty(value = "Environment ID", notes = "Visit docs site for parameters reference")
    String getEnvironmentId();

    void setEnvironmentId(String environmentId);

    RunOptions withEnvironmentId(String environmentId);

    /** Get memory size (in megabytes) that is required for starting application. */
    @ApiModelProperty(value = "Memory allocated per run")
    int getMemorySize();

    void setMemorySize(int mem);

    RunOptions withMemorySize(int mem);

    /** Enables or disables debug mode of runner. */
    boolean isInDebugMode();

    void setInDebugMode(boolean debugMode);

    RunOptions withInDebugMode(boolean debugMode);

    /**
     * Gets options for runner. Supported options depend on runner implementation. Runner may have own set of options. Caller specified
     * options have preference over runner's default options.
     */
    Map<String, String> getOptions();

    RunOptions withOptions(Map<String, String> options);

    void setOptions(Map<String, String> options);

    /**
     * Gets environment variables for runner. Supported variables depend on runner implementation. Runner may have own set of variables.
     * Caller specified variables have preference over runner's default environment variables.
     */
    Map<String, String> getVariables();

    /**
     * Sets environment variables (runner type and(or) receipt specific).
     *
     * @see #getVariables()
     */
    void setVariables(Map<String, String> variables);

    RunOptions withVariables(Map<String, String> variables);

    /** Force skip build before run. Build stage is skipped even project has configuration for builder. */
    @ApiModelProperty(value = "Skip build", dataType = "boolean", allowableValues = "true,false")
    boolean getSkipBuild();

    void setSkipBuild(boolean skipBuild);

    RunOptions withSkipBuild(boolean skipBuild);

    /**
     * Get builder options. Make sense only for application that requires build before run. This parameter has preference over builder
     * options that is configured in properties of project.
     *
     * @see org.eclipse.che.api.builder.dto.BuildOptions
     */
    @ApiModelProperty(value = "Build options", notes = "This parameter overrides builder properties of a project")
    BuildOptions getBuildOptions();

    void setBuildOptions(BuildOptions options);

    RunOptions withBuildOptions(BuildOptions options);

    /**
     * Runner may provide shell console to the instance with running application. Map that is returned by this method contains
     * configuration parameters for shell console. Supporting of shell console is optional feature and not all runner's implementation may
     * support this feature.
     */
    @ApiModelProperty(value = "Terminal Access")
    Map<String, String> getShellOptions();

    RunOptions withShellOptions(Map<String, String> options);

    void setShellOptions(Map<String, String> options);
}
