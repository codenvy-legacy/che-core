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

import com.google.inject.Inject;

import org.eclipse.che.api.project.shared.dto.ProjectTemplateDescriptor;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.AsyncRequestFactory;
import org.eclipse.che.ide.rest.AsyncRequestLoader;

import javax.validation.constraints.NotNull;
import javax.inject.Named;
import java.util.List;

import java.util.List;

import static org.eclipse.che.ide.MimeType.APPLICATION_JSON;
import static org.eclipse.che.ide.rest.HTTPHeader.ACCEPT;

/**
 * The implementation for {@link ProjectTemplateServiceClient}.
 *
 * @author Artem Zatsarynnyi
 */
public class ProjectTemplateServiceClientImpl implements ProjectTemplateServiceClient {
    private final String              baseUrl;
    private final AsyncRequestFactory asyncRequestFactory;
    private final AsyncRequestLoader  loader;

    @Inject
    protected ProjectTemplateServiceClientImpl(@Named("cheExtensionPath") String extPath,
                                               @Named("workspaceId") String workspaceId,
                                               AsyncRequestFactory asyncRequestFactory,
                                               AsyncRequestLoader loader) {
        this.asyncRequestFactory = asyncRequestFactory;
        this.loader = loader;
        baseUrl = extPath + "/project-template/" + workspaceId +"/";
    }

    @Override
    public void getProjectTemplates(@NotNull String projectTypeId,
                                    @NotNull AsyncRequestCallback<List<ProjectTemplateDescriptor>> callback) {
        final String requestUrl = baseUrl + projectTypeId;
        asyncRequestFactory.createGetRequest(requestUrl).header(ACCEPT, APPLICATION_JSON).loader(loader).send(callback);
    }

    @Override
    public void getProjectTemplates(@NotNull AsyncRequestCallback<List<ProjectTemplateDescriptor>> callback) {
        asyncRequestFactory.createGetRequest(baseUrl).header(ACCEPT, APPLICATION_JSON).loader(loader).send(callback);
    }
}