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

import org.eclipse.che.api.machine.gwt.client.events.DevMachineStateEvent;
import org.eclipse.che.api.machine.gwt.client.OutputMessageUnmarshaller;
import org.eclipse.che.api.machine.shared.dto.event.MachineStatusEvent;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.api.workspace.gwt.client.WorkspaceServiceClient;
import org.eclipse.che.api.workspace.shared.dto.CommandDto;
import org.eclipse.che.api.workspace.shared.dto.EnvironmentDto;
import org.eclipse.che.api.workspace.shared.dto.MachineConfigDto;
import org.eclipse.che.api.workspace.shared.dto.MachineSourceDto;
import org.eclipse.che.api.workspace.shared.dto.UsersWorkspaceDto;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceConfigDto;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.core.Component;
import org.eclipse.che.ide.dto.DtoFactory;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Evgen Vidolob
 */
@Singleton
public class WorkspaceComponent implements Component {

    private static final String RECIPE_URL =
            "https://gist.githubusercontent.com/vparfonov/5c633534bfb0c127854f/raw/f176ee3428c2d39d08c7b4762aee6855dc5c8f75/jdk8_maven3_tomcat8";

    private final WorkspaceServiceClient   workspaceServiceClient;
    private final CoreLocalizationConstant localizedConstants;
    private       DtoUnmarshallerFactory   dtoUnmarshallerFactory;
    private       MessageBus               messageBus;
    private       EventBus                 eventBus;
    private final LoaderPresenter          loader;
    private final AppContext               appContext;
    private final DtoFactory               dtoFactory;
    private       OperationInfo            startMachineOperation;

    @Inject
    public WorkspaceComponent(WorkspaceServiceClient workspaceServiceClient,
                              CoreLocalizationConstant localizedConstants,
                              DtoUnmarshallerFactory dtoUnmarshallerFactory,
                              MessageBus messageBus,
                              EventBus eventBus,
                              LoaderPresenter loader,
                              AppContext appContext,
                              DtoFactory dtoFactory) {
        this.workspaceServiceClient = workspaceServiceClient;
        this.localizedConstants = localizedConstants;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.messageBus = messageBus;
        this.eventBus = eventBus;
        this.loader = loader;
        this.appContext = appContext;
        this.dtoFactory = dtoFactory;
    }

