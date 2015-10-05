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
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.machine.gwt.client.OutputMessageUnmarshaller;
import org.eclipse.che.api.machine.gwt.client.events.DevMachineStateEvent;
import org.eclipse.che.api.machine.gwt.client.events.ExtServerStateEvent;
import org.eclipse.che.api.machine.gwt.client.events.ExtServerStateHandler;
import org.eclipse.che.api.machine.shared.dto.event.MachineStatusEvent;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.api.workspace.gwt.client.WorkspaceServiceClient;
import org.eclipse.che.api.workspace.shared.dto.MachineConfigDto;
import org.eclipse.che.api.workspace.shared.dto.UsersWorkspaceDto;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.core.Component;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.ui.loaders.initializationLoader.LoaderPresenter;
import org.eclipse.che.ide.ui.loaders.initializationLoader.OperationInfo;
import org.eclipse.che.ide.ui.loaders.initializationLoader.OperationInfo.Status;
import org.eclipse.che.ide.util.Config;
import org.eclipse.che.ide.util.loging.Log;
import org.eclipse.che.ide.websocket.MessageBus;
import org.eclipse.che.ide.websocket.MessageBusProvider;
import org.eclipse.che.ide.websocket.WebSocketException;
import org.eclipse.che.ide.websocket.events.ConnectionOpenedHandler;
import org.eclipse.che.ide.websocket.rest.SubscriptionHandler;
import org.eclipse.che.ide.websocket.rest.Unmarshallable;
import org.eclipse.che.ide.workspace.BrowserQueryFieldViewer;
import org.eclipse.che.ide.workspace.create.CreateWorkspacePresenter;
import org.eclipse.che.ide.workspace.start.StartWorkspaceEvent;
import org.eclipse.che.ide.workspace.start.StartWorkspacePresenter;

import java.util.List;

import static org.eclipse.che.api.core.model.workspace.WorkspaceStatus.RUNNING;

/**
 * @author Evgen Vidolob
 * @author Dmitry Shnurenko
 */
@Singleton
public class WorkspaceComponent implements Component, ExtServerStateHandler {

    private final WorkspaceServiceClient   workspaceServiceClient;
    private final CoreLocalizationConstant locale;
    private final CreateWorkspacePresenter createWorkspacePresenter;
    private final StartWorkspacePresenter  startWorkspacePresenter;
    private final DtoUnmarshallerFactory   dtoUnmarshallerFactory;
    private final EventBus                 eventBus;
    private final LoaderPresenter          loader;
    private final AppContext               appContext;
    private final NotificationManager      notificationManager;
    private final MessageBusProvider       messageBusProvider;
    private final BrowserQueryFieldViewer  browserQueryFieldViewer;

    private OperationInfo                  startMachineOperation;
    private Callback<Component, Exception> callback;
    private MessageBus                     messageBus;

    @Inject
    public WorkspaceComponent(WorkspaceServiceClient workspaceServiceClient,
                              CreateWorkspacePresenter createWorkspacePresenter,
                              StartWorkspacePresenter startWorkspacePresenter,
                              CoreLocalizationConstant locale,
                              DtoUnmarshallerFactory dtoUnmarshallerFactory,
                              EventBus eventBus,
                              LoaderPresenter loader,
                              AppContext appContext,
                              NotificationManager notificationManager,
                              MessageBusProvider messageBusProvider,
                              BrowserQueryFieldViewer browserQueryFieldViewer) {
        this.workspaceServiceClient = workspaceServiceClient;
        this.createWorkspacePresenter = createWorkspacePresenter;
        this.startWorkspacePresenter = startWorkspacePresenter;
        this.locale = locale;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.eventBus = eventBus;
        this.loader = loader;
        this.appContext = appContext;
        this.notificationManager = notificationManager;
        this.messageBusProvider = messageBusProvider;
        this.browserQueryFieldViewer = browserQueryFieldViewer;

        eventBus.addHandler(ExtServerStateEvent.TYPE, this);
    }

    /** {@inheritDoc} */
    @Override
    public void onExtServerStarted(ExtServerStateEvent event) {
        notificationManager.showInfo(locale.extServerStarted());
    }

    /** {@inheritDoc} */
    @Override
    public void onExtServerStopped(ExtServerStateEvent event) {
        notificationManager.showInfo(locale.extServerStopped());
    }

