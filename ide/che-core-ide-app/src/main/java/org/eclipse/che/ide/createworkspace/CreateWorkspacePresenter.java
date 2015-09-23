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
package org.eclipse.che.ide.createworkspace;

import com.google.gwt.core.client.Callback;
import com.google.gwt.regexp.shared.RegExp;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import org.eclipse.che.api.machine.gwt.client.RecipeServiceClient;
import org.eclipse.che.api.machine.shared.dto.recipe.RecipeDescriptor;
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
import org.eclipse.che.ide.bootstrap.WorkspaceComponent;
import org.eclipse.che.ide.core.Component;
import org.eclipse.che.ide.createworkspace.CreateWorkSpaceView.HidePopupCallBack;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ui.loaders.initializationLoader.LoaderPresenter;
import org.eclipse.che.ide.ui.loaders.initializationLoader.OperationInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The class contains business logic which allow to create user workspace if it doesn't exist.
 *
 * @author Dmitry Shnurenko
 */
@Singleton
public class CreateWorkspacePresenter implements CreateWorkSpaceView.ActionDelegate {

    private static final RegExp FILE_NAME   = RegExp.compile("^[A-Za-z0-9_\\s-\\.]+$");
    private static final String URL_PATTERN =
            "(https?|ftp)://(www\\.)?(((([a-zA-Z0-9.-]+\\.){1,}[a-zA-Z]{2,4}|localhost))|((\\d{1,3}\\.){3}(\\d{1,3})))(:(\\d+))?(/" +
            "([a-zA-Z0-9-._~!$&'()*+,;=:@/]|%[0-9A-F]{2})*)?(\\?([a-zA-Z0-9-._~!$&'()*+,;=:/?@]|%[0-9A-F]{2})*)?(#([a-zA-Z0-9" +
            "._-]|%[0-9A-F]{2})*)?";
    private static final RegExp URL         = RegExp.compile(URL_PATTERN);

    static final String RECIPE_TYPE = "docker";
    static final int    SKIP_COUNT  = 0;
    static final int    MAX_COUNT   = 100;

    private final CreateWorkSpaceView          view;
    private final LoaderPresenter              loader;
    private final DtoFactory                   dtoFactory;
    private final WorkspaceServiceClient       workspaceClient;
    private final CoreLocalizationConstant     locale;
    private final Provider<WorkspaceComponent> wsComponentProvider;
    private final RecipeServiceClient          recipeServiceClient;

    private OperationInfo                  operationInfo;
    private Callback<Component, Exception> callback;

    @Inject
    public CreateWorkspacePresenter(CreateWorkSpaceView view,
                                    LoaderPresenter loader,
                                    DtoFactory dtoFactory,
                                    WorkspaceServiceClient workspaceClient,
                                    CoreLocalizationConstant locale,
                                    Provider<WorkspaceComponent> wsComponentProvider,
                                    RecipeServiceClient recipeServiceClient) {
        this.view = view;
        this.view.setDelegate(this);

        this.loader = loader;
        this.dtoFactory = dtoFactory;
        this.workspaceClient = workspaceClient;
        this.locale = locale;
        this.wsComponentProvider = wsComponentProvider;
        this.recipeServiceClient = recipeServiceClient;
    }

    /**
     * Shows special dialog window which allows set up workspace which will be created.
     *
     * @param operationInfo
     *         info which needs for dispaying information about creating workspace
     */
    public void show(OperationInfo operationInfo, Callback<Component, Exception> callback) {
        this.operationInfo = operationInfo;
        this.callback = callback;

        view.show();
    }

    /** {@inheritDoc} */
    @Override
    public void onNameChanged(String name) {
        view.setVisibleNameError(!FILE_NAME.test(name));
    }

    /** {@inheritDoc} */
    @Override
    public void onRecipeUrlChanged() {
        String recipeURL = view.getRecipeUrl();
        boolean urlValid = URL.test(recipeURL);

        view.setVisibleUrlError(!urlValid);

        view.setEnableCreateButton(urlValid);
    }

