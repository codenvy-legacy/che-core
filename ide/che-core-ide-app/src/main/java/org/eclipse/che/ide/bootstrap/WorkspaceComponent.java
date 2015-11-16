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
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.machine.gwt.client.OutputMessageUnmarshaller;
import org.eclipse.che.api.machine.gwt.client.events.DevMachineStateEvent;
import org.eclipse.che.api.machine.gwt.client.events.ExtServerStateEvent;
import org.eclipse.che.api.machine.gwt.client.events.ExtServerStateHandler;
import org.eclipse.che.api.machine.shared.dto.MachineStateDto;
import org.eclipse.che.api.machine.shared.dto.event.MachineStatusEvent;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.api.workspace.gwt.client.WorkspaceServiceClient;
import org.eclipse.che.api.workspace.shared.dto.UsersWorkspaceDto;
import org.eclipse.che.api.workspace.shared.dto.event.WorkspaceStatusEvent;
import org.eclipse.che.api.workspace.shared.dto.event.WorkspaceStatusEvent.EventType;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.preferences.PreferencesManager;
import org.eclipse.che.ide.core.Component;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.ui.dialogs.ConfirmCallback;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;
import org.eclipse.che.ide.ui.loaders.initializationLoader.LoaderPresenter;
import org.eclipse.che.ide.ui.loaders.initializationLoader.OperationInfo;
import org.eclipse.che.ide.ui.loaders.initializationLoader.OperationInfo.Status;
import org.eclipse.che.ide.util.Config;
import org.eclipse.che.ide.util.loging.Log;
import org.eclipse.che.ide.websocket.MessageBus;
import org.eclipse.che.ide.websocket.MessageBusProvider;
import org.eclipse.che.ide.websocket.WebSocketException;
import org.eclipse.che.ide.websocket.events.ConnectionOpenedHandler;
import org.eclipse.che.ide.websocket.events.MessageHandler;
import org.eclipse.che.ide.websocket.rest.SubscriptionHandler;
import org.eclipse.che.ide.websocket.rest.Unmarshallable;
import org.eclipse.che.ide.workspace.BrowserQueryFieldRenderer;
import org.eclipse.che.ide.workspace.create.CreateWorkspacePresenter;
import org.eclipse.che.ide.workspace.start.StartWorkspaceEvent;
import org.eclipse.che.ide.workspace.start.StartWorkspacePresenter;
import org.eclipse.che.ide.workspace.start.StopWorkspaceEvent;

import java.util.List;

import static org.eclipse.che.api.core.model.workspace.WorkspaceStatus.RUNNING;

/**
 * @author Evgen Vidolob
 * @author Dmitry Shnurenko
 */
@Singleton
public abstract class WorkspaceComponent implements Component, ExtServerStateHandler {

    protected final static int SKIP_COUNT = 0;
    protected final static int MAX_COUNT  = 10;

    protected final WorkspaceServiceClient   workspaceServiceClient;
    protected final CoreLocalizationConstant locale;
    protected final CreateWorkspacePresenter createWorkspacePresenter;
    protected final DtoUnmarshallerFactory   dtoUnmarshallerFactory;

    protected final AppContext                appContext;
    protected final BrowserQueryFieldRenderer browserQueryFieldRenderer;
    protected final DialogFactory             dialogFactory;
    protected final PreferencesManager        preferencesManager;
    protected final DtoFactory                dtoFactory;

    private final NotificationManager     notificationManager;
    private final MessageBusProvider      messageBusProvider;
    private final EventBus                eventBus;
    private final LoaderPresenter         loader;
    private final StartWorkspacePresenter startWorkspacePresenter;

