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

import org.eclipse.che.api.core.model.workspace.WorkspaceConfig;
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
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.core.Component;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.util.Config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Evgen Vidolob
 */
@Singleton
public class WorkspaceComponent implements Component {

    private final WorkspaceServiceClient workspaceServiceClient;
    private final AppContext             appContext;
    private final DtoFactory             dtoFactory;

    @Inject
    public WorkspaceComponent(WorkspaceServiceClient workspaceServiceClient, AppContext appContext, DtoFactory dtoFactory) {
        this.workspaceServiceClient = workspaceServiceClient;
        this.appContext = appContext;
        this.dtoFactory = dtoFactory;
    }

    @Override
    public void start(final Callback<Component, Exception> callback) {
        workspaceServiceClient.getWorkspaces(0, 0).then(new Operation<List<UsersWorkspaceDto>>() {
            @Override
            public void apply(List<UsersWorkspaceDto> arg) throws OperationException {
                if (!arg.isEmpty()) {
                    Config.setCurrentWorkspace(arg.get(0));
                    appContext.setWorkspace(arg.get(0));
                    callback.onSuccess(WorkspaceComponent.this);
                } else {
                    workspaceServiceClient.startTemporary(getWorkspaceConfig(), null).then(new Operation<UsersWorkspaceDto>() {
                        @Override
                        public void apply(UsersWorkspaceDto arg) throws OperationException {
                            Config.setCurrentWorkspace(arg);
                            appContext.setWorkspace(arg);
                            callback.onSuccess(WorkspaceComponent.this);
                        }
                    }).catchError(new Operation<PromiseError>() {
                        @Override
                        public void apply(PromiseError arg) throws OperationException {
                            callback.onFailure(new Exception(arg.getCause()));
                        }
                    });
                }
            }
        }).catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError arg) throws OperationException {
                callback.onFailure(new Exception(arg.getCause()));
            }
        });
    }

    private WorkspaceConfig getWorkspaceConfig() {
        final String recipeURL =
                "https://gist.githubusercontent.com/evoevodin/100608a19c255cec28df/raw/0c2fafa2e918abfc22eb6d694f068b6d3ff1d2d0/dockerfile-test";

        List<MachineConfigDto> machineConfigs = new ArrayList<>();
        machineConfigs.add(dtoFactory.createDto(MachineConfigDto.class)
                                        .withName("dev-machine")
                                        .withType("docker")
                                        .withSource(dtoFactory.createDto(MachineSourceDto.class)
                                                              .withType("recipe")
                                                              .withLocation(recipeURL))
                                        .withDev(true)
                                        .withMemorySize(512));

        List<CommandDto> commands = new ArrayList<>();
        commands.add(dtoFactory.createDto(CommandDto.class)
                               .withName("MCI")
                               .withCommandLine("mvn clean install"));

        Map<String, String> attrs = new HashMap<>();
        attrs.put("fake_attr", "attr_value");

        Map<String, EnvironmentDto> environments = new HashMap<>();
        environments.put("dev-env", dtoFactory.createDto(EnvironmentDto.class).withName("dev-env").withMachineConfigs(machineConfigs));

        return dtoFactory.createDto(WorkspaceConfigDto.class)
                         .withName("dev-cfg")
                         .withDefaultEnvName("dev-env")
                         .withEnvironments(environments)
                         .withCommands(commands)
                         .withAttributes(attrs);
    }
}
