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
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.project.shared.dto.ProjectTemplateDescriptor;
import org.eclipse.che.api.workspace.gwt.client.event.StartWorkspaceEvent;
import org.eclipse.che.api.workspace.gwt.client.event.StartWorkspaceHandler;
import org.eclipse.che.api.workspace.shared.dto.UsersWorkspaceDto;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.AsyncRequestFactory;
import org.eclipse.che.ide.rest.AsyncRequestLoader;
import org.eclipse.che.ide.ui.loaders.request.LoaderFactory;

import javax.inject.Named;
import javax.validation.constraints.NotNull;
import java.util.List;

import static org.eclipse.che.ide.MimeType.APPLICATION_JSON;
import static org.eclipse.che.ide.rest.HTTPHeader.ACCEPT;

/**
 * The implementation for {@link ProjectTemplateServiceClient}.
 *
 * @author Artem Zatsarynnyi
 */
public class ProjectTemplateServiceClientImpl implements ProjectTemplateServiceClient, StartWorkspaceHandler {

    private final AsyncRequestFactory asyncRequestFactory;
    private final AsyncRequestLoader  loader;
    private final String              extPath;

    private String baseUrl;

    @Inject
    protected ProjectTemplateServiceClientImpl(@Named("cheExtensionPath") String extPath,
                                               EventBus eventBus,
                                               AsyncRequestFactory asyncRequestFactory,
                                               LoaderFactory loaderFactory) {
        this.extPath = extPath;
        this.asyncRequestFactory = asyncRequestFactory;
        this.loader = loaderFactory.newLoader();

        eventBus.addHandler(StartWorkspaceEvent.TYPE, this);
    }

    @Override
    public void onWorkspaceStarted(UsersWorkspaceDto workspace) {
        baseUrl = extPath + "/project-template/" + workspace.getId() + "/";
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