    protected Callback<Component, Exception> callback;
    protected boolean                        needToReloadComponents;
    private   MessageBus                     messageBus;
    private   OperationInfo                  startMachineOperation;
    private   OperationInfo                  startWorkspaceOperation;

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
                              BrowserQueryFieldRenderer browserQueryFieldRenderer,
                              DialogFactory dialogFactory,
                              PreferencesManager preferencesManager,
                              DtoFactory dtoFactory) {
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
        this.browserQueryFieldRenderer = browserQueryFieldRenderer;
        this.dialogFactory = dialogFactory;
        this.preferencesManager = preferencesManager;
        this.dtoFactory = dtoFactory;

        this.needToReloadComponents = true;

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


    abstract void tryStartWorkspace();

    protected void showWorkspaceDialog() {
        workspaceServiceClient.getWorkspaces(SKIP_COUNT, MAX_COUNT).then(new Operation<List<UsersWorkspaceDto>>() {
            @Override
            public void apply(List<UsersWorkspaceDto> workspaces) throws OperationException {
                if (workspaces.isEmpty()) {
                    createWorkspacePresenter.show(workspaces, callback);

                    return;
                }

                startWorkspacePresenter.show(workspaces, callback);
            }
        });
    }

    /**
     * Sets workspace to app context as current.
     *
     * @param workspace
     *         workspace which will be current
     */
    public void setCurrentWorkspace(UsersWorkspaceDto workspace) {
        Config.setCurrentWorkspace(workspace);
        appContext.setWorkspace(workspace);

        if (needToReloadComponents) {
            callback.onSuccess(WorkspaceComponent.this);

            needToReloadComponents = false;
        }

        browserQueryFieldRenderer.setWorkspaceName(workspace.getName());
    }

    /**
     * Starts workspace by id when web socket connected.
     *
     * @param workspace
     *         workspace which will be started
     */
    public void startWorkspaceById(final UsersWorkspaceDto workspace) {
        startWorkspaceOperation = new OperationInfo(locale.startingOperation("workspace"), Status.IN_PROGRESS, loader);

        messageBus = messageBusProvider.createMessageBus(workspace.getId());

        messageBus.addOnOpenHandler(new ConnectionOpenedHandler() {
            @Override
            public void onOpen() {
                subscribeToWorkspaceStatusWebSocket(workspace);

                if (!RUNNING.equals(workspace.getStatus())) {
                    loader.show(startWorkspaceOperation);

                    workspaceServiceClient.startById(workspace.getId(),
                                                     workspace.getDefaultEnvName()).then(new Operation<UsersWorkspaceDto>() {
                        @Override
                        public void apply(UsersWorkspaceDto workspace) throws OperationException {
                            List<MachineStateDto> machineStates = workspace.getEnvironments()
                                                                           .get(workspace.getDefaultEnvName()).getMachineConfigs();

                            for (MachineStateDto machineState : machineStates) {
                                if (machineState.isDev()) {
                                    startMachineOperation = new OperationInfo(locale.startingMachine(machineState.getName()),
                                                                              Status.IN_PROGRESS,
                                                                              loader);
                                    loader.show(startMachineOperation);

                                    subscribeToOutput(machineState.getChannels().getOutput());
                                    subscribeToMachineStatus(machineState.getChannels().getStatus());
                                }
                            }
                        }
                    }).catchError(new Operation<PromiseError>() {
                        @Override
                        public void apply(PromiseError arg) throws OperationException {
                            startWorkspaceOperation.setStatus(Status.ERROR);
                            callback.onFailure(new Exception(arg.getCause()));
                        }
                    });
                }
            }
        });
    }

    private void subscribeToWorkspaceStatusWebSocket(final UsersWorkspaceDto workspace) {
        Unmarshallable<WorkspaceStatusEvent> unmarshaller = dtoUnmarshallerFactory.newWSUnmarshaller(WorkspaceStatusEvent.class);

        try {
            messageBus.subscribe("workspace:" + workspace.getId(), new SubscriptionHandler<WorkspaceStatusEvent>(unmarshaller) {
                @Override
                protected void onMessageReceived(WorkspaceStatusEvent statusEvent) {
                    EventType workspaceStatus = statusEvent.getEventType();

                    String workspaceName = workspace.getName();

                    if (EventType.RUNNING.equals(workspaceStatus)) {
                        setCurrentWorkspace(workspace);

                        notificationManager.showInfo(locale.startedWs(workspaceName));

                        eventBus.fireEvent(new StartWorkspaceEvent(workspace));
                    }

                    if (EventType.ERROR.equals(workspaceStatus)) {
                        notificationManager.showError(locale.workspaceStartFailed(workspaceName));

                        startWorkspaceOperation.setStatus(Status.ERROR);

                        showErrorDialog(workspaceName, statusEvent.getError());
                    }

                    if (EventType.STOPPED.equals(workspaceStatus)) {
                        unSubscribeWorkspace(statusEvent.getWorkspaceId());

                        eventBus.fireEvent(new StopWorkspaceEvent(workspace));

                        workspaceServiceClient.getWorkspaces(SKIP_COUNT, MAX_COUNT).then(new Operation<List<UsersWorkspaceDto>>() {
                            @Override
                            public void apply(List<UsersWorkspaceDto> workspaces) throws OperationException {
                                startWorkspacePresenter.show(workspaces, callback);
                            }
                        });
                    }
                }

                @Override
                protected void onErrorReceived(Throwable exception) {
                    notificationManager.showError(exception.getMessage());
                }
            });
        } catch (WebSocketException exception) {
            Log.error(getClass(), exception);
        }
    }

    private void showErrorDialog(final String wsName, final String errorMessage) {
        workspaceServiceClient.getWorkspaces(SKIP_COUNT, MAX_COUNT).then(new Operation<List<UsersWorkspaceDto>>() {
            @Override
            public void apply(final List<UsersWorkspaceDto> workspaces) throws OperationException {
                dialogFactory.createMessageDialog(locale.startWsErrorTitle(),
                                                  locale.startWsErrorContent(wsName, errorMessage),
                                                  new ConfirmCallback() {
                                                      @Override
                                                      public void accepted() {
                                                          startWorkspacePresenter.show(workspaces, callback);
                                                      }
                                                  }).show();

                loader.hide();
            }
        });

    }

    private void unSubscribeWorkspace(String workspaceId) {
        try {
            messageBus.unsubscribe("workspace:" + workspaceId, new MessageHandler() {
                @Override
                public void onMessage(String message) {
                    Log.info(getClass(), message);
                }
            });
        } catch (WebSocketException exception) {
            Log.error(getClass(), exception);
        }
    }

    private void subscribeToOutput(String outputChanel) {
        try {
            messageBus.subscribe(outputChanel, new SubscriptionHandler<String>(new OutputMessageUnmarshaller()) {
                @Override
                protected void onMessageReceived(String result) {
                    //TODO machine output turned off temporarily, because it freezes browser. Need find other solution.
//                    if (result.startsWith("[DOCKER] Step 0")) {
//                        return;
//                    }
//                    loader.printToDetails(new OperationInfo(result));
                }

                @Override
                protected void onErrorReceived(Throwable exception) {
                    loader.printToDetails(new OperationInfo(exception.getMessage(), Status.ERROR));
                }
            });
        } catch (WebSocketException exception) {
            Log.error(getClass(), exception);
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
        } catch (WebSocketException exception) {
            Log.error(getClass(), exception);
        }
    }

    private void onMachineStatusChanged(MachineStatusEvent event) {
        switch (event.getEventType()) {
            case RUNNING:
                if (startMachineOperation != null) {
                    startMachineOperation.setStatus(Status.FINISHED);
                }

                startWorkspaceOperation.setStatus(Status.FINISHED);

                eventBus.fireEvent(new DevMachineStateEvent(event));
                break;
            case ERROR:
                if (startMachineOperation != null) {
                    startMachineOperation.setStatus(Status.ERROR);
                }

                startWorkspaceOperation.setStatus(Status.ERROR);
                break;
            default:
        }
    }
}
