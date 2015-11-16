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
package org.eclipse.che.ide.workspace.create;

import com.google.gwt.core.client.Callback;
import com.google.gwt.regexp.shared.RegExp;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import org.eclipse.che.api.machine.gwt.client.RecipeServiceClient;
import org.eclipse.che.api.machine.shared.dto.LimitsDto;
import org.eclipse.che.api.machine.shared.dto.MachineConfigDto;
import org.eclipse.che.api.machine.shared.dto.MachineSourceDto;
import org.eclipse.che.api.machine.shared.dto.recipe.RecipeDescriptor;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.api.workspace.gwt.client.WorkspaceServiceClient;
import org.eclipse.che.api.workspace.shared.dto.EnvironmentDto;
import org.eclipse.che.api.workspace.shared.dto.UsersWorkspaceDto;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceConfigDto;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.bootstrap.DefaultWorkspaceComponent;
import org.eclipse.che.ide.core.Component;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.workspace.BrowserQueryFieldRenderer;
import org.eclipse.che.ide.workspace.create.CreateWorkspaceView.HidePopupCallBack;

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
public class CreateWorkspacePresenter implements CreateWorkspaceView.ActionDelegate {

    private static final RegExp FILE_NAME   = RegExp.compile("^[A-Za-z0-9_\\s-\\.]+$");
    private static final String URL_PATTERN =
            "(https?|ftp)://(www\\.)?(((([a-zA-Z0-9.-]+\\.){1,}[a-zA-Z]{2,4}|localhost))|((\\d{1,3}\\.){3}(\\d{1,3})))(:(\\d+))?(/" +
            "([a-zA-Z0-9-._~!$&'()*+,;=:@/]|%[0-9A-F]{2})*)?(\\?([a-zA-Z0-9-._~!$&'()*+,;=:/?@]|%[0-9A-F]{2})*)?(#([a-zA-Z0-9" +
            "._-]|%[0-9A-F]{2})*)?";
    private static final RegExp URL         = RegExp.compile(URL_PATTERN);

    static final String RECIPE_TYPE     = "docker";
    static final int    SKIP_COUNT      = 0;
    static final int    MAX_COUNT       = 100;
    static final int    MAX_NAME_LENGTH = 20;
    static final int    MIN_NAME_LENGTH = 3;

    private final CreateWorkspaceView                 view;
    private final DtoFactory                          dtoFactory;
    private final WorkspaceServiceClient              workspaceClient;
    private final CoreLocalizationConstant            locale;
    private final Provider<DefaultWorkspaceComponent> wsComponentProvider;
    private final RecipeServiceClient                 recipeService;
    private final BrowserQueryFieldRenderer           browserQueryFieldRenderer;

    private Callback<Component, Exception> callback;
    private List<RecipeDescriptor>         recipes;
    private List<String>                   workspacesNames;

    @Inject
    public CreateWorkspacePresenter(CreateWorkspaceView view,
                                    DtoFactory dtoFactory,
                                    WorkspaceServiceClient workspaceClient,
                                    CoreLocalizationConstant locale,
                                    Provider<DefaultWorkspaceComponent> wsComponentProvider,
                                    RecipeServiceClient recipeService,
                                    BrowserQueryFieldRenderer browserQueryFieldRenderer) {
        this.view = view;
        this.view.setDelegate(this);

        this.dtoFactory = dtoFactory;
        this.workspaceClient = workspaceClient;
        this.locale = locale;
        this.wsComponentProvider = wsComponentProvider;
        this.recipeService = recipeService;
        this.browserQueryFieldRenderer = browserQueryFieldRenderer;

        this.workspacesNames = new ArrayList<>();
    }

