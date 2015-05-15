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

import org.eclipse.che.api.builder.BuildStatus;
import org.eclipse.che.api.core.rest.shared.dto.Hyperlinks;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.dto.shared.DTO;

import java.util.List;

/**
 * Describes one build process. Typically instance of this class should provide set of links to make possible to get more info about build.
 * Set of links is depends to status of build. E.g. if build successful then one of the links provides location for download build result,
 * get build report, etc.
 *
 * @author andrew00x
 */
@DTO
public interface BuildTaskDescriptor extends Hyperlinks {
    String getWorkspace();

    void setWorkspace(String workspace);

    BuildTaskDescriptor withWorkspace(String workspace);

    String getProject();

    void setProject(String project);

    BuildTaskDescriptor withProject(String project);

    BuildStatus getStatus();

    BuildTaskDescriptor withStatus(BuildStatus status);

    void setStatus(BuildStatus status);

    long getCreationTime();

    BuildTaskDescriptor withCreationTime(long creationTime);

    void setCreationTime(long creationTime);

    long getStartTime();

    BuildTaskDescriptor withStartTime(long startTime);

    void setStartTime(long startTime);

    long getEndTime();

    BuildTaskDescriptor withEndTime(long endTime);

    void setEndTime(long endTime);

    long getTaskId();

    BuildTaskDescriptor withTaskId(long taskId);

    void setTaskId(long taskId);

    String getCommandLine();

    void setCommandLine(String cmd);

    BuildTaskDescriptor withCommandLine(String cmd);

    List<BuilderMetric> getBuildStats();

    BuildTaskDescriptor withBuildStats(List<BuilderMetric> stats);

    void setBuildStats(List<BuilderMetric> stats);

    BuildTaskDescriptor withLinks(List<Link> links);
}
