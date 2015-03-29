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

import org.eclipse.che.api.core.rest.shared.dto.Hyperlinks;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.dto.shared.DTO;

import java.util.List;

/**
 * @author andrew00x
 */
@DTO
public interface RunnerServer extends Hyperlinks {
    String getUrl();

    void setUrl(String url);

    RunnerServer withUrl(String url);

    String getDescription();

    void setDescription(String description);

    RunnerServer withDescription(String description);

    boolean isDedicated();

    void setDedicated(boolean dedicated);

    RunnerServer withDedicated(boolean dedicated);

    String getWorkspace();

    RunnerServer withWorkspace(String workspace);

    void setWorkspace(String workspace);

    String getProject();

    RunnerServer withProject(String project);

    void setProject(String project);

    ServerState getServerState();

    RunnerServer withServerState(ServerState serverState);

    void setServerState(ServerState serverState);

    RunnerServer withLinks(List<Link> links);
}
