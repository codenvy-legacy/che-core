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
package org.eclipse.che.api.builder.gwt.client;

import org.eclipse.che.api.builder.dto.BuildOptions;
import org.eclipse.che.api.builder.dto.BuildTaskDescriptor;
import org.eclipse.che.api.builder.dto.BuilderDescriptor;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.ide.rest.AsyncRequestCallback;

import java.util.List;

/**
 * Client for Builder service.
 *
 * @author Artem Zatsarynnyy
 */
public interface BuilderServiceClient {
    /**
     * Start new build.
     *
     * @param projectName
     *         identifier of the project we want to send for build
     * @param callback
     *         callback
     */
    void build(String projectName, AsyncRequestCallback<BuildTaskDescriptor> callback);

    /**
     * Start new build.
     *
     * @param projectName
     *         identifier of the project we want to send for build
     * @param buildOptions
     *         Options to configure build process
     * @param callback
     *         callback
     */
    void build(String projectName, BuildOptions buildOptions, AsyncRequestCallback<BuildTaskDescriptor> callback);

    /**
     * Cancel previously launched build.
     *
     * @param buildId
     *         ID of build
     * @param callback
     *         callback
     */
    void cancel(String buildId, AsyncRequestCallback<StringBuilder> callback);

    /**
     * Check current status of previously launched build.
     * <p/>
     * identifier of build
     *
     * @param callback
     *         callback
     */
    void status(Link link, AsyncRequestCallback<String> callback);

    /** Get build log. */
    void log(Link link, AsyncRequestCallback<String> callback);

    /**
     * Get build result.
     *
     * @param buildId
     *         ID of build
     * @param callback
     *         callback
     */
    void result(String buildId, AsyncRequestCallback<String> callback);

    /**
     * Get build result.
     *
     * @param callback
     *         callback
     */
    void getRegisteredServers(AsyncRequestCallback<List<BuilderDescriptor>> callback);
}