    /**
     * Shows special dialog window which allows set up workspace which will be created.
     *
     * @param callback
     *         callback which is necessary to notify that workspace component started or failed
     * @param workspaces
     *         list of existing workspaces
     */
    public void show(List<UsersWorkspaceDto> workspaces, final Callback<Component, Exception> callback) {
        this.callback = callback;

        workspacesNames.clear();

        for (UsersWorkspaceDto workspace : workspaces) {
            workspacesNames.add(workspace.getName());
        }

        Promise<List<RecipeDescriptor>> recipes = recipeService.getRecipes(SKIP_COUNT, MAX_COUNT);

        recipes.then(new Operation<List<RecipeDescriptor>>() {
            @Override
            public void apply(List<RecipeDescriptor> recipeDescriptors) throws OperationException {
                CreateWorkspacePresenter.this.recipes = recipeDescriptors;
            }
        });

        String workspaceName = browserQueryFieldRenderer.getWorkspaceName();

        view.setWorkspaceName(workspaceName);

        validateCreateWorkspaceForm();

        view.show();
    }

    private void validateCreateWorkspaceForm() {
        String workspaceName = view.getWorkspaceName();

        int nameLength = workspaceName.length();

        String errorDescription = "";

        boolean nameLengthIsInCorrect = nameLength < MIN_NAME_LENGTH || nameLength > MAX_NAME_LENGTH;

        if (nameLengthIsInCorrect) {
            errorDescription = locale.createWsNameLengthIsNotCorrect();
        }

        boolean nameIsInCorrect = !FILE_NAME.test(workspaceName);

        if (nameIsInCorrect) {
            errorDescription = locale.createWsNameIsNotCorrect();
        }

        boolean nameAlreadyExist = workspacesNames.contains(workspaceName);

        if (nameAlreadyExist) {
            errorDescription = locale.createWsNameAlreadyExist();
        }

        view.showValidationNameError(errorDescription);

        String recipeUrl = view.getRecipeUrl();

        boolean urlIsIncorrect = !URL.test(recipeUrl);

        view.setVisibleUrlError(urlIsIncorrect);

        view.setEnableCreateButton(!urlIsIncorrect && errorDescription.isEmpty());
    }

    /** {@inheritDoc} */
    @Override
    public void onNameChanged() {
        validateCreateWorkspaceForm();
    }

    /** {@inheritDoc} */
    @Override
    public void onRecipeUrlChanged() {
        validateCreateWorkspaceForm();
    }

    /** {@inheritDoc} */
    @Override
    public void onTagsChanged(final HidePopupCallBack callBack) {
        recipeService.searchRecipes(view.getTags(), RECIPE_TYPE, SKIP_COUNT, MAX_COUNT).then(new Operation<List<RecipeDescriptor>>() {
            @Override
            public void apply(List<RecipeDescriptor> recipes) throws OperationException {
                boolean isRecipesEmpty = recipes.isEmpty();

                if (isRecipesEmpty) {
                    callBack.hidePopup();
                } else {
                    view.showFoundByTagRecipes(recipes);
                }

                view.setVisibleTagsError(isRecipesEmpty);
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void onPredefinedRecipesClicked() {
        view.showPredefinedRecipes(recipes);
    }

    /** {@inheritDoc} */
    @Override
    public void onCreateButtonClicked() {
        view.hide();

        createWorkspace();
    }

    private void createWorkspace() {
        WorkspaceConfigDto workspaceConfig = getWorkspaceConfig();

        workspaceClient.create(workspaceConfig, null).then(new Operation<UsersWorkspaceDto>() {
            @Override
            public void apply(UsersWorkspaceDto workspace) throws OperationException {
                DefaultWorkspaceComponent component = wsComponentProvider.get();

                component.startWorkspaceById(workspace);
            }
        }).catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError arg) throws OperationException {
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
                                     .withLimits(dtoFactory.createDto(LimitsDto.class).withMemory(2048)));

        Map<String, EnvironmentDto> environments = new HashMap<>();
        environments.put(wsName, dtoFactory.createDto(EnvironmentDto.class)
                                           .withName(wsName)
                                           .withMachineConfigs(machineConfigs));

        return dtoFactory.createDto(WorkspaceConfigDto.class)
                         .withName(wsName)
                         .withDefaultEnvName(wsName)
                         .withEnvironments(environments);
    }
}