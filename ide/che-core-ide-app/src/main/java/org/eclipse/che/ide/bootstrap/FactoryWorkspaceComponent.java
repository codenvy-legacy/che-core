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

import org.eclipse.che.api.factory.gwt.client.FactoryServiceClient;
import org.eclipse.che.api.factory.shared.dto.Factory;
import org.eclipse.che.api.machine.gwt.client.MachineManager;
import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.api.promises.client.js.Promises;
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
import org.eclipse.che.ide.ui.loaders.initializationLoader.InitialLoadingInfo;
import org.eclipse.che.ide.ui.loaders.initializationLoader.LoaderPresenter;
import org.eclipse.che.ide.util.Config;
import org.eclipse.che.ide.util.loging.Log;
import org.eclipse.che.ide.websocket.MessageBusProvider;
import org.eclipse.che.ide.workspace.BrowserQueryFieldRenderer;
import org.eclipse.che.ide.workspace.create.CreateWorkspacePresenter;
import org.eclipse.che.ide.workspace.start.StartWorkspacePresenter;

import java.util.List;

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
                                        });
    }

    @Override
    public void tryStartWorkspace() {
        final WorkspaceConfigDto workspaceConfigDto = factory.getWorkspace();

        if (workspaceConfigDto == null) {
            //TODO handle this situation
        }

        getWorkspaceToStart().then(startWorkspace()).catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError arg) throws OperationException {
                Log.error(getClass(), arg.getMessage());
            }
        });
    }

    /**
     * Gets {@link Promise} of workspace according to {@code factory} {@link org.eclipse.che.api.factory.shared.dto.Policies}.
     *
     * <p>Return policy for workspace:
     * <p><i>perClick</i> - every click from any user always creates a new workspace every time and if policy is not specified<br/>
     * it will be used by default.
     *
     * <p><i>perUser</i> - create one workspace for a user, a 2nd click from same user reloads the same workspace.
     *
     * <p><i>perAccount</i> - only create workspace for all users. A 2nd click from any user reloads the same workspace<br/>
     * Note that if location = owner, then only 1 workspace ever is created. If location = acceptor<br/>
     * it's one workspace for each unique user.
     */
    private Promise<UsersWorkspaceDto> getWorkspaceToStart() {
        final WorkspaceConfigDto workspaceConfigDto = factory.getWorkspace();

        switch (factory.getPolicies().getCreate()) {
            case "perUser":
                final String currentUserId = appContext.getCurrentUser().getProfile().getUserId();
                return getWorkspaceByConditionOrCreateNew(workspaceConfigDto, new Function<UsersWorkspaceDto, Boolean>() {
                    @Override
                    public Boolean apply(UsersWorkspaceDto workspaceDto) throws FunctionException {
                        return currentUserId.equals(workspaceDto.getOwner())
                               && workspaceDto.getName().equals(workspaceConfigDto.getName());

                    }
                });
            case "perAccount":
                return getWorkspaceByConditionOrCreateNew(workspaceConfigDto, new Function<UsersWorkspaceDto, Boolean>() {
                    @Override
                    public Boolean apply(UsersWorkspaceDto arg) throws FunctionException {
                        //TODO rework it when account will be ready
                        return workspaceConfigDto.getName().equals(arg.getName());
                    }
                });
            case "perClick":
            default:
                return workspaceServiceClient.create(workspaceConfigDto.withName("xXx"), null);
        }
    }

    /**
     * Gets the workspace by condition which is determined by given {@link Function}<br/>
     * if workspace found by condition then it will return {@link Promise} of it<br/>
     * else it wil produce {@link Promise} of new workspace.
     */
    private Promise<UsersWorkspaceDto> getWorkspaceByConditionOrCreateNew(final WorkspaceConfigDto workspaceConfigDto,
                                                                          final Function<UsersWorkspaceDto, Boolean> condition) {
        return workspaceServiceClient.getWorkspaces(0, 0)
                                     .thenPromise(new Function<List<UsersWorkspaceDto>, Promise<UsersWorkspaceDto>>() {
                                         @Override
                                         public Promise<UsersWorkspaceDto> apply(List<UsersWorkspaceDto> workspaces)
                                                 throws FunctionException {
                                             for (UsersWorkspaceDto existsWs : workspaces) {
                                                 if (condition.apply(existsWs)) {
                                                     return Promises.resolve(existsWs);
                                                 }
                                             }

                                             return workspaceServiceClient.create(workspaceConfigDto, null);
                                         }
                                     });
    }
}

