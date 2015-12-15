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
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.core.rest.shared.dto.ServiceError;
import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.Constants;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.ide.api.event.ModuleCreatedEvent;
import org.eclipse.che.ide.api.event.project.CreateProjectEvent;
import org.eclipse.che.ide.api.event.project.OpenProjectEvent;
import org.eclipse.che.ide.api.event.project.ProjectUpdatedEvent;
import org.eclipse.che.ide.api.project.node.HasStorablePath;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.api.project.type.wizard.ProjectWizardMode;
import org.eclipse.che.ide.api.selection.Selection;
import org.eclipse.che.ide.api.selection.SelectionAgent;
import org.eclipse.che.ide.api.wizard.AbstractWizard;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.Unmarshallable;
import org.eclipse.che.ide.ui.dialogs.CancelCallback;
import org.eclipse.che.ide.ui.dialogs.ConfirmCallback;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;
import org.eclipse.che.ide.websocket.rest.RequestCallback;

import javax.validation.constraints.NotNull;

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
 * @author Artem Zatsarynnyi
 */
public class ProjectWizard extends AbstractWizard<ProjectConfigDto> {

    private final ProjectWizardMode      mode;
    private final ProjectServiceClient   projectServiceClient;
    private final DtoUnmarshallerFactory dtoUnmarshallerFactory;
    private final DtoFactory             dtoFactory;
    private final DialogFactory          dialogFactory;
    private final EventBus               eventBus;
    private final SelectionAgent         selectionAgent;
    private final UpdateProjectCallBack  updateAfterImportCallback;
    private final UpdateProjectCallBack  projectChangedCallBack;

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
     * @param dtoUnmarshallerFactory
     *         {@link org.eclipse.che.ide.rest.DtoUnmarshallerFactory} instance
     * @param dialogFactory
     *         {@link org.eclipse.che.ide.ui.dialogs.DialogFactory} instance
     * @param eventBus
     *         {@link com.google.web.bindery.event.shared.EventBus} instance
     */
    @Inject
    public ProjectWizard(@Assisted ProjectConfigDto dataObject,
                         @Assisted ProjectWizardMode mode,
                         @Assisted String projectPath,
                         ProjectServiceClient projectServiceClient,
                         DtoUnmarshallerFactory dtoUnmarshallerFactory,
                         DtoFactory dtoFactory,
                         DialogFactory dialogFactory,
                         final EventBus eventBus,
                         SelectionAgent selectionAgent) {
        super(dataObject);
        this.mode = mode;
        this.projectServiceClient = projectServiceClient;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.dtoFactory = dtoFactory;
        this.dialogFactory = dialogFactory;
        this.eventBus = eventBus;
        this.selectionAgent = selectionAgent;

        context.put(WIZARD_MODE_KEY, mode.toString());
        context.put(PROJECT_NAME_KEY, dataObject.getName());
        if (mode == UPDATE || mode == CREATE_MODULE) {
            context.put(PROJECT_PATH_KEY, projectPath);
        }

        updateAfterImportCallback = new UpdateProjectCallBack() {
            @Override
            public void onProjectUpdated(ProjectConfigDto projectConfig) {
                eventBus.fireEvent(new CreateProjectEvent(projectConfig));
            }
        };

        projectChangedCallBack = new UpdateProjectCallBack() {
            @Override
            public void onProjectUpdated(ProjectConfigDto projectConfig) {
                eventBus.fireEvent(new ProjectUpdatedEvent(context.get(PROJECT_PATH_KEY), projectConfig));
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public void complete(@NotNull final CompleteCallback callback) {
        if (mode == CREATE) {
            createProject(callback);
        } else if (mode == CREATE_MODULE) {
            createModule(callback);
        } else if (mode == UPDATE) {
            updateProject(new UpdateCallback(callback), projectChangedCallBack);
        } else if (mode == IMPORT) {
            importProject(callback);
        }
    }

    private void doSaveAsBlank(final CompleteCallback callback) {
        dataObject.setType(Constants.BLANK_ID);
        final Unmarshallable<ProjectConfigDto> unmarshaller = dtoUnmarshallerFactory.newUnmarshaller(ProjectConfigDto.class);
        projectServiceClient.updateProject(dataObject.getName(), dataObject, new AsyncRequestCallback<ProjectConfigDto>(unmarshaller) {
            @Override
            protected void onSuccess(ProjectConfigDto result) {
                // just re-open project if it's already opened
                ProjectWizard.this.eventBus.fireEvent(new OpenProjectEvent(result));
                callback.onCompleted();
            }

            @Override
            protected void onFailure(Throwable exception) {
                final String message =
                        ProjectWizard.this.dtoFactory.createDtoFromJson(exception.getMessage(), ServiceError.class).getMessage();
                callback.onFailure(new Exception(message));
            }
        });
    }

    private void createProject(final CompleteCallback callback) {
        final Unmarshallable<ProjectConfigDto> unmarshaller = dtoUnmarshallerFactory.newUnmarshaller(ProjectConfigDto.class);
        projectServiceClient.createProject(dataObject.getName(), dataObject, new AsyncRequestCallback<ProjectConfigDto>(unmarshaller) {
            @Override
            protected void onSuccess(ProjectConfigDto result) {
                eventBus.fireEvent(new CreateProjectEvent(result));

                callback.onCompleted();
            }

            @Override
            protected void onFailure(Throwable exception) {
                final String message = dtoFactory.createDtoFromJson(exception.getMessage(), ServiceError.class).getMessage();
                callback.onFailure(new Exception(message));
            }
        });
    }

    private void createModule(final CompleteCallback callback) {
        final String pathToSelectedNodeParent = getPathToSelectedNodeParent();

        final Unmarshallable<ProjectConfigDto> unmarshaller = dtoUnmarshallerFactory.newUnmarshaller(ProjectConfigDto.class);
        projectServiceClient.createModule(pathToSelectedNodeParent, dataObject, new AsyncRequestCallback<ProjectConfigDto>(unmarshaller) {
            @Override
            protected void onSuccess(ProjectConfigDto result) {
                eventBus.fireEvent(new ModuleCreatedEvent(result));
                callback.onCompleted();
            }

            @Override
            protected void onFailure(Throwable exception) {
                callback.onFailure(exception);
            }
        });
    }

    private String getPathToSelectedNodeParent() {
        Selection<?> selection = selectionAgent.getSelection();

        if (selection.isMultiSelection() || selection.isEmpty()) {
            return "";
        }

        Object selectedElement = selection.getHeadElement();

        if (selectedElement instanceof Node) {
            Node element = (Node)selectedElement;

            Node parent = element.getParent();

            if (parent instanceof HasStorablePath) {
                return ((HasStorablePath)parent).getStorablePath();
            }
        }

        return "";
    }

    private void importProject(final CompleteCallback callback) {
        projectServiceClient.importProject(dataObject.getName(), false, dataObject.getSource(), new RequestCallback<Void>() {
            @Override
            protected void onSuccess(Void result) {
                updateAfterImport(callback, updateAfterImportCallback);
            }

            @Override
            protected void onFailure(Throwable exception) {
                final String message = dtoFactory.createDtoFromJson(exception.getMessage(), ServiceError.class).getMessage();
                callback.onFailure(new Exception(message));
            }
        });
    }

    private void updateAfterImport(final CompleteCallback callback, final UpdateProjectCallBack updateProjectCallBack) {
        doUpdateProject(callback, updateProjectCallBack);
    }

    private void updateProject(final CompleteCallback callback, final UpdateProjectCallBack updateProjectCallBack) {
        final String currentName = context.get(PROJECT_NAME_KEY);
        if (currentName.equals(dataObject.getName())) {
            doUpdateProject(callback, updateProjectCallBack);
        } else {
            renameProject(new AsyncCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    doUpdateProject(callback, updateProjectCallBack);
                }

                @Override
                public void onFailure(Throwable caught) {
                    final String message = dtoFactory.createDtoFromJson(caught.getMessage(), ServiceError.class).getMessage();
                    callback.onFailure(new Exception(message));
                }
            });
        }
    }

    private void doUpdateProject(final CompleteCallback callback, final UpdateProjectCallBack updateProjectCallBack) {
        //final NewProject project = dataObject.getProjectConfig();
        final Unmarshallable<ProjectConfigDto> unmarshaller = dtoUnmarshallerFactory.newUnmarshaller(ProjectConfigDto.class);
        projectServiceClient.updateProject(dataObject.getName(), dataObject, new AsyncRequestCallback<ProjectConfigDto>(unmarshaller) {
            @Override
            protected void onSuccess(ProjectConfigDto result) {
                updateProjectCallBack.onProjectUpdated(result);

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
        projectServiceClient.rename(path, dataObject.getName(), null, new AsyncRequestCallback<Void>() {
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

    public class UpdateCallback implements CompleteCallback {
        private final CompleteCallback callback;

        public UpdateCallback(CompleteCallback callback) {
            this.callback = callback;
        }

        @Override
        public void onCompleted() {
            callback.onCompleted();
        }

        @Override
        public void onFailure(Throwable e) {
            dialogFactory.createConfirmDialog("Project Configuration Fail",
                                              "Configure project type as BLANK? You can re-configure it later",

                                              new ConfirmCallback() {
                                                  @Override
                                                  public void accepted() {
                                                      doSaveAsBlank(callback);
                                                  }
                                              },
                                              new CancelCallback() {
                                                  @Override
                                                  public void cancelled() {
                                                      callback.onCompleted();
                                                  }
                                              }).show();
        }
    }

    private interface UpdateProjectCallBack {
        /**
         * The method is called when project was changed or after project import at once.
         *
         * @param projectConfig
         *         configuration of project
         */
        void onProjectUpdated(ProjectConfigDto projectConfig);
    }
}