    /** {@inheritDoc} */
    @Override
    public void start(final Callback<Component, Exception> callback) {
        this.callback = callback;

        final OperationInfo operationInfo = new OperationInfo(locale.gettingWorkspace(), Status.IN_PROGRESS, loader);

        workspaceServiceClient.getWorkspaces(0, 10).then(new Operation<List<UsersWorkspaceDto>>() {
            @Override
            public void apply(List<UsersWorkspaceDto> workspaces) throws OperationException {
                operationInfo.setStatus(Status.FINISHED);

                String wsNameFromBrowser = browserQueryFieldViewer.getWorkspaceName();

                for (UsersWorkspaceDto workspace : workspaces) {
                    boolean isWorkspaceExist = wsNameFromBrowser.equals(workspace.getName());

                    if (isWorkspaceExist && RUNNING.equals(workspace.getStatus())) {
                        messageBus = messageBusProvider.createMessageBus(workspace.getId());

                        setCurrentWorkspace(operationInfo, workspace);

                        return;
                    }

                    if (isWorkspaceExist || wsNameFromBrowser.isEmpty()) {
                        startWorkspacePresenter.show(workspaces, callback, operationInfo);

                        return;
                    }
                }

                createWorkspacePresenter.show(operationInfo, callback);
            }
        }).catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError arg) throws OperationException {
                operationInfo.setStatus(Status.ERROR);
                loader.show(operationInfo);

                callback.onFailure(new Exception(arg.getCause()));
            }
        });
    }

    public void setCurrentWorkspace(OperationInfo operationInfo, UsersWorkspaceDto workspace) {
        operationInfo.setStatus(Status.FINISHED);
        Config.setCurrentWorkspace(workspace);
        appContext.setWorkspace(workspace);
        callback.onSuccess(WorkspaceComponent.this);

        browserQueryFieldViewer.setWorkspaceName(workspace.getName());
    }

    public void startWorkspace(String id, final String envName) {
        final OperationInfo startWsOperation = new OperationInfo(locale.startingOperation("workspace"), Status.IN_PROGRESS, loader);

        loader.print(startWsOperation);

        workspaceServiceClient.startById(id, envName).then(new Operation<UsersWorkspaceDto>() {
            @Override
            public void apply(UsersWorkspaceDto workspace) throws OperationException {
                messageBus = messageBusProvider.createMessageBus(workspace.getId());

                connect(workspace, startWsOperation);
            }
        }).catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError arg) throws OperationException {
                startWsOperation.setStatus(Status.ERROR);
                callback.onFailure(new Exception(arg.getCause()));
            }
        });
    }

    private void connect(final UsersWorkspaceDto workspace, final OperationInfo startWsOperation) {
        messageBus.addOnOpenHandler(new ConnectionOpenedHandler() {
            @Override
            public void onOpen() {
                setCurrentWorkspace(startWsOperation, workspace);

                notificationManager.showInfo(locale.startedWs(workspace.getDefaultEnvName()));

                eventBus.fireEvent(new StartWorkspaceEvent(workspace));

                List<MachineConfigDto> machineConfigs = workspace.getEnvironments().get(workspace.getDefaultEnvName()).getMachineConfigs();

                for (MachineConfigDto machineConfig : machineConfigs) {
                    if (machineConfig.isDev()) {
                        subscribeToOutput(machineConfig.getOutputChannel());
                        subscribeToMachineStatus(machineConfig.getStatusChannel());
                    }
                }
            }
        });
    }

    private void subscribeToOutput(String outputChanel) {
        try {
            messageBus.subscribe(outputChanel, new SubscriptionHandler<String>(new OutputMessageUnmarshaller()) {
                @Override
                protected void onMessageReceived(String result) {
                    //TODO first docker log appears when extensions logs are displaying, so need display it later(together other docker
                    //TODO  logs)
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

    private void subscribeToMachineStatus(String machineStatusChanel) {
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
                if (startMachineOperation != null) {
                    startMachineOperation.setStatus(Status.FINISHED);
                }
                break;
            case ERROR:
                if (startMachineOperation != null) {
                    startMachineOperation.setStatus(Status.ERROR);
                }
                break;
            default:
        }
    }
}