    /** {@inheritDoc} */
    @Override
    public void onTagsChanged(final HidePopupCallBack callBack) {
        recipeServiceClient.searchRecipes(view.getTags(), RECIPE_TYPE, SKIP_COUNT, MAX_COUNT).then(new Operation<List<RecipeDescriptor>>() {
            @Override
            public void apply(List<RecipeDescriptor> recipes) throws OperationException {
                boolean isRecipesEmpty = recipes.isEmpty();

                if (isRecipesEmpty) {
                    callBack.hidePopup();
                } else {
                    view.showRecipes(recipes);
                }

                view.setVisibleTagsError(isRecipesEmpty);
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void onCreateButtonClicked() {
        loader.show(operationInfo);

        view.hide();

        createWorkspace(operationInfo);
    }

    private void createWorkspace(final OperationInfo getWsOperation) {
        WorkspaceConfigDto workspaceConfig = getWorkspaceConfig();

        UsersWorkspaceDto usersWorkspaceDto = dtoFactory.createDto(UsersWorkspaceDto.class)
                                                        .withName(workspaceConfig.getName())
                                                        .withAttributes(workspaceConfig.getAttributes())
                                                        .withCommands(workspaceConfig.getCommands())
                                                        .withEnvironments(workspaceConfig.getEnvironments())
                                                        .withDefaultEnvName(workspaceConfig.getDefaultEnvName())
                                                        .withTemporary(true);

        final OperationInfo createWsOperation = new OperationInfo(locale.creatingWorkspace(), OperationInfo.Status.IN_PROGRESS, loader);

        loader.print(createWsOperation);

        workspaceClient.create(usersWorkspaceDto, null).then(new Operation<UsersWorkspaceDto>() {
            @Override
            public void apply(UsersWorkspaceDto workspace) throws OperationException {
                getWsOperation.setStatus(OperationInfo.Status.FINISHED);
                createWsOperation.setStatus(OperationInfo.Status.FINISHED);

                String defaultEnvName = workspace.getDefaultEnvName();

                List<MachineConfigDto> machineConfigs = workspace.getEnvironments().get(defaultEnvName).getMachineConfigs();

                WorkspaceComponent component = wsComponentProvider.get();

                for (MachineConfigDto machineConfig : machineConfigs) {
                    if (machineConfig.isDev()) {
                        component.subscribeToOutput(machineConfig.getOutputChannel());
                        component.subscribeToMachineStatus(machineConfig.getStatusChannel());
                    }
                }

                component.startWorkspace(workspace.getId(), defaultEnvName, callback);
            }
        }).catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError arg) throws OperationException {
                getWsOperation.setStatus(OperationInfo.Status.ERROR);
                createWsOperation.setStatus(OperationInfo.Status.ERROR);
                callback.onFailure(new Exception(arg.getCause()));
            }
        });
    }

    private WorkspaceConfigDto getWorkspaceConfig() {
        String wsName = view.getWorkspaceName();

        List<MachineConfigDto> machineConfigs = new ArrayList<>();
        machineConfigs.add(dtoFactory.createDto(MachineConfigDto.class)
                                     .withName("dev-machine")
                                     .withType("docker")
                                     .withSource(dtoFactory.createDto(MachineSourceDto.class)
                                                           .withType("recipe")
                                                           .withLocation(view.getRecipeUrl()))
                                     .withDev(true)
                                     .withMemorySize(2048));

        Map<String, EnvironmentDto> environments = new HashMap<>();
        environments.put(wsName, dtoFactory.createDto(EnvironmentDto.class)
                                           .withName(wsName)
                                           .withMachineConfigs(machineConfigs));

        List<CommandDto> commands = new ArrayList<>();
        commands.add(dtoFactory.createDto(CommandDto.class)
                               .withName("MCI")
                               .withCommandLine("mvn clean install"));

        Map<String, String> attrs = new HashMap<>();
        attrs.put("fake_attr", "attr_value");

        return dtoFactory.createDto(WorkspaceConfigDto.class)
                         .withName("dev-cfg")
                         .withDefaultEnvName(wsName)
                         .withEnvironments(environments)
                         .withCommands(commands)
                         .withAttributes(attrs);
    }
}