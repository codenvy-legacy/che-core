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
package org.eclipse.che.api.project.gwt.client;

import org.eclipse.che.api.project.shared.dto.ProjectImporterDescriptor;
import org.eclipse.che.ide.MimeType;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.AsyncRequestFactory;
import org.eclipse.che.ide.rest.HTTPHeader;
import org.eclipse.che.ide.rest.RestContext;

import javax.inject.Inject;
import java.util.List;

/**
 * @author Vitaly Parfonov
 */
public class ProjectImportersServiceClientImpl implements ProjectImportersServiceClient {


    private String              restContext;
    private AsyncRequestFactory asyncRequestFactory;

    @Inject
    public ProjectImportersServiceClientImpl(@RestContext String restContext,
                                             AsyncRequestFactory asyncRequestFactory) {
        this.restContext = restContext;
        this.asyncRequestFactory = asyncRequestFactory;
    }

    @Override
    public void getProjectImporters(AsyncRequestCallback<List<ProjectImporterDescriptor>> callback) {
        asyncRequestFactory.createGetRequest(restContext + "/project-importers")
                           .header(HTTPHeader.CONTENT_TYPE, MimeType.APPLICATION_JSON)
                           .send(callback);

    }
}
