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
import com.google.inject.Provider;

import org.eclipse.che.api.machine.gwt.client.RecipeServiceClient;
import org.eclipse.che.api.machine.shared.dto.recipe.RecipeDescriptor;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.Promise;
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.che.ide.createworkspace.CreateWorkspacePresenter.MAX_COUNT;
import static org.eclipse.che.ide.createworkspace.CreateWorkspacePresenter.RECIPE_TYPE;
import static org.eclipse.che.ide.createworkspace.CreateWorkspacePresenter.SKIP_COUNT;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Dmitry Shnurenko
 */
@RunWith(MockitoJUnitRunner.class)
public class CreateWorkspacePresenterTest {

    //constructor mocks
    @Mock
    private CreateWorkSpaceView          view;
    @Mock
    private LoaderPresenter              loader;
    @Mock
    private DtoFactory                   dtoFactory;
    @Mock
    private WorkspaceServiceClient       workspaceClient;
    @Mock
    private CoreLocalizationConstant     locale;
    @Mock
    private Provider<WorkspaceComponent> wsComponentProvider;
    @Mock
    private RecipeServiceClient          recipeServiceClient;

    //additional mocks
    @Mock
    private OperationInfo                   operationInfo;
    @Mock
    private Callback<Component, Exception>  componentCallback;
    @Mock
    private HidePopupCallBack               popupCallBack;
    @Mock
    private Promise<List<RecipeDescriptor>> recipesPromise;
    @Mock
    private Promise<UsersWorkspaceDto>      userWsPromise;
    @Mock
    private RecipeDescriptor                recipeDescriptor;
    @Mock
    private WorkspaceComponent              workspaceComponent;

    //DTOs
    @Mock
    private MachineConfigDto   machineConfigDto;
    @Mock
    private MachineSourceDto   machineSourceDto;
    @Mock
    private EnvironmentDto     environmentDto;
    @Mock
    private CommandDto         commandDto;
    @Mock
    private WorkspaceConfigDto workspaceConfigDto;
    @Mock
    private UsersWorkspaceDto  usersWorkspaceDto;

    @Captor
    private ArgumentCaptor<Operation<List<RecipeDescriptor>>> recipeOperation;
    @Captor
    private ArgumentCaptor<Operation<UsersWorkspaceDto>>      workspaceOperation;
    @Captor
    private ArgumentCaptor<Operation<PromiseError>>           errorOperation;

    @InjectMocks
    private CreateWorkspacePresenter presenter;

    @Before
    public void setUp() {
        when(dtoFactory.createDto(MachineConfigDto.class)).thenReturn(machineConfigDto);
        when(machineConfigDto.withName(anyString())).thenReturn(machineConfigDto);
        when(machineConfigDto.withType(anyString())).thenReturn(machineConfigDto);
        when(machineConfigDto.withSource(machineSourceDto)).thenReturn(machineConfigDto);
        when(machineConfigDto.withDev(anyBoolean())).thenReturn(machineConfigDto);
        when(machineConfigDto.withMemorySize(anyInt())).thenReturn(machineConfigDto);

        when(dtoFactory.createDto(MachineSourceDto.class)).thenReturn(machineSourceDto);
        when(machineSourceDto.withType(anyString())).thenReturn(machineSourceDto);
        when(machineSourceDto.withLocation(anyString())).thenReturn(machineSourceDto);

        when(dtoFactory.createDto(EnvironmentDto.class)).thenReturn(environmentDto);
        when(environmentDto.withName(anyString())).thenReturn(environmentDto);
        when(environmentDto.withMachineConfigs(Matchers.<List<MachineConfigDto>>anyObject())).thenReturn(environmentDto);

        when(dtoFactory.createDto(CommandDto.class)).thenReturn(commandDto);
        when(commandDto.withName(anyString())).thenReturn(commandDto);
        when(commandDto.withCommandLine(anyString())).thenReturn(commandDto);

        when(dtoFactory.createDto(WorkspaceConfigDto.class)).thenReturn(workspaceConfigDto);
        when(workspaceConfigDto.withName(anyString())).thenReturn(workspaceConfigDto);
        when(workspaceConfigDto.withDefaultEnvName(anyString())).thenReturn(workspaceConfigDto);
        when(workspaceConfigDto.withEnvironments(Matchers.<Map<String, EnvironmentDto>>anyObject())).thenReturn(workspaceConfigDto);
        when(workspaceConfigDto.withCommands(Matchers.<List<CommandDto>>anyObject())).thenReturn(workspaceConfigDto);
        when(workspaceConfigDto.withAttributes(Matchers.<Map<String, String>>anyObject())).thenReturn(workspaceConfigDto);

        when(dtoFactory.createDto(UsersWorkspaceDto.class)).thenReturn(usersWorkspaceDto);
        when(usersWorkspaceDto.withName(anyString())).thenReturn(usersWorkspaceDto);
        when(usersWorkspaceDto.withAttributes(Matchers.<Map<String, String>>anyObject())).thenReturn(usersWorkspaceDto);
        when(usersWorkspaceDto.withCommands(Matchers.<List<CommandDto>>anyObject())).thenReturn(usersWorkspaceDto);
        when(usersWorkspaceDto.withEnvironments(Matchers.<Map<String, EnvironmentDto>>anyObject())).thenReturn(usersWorkspaceDto);
        when(usersWorkspaceDto.withDefaultEnvName(anyString())).thenReturn(usersWorkspaceDto);
        when(usersWorkspaceDto.withTemporary(anyBoolean())).thenReturn(usersWorkspaceDto);

        when(wsComponentProvider.get()).thenReturn(workspaceComponent);
    }

