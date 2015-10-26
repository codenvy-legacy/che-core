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
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import org.eclipse.che.api.workspace.shared.dto.UsersWorkspaceDto;
import org.eclipse.che.ide.bootstrap.WorkspaceComponent;
import org.eclipse.che.ide.core.Component;
import org.eclipse.che.ide.ui.loaders.initializationLoader.LoaderPresenter;
import org.eclipse.che.ide.ui.loaders.initializationLoader.OperationInfo;
import org.eclipse.che.ide.workspace.BrowserQueryFieldRenderer;
import org.eclipse.che.ide.workspace.WorkspaceWidgetFactory;
import org.eclipse.che.ide.workspace.create.CreateWorkspacePresenter;
import org.eclipse.che.ide.workspace.start.workspacewidget.WorkspaceWidget;

import java.util.List;

import static org.eclipse.che.api.core.model.workspace.WorkspaceStatus.RUNNING;

/**
 * The class contains business logic which allows start existing workspace which was stopped before.
 *
 * @author Dmitry Shnurenko
 */
@Singleton
public class StartWorkspacePresenter implements StartWorkspaceView.ActionDelegate, WorkspaceWidget.ActionDelegate {

    private final StartWorkspaceView           view;
    private final Provider<WorkspaceComponent> wsComponentProvider;
    private final WorkspaceWidgetFactory       widgetFactory;
    private final LoaderPresenter              loader;
    private final CreateWorkspacePresenter     createWorkspacePresenter;
    private final BrowserQueryFieldRenderer    browserQueryFieldRenderer;

    private UsersWorkspaceDto              selectedWorkspace;
    private Callback<Component, Exception> callback;
    private OperationInfo                  operationInfo;
    private List<UsersWorkspaceDto>        workspaces;

    @Inject
    public StartWorkspacePresenter(StartWorkspaceView view,
                                   Provider<WorkspaceComponent> wsComponentProvider,
                                   WorkspaceWidgetFactory widgetFactory,
                                   LoaderPresenter loader,
                                   CreateWorkspacePresenter createWorkspacePresenter,
                                   BrowserQueryFieldRenderer browserQueryFieldRenderer) {
        this.view = view;
        this.view.setDelegate(this);

        this.wsComponentProvider = wsComponentProvider;
        this.widgetFactory = widgetFactory;
        this.loader = loader;
        this.createWorkspacePresenter = createWorkspacePresenter;
        this.browserQueryFieldRenderer = browserQueryFieldRenderer;
    }

    /**
     * Shows special dialog which contains workspaces which can be started at this time.
     *
     * @param operationInfo
     *         info which needs for displaying information about creating workspace
     * @param callback
     *         callback which is necessary to notify that workspace component started or failed
     * @param workspaces
     *         available workspaces which will be displayed
     */
    public void show(List<UsersWorkspaceDto> workspaces, Callback<Component, Exception> callback, OperationInfo operationInfo) {
        this.callback = callback;
        this.operationInfo = operationInfo;
        this.workspaces = workspaces;

        view.clearWorkspacesPanel();

        String workspaceName = browserQueryFieldRenderer.getWorkspaceName();

        createWsWidgets(workspaces);

        for (UsersWorkspaceDto workspace : workspaces) {
            if (workspaceName.equals(workspace.getName())) {
                selectedWorkspace = workspace;

                break;
            }
        }

        view.setWsName(workspaceName);

        view.show();
    }

    private void createWsWidgets(List<UsersWorkspaceDto> workspaces) {
        for (UsersWorkspaceDto workspace : workspaces) {
            WorkspaceWidget widget = widgetFactory.create(workspace);
            widget.setDelegate(this);

            view.addWorkspace(widget);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onWorkspaceSelected(UsersWorkspaceDto workspace) {
        selectedWorkspace = workspace;

        String wsName = workspace.getDefaultEnvName();

        view.setWsName(wsName);

        view.setEnableStartButton(!wsName.isEmpty());

        if (RUNNING.equals(workspace.getStatus())) {
            WorkspaceComponent workspaceComponent = wsComponentProvider.get();

            workspaceComponent.setCurrentWorkspace(workspace, operationInfo);

            workspaceComponent.startWorkspaceById(workspace);

            view.hide();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onCreateWorkspaceClicked() {
        view.hide();

        createWorkspacePresenter.show(workspaces, operationInfo, callback);
    }

    /** {@inheritDoc} */
    @Override
    public void onStartWorkspaceClicked() {
        loader.show(operationInfo);

        WorkspaceComponent workspaceComponent = wsComponentProvider.get();

        workspaceComponent.startWorkspaceById(selectedWorkspace);

        view.hide();
    }
}
