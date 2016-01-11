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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.core.rest.shared.dto.ServiceError;
import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.Constants;
import org.eclipse.che.api.project.shared.dto.ProjectTypeDto;
import org.eclipse.che.api.project.shared.dto.SourceEstimation;
import org.eclipse.che.api.vfs.gwt.client.VfsServiceClient;
import org.eclipse.che.api.vfs.shared.dto.Item;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.event.ConfigureProjectEvent;
import org.eclipse.che.ide.api.event.project.CreateProjectEvent;
import org.eclipse.che.ide.api.project.type.ProjectTypeRegistry;
import org.eclipse.che.ide.api.project.wizard.ImportProjectNotificationSubscriber;
import org.eclipse.che.ide.api.wizard.Wizard.CompleteCallback;
import org.eclipse.che.ide.commons.exception.JobNotFoundException;
import org.eclipse.che.ide.commons.exception.UnauthorizedException;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.Unmarshallable;
import org.eclipse.che.ide.util.loging.Log;
import org.eclipse.che.ide.websocket.rest.RequestCallback;

import javax.annotation.Nullable;
import java.util.List;

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.getFirst;
import static com.google.common.collect.Iterables.size;
import static com.google.common.collect.Iterables.transform;
import static org.eclipse.che.ide.util.StringUtils.isNullOrEmpty;

/**
 * @author Dmitry Shnurenko
 */
@Singleton
public class ProjectImporter {

    private final ProjectServiceClient                projectServiceClient;
    private final ProjectTypeRegistry                 projectTypeRegistry;
    private final DtoUnmarshallerFactory              dtoUnmarshallerFactory;
    private final DtoFactory                          dtoFactory;
    private final VfsServiceClient                    vfsServiceClient;
    private final EventBus                            eventBus;
    private final CoreLocalizationConstant            localizationConstant;
    private final ImportProjectNotificationSubscriber importProjectNotificationSubscriber;
    private final String                              workspaceId;

    private ProjectConfigDto projectConfig;

    @Inject
    public ProjectImporter(ProjectServiceClient projectServiceClient,
                           ProjectTypeRegistry projectTypeRegistry,
                           DtoUnmarshallerFactory dtoUnmarshallerFactory,
                           DtoFactory dtoFactory,
                           VfsServiceClient vfsServiceClient,
                           EventBus eventBus,
                           CoreLocalizationConstant localizationConstant,
                           ImportProjectNotificationSubscriber importProjectNotificationSubscriber,
                           AppContext appContext) {
        this.projectServiceClient = projectServiceClient;
        this.projectTypeRegistry = projectTypeRegistry;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.dtoFactory = dtoFactory;
        this.vfsServiceClient = vfsServiceClient;
        this.eventBus = eventBus;
        this.localizationConstant = localizationConstant;
        this.importProjectNotificationSubscriber = importProjectNotificationSubscriber;

        this.workspaceId = appContext.getWorkspaceId();
    }

    public void checkFolderExistenceAndImport(final CompleteCallback callback, final ProjectConfigDto projectConfig) {
        this.projectConfig = projectConfig;
        // check on VFS because need to check whether the folder with the same name already exists in the root of workspace
        final String projectName = projectConfig.getName();
        vfsServiceClient.getItemByPath(workspaceId, projectName, new AsyncRequestCallback<Item>() {
            @Override
            protected void onSuccess(Item result) {
                callback.onFailure(new Exception(localizationConstant.createProjectFromTemplateProjectExists(projectName)));
            }

            @Override
            protected void onFailure(Throwable exception) {
                importProject(callback);
            }
        });
    }

    private void importProject(final CompleteCallback callback) {
        final String projectName = projectConfig.getName();
        importProjectNotificationSubscriber.subscribe(projectName);

        projectServiceClient.importProject(workspaceId, projectName, false, projectConfig.getSource(), new RequestCallback<Void>() {
            @Override
            protected void onSuccess(final Void result) {
                resolveProject(callback);
                importProjectNotificationSubscriber.onSuccess();
            }

            @Override
            protected void onFailure(Throwable exception) {
                importProjectNotificationSubscriber.onFailure(exception.getMessage());
                String errorMessage = getImportErrorMessage(exception);
                if (errorMessage.equals("Unable get private ssh key")) {
                    callback.onFailure(new Exception(localizationConstant.importProjectMessageUnableGetSshKey()));
                    return;
                }
                callback.onFailure(new Exception(errorMessage));
            }
        });
    }

    private void resolveProject(final CompleteCallback callback) {
        final String projectName = projectConfig.getName();
        Unmarshallable<List<SourceEstimation>> unmarshaller = dtoUnmarshallerFactory.newListUnmarshaller(SourceEstimation.class);
        projectServiceClient.resolveSources(workspaceId, projectName, new AsyncRequestCallback<List<SourceEstimation>>(unmarshaller) {

            Function<SourceEstimation, ProjectTypeDto> estimateToType = new Function<SourceEstimation, ProjectTypeDto>() {
                @Nullable
                @Override
                public ProjectTypeDto apply(@Nullable SourceEstimation input) {
                    if (input != null) {
                        return projectTypeRegistry.getProjectType(input.getType());
                    }

                    return null;
                }
            };

            Predicate<ProjectTypeDto> isPrimaryable = new Predicate<ProjectTypeDto>() {
                @Override
                public boolean apply(@Nullable ProjectTypeDto input) {
                    if (input != null) {
                        return input.isPrimaryable();
                    }

                    return false;
                }
            };

            @Override
            protected void onSuccess(List<SourceEstimation> result) {
                Iterable<ProjectTypeDto> types = filter(transform(result, estimateToType), isPrimaryable);

                if (size(types) == 1) {
                    ProjectTypeDto typeDto = getFirst(types, null);

                    if (typeDto != null) {
                        projectConfig.withType(typeDto.getId());
                    }
                }

                boolean configRequire = false;

                if (isNullOrEmpty(projectConfig.getType())) {
                    projectConfig.withType(Constants.BLANK_ID);
                    configRequire = true;
                }

                updateProject(callback, configRequire);
            }

            @Override
            protected void onFailure(Throwable exception) {
                importProjectNotificationSubscriber.onFailure(exception.getMessage());
                String errorMessage = getImportErrorMessage(exception);
                callback.onFailure(new Exception(errorMessage));
            }
        });
    }

    private void updateProject(final CompleteCallback callback, final boolean configRequire) {
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
                                                   if (!result.getProblems().isEmpty() || configRequire) {
                                                       eventBus.fireEvent(new ConfigureProjectEvent(result));
                                                   }
                                               }

                                               @Override
                                               protected void onFailure(Throwable exception) {
                                                   importProjectNotificationSubscriber.onFailure(exception.getMessage());
                                                   deleteProject(projectName);
                                                   String errorMessage = getImportErrorMessage(exception);
                                                   callback.onFailure(new Exception(errorMessage));
                                               }
                                           });
    }

    private String getImportErrorMessage(Throwable exception) {
        if (exception instanceof JobNotFoundException) {
            return "Project import failed";
        } else if (exception instanceof UnauthorizedException) {
            UnauthorizedException unauthorizedException = (UnauthorizedException)exception;
            ServiceError serverError = dtoFactory.createDtoFromJson(unauthorizedException.getResponse().getText(),
                                                                    ServiceError.class);
            return serverError.getMessage();
        } else {
            return dtoFactory.createDtoFromJson(exception.getMessage(), ServiceError.class).getMessage();
        }
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