    @Override
    public void start(final Callback<Component, Exception> callback) {
        final OperationInfo getWsOperation = new OperationInfo(localizedConstants.gettingWorkspace(), Status.IN_PROGRESS, loader);
        loader.show(getWsOperation);
        workspaceServiceClient.getWorkspaces(0, 1).then(new Operation<List<UsersWorkspaceDto>>() {
            @Override
            public void apply(List<UsersWorkspaceDto> arg) throws OperationException {
                if (!arg.isEmpty()) {
                    getWsOperation.setStatus(Status.FINISHED);
                    Config.setCurrentWorkspace(arg.get(0));
                    appContext.setWorkspace(arg.get(0));
                    callback.onSuccess(WorkspaceComponent.this);
                } else {
                    createWorkspace(callback, getWsOperation);
                }
            }
        }).catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError arg) throws OperationException {
                getWsOperation.setStatus(Status.ERROR);
                callback.onFailure(new Exception(arg.getCause()));
            }
        });
    }

    private void createWorkspace(final Callback<Component, Exception> callback, final OperationInfo getWsOperation) {
        WorkspaceConfigDto workspaceConfig = getWorkspaceConfig();
        UsersWorkspaceDto usersWorkspaceDto = dtoFactory.createDto(UsersWorkspaceDto.class)
                                                        .withName(workspaceConfig.getName())
                                                        .withAttributes(workspaceConfig.getAttributes())
                                                        .withCommands(workspaceConfig.getCommands())
                                                        .withEnvironments(workspaceConfig.getEnvironments())
                                                        .withDefaultEnvName(workspaceConfig.getDefaultEnvName())
                                                        .withTemporary(true);
        final OperationInfo createWsOperation = new OperationInfo(localizedConstants.creatingWorkspace(), Status.IN_PROGRESS, loader);
        loader.print(createWsOperation);
        workspaceServiceClient.create(usersWorkspaceDto, null).then(new Operation<UsersWorkspaceDto>() {
            @Override
            public void apply(UsersWorkspaceDto arg) throws OperationException {
                getWsOperation.setStatus(Status.FINISHED);
                createWsOperation.setStatus(Status.FINISHED);

                String defaultEnvName = arg.getDefaultEnvName();
                List<MachineConfigDto> machineConfigs = arg.getEnvironments().get(defaultEnvName).getMachineConfigs();
                for (MachineConfigDto machineConfig : machineConfigs) {
                    if (machineConfig.isDev()) {
                        subscribeToOutput(machineConfig.getOutputChannel());
                        subscribeToMachineStatus(machineConfig.getStatusChannel());
                    }
                }
                startWorkspace(arg.getId(), defaultEnvName, callback);
            }
        }).catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError arg) throws OperationException {
                getWsOperation.setStatus(Status.ERROR);
                createWsOperation.setStatus(Status.ERROR);
                callback.onFailure(new Exception(arg.getCause()));
            }
        });
    }

    private void startWorkspace(String id, String envName, final Callback<Component, Exception> callback) {
        final OperationInfo startWsOperation =
                new OperationInfo(localizedConstants.startingOperation("workspace"), Status.IN_PROGRESS, loader);
        loader.print(startWsOperation);
        workspaceServiceClient.startById(id, envName).then(new Operation<UsersWorkspaceDto>() {
            @Override
            public void apply(UsersWorkspaceDto arg) throws OperationException {
                startWsOperation.setStatus(Status.FINISHED);
                Config.setCurrentWorkspace(arg);
                appContext.setWorkspace(arg);
                callback.onSuccess(WorkspaceComponent.this);
            }
        }).catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError arg) throws OperationException {
                startWsOperation.setStatus(Status.ERROR);
                callback.onFailure(new Exception(arg.getCause()));
            }
        });
    }

    private void subscribeToOutput(String outputChanel) {
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

    private void subscribeToMachineStatus(String machineStatusChanel) {
        final Unmarshallable<MachineStatusEvent> unmarshaller = dtoUnmarshallerFactory.newWSUnmarshaller(MachineStatusEvent.class);
        try {
            messageBus.subscribe(machineStatusChanel, new SubscriptionHandler<MachineStatusEvent>(unmarshaller) {
                @Override
                protected void onMessageReceived(MachineStatusEvent event) {
                    onMachineStatusChanged(event, this);
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

    private void onMachineStatusChanged(MachineStatusEvent event, SubscriptionHandler<MachineStatusEvent> handler) {
        eventBus.fireEvent(new DevMachineStateEvent(event));
        switch (event.getEventType()) {
            case CREATING:
                startMachineOperation = new OperationInfo(localizedConstants.startingMachine(event.getMachineName()), Status.IN_PROGRESS, loader);
                loader.print(startMachineOperation);
                break;
            case RUNNING:
                startMachineOperation.setStatus(Status.FINISHED);
                break;
            case ERROR:
                startMachineOperation.setStatus(Status.ERROR);
                break;
            default:
                break;
        }
    }

    private WorkspaceConfigDto getWorkspaceConfig() {
        List<MachineConfigDto> machineConfigs = new ArrayList<>();
        machineConfigs.add(dtoFactory.createDto(MachineConfigDto.class)
                                     .withName("dev-machine")
                                     .withType("docker")
                                     .withSource(dtoFactory.createDto(MachineSourceDto.class)
                                                           .withType("recipe")
                                                           .withLocation(RECIPE_URL))
                                     .withDev(true)
                                     .withMemorySize(512));

        Map<String, EnvironmentDto> environments = new HashMap<>();
        environments.put("dev-env", dtoFactory.createDto(EnvironmentDto.class)
                                              .withName("dev-env")
                                              .withMachineConfigs(machineConfigs));

        List<CommandDto> commands = new ArrayList<>();
        commands.add(dtoFactory.createDto(CommandDto.class)
                               .withName("MCI")
                               .withCommandLine("mvn clean install"));

        Map<String, String> attrs = new HashMap<>();
        attrs.put("fake_attr", "attr_value");

        return dtoFactory.createDto(WorkspaceConfigDto.class)
                         .withName("dev-cfg")
                         .withDefaultEnvName("dev-env")
                         .withEnvironments(environments)
                         .withCommands(commands)
                         .withAttributes(attrs);
    }
}
