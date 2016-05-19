/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.builder.dto;

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.dto.shared.DTO;

import java.util.List;
import java.util.Map;

/**
 * Base request.
 *
 * @author andrew00x
 */
@DTO
public interface BaseBuilderRequest {
    long getId();

    void setId(long id);

    BaseBuilderRequest withId(long id);

    /**
     * Location of source code for build. It is required to have {@link org.eclipse.che.api.core.util.DownloadPlugin} which supports such type
     * of URL.
     *
     * @see org.eclipse.che.api.core.util.DownloadPlugin#download(String, java.io.File, org.eclipse.che.api.core.util.DownloadPlugin.Callback)
     */
    String getSourcesUrl();

    BaseBuilderRequest withSourcesUrl(String url);

    void setSourcesUrl(String url);

    /**
     * Name of which should be used for build. Client should use method {@link org.eclipse.che.api.builder.internal.SlaveBuilderService#availableBuilders()}
     * to get list of available builders.
     */
    String getBuilder();

    BaseBuilderRequest withBuilder(String builder);

    void setBuilder(String builder);

    /**
     * Get build timeout in seconds. If build is running longer then this time {@link org.eclipse.che.api.builder.internal.Builder} must
     * terminate the build.
     */
    long getTimeout();

    void setTimeout(long time);

    BaseBuilderRequest withTimeout(long time);

    /**
     * Build targets, e.g. "clean", "compile", ... . Supported targets depend on builder implementation. Builder uses default targets if
     * this parameter is not provided by client.
     */
    List<String> getTargets();

    BaseBuilderRequest withTargets(List<String> targets);

    void setTargets(List<String> targets);

    /**
     * Optional parameters for builder. Supported options depend on builder implementation. Builder may provide own set of options. User
     * specified options have preference over builder's default options.
     */
    Map<String, String> getOptions();

    BaseBuilderRequest withOptions(Map<String, String> options);

    void setOptions(Map<String, String> options);

    /** Name of workspace which the sources are belong. */
    String getWorkspace();

    BaseBuilderRequest withWorkspace(String workspace);

    void setWorkspace(String workspace);

    /** Name of project which represents sources on the ide side. */
    String getProject();

    BaseBuilderRequest withProject(String project);

    void setProject(String project);

    ProjectDescriptor getProjectDescriptor();

    void setProjectDescriptor(ProjectDescriptor project);

    BaseBuilderRequest withProjectDescriptor(ProjectDescriptor project);

    boolean isIncludeDependencies();

    void setIncludeDependencies(boolean includeDependencies);

    BaseBuilderRequest withIncludeDependencies(boolean includeDependencies);

    String getProjectUrl();

    BaseBuilderRequest withProjectUrl(String url);

    void setProjectUrl(String url);

    String getUserId();

    BaseBuilderRequest withUserId(String userId);

    void setUserId(String userId);
}
