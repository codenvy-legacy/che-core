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

package org.eclipse.che.ide.bootstrap;

import com.google.gwt.core.client.Callback;
import com.google.gwt.user.client.Window;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.core.model.workspace.WorkspaceStatus;
import org.eclipse.che.api.machine.gwt.client.OutputMessageUnmarshaller;
import org.eclipse.che.api.machine.gwt.client.events.DevMachineStateEvent;
import org.eclipse.che.api.machine.shared.dto.event.MachineStatusEvent;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.api.workspace.gwt.client.WorkspaceServiceClient;
import org.eclipse.che.api.workspace.shared.dto.EnvironmentDto;
import org.eclipse.che.api.workspace.shared.dto.MachineConfigDto;
import org.eclipse.che.api.workspace.shared.dto.UsersWorkspaceDto;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.core.Component;
import org.eclipse.che.ide.createworkspace.CreateWorkspacePresenter;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.ui.loaders.initializationLoader.LoaderPresenter;
import org.eclipse.che.ide.ui.loaders.initializationLoader.OperationInfo;
import org.eclipse.che.ide.ui.loaders.initializationLoader.OperationInfo.Status;
import org.eclipse.che.ide.util.Config;
import org.eclipse.che.ide.util.loging.Log;
import org.eclipse.che.ide.websocket.MessageBus;
import org.eclipse.che.ide.websocket.WebSocketException;
import org.eclipse.che.ide.websocket.rest.SubscriptionHandler;
import org.eclipse.che.ide.websocket.rest.Unmarshallable;

import java.util.List;

import static org.eclipse.che.api.core.model.workspace.WorkspaceStatus.STARTING;
import static org.eclipse.che.ide.api.notification.Notification.Status.FINISHED;
import static org.eclipse.che.ide.api.notification.Notification.Type.ERROR;

/**
 * @author Evgen Vidolob
 */
@Singleton
public class WorkspaceComponent implements Component {

    private final WorkspaceServiceClient   workspaceServiceClient;
    private final CoreLocalizationConstant locale;
    private final CreateWorkspacePresenter createWorkspacePresenter;
    private final DtoUnmarshallerFactory   dtoUnmarshallerFactory;
    private final MessageBus               messageBus;
    private final EventBus                 eventBus;
    private final LoaderPresenter          loader;
    private final AppContext               appContext;
    private final NotificationManager      notificationManager;
    private       Notification             notification;//TODO:

    private OperationInfo startMachineOperation;

    @Inject
    public WorkspaceComponent(WorkspaceServiceClient workspaceServiceClient,
                              CreateWorkspacePresenter createWorkspacePresenter,
                              CoreLocalizationConstant locale,
                              DtoUnmarshallerFactory dtoUnmarshallerFactory,
                              MessageBus messageBus,
                              EventBus eventBus,
                              LoaderPresenter loader,
                              AppContext appContext,
                              NotificationManager notificationManager) {
        this.workspaceServiceClient = workspaceServiceClient;
        this.createWorkspacePresenter = createWorkspacePresenter;
        this.locale = locale;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.messageBus = messageBus;
        this.eventBus = eventBus;
        this.loader = loader;
        this.appContext = appContext;
        this.notificationManager = notificationManager;

    }

