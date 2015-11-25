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
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.core.model.workspace.WorkspaceStatus;
import org.eclipse.che.api.machine.gwt.client.MachineManager;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.api.workspace.gwt.client.WorkspaceServiceClient;
import org.eclipse.che.api.workspace.shared.dto.UsersWorkspaceDto;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.preferences.PreferencesManager;
import org.eclipse.che.ide.core.Component;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.statepersistance.dto.AppState;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;
import org.eclipse.che.ide.ui.loaders.initializationLoader.InitialLoadingInfo;
import org.eclipse.che.ide.ui.loaders.initializationLoader.LoaderPresenter;
import org.eclipse.che.ide.util.loging.Log;
import org.eclipse.che.ide.websocket.MessageBusProvider;
import org.eclipse.che.ide.workspace.BrowserQueryFieldRenderer;
import org.eclipse.che.ide.workspace.create.CreateWorkspacePresenter;
import org.eclipse.che.ide.workspace.start.StartWorkspacePresenter;

import java.util.List;

import static org.eclipse.che.api.core.model.workspace.WorkspaceStatus.RUNNING;
import static org.eclipse.che.ide.statepersistance.AppStateManager.PREFERENCE_PROPERTY_NAME;

/**
 * Performs default start of IDE - creates new or starts latest workspace.
 * Used when no factory specified.
 *
 * @author Max Shaposhnik (mshaposhnik@codenvy.com)
 *
 */
@Singleton
public class DefaultWorkspaceComponent extends WorkspaceComponent implements Component {

    @Inject
    public DefaultWorkspaceComponent(WorkspaceServiceClient workspaceServiceClient,
                              CreateWorkspacePresenter createWorkspacePresenter,
                              StartWorkspacePresenter startWorkspacePresenter,
                              CoreLocalizationConstant locale,
                              DtoUnmarshallerFactory dtoUnmarshallerFactory,
                              EventBus eventBus,
                              LoaderPresenter loader,
                              AppContext appContext,
                              Provider<MachineManager> machineManagerProvider,
                              NotificationManager notificationManager,
                              MessageBusProvider messageBusProvider,
                              BrowserQueryFieldRenderer browserQueryFieldRenderer,
                              DialogFactory dialogFactory,
                              PreferencesManager preferencesManager,
                              DtoFactory dtoFactory,
                              InitialLoadingInfo initialLoadingInfo) {
        super(workspaceServiceClient,
              createWorkspacePresenter,
              startWorkspacePresenter,
              locale,
              dtoUnmarshallerFactory,
              eventBus,
              loader,
              appContext,
              machineManagerProvider,
              notificationManager,
              messageBusProvider,
              browserQueryFieldRenderer,
              dialogFactory,
              preferencesManager,
              dtoFactory,
              initialLoadingInfo);
    }

    /** {@inheritDoc} */
    @Override
    public void start(final Callback<Component, Exception> callback) {
        this.callback = callback;

        workspaceServiceClient.getWorkspaces(SKIP_COUNT, MAX_COUNT).then(new Operation<List<UsersWorkspaceDto>>() {
            @Override
            public void apply(List<UsersWorkspaceDto> workspaces) throws OperationException {
                String wsNameFromBrowser = browserQueryFieldRenderer.getWorkspaceName();

                for (UsersWorkspaceDto workspace : workspaces) {
                    boolean isWorkspaceExist = wsNameFromBrowser.equals(workspace.getName());

                    if (wsNameFromBrowser.isEmpty()) {
                        tryStartWorkspace();

                        return;
                    }

                    if (isWorkspaceExist && RUNNING.equals(workspace.getStatus())) {
                        setCurrentWorkspace(workspace);

                        startWorkspaceById(workspace);

                        return;
                    }

                    if (isWorkspaceExist) {
                        startWorkspaceById(workspace);

                        return;
                    }
                }

                createWorkspacePresenter.show(workspaces, callback);
            }
        }).catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError error) throws OperationException {
                needToReloadComponents = true;

                dialogFactory.createMessageDialog(locale.getWsErrorDialogTitle(),
                                                  locale.getWsErrorDialogContent(error.getMessage()),
                                                  null).show();

                callback.onFailure(new Exception(error.getCause()));
            }
        });
    }

    @Override
    public void tryStartWorkspace() {
        String json = preferencesManager.getValue(PREFERENCE_PROPERTY_NAME);

        AppState appState = null;

        try {
            appState = dtoFactory.createDtoFromJson(json, AppState.class);
        } catch (Exception exception) {
            Log.error(getClass(), "Can't create object using json: " + exception);
        }

        if (appState != null) {
            String recentWorkspaceId = appState.getRecentWorkspaceId();

            if (recentWorkspaceId != null) {
                workspaceServiceClient.getWorkspaceById(recentWorkspaceId)
                                      .then(startWorkspace())
                                      .catchError(new Operation<PromiseError>() {
                                          @Override
                                          public void apply(PromiseError promiseError) throws OperationException {
                                              showWorkspaceDialog();
                                          }
                                      });
                return;
            }
        }

        showWorkspaceDialog();
    }
}
