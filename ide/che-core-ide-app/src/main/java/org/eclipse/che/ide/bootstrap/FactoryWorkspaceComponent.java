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

import org.eclipse.che.api.core.model.workspace.WorkspaceStatus;
import org.eclipse.che.api.factory.gwt.client.FactoryServiceClient;
import org.eclipse.che.api.factory.shared.dto.Factory;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.api.workspace.gwt.client.WorkspaceServiceClient;
import org.eclipse.che.api.workspace.shared.dto.UsersWorkspaceDto;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceConfigDto;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.preferences.PreferencesManager;
import org.eclipse.che.ide.core.Component;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;
import org.eclipse.che.ide.ui.loaders.initializationLoader.LoaderPresenter;
import org.eclipse.che.ide.util.Config;
import org.eclipse.che.ide.util.loging.Log;
import org.eclipse.che.ide.websocket.MessageBusProvider;
import org.eclipse.che.ide.workspace.BrowserQueryFieldRenderer;
import org.eclipse.che.ide.workspace.create.CreateWorkspacePresenter;
import org.eclipse.che.ide.workspace.start.StartWorkspacePresenter;

import com.google.gwt.core.client.Callback;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import static org.eclipse.che.api.core.model.workspace.WorkspaceStatus.RUNNING;

/**
 * Retrieves specified factory, and creates and/or starts workspace configured in it.
 *
 * @author Max Shaposhnik
 */
@Singleton
public class FactoryWorkspaceComponent extends WorkspaceComponent implements Component {

    private final FactoryServiceClient factoryServiceClient;
    private       Factory              factory;

    @Inject
    public FactoryWorkspaceComponent(WorkspaceServiceClient workspaceServiceClient,
                                     FactoryServiceClient factoryServiceClient,
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
        super(workspaceServiceClient,
              createWorkspacePresenter,
              startWorkspacePresenter,
              locale,
              dtoUnmarshallerFactory,
              eventBus,
              loader,
              appContext,
              notificationManager,
              messageBusProvider,
              browserQueryFieldRenderer,
              dialogFactory,
              preferencesManager,
              dtoFactory);
        this.factoryServiceClient = factoryServiceClient;
    }

    @Override
    public void start(final Callback<Component, Exception> callback) {
        this.callback = callback;
        String factoryParams = Config.getStartupParam("factory");
        factoryServiceClient.getFactory(factoryParams,
                                        new AsyncRequestCallback<Factory>(dtoUnmarshallerFactory.newUnmarshaller(Factory.class)) {
                                            @Override
                                            protected void onSuccess(Factory result) {
                                                factory = result;
                                                appContext.setFactory(result);
                                                tryStartWorkspace();
                                            }

                                            @Override
                                            protected void onFailure(Throwable error) {
                                                Log.error(FactoryWorkspaceComponent.class, "Unable to load Factory", error);
                                                callback.onFailure(new Exception(error.getCause()));
                                      }
                                  }
                                 );
       
    }

    @Override
    void tryStartWorkspace() {

        WorkspaceConfigDto workspaceConfigDto = factory.getWorkspace();
        if (workspaceConfigDto != null) {

            Operation<UsersWorkspaceDto> workspaceOperation = new Operation<UsersWorkspaceDto>() {
                @Override
                public void apply(UsersWorkspaceDto workspace) throws OperationException {
                    WorkspaceStatus wsFromReferenceStatus = workspace.getStatus();

                    if (RUNNING.equals(wsFromReferenceStatus)) {
                        setCurrentWorkspace(workspace);
                    }

                    startWorkspaceById(workspace);
                }
            };

            Operation<PromiseError> errorOperation = new Operation<PromiseError>() {
                @Override
                public void apply(PromiseError promiseError) throws OperationException {
                    //showWorkspaceDialog();
                    callback.onFailure(new Exception(promiseError.getMessage()));
                }
            };

            workspaceServiceClient.create(workspaceConfigDto, null)
                                  .then(workspaceOperation)
                                  .catchError(errorOperation);
        }
    }

}