    @Test
    public void delegateShouldBeSet() {
        verify(view).setDelegate(presenter);
    }

    @Test
    public void dialogShouldBeShown() {
        presenter.show(operationInfo, componentCallback);

        verify(view).show();
    }

    @Test
    public void errorLabelShouldNotBeShownWhenWorkspaceNameIsCorrect() {
        presenter.onNameChanged("test");

        verify(view).setVisibleNameError(false);
    }

    @Test
    public void errorLabelShouldBeShownWhenWorkspaceNameIsNotCorrect() {
        presenter.onNameChanged("test*");

        verify(view).setVisibleNameError(true);
    }

    @Test
    public void errorLabelShouldNotBeShownWhenRecipeUrlIsCorrect() {
        when(view.getRecipeUrl()).thenReturn("http://localhost/correct/url");

        presenter.onRecipeUrlChanged();

        verify(view).setVisibleUrlError(false);
        verify(view).setEnableCreateButton(true);
    }

    @Test
    public void errorLabelShouldBeShownWhenRecipeUrlIsNotCorrect() {
        when(view.getRecipeUrl()).thenReturn("http://localh/correct/url");

        presenter.onRecipeUrlChanged();

        verify(view).setVisibleUrlError(true);
        verify(view).setEnableCreateButton(false);
    }

    @Test
    public void recipesShouldBeFoundAndShown() throws Exception {
        List<RecipeDescriptor> recipes = Arrays.asList(recipeDescriptor);

        callSearchRecipesApplyMethod(recipes);

        verify(popupCallBack, never()).hidePopup();
        verify(view).showRecipes(recipes);
        verify(view).setVisibleTagsError(false);
    }

    private void callSearchRecipesApplyMethod(List<RecipeDescriptor> recipes) throws Exception {
        List<String> tags = Arrays.asList("test1 test2");

        when(view.getTags()).thenReturn(tags);
        when(recipeServiceClient.searchRecipes(Matchers.<List<String>>anyObject(),
                                               anyString(),
                                               anyInt(),
                                               anyInt())).thenReturn(recipesPromise);

        presenter.onTagsChanged(popupCallBack);

        verify(view).getTags();
        verify(recipeServiceClient).searchRecipes(tags, RECIPE_TYPE, SKIP_COUNT, MAX_COUNT);
        verify(recipesPromise).then(recipeOperation.capture());

        recipeOperation.getValue().apply(recipes);
    }

    @Test
    public void errorLabelShouldBeShowWhenRecipesNotFound() throws Exception {
        callSearchRecipesApplyMethod(new ArrayList<>());

        verify(view).setVisibleTagsError(true);
        verify(popupCallBack).hidePopup();
        verify(view, never()).showRecipes(Matchers.<List<RecipeDescriptor>>anyObject());
    }

    @Test
    public void dialogShouldBeHiddenWhenUserClicksOnCreateButton() {
        clickOnCreateButton();

        verify(loader).show(operationInfo);
        verify(view).hide();
    }

    private void clickOnCreateButton() {
        when(workspaceClient.create(Matchers.<UsersWorkspaceDto>anyObject(), anyString())).thenReturn(userWsPromise);
        when(userWsPromise.then(Matchers.<Operation<UsersWorkspaceDto>>anyObject())).thenReturn(userWsPromise);
        when(userWsPromise.catchError(Matchers.<Operation<PromiseError>>anyObject())).thenReturn(userWsPromise);
        presenter.show(operationInfo, componentCallback);

        presenter.onCreateButtonClicked();
    }

    @Test
    public void workspaceConfigShouldBeGot() {
        when(view.getWorkspaceName()).thenReturn("name");
        when(view.getRecipeUrl()).thenReturn("test");

        clickOnCreateButton();

        verify(view).getWorkspaceName();
        verify(dtoFactory).createDto(MachineConfigDto.class);
        verify(machineConfigDto).withName("dev-machine");
        verify(machineConfigDto).withType("docker");
        verify(machineConfigDto).withSource(machineSourceDto);
        verify(machineConfigDto).withDev(true);
        verify(machineConfigDto).withMemorySize(2048);

        verify(dtoFactory).createDto(MachineSourceDto.class);
        verify(machineSourceDto).withType("recipe");
        verify(machineSourceDto).withLocation("test");

        verify(dtoFactory).createDto(EnvironmentDto.class);
        verify(environmentDto).withName("name");
        verify(environmentDto).withMachineConfigs(Matchers.<List<MachineConfigDto>>anyObject());

        verify(dtoFactory).createDto(CommandDto.class);
        verify(commandDto).withName("MCI");
        verify(commandDto).withCommandLine("mvn clean install");

        verify(dtoFactory).createDto(UsersWorkspaceDto.class);
        verify(usersWorkspaceDto).withName(anyString());
        verify(usersWorkspaceDto).withAttributes(Matchers.<Map<String, String>>anyObject());
        verify(usersWorkspaceDto).withCommands(Matchers.<List<CommandDto>>anyObject());
        verify(usersWorkspaceDto).withEnvironments(Matchers.<Map<String, EnvironmentDto>>anyObject());
        verify(usersWorkspaceDto).withDefaultEnvName(anyString());
        verify(usersWorkspaceDto).withTemporary(anyBoolean());
    }

    @Test
    public void workspaceShouldBeCreatedForDevMachine() throws Exception {
        when(machineConfigDto.isDev()).thenReturn(true);

        callApplyCreateWorkspaceMethod();

        verify(machineConfigDto).getOutputChannel();
        verify(machineConfigDto).getStatusChannel();

        verify(workspaceComponent).subscribeToOutput(anyString());
        verify(workspaceComponent).subscribeToMachineStatus(anyString());

        verify(workspaceComponent).startWorkspace(anyString(), anyString(), eq(componentCallback));
    }

    private void callApplyCreateWorkspaceMethod() throws Exception {
        Map<String, EnvironmentDto> environments = new HashMap<>();
        environments.put("name", environmentDto);

        when(usersWorkspaceDto.getDefaultEnvName()).thenReturn("name");
        when(usersWorkspaceDto.getEnvironments()).thenReturn(environments);

        when(environmentDto.getMachineConfigs()).thenReturn(Arrays.asList(machineConfigDto));

        clickOnCreateButton();

        verify(userWsPromise).then(workspaceOperation.capture());
        workspaceOperation.getValue().apply(usersWorkspaceDto);
    }

    @Test
    public void workspaceShouldBeCreatedForNotDevMachine() throws Exception {
        when(machineConfigDto.isDev()).thenReturn(false);

        callApplyCreateWorkspaceMethod();

        verify(machineConfigDto, never()).getOutputChannel();
        verify(machineConfigDto, never()).getStatusChannel();

        verify(workspaceComponent, never()).subscribeToOutput(anyString());
        verify(workspaceComponent, never()).subscribeToMachineStatus(anyString());

        verify(workspaceComponent).startWorkspace(anyString(), anyString(), eq(componentCallback));
    }

    @Test
    public void errorShouldBeCaughtWhenCreatesWorkSpace() throws Exception {
        final PromiseError promiseError = mock(PromiseError.class);

        clickOnCreateButton();

        verify(userWsPromise).catchError(errorOperation.capture());
        errorOperation.getValue().apply(promiseError);

        //noinspection ThrowableResultOfMethodCallIgnored
        verify(promiseError).getCause();
        verify(componentCallback).onFailure(Matchers.<Exception>anyObject());
    }
}