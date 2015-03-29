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

import org.eclipse.che.api.builder.dto.BuildTaskDescriptor;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.dto.shared.DTO;

import java.util.List;
import java.util.Map;

/**
 * Run application request.
 *
 * @author andrew00x
 */
@DTO
public interface RunRequest {
    long getId();

    void setId(long id);

    RunRequest withId(long id);

    /** Describes build process if application was build before run task, e.g. for compilable languages. */
    BuildTaskDescriptor getBuildTaskDescriptor();

    void setBuildTaskDescriptor(BuildTaskDescriptor buildDescriptor);

    RunRequest withBuildTaskDescriptor(BuildTaskDescriptor buildDescriptor);

    ProjectDescriptor getProjectDescriptor();

    void setProjectDescriptor(ProjectDescriptor project);

    RunRequest withProjectDescriptor(ProjectDescriptor project);

    /** Name of {@link org.eclipse.che.api.runner.internal.Runner} which should be used for running this application. */
    String getRunner();

    void setRunner(String runner);

    RunRequest withRunner(String runner);

    /**
     * Get id of environment that should be used for running an application. If this parameter is omitted then runner will use default
     * environment.
     */
    String getEnvironmentId();

    void setEnvironmentId(String environmentId);

    RunRequest withEnvironmentId(String environmentId);

    /** Location of files that contains run recipes. */
    List<String> getRecipeUrls();

    void setRecipeUrls(List<String> recipes);

    RunRequest withRecipeUrls(List<String> scripts);

    /** Enables or disables debug mode of runner. Not all Runner implementations support debug mode. */
    boolean isInDebugMode();

    void setInDebugMode(boolean debugMode);

    RunRequest withInDebugMode(boolean debugMode);

    /** Get memory size (in megabytes) that is required for starting application. */
    int getMemorySize();

    void setMemorySize(int mem);

    RunRequest withMemorySize(int mem);

    /** Options for Runner. Supported options depend on Runner implementation. */
    Map<String, String> getOptions();

    void setOptions(Map<String, String> options);

    RunRequest withOptions(Map<String, String> options);

    /** Environment variables for runner. Supported variables depend on Runner implementation. */
    Map<String, String> getVariables();

    void setVariables(Map<String, String> variables);

    RunRequest withVariables(Map<String, String> variables);

    /** @see RunOptions#getShellOptions() */
    Map<String, String> getShellOptions();

    RunRequest withShellOptions(Map<String, String> options);

    void setShellOptions(Map<String, String> options);

    /**
     * Get application lifetime in seconds. If application is running longer then this time {@link org.eclipse.che.api.runner.internal.Runner}
     * must terminate the application.
     */
    long getLifetime();

    void setLifetime(long time);

    RunRequest withLifetime(long time);

    /** Name of workspace which the project is belong. */
    String getWorkspace();

    void setWorkspace(String workspace);

    RunRequest withWorkspace(String workspace);

    /** Name of project which represents sources on the ide side. */
    String getProject();

    void setProject(String project);

    RunRequest withProject(String project);


    String getUserId();

    RunRequest withUserId(String userId);

    void setUserId(String userId);

    String getUserToken();

    RunRequest withUserToken(String token);

    void setUserToken(String token);
}
