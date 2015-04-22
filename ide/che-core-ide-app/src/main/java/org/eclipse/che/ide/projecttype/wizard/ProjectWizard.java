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
package org.eclipse.che.ide.projecttype.wizard;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.core.rest.shared.dto.ServiceError;
import org.eclipse.che.api.machine.gwt.client.MachineServiceClient;
import org.eclipse.che.api.machine.shared.dto.MachineDescriptor;
import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ImportProject;
import org.eclipse.che.api.project.shared.dto.ImportResponse;
import org.eclipse.che.api.project.shared.dto.NewProject;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.event.OpenProjectEvent;
import org.eclipse.che.ide.api.event.RefreshProjectTreeEvent;
import org.eclipse.che.ide.api.project.type.wizard.ProjectWizardMode;
import org.eclipse.che.ide.api.wizard.AbstractWizard;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.Unmarshallable;
import org.eclipse.che.ide.util.loging.Log;
import org.eclipse.che.ide.websocket.MessageBus;
import org.eclipse.che.ide.websocket.WebSocketException;
import org.eclipse.che.ide.websocket.rest.StringUnmarshallerWS;
import org.eclipse.che.ide.websocket.rest.SubscriptionHandler;

import javax.annotation.Nonnull;

import static org.eclipse.che.ide.api.project.type.wizard.ProjectWizardMode.CREATE;
import static org.eclipse.che.ide.api.project.type.wizard.ProjectWizardMode.CREATE_MODULE;
import static org.eclipse.che.ide.api.project.type.wizard.ProjectWizardMode.IMPORT;
import static org.eclipse.che.ide.api.project.type.wizard.ProjectWizardMode.UPDATE;
import static org.eclipse.che.ide.api.project.type.wizard.ProjectWizardRegistrar.PROJECT_NAME_KEY;
import static org.eclipse.che.ide.api.project.type.wizard.ProjectWizardRegistrar.PROJECT_PATH_KEY;
import static org.eclipse.che.ide.api.project.type.wizard.ProjectWizardRegistrar.WIZARD_MODE_KEY;

/**
 * Project wizard used for creating new a project or updating an existing one.
 *
 * @author Artem Zatsarynnyy
 */
public class ProjectWizard extends AbstractWizard<ImportProject> {

    private final ProjectWizardMode      mode;
    private final ProjectServiceClient   projectServiceClient;
    private final MachineServiceClient   machineServiceClient;
    private final DtoUnmarshallerFactory dtoUnmarshallerFactory;
    private final DtoFactory             dtoFactory;
    private final EventBus               eventBus;
    private final AppContext             appContext;
    private final String                 workspaceId;
    private final MessageBus messageBus;

