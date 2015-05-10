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
import org.eclipse.che.api.runner.dto.ApplicationProcessDescriptor;
import org.eclipse.che.api.runner.dto.RunOptions;
import org.eclipse.che.api.runner.dto.RunnerDescriptor;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.AsyncRequestFactory;
import org.eclipse.che.ide.rest.AsyncRequestLoader;
import org.eclipse.che.ide.rest.RestContext;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

/**
 * Implementation of {@link RunnerServiceClient} service.
 *
 * @author Artem Zatsarynnyy
 */
@Singleton
public class RunnerServiceClientImpl implements RunnerServiceClient {
    private final String              baseUrl;
    private final String              workspaceId;
    private final AsyncRequestLoader  loader;
    private final AsyncRequestFactory asyncRequestFactory;

    @Inject
    public RunnerServiceClientImpl(@RestContext String baseUrl,
                                   @Named("workspaceId") String workspaceId,
                                   AsyncRequestLoader loader,
                                   AsyncRequestFactory asyncRequestFactory) {
        this.baseUrl = baseUrl;
        this.workspaceId = workspaceId;
        this.loader = loader;
        this.asyncRequestFactory = asyncRequestFactory;
    }

    /** {@inheritDoc} */
    @Override
    public void run(String projectName, RunOptions runOptions, AsyncRequestCallback<ApplicationProcessDescriptor> callback) {
        final String requestUrl = baseUrl + "/runner/" + workspaceId + "/run";
        final String params = "project=" + projectName;
        asyncRequestFactory.createPostRequest(requestUrl + "?" + params, runOptions).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void getStatus(Link link, AsyncRequestCallback<ApplicationProcessDescriptor> callback) {
        asyncRequestFactory.createGetRequest(link.getHref()).send(callback);
    }

    @Override
    public void getRunningProcesses(String projectName, AsyncRequestCallback<Array<ApplicationProcessDescriptor>> callback) {
        final String requestUrl = baseUrl + "/runner/" + workspaceId + "/processes";
        final String params = "project=" + projectName;
        asyncRequestFactory.createGetRequest(requestUrl + "?" + params).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void getLogs(Link link, AsyncRequestCallback<String> callback) {
        asyncRequestFactory.createGetRequest(link.getHref())
                           .loader(loader, "Retrieving logs…")
                           .send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void stop(Link link, AsyncRequestCallback<ApplicationProcessDescriptor> callback) {
        asyncRequestFactory.createPostRequest(link.getHref(), null)
                           .send(callback);
    }

    @Override
    public void getRunners(AsyncRequestCallback<RunnerEnvironmentTree> callback) {
        final String requestUrl = baseUrl + "/runner/" + workspaceId + "/available";
        asyncRequestFactory.createGetRequest(requestUrl, false).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void getRunners(String projectName, AsyncRequestCallback<Array<RunnerDescriptor>> callback) {
        final String requestUrl = baseUrl + "/runner/" + workspaceId + "/available";
        final String params = "project=" + projectName;
        asyncRequestFactory.createGetRequest(requestUrl + "?" + params, false).send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void getResources(AsyncRequestCallback<ResourcesDescriptor> callback) {
        final String requestUrl = baseUrl + "/runner/" + workspaceId + "/resources";
        asyncRequestFactory.createGetRequest(requestUrl, false).send(callback);
    }
}
