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
package org.eclipse.che.ide.core;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.core.rest.shared.dto.ServiceError;
import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.Constants;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.project.shared.dto.SourceEstimation;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.ide.api.event.ConfigureProjectEvent;
import org.eclipse.che.ide.api.event.OpenProjectEvent;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.core.problemDialog.ProjectProblemDialogCallback;
import org.eclipse.che.ide.core.problemDialog.ProjectProblemDialogPresenter;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.Unmarshallable;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * The purpose of this class is resolve problems when we can't recognize project type.
 *
 * @author Roman Nikitenko
 */
@Singleton
public class ProjectProblemResolver implements ProjectProblemDialogCallback {

    private final ProjectServiceClient          projectServiceClient;
    private final DtoUnmarshallerFactory        dtoUnmarshallerFactory;
    private final EventBus                      eventBus;
    private final DtoFactory                    dtoFactory;
    private final NotificationManager           notificationManager;
    private final ProjectProblemDialogPresenter problemDialogPresenter;
    private       ProjectDescriptor             projectDescriptor;


    @Inject
    public ProjectProblemResolver(final ProjectServiceClient projectServiceClient,
                                  final EventBus eventBus,
                                  final DtoFactory dtoFactory,
                                  final NotificationManager notificationManager,
                                  final DtoUnmarshallerFactory dtoUnmarshallerFactory,
                                  final ProjectProblemDialogPresenter problemDialogPresenter) {
        this.eventBus = eventBus;
        this.dtoFactory = dtoFactory;
        this.notificationManager = notificationManager;
        this.problemDialogPresenter = problemDialogPresenter;
        this.projectServiceClient = projectServiceClient;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
    }

    /**
     * Receives estimated project types and displays project problem dialog to the end user.
     *
     * @param projectDescriptor
     *         projectDescriptor of the project for resolve project type
     */
    public void resolve(final @NotNull ProjectDescriptor projectDescriptor) {
        this.projectDescriptor = projectDescriptor;

        final Unmarshallable<List<SourceEstimation>> unmarshaller = dtoUnmarshallerFactory.newListUnmarshaller(SourceEstimation.class);
        projectServiceClient.resolveSources(projectDescriptor.getPath(), new AsyncRequestCallback<List<SourceEstimation>>(unmarshaller) {
            @Override
            protected void onSuccess(List<SourceEstimation> result) {
                problemDialogPresenter.showDialog(result, ProjectProblemResolver.this);
            }

            @Override
            protected void onFailure(Throwable exception) {
                problemDialogPresenter.showDialog(null, ProjectProblemResolver.this);
            }
        });
    }

    @Override
    public void onConfigure(@Nullable SourceEstimation estimatedType) {
        updateProjectDescriptor(estimatedType);
        eventBus.fireEvent(new ConfigureProjectEvent(projectDescriptor));
    }

    @Override
    public void onOpenAsIs() {
        updateProjectDescriptor(null);
        updateProject();
    }

    @Override
    public void onOpenAs(@Nullable SourceEstimation estimatedType) {
        updateProjectDescriptor(estimatedType);
        updateProject();
    }

    private void updateProject() {
        final Unmarshallable<ProjectDescriptor> unmarshaller = dtoUnmarshallerFactory.newUnmarshaller(ProjectDescriptor.class);
        projectServiceClient
                .updateProject(projectDescriptor.getName(), projectDescriptor, new AsyncRequestCallback<ProjectDescriptor>(unmarshaller) {
                    @Override
                    protected void onSuccess(ProjectDescriptor result) {
                        eventBus.fireEvent(new OpenProjectEvent(result.getName()));
                    }

                    @Override
                    protected void onFailure(Throwable exception) {
                        final String message = dtoFactory.createDtoFromJson(exception.getMessage(), ServiceError.class).getMessage();
                        notificationManager.showError(message);
                    }
                });
    }

    private void updateProjectDescriptor(@Nullable SourceEstimation estimatedType) {
        if (estimatedType != null) {
            projectDescriptor.setType(estimatedType.getType());
            projectDescriptor.setAttributes(estimatedType.getAttributes());
        } else {
            projectDescriptor.setType(Constants.BLANK_ID);
        }
    }
}