    /**
     * Creates project wizard.
     *
     * @param dataObject
     *         wizard's data-object
     * @param mode
     *         mode of project wizard
     * @param projectPath
     *         path to the project to update if wizard created in {@link ProjectWizardMode#UPDATE} mode
     *         or path to the folder to convert it to module if wizard created in {@link ProjectWizardMode#CREATE_MODULE} mode
     * @param projectServiceClient
     *         GWT-client for Project service
     * @param machineServiceClient
     *         GWT-client for Machine service
     * @param dtoUnmarshallerFactory
     *         {@link org.eclipse.che.ide.rest.DtoUnmarshallerFactory} instance
     * @param eventBus
     *         {@link com.google.web.bindery.event.shared.EventBus} instance
     * @param appContext
     *         {@link org.eclipse.che.ide.api.app.AppContext} instance
     */
    @Inject
    public ProjectWizard(@Assisted ImportProject dataObject,
                         @Assisted ProjectWizardMode mode,
                         @Assisted String projectPath,
                         ProjectServiceClient projectServiceClient,
                         MachineServiceClient machineServiceClient,
                         DtoUnmarshallerFactory dtoUnmarshallerFactory,
                         DtoFactory dtoFactory,
                         EventBus eventBus,
                         AppContext appContext,
                         @Named("workspaceId") String workspaceId,
                         MessageBus messageBus) {
        super(dataObject);
        this.mode = mode;
        this.projectServiceClient = projectServiceClient;
        this.machineServiceClient = machineServiceClient;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.dtoFactory = dtoFactory;
        this.eventBus = eventBus;
        this.appContext = appContext;
        this.workspaceId = workspaceId;
        this.messageBus = messageBus;

        context.put(WIZARD_MODE_KEY, mode.toString());
        context.put(PROJECT_NAME_KEY, dataObject.getProject().getName());
        if (mode == UPDATE || mode == CREATE_MODULE) {
            context.put(PROJECT_PATH_KEY, projectPath);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void complete(@Nonnull final CompleteCallback callback) {
        if (mode == CREATE) {
            createProject(callback);
        } else if (mode == CREATE_MODULE) {
            createModule(callback);
        } else if (mode == UPDATE) {
            updateProject(callback);
        } else if (mode == IMPORT) {
            importProject(callback);
        }
    }

    private void createProject(final CompleteCallback callback) {
        final NewProject project = dataObject.getProject();
        final Unmarshallable<ProjectDescriptor> unmarshaller = dtoUnmarshallerFactory.newUnmarshaller(ProjectDescriptor.class);
        projectServiceClient.createProject(project.getName(), project, new AsyncRequestCallback<ProjectDescriptor>(unmarshaller) {
            @Override
            protected void onSuccess(ProjectDescriptor result) {
                eventBus.fireEvent(new OpenProjectEvent(result.getName()));

                startMachineAndBindProject(result.getPath());

                callback.onCompleted();
            }

            @Override
            protected void onFailure(Throwable exception) {
                final String message = dtoFactory.createDtoFromJson(exception.getMessage(), ServiceError.class).getMessage();
                callback.onFailure(new Exception(message));
            }
        });
    }

    private void startMachineAndBindProject(final String projectPath) {
        final String wsChannel = "machine_output";

        try {
            messageBus.subscribe(
                    wsChannel,
                    new SubscriptionHandler<String>(new StringUnmarshallerWS()) {
                        @Override
                        protected void onMessageReceived(String result) {
                            Log.info(ProjectWizard.class, result);
                        }

                        @Override
                        protected void onErrorReceived(Throwable exception) {
                            Log.error(ProjectWizard.class, exception);
                        }
                    });
        } catch (WebSocketException e) {
            Log.error(ProjectWizard.class, e);
        }

        machineServiceClient.createMachineFromRecipe(
                workspaceId,
                "docker",
                "Dockerfile",
                "FROM garagatyi/jdk7_mvn_tomcat\nCMD tail -f /dev/null",
                wsChannel,
                new AsyncRequestCallback<MachineDescriptor>(dtoUnmarshallerFactory.newUnmarshaller(MachineDescriptor.class)) {
                    @Override
                    protected void onSuccess(MachineDescriptor result) {
//                        machineServiceClient.bindProject(result.getId(), projectPath, new AsyncRequestCallback<Void>() {
//                            @Override
//                            protected void onSuccess(Void result) {
//                                Log.info(ProjectWizard.class, "Project " + projectPath + " bound");
//                            }
//
//                            @Override
//                            protected void onFailure(Throwable exception) {
//                                Log.error(ProjectWizard.class, exception);
//                            }
//                        });
                    }

                    @Override
                    protected void onFailure(Throwable exception) {
                        Log.error(ProjectWizard.class, exception);
                    }
                });
    }

    private void createModule(final CompleteCallback callback) {
        final String parentPath = appContext.getCurrentProject().getRootProject().getPath();
        final String modulePath = context.get(PROJECT_PATH_KEY);
        final NewProject project = dataObject.getProject();
        final Unmarshallable<ProjectDescriptor> unmarshaller = dtoUnmarshallerFactory.newUnmarshaller(ProjectDescriptor.class);
        projectServiceClient.createModule(
                parentPath, modulePath, project, new AsyncRequestCallback<ProjectDescriptor>(unmarshaller) {
                    @Override
                    protected void onSuccess(ProjectDescriptor result) {
                        eventBus.fireEvent(new RefreshProjectTreeEvent());
                        callback.onCompleted();
                    }

                    @Override
                    protected void onFailure(Throwable exception) {
                        callback.onFailure(exception);
                    }
                });
    }

    private void importProject(final CompleteCallback callback) {
        final NewProject project = dataObject.getProject();
        final Unmarshallable<ImportResponse> unmarshaller = dtoUnmarshallerFactory.newUnmarshaller(ImportResponse.class);
        projectServiceClient.importProject(
                project.getName(), true, dataObject, new AsyncRequestCallback<ImportResponse>(unmarshaller) {
                    @Override
                    protected void onSuccess(ImportResponse result) {
                        eventBus.fireEvent(new OpenProjectEvent(result.getProjectDescriptor().getName()));
                        callback.onCompleted();
                    }

                    @Override
                    protected void onFailure(Throwable exception) {
                        final String message = dtoFactory.createDtoFromJson(exception.getMessage(), ServiceError.class).getMessage();
                        callback.onFailure(new Exception(message));
                    }
                });
    }

    private void updateProject(final CompleteCallback callback) {
        final NewProject project = dataObject.getProject();
        final String currentName = context.get(PROJECT_NAME_KEY);
        if (currentName.equals(project.getName())) {
            doUpdateProject(callback);
        } else {
            renameProject(new AsyncCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    doUpdateProject(callback);
                }

                @Override
                public void onFailure(Throwable caught) {
                    final String message = dtoFactory.createDtoFromJson(caught.getMessage(), ServiceError.class).getMessage();
                    callback.onFailure(new Exception(message));
                }
            });
        }
    }

    private void doUpdateProject(final CompleteCallback callback) {
        final NewProject project = dataObject.getProject();
        final Unmarshallable<ProjectDescriptor> unmarshaller = dtoUnmarshallerFactory.newUnmarshaller(ProjectDescriptor.class);
        projectServiceClient.updateProject(project.getName(), project, new AsyncRequestCallback<ProjectDescriptor>(unmarshaller) {
            @Override
            protected void onSuccess(ProjectDescriptor result) {
                // just re-open project if it's already opened
                eventBus.fireEvent(new OpenProjectEvent(result.getName()));
                callback.onCompleted();
            }

            @Override
            protected void onFailure(Throwable exception) {
                final String message = dtoFactory.createDtoFromJson(exception.getMessage(), ServiceError.class).getMessage();
                callback.onFailure(new Exception(message));
            }
        });
    }

    private void renameProject(final AsyncCallback<Void> callback) {
        final String path = context.get(PROJECT_PATH_KEY);
        projectServiceClient.rename(path, dataObject.getProject().getName(), null, new AsyncRequestCallback<Void>() {
            @Override
            protected void onSuccess(Void result) {
                callback.onSuccess(result);
            }

            @Override
            protected void onFailure(Throwable exception) {
                callback.onFailure(exception);
            }
        });
    }
}
