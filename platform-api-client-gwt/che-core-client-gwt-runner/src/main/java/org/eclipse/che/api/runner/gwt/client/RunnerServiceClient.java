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
package org.eclipse.che.api.runner.gwt.client;

import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.project.shared.dto.RunnerEnvironmentTree;
import org.eclipse.che.api.runner.dto.ResourcesDescriptor;
import org.eclipse.che.api.runner.dto.RunOptions;
import org.eclipse.che.api.runner.dto.RunnerDescriptor;
import org.eclipse.che.api.runner.dto.ApplicationProcessDescriptor;

import org.eclipse.che.ide.rest.AsyncRequestCallback;

import java.util.List;

/**
 * Client for Runner service.
 *
 * @author Artem Zatsarynnyy
 */
public interface RunnerServiceClient {
    /**
     * Run an app on the application server.
     *
     * @param projectName
     *         name of the project to run
     * @param runOptions
     *         options to configure run process
     * @param callback
     *         the callback to use for the response
     */
    void run(String projectName, RunOptions runOptions, AsyncRequestCallback<ApplicationProcessDescriptor> callback);

    /**
     * Get status of app.
     *
     * @param link
     *         link to get application's status
     * @param callback
     *         the callback to use for the response
     */
    void getStatus(Link link, AsyncRequestCallback<ApplicationProcessDescriptor> callback);

    /**
     * Get runner processes by project name.
     *
     * @param project
     *         name of the project to get an appropriate runner processes
     * @param callback
     *         the callback to use for the response
     */
    void getRunningProcesses(String project, AsyncRequestCallback<List<ApplicationProcessDescriptor>> callback);

    /**
     * Retrieve logs from application server where app is launched.
     *
     * @param link
     *         link to retrieve logs
     * @param callback
     *         the callback to use for the response
     */
    void getLogs(Link link, AsyncRequestCallback<String> callback);

    /**
     * Stop application server where app is launched.
     *
     * @param link
     *         link to stop an app
     * @param callback
     *         the callback to use for the response
     */
    void stop(Link link, AsyncRequestCallback<ApplicationProcessDescriptor> callback);

    /**
     * Get available runners.
     *
     * @param callback
     *         the callback to use for the response
     */
    void getRunners(AsyncRequestCallback<RunnerEnvironmentTree> callback);

    /**
     * Get available runners.
     *
     * @param projectName
     *         name of the project
     * @param callback
     *         the callback to use for the response
     */
    void getRunners(String projectName, AsyncRequestCallback<List<RunnerDescriptor>> callback);

    /**
     * Get resources for user in current workspace.
     *
     * @param callback
     *         the callback to use for the response
     */
    void getResources(AsyncRequestCallback<ResourcesDescriptor> callback);
}
