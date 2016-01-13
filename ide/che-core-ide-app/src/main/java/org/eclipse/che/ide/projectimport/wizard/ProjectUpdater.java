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
package org.eclipse.che.ide.projectimport.wizard;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.event.ConfigureProjectEvent;
import org.eclipse.che.ide.api.event.project.CreateProjectEvent;
import org.eclipse.che.ide.api.project.wizard.ImportProjectNotificationSubscriber;
import org.eclipse.che.ide.api.wizard.Wizard.CompleteCallback;
import org.eclipse.che.ide.projectimport.ErrorMessageUtils;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.Unmarshallable;
import org.eclipse.che.ide.util.loging.Log;

import javax.validation.constraints.NotNull;

/**
 * The class contains business logic which allows update project.
 *
 * @author Dmitry Shnurenko
 */
@Singleton
public class ProjectUpdater {

    private final DtoUnmarshallerFactory              dtoUnmarshallerFactory;
    private final ProjectServiceClient                projectServiceClient;
    private final ImportProjectNotificationSubscriber importProjectNotificationSubscriber;
    private final EventBus                            eventBus;
    private final String                              workspaceId;

    @Inject
    public ProjectUpdater(DtoUnmarshallerFactory dtoUnmarshallerFactory,
                          ProjectServiceClient projectServiceClient,
                          ImportProjectNotificationSubscriber importProjectNotificationSubscriber,
                          EventBus eventBus,
                          AppContext appContext) {
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.projectServiceClient = projectServiceClient;
        this.importProjectNotificationSubscriber = importProjectNotificationSubscriber;
        this.eventBus = eventBus;
        this.workspaceId = appContext.getWorkspaceId();
    }

    /**
     * The method updates project and take resolution should project be configured or not.
     *
     * @param callback
     *         callback which is necessary to inform that resolving completed
     * @param projectConfig
     *         project which will be resolved
     * @param isConfigurationRequired
     *         special flag which defines will project be configured or not.<code>true</code> project will be configured,
     *         <code>false</code> project will not be configured
     */
    public void updateProject(@NotNull final CompleteCallback callback,
                              @NotNull ProjectConfigDto projectConfig,
                              final boolean isConfigurationRequired) {
        final String projectName = projectConfig.getName();
        Unmarshallable<ProjectConfigDto> unmarshaller = dtoUnmarshallerFactory.newUnmarshaller(ProjectConfigDto.class);
        projectServiceClient.updateProject(workspaceId,
                                           projectName,
                                           projectConfig,
                                           new AsyncRequestCallback<ProjectConfigDto>(unmarshaller) {
                                               @Override
                                               protected void onSuccess(final ProjectConfigDto result) {
                                                   eventBus.fireEvent(new CreateProjectEvent(result));
                                                   importProjectNotificationSubscriber.onSuccess();
                                                   callback.onCompleted();
                                                   if (!result.getProblems().isEmpty() || isConfigurationRequired) {
                                                       eventBus.fireEvent(new ConfigureProjectEvent(result));
                                                   }
                                               }

                                               @Override
                                               protected void onFailure(Throwable exception) {
                                                   importProjectNotificationSubscriber.onFailure(exception.getMessage());
                                                   deleteProject(projectName);
                                                   String errorMessage = ErrorMessageUtils.getErrorMessage(exception);
                                                   callback.onFailure(new Exception(errorMessage));
                                               }
                                           });
    }

    private void deleteProject(final String name) {
        projectServiceClient.delete(workspaceId, name, new AsyncRequestCallback<Void>() {
            @Override
            protected void onSuccess(Void result) {
                Log.info(ImportWizard.class, "Project " + name + " deleted.");
            }

            @Override
            protected void onFailure(Throwable exception) {
                Log.error(ImportWizard.class, exception);
            }
        });
    }
}
