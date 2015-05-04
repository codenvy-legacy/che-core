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

import com.wordnik.swagger.annotations.ApiModelProperty;

import org.eclipse.che.api.builder.dto.BuilderMetric;
import org.eclipse.che.api.core.rest.shared.dto.Hyperlinks;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.runner.ApplicationStatus;
import org.eclipse.che.dto.shared.DTO;

import java.util.List;

/**
 * Describes one application process. Typically instance of this class should provide set of links to make possible to get more info about
 * application. Set of links is depends to status of process.
 *
 * @author andrew00x
 */
@DTO
public interface ApplicationProcessDescriptor extends Hyperlinks {
    @ApiModelProperty(value = "Process ID")
    long getProcessId();

    ApplicationProcessDescriptor withProcessId(long processId);

    void setProcessId(long processId);

    @ApiModelProperty(value = "Application status")
    ApplicationStatus getStatus();

    void setStatus(ApplicationStatus status);

    ApplicationProcessDescriptor withStatus(ApplicationStatus status);

    @ApiModelProperty(value = "Creation time")
    long getCreationTime();

    ApplicationProcessDescriptor withCreationTime(long creationTime);

    void setCreationTime(long creationTime);

    @ApiModelProperty(value = "Start time")
    long getStartTime();

    ApplicationProcessDescriptor withStartTime(long startTime);

    void setStartTime(long startTime);

    @ApiModelProperty(value = "Stop time")
    long getStopTime();

    ApplicationProcessDescriptor withStopTime(long stopTime);

    void setStopTime(long stopTime);

    @ApiModelProperty(value = "Debug port")
    int getDebugPort();

    void setDebugPort(int port);

    ApplicationProcessDescriptor withDebugPort(int port);

    @ApiModelProperty(value = "Debug host")
    String getDebugHost();

    void setDebugHost(String host);

    ApplicationProcessDescriptor withDebugHost(String host);

    @ApiModelProperty(value = "Run stats")
    List<RunnerMetric> getRunStats();

    ApplicationProcessDescriptor withRunStats(List<RunnerMetric> stats);

    void setRunStats(List<RunnerMetric> stats);

    @ApiModelProperty(value = "Build stats")
    List<BuilderMetric> getBuildStats();

    ApplicationProcessDescriptor withBuildStats(List<BuilderMetric> stats);

    void setBuildStats(List<BuilderMetric> stats);

    /** Name of workspace which the project is belong. */
    @ApiModelProperty(value = "Workspace name")
    String getWorkspace();

    void setWorkspace(String workspace);

    ApplicationProcessDescriptor withWorkspace(String workspace);

    /** Name of project which represents sources on the ide side. */
    @ApiModelProperty(value = "Project name")
    String getProject();

    void setProject(String project);

    ApplicationProcessDescriptor withProject(String project);

    /** The id of user who ran application. */
    @ApiModelProperty(value = "User who initiated run")
    String getUserId();

    ApplicationProcessDescriptor withUserId(String userId);

    void setUserId(String userId);

    /**
     * The URL of server where application is running. This information is accessible only over RunnerAdminServer and is not available for
     * regular users.
     */
    @ApiModelProperty(value = "Server URL where the app is running")
    String getServerUrl();

    void setServerUrl(String server);

    ApplicationProcessDescriptor withServerUrl(String server);

    ApplicationProcessDescriptor withLinks(List<Link> links);

    /** @see PortMapping */
    PortMapping getPortMapping();

    void setPortMapping(PortMapping portMapping);

    ApplicationProcessDescriptor withPortMapping(PortMapping portMapping);

    @ApiModelProperty(value = "Amount of available RAM")
    int getMemorySize();

    void setMemorySize(int memory);

    ApplicationProcessDescriptor withMemorySize(int memory);

    @ApiModelProperty(value = "Id of started environment")
    String getEnvironmentId();

    void setEnvironmentId(String environmentId);

    ApplicationProcessDescriptor withEnvironmentId(String environmentId);
}