    @Override
    public void start(final Callback<Component, Exception> callback) {
        final OperationInfo operationInfo = new OperationInfo(locale.gettingWorkspace(), Status.IN_PROGRESS, loader);

        workspaceServiceClient.getWorkspaces(0, 1).then(new Operation<List<UsersWorkspaceDto>>() {
            @Override
            public void apply(List<UsersWorkspaceDto> workspaces) throws OperationException {
                if (!workspaces.isEmpty()) {
                    operationInfo.setStatus(Status.FINISHED);
                    final UsersWorkspaceDto workspace = workspaces.get(0);//For now will work only in SDK bundle in case we have only one WS
                    Config.setCurrentWorkspace(workspace);
                    appContext.setWorkspace(workspace);
                    callback.onSuccess(WorkspaceComponent.this);

                    if (!WorkspaceStatus.RUNNING.equals(workspace.getStatus())) {
                        notification =
                                new Notification(locale.startingOperation("workspace"), Notification.Type.INFO, Notification.Status.PROGRESS);
                        notification.setImportant(true);
                        notificationManager.showNotification(notification);
                        final String defaultEnvName = workspace.getDefaultEnvName();
                        final EnvironmentDto environment = workspace.getEnvironments().get(defaultEnvName);
                        for (MachineConfigDto machineConfig : environment.getMachineConfigs()) {
                            if (machineConfig.isDev()) {
                                subscribeToOutput(machineConfig.getOutputChannel());
                                subscribeToMachineStatus(machineConfig.getStatusChannel());
                            }
                        }
                        startWorkspace(workspace.getId(), defaultEnvName, callback);
                    }
                } else {
                    createWorkspacePresenter.show(operationInfo, callback);
                }
            }
        }).catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError arg) throws OperationException {
                operationInfo.setStatus(Status.ERROR);
                callback.onFailure(new Exception(arg.getCause()));
            }
        });
    }

    public void startWorkspace(String id, String envName, final Callback<Component, Exception> callback) {
        final OperationInfo startWsOperation =
                new OperationInfo(locale.startingOperation("workspace"), Status.IN_PROGRESS, loader);
        loader.print(startWsOperation);
        workspaceServiceClient.startById(id, envName).then(new Operation<UsersWorkspaceDto>() {
            @Override
            public void apply(UsersWorkspaceDto arg) throws OperationException {
                startWsOperation.setStatus(Status.FINISHED);
                Config.setCurrentWorkspace(arg);
                appContext.setWorkspace(arg);
                callback.onSuccess(WorkspaceComponent.this);
                if (notification != null) {
                    notification.setStatus(FINISHED);
                    notification.setImportant(false);
                    notification.setMessage(locale.workspaceStarted(arg.getName()));
                }
            }
        }).catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError arg) throws OperationException {
                startWsOperation.setStatus(Status.ERROR);
                if (notification != null) {
                    notification.setStatus(FINISHED);
                    notification.setType(ERROR);
                    notification.setImportant(false);
                    notification.setMessage(locale.workspaceStartingFailed());
                }
                callback.onFailure(new Exception(arg.getCause()));
            }
        });
    }

    public void subscribeToOutput(String outputChanel) {
        try {
            messageBus.subscribe(outputChanel, new SubscriptionHandler<String>(new OutputMessageUnmarshaller()) {
                @Override
                protected void onMessageReceived(String result) {
                    //TODO first docker log appears when extensions logs are displaying, so need display it later(together other docker logs)
                    if (result.startsWith("[DOCKER] Step 0")) {
                        return;
                    }
                    loader.printToDetails(new OperationInfo(result));
                }

                @Override
                protected void onErrorReceived(Throwable exception) {
                    loader.printToDetails(new OperationInfo(exception.getMessage(), Status.ERROR));
                }
            });
        } catch (WebSocketException e) {
            Log.error(getClass(), e);
        }
    }

    public void subscribeToMachineStatus(String machineStatusChanel) {
        final Unmarshallable<MachineStatusEvent> unmarshaller = dtoUnmarshallerFactory.newWSUnmarshaller(MachineStatusEvent.class);
        try {
            messageBus.subscribe(machineStatusChanel, new SubscriptionHandler<MachineStatusEvent>(unmarshaller) {
                @Override
                protected void onMessageReceived(MachineStatusEvent event) {
                    onMachineStatusChanged(event);
                }

                @Override
                protected void onErrorReceived(Throwable exception) {
                    loader.printToDetails(new OperationInfo(exception.getMessage(), Status.ERROR));
                }
            });
        } catch (WebSocketException e) {
            Log.error(getClass(), e);
        }
    }

    private void onMachineStatusChanged(MachineStatusEvent event) {
        eventBus.fireEvent(new DevMachineStateEvent(event));
        switch (event.getEventType()) {
            case CREATING:
                startMachineOperation = new OperationInfo(locale.startingMachine(event.getMachineName()),
                                                          Status.IN_PROGRESS,
                                                          loader);
                loader.print(startMachineOperation);
                break;
            case RUNNING:
                startMachineOperation.setStatus(Status.FINISHED);
                break;
            case ERROR:
                startMachineOperation.setStatus(Status.ERROR);
                break;
            default:
        }
    }
}
