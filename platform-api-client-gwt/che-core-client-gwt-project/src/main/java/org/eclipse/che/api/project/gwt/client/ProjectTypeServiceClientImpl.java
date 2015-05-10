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
package org.eclipse.che.api.project.gwt.client;

import org.eclipse.che.api.project.shared.dto.ProjectTypeDefinition;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.AsyncRequestFactory;
import org.eclipse.che.ide.rest.AsyncRequestLoader;
import org.eclipse.che.ide.rest.RestContext;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import static org.eclipse.che.ide.MimeType.APPLICATION_JSON;
import static org.eclipse.che.ide.rest.HTTPHeader.ACCEPT;

/**
 * The implementation of {@link ProjectTypeServiceClient}.
 *
 * @author Artem Zatsarynnyy
 */
public class ProjectTypeServiceClientImpl implements ProjectTypeServiceClient {
    private final String              baseUrl;
    private final AsyncRequestFactory asyncRequestFactory;
    private final AsyncRequestLoader  loader;

    @Inject
    protected ProjectTypeServiceClientImpl(@RestContext String restContext,
                                           AsyncRequestLoader loader,
                                           AsyncRequestFactory asyncRequestFactory) {
        this.loader = loader;
        this.asyncRequestFactory = asyncRequestFactory;
        baseUrl = restContext + "/project-type";
    }

    @Override
    public void getProjectTypes(AsyncRequestCallback<Array<ProjectTypeDefinition>> callback) {
        asyncRequestFactory.createGetRequest(baseUrl).header(ACCEPT, APPLICATION_JSON).loader(loader).send(callback);
    }
}