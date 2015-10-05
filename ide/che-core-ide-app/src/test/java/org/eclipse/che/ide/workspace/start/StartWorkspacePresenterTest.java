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
package org.eclipse.che.ide.workspace.start;

import com.google.gwt.core.client.Callback;
import com.google.inject.Provider;

import org.eclipse.che.api.core.model.workspace.WorkspaceStatus;
import org.eclipse.che.api.workspace.shared.dto.UsersWorkspaceDto;
import org.eclipse.che.ide.bootstrap.WorkspaceComponent;
import org.eclipse.che.ide.core.Component;
import org.eclipse.che.ide.ui.loaders.initializationLoader.LoaderPresenter;
import org.eclipse.che.ide.ui.loaders.initializationLoader.OperationInfo;
import org.eclipse.che.ide.workspace.BrowserQueryFieldViewer;
import org.eclipse.che.ide.workspace.WorkspaceWidgetFactory;
import org.eclipse.che.ide.workspace.create.CreateWorkspacePresenter;
import org.eclipse.che.ide.workspace.start.workspacewidget.WorkspaceWidget;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Dmitry Shnurenko
 */
@RunWith(MockitoJUnitRunner.class)
public class StartWorkspacePresenterTest {

    //constructor mocks
    @Mock
    private StartWorkspaceView           view;
    @Mock
    private Provider<WorkspaceComponent> wsComponentProvider;
    @Mock
    private WorkspaceWidgetFactory       widgetFactory;
    @Mock
    private CreateWorkspacePresenter     createWorkspacePresenter;
    @Mock
    private LoaderPresenter              loaderPresenter;
    @Mock
    private BrowserQueryFieldViewer      browserQueryFieldViewer;

    //additional mocks
    @Mock
    private UsersWorkspaceDto              workspaceDto;
    @Mock
    private WorkspaceWidget                widget;
    @Mock
    private WorkspaceComponent             workspaceComponent;
    @Mock
    private Callback<Component, Exception> callback;
    @Mock
    private OperationInfo                  operationInfo;

    @InjectMocks
    private StartWorkspacePresenter presenter;

    @Test
    public void delegateShouldBeSet() {
        verify(view).setDelegate(presenter);
    }

    @Test
    public void dialogStartWorkspaceShouldBeShown() {
        when(browserQueryFieldViewer.getWorkspaceName()).thenReturn("test");
        when(widgetFactory.create(workspaceDto)).thenReturn(widget);

        presenter.show(Arrays.asList(workspaceDto), callback, operationInfo);

        verify(browserQueryFieldViewer).getWorkspaceName();
        verify(widgetFactory).create(workspaceDto);
        verify(widget).setDelegate(presenter);
        verify(view).addWorkspace(widget);
        verify(view).setWsName(anyString());

        verify(view).show();
    }

    @Test
    public void workspaceWithExistingNameShouldBeSelected() {
        when(browserQueryFieldViewer.getWorkspaceName()).thenReturn("test");
        when(wsComponentProvider.get()).thenReturn(workspaceComponent);
        when(widgetFactory.create(workspaceDto)).thenReturn(widget);
        when(workspaceDto.getName()).thenReturn("test");

        presenter.show(Arrays.asList(workspaceDto), callback, operationInfo);

        presenter.onStartWorkspaceClicked();

        verify(workspaceDto).getId();
        verify(workspaceDto).getDefaultEnvName();
    }

    @Test
    public void onCreateWorkspaceButtonShouldBeClicked() {
        when(browserQueryFieldViewer.getWorkspaceName()).thenReturn("test");
        when(widgetFactory.create(workspaceDto)).thenReturn(widget);
        presenter.show(Arrays.asList(workspaceDto), callback, operationInfo);

        presenter.onCreateWorkspaceClicked();

        verify(view).hide();
        verify(createWorkspacePresenter).show(operationInfo, callback);
    }

    @Test
    public void workspaceWidgetShouldBeSelected() {
        when(workspaceDto.getDefaultEnvName()).thenReturn("text");

        presenter.onWorkspaceSelected(workspaceDto);

        verify(workspaceDto).getDefaultEnvName();
        verify(view).setWsName("text");
        verify(view).setEnableStartButton(true);

        verify(view, never()).hide();
    }

    @Test
    public void workspaceShouldBeStartedWhenRunningWsWasSelected() {
        when(workspaceDto.getStatus()).thenReturn(WorkspaceStatus.RUNNING);
        when(workspaceDto.getDefaultEnvName()).thenReturn("test");
        when(wsComponentProvider.get()).thenReturn(workspaceComponent);

        presenter.onWorkspaceSelected(workspaceDto);

        verify(wsComponentProvider).get();
        verify(workspaceComponent).setCurrentWorkspace(Matchers.<OperationInfo>anyObject(), eq(workspaceDto));
        verify(view).hide();
    }

    @Test
    public void selectedWorkspaceShouldBeStarted() {
        when(widgetFactory.create(workspaceDto)).thenReturn(widget);
        when(workspaceDto.getDefaultEnvName()).thenReturn("text");
        when(browserQueryFieldViewer.getWorkspaceName()).thenReturn("test");
        when(wsComponentProvider.get()).thenReturn(workspaceComponent);

        presenter.show(Arrays.asList(workspaceDto), callback, operationInfo);
        presenter.onWorkspaceSelected(workspaceDto);
        reset(workspaceDto);

        presenter.onStartWorkspaceClicked();

        verify(loaderPresenter).show(operationInfo);

        verify(wsComponentProvider).get();

        verify(workspaceDto).getId();
        verify(workspaceDto).getDefaultEnvName();

        verify(workspaceComponent).startWorkspace(anyString(), anyString());

        verify(view).hide();
    }
}