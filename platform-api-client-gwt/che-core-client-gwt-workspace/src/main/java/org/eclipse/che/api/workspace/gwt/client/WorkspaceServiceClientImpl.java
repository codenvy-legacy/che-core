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
package org.eclipse.che.api.workspace.gwt.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;

import org.eclipse.che.api.core.model.workspace.WorkspaceConfig;
import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.callback.AsyncPromiseHelper;
import org.eclipse.che.api.workspace.shared.dto.CommandDto;
import org.eclipse.che.api.workspace.shared.dto.EnvironmentDto;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.api.workspace.shared.dto.RuntimeWorkspaceDto;
import org.eclipse.che.api.workspace.shared.dto.UsersWorkspaceDto;
import org.eclipse.che.ide.rest.AsyncRequestFactory;
import org.eclipse.che.ide.rest.AsyncRequestLoader;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.RestContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static org.eclipse.che.api.promises.client.callback.PromiseHelper.newCallback;
import static org.eclipse.che.api.promises.client.callback.PromiseHelper.newPromise;
import static org.eclipse.che.ide.MimeType.APPLICATION_JSON;
import static org.eclipse.che.ide.rest.HTTPHeader.ACCEPT;
import static org.eclipse.che.ide.rest.HTTPHeader.CONTENT_TYPE;

/**
 * Implementation for {@link WorkspaceServiceClient}.
 *
 * @author Roman Nikitenko
 */
public class WorkspaceServiceClientImpl implements WorkspaceServiceClient {

    private final DtoUnmarshallerFactory dtoUnmarshallerFactory;
    private final AsyncRequestFactory asyncRequestFactory;
    private final AsyncRequestLoader  loader;
    private final String              baseHttpUrl;

    @Inject
    protected WorkspaceServiceClientImpl(@RestContext String restContext,
                                         DtoUnmarshallerFactory dtoUnmarshallerFactory,
                                         AsyncRequestFactory asyncRequestFactory,
                                         AsyncRequestLoader loader) {
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.asyncRequestFactory = asyncRequestFactory;
        this.loader = loader;
        this.baseHttpUrl = restContext + "/workspace";
    }

    @Override
    public Promise<UsersWorkspaceDto> create(UsersWorkspaceDto newWorkspace, String account) {
        return null;
    }

    @Override
    public Promise<UsersWorkspaceDto> getUsersWorkspace(final String wsId) {
        return newPromise(new AsyncPromiseHelper.RequestCall<UsersWorkspaceDto>() {
            @Override
            public void makeCall(AsyncCallback<UsersWorkspaceDto> callback) {
                getUsersWorkspace(wsId, callback);
            }
        });
    }

    private void getUsersWorkspace(@Nonnull String wsId, @Nonnull AsyncCallback<UsersWorkspaceDto> callback) {
        final String url = baseHttpUrl + '/' + wsId;
        asyncRequestFactory.createGetRequest(url)
                           .header(ACCEPT, APPLICATION_JSON)
                           .loader(loader, "Getting info about workspace...")
                           .send(newCallback(callback, dtoUnmarshallerFactory.newUnmarshaller(UsersWorkspaceDto.class)));
    }

    @Override
    public Promise<RuntimeWorkspaceDto> getRuntimeWorkspace(String wsId) {
        return null;
    }

    @Override
    public Promise<List<UsersWorkspaceDto>> getWorkspaces(Integer skip, Integer limit) {
        return newPromise(new AsyncPromiseHelper.RequestCall<List<UsersWorkspaceDto>>() {
            @Override
            public void makeCall(AsyncCallback<List<UsersWorkspaceDto>> callback) {
                getWorkspaces(callback);
            }
        }).then(new Function<List<UsersWorkspaceDto>, List<UsersWorkspaceDto>>() {
            @Override
            public List<UsersWorkspaceDto> apply(List<UsersWorkspaceDto> arg) throws FunctionException {
                final List<UsersWorkspaceDto> descriptors = new ArrayList<>();
                for (UsersWorkspaceDto descriptor : arg) {
                    descriptors.add(descriptor);
                }
                return descriptors;
            }
        });
    }

    private void getWorkspaces(@Nonnull AsyncCallback<List<UsersWorkspaceDto>> callback) {
        final String url = baseHttpUrl;
        asyncRequestFactory.createGetRequest(url)
                           .header(ACCEPT, APPLICATION_JSON)
                           .loader(loader, "Getting info about bound workspaces...")
                           .send(newCallback(callback, dtoUnmarshallerFactory.newListUnmarshaller(UsersWorkspaceDto.class)));
    }

    @Override
    public Promise<List<RuntimeWorkspaceDto>> getRuntimeWorkspaces(Integer skip, Integer limit) {
        return null;
    }

    @Override
    public Promise<UsersWorkspaceDto> update(String wsId, WorkspaceConfig newCfg) {
        return null;
    }

    @Override
    public Promise<Void> delete(String wsId) {
        return null;
    }

    @Override
    public Promise<UsersWorkspaceDto> startTemporary(final WorkspaceConfig cfg, final String accountId) {
        return newPromise(new AsyncPromiseHelper.RequestCall<UsersWorkspaceDto>() {
            @Override
            public void makeCall(AsyncCallback<UsersWorkspaceDto> callback) {
                startTemporary(cfg, accountId, callback);
            }
        });
    }

    private void startTemporary(@Nonnull WorkspaceConfig cfg, @Nonnull String accountId, @Nonnull AsyncCallback<UsersWorkspaceDto> callback) {
        asyncRequestFactory.createPostRequest(baseHttpUrl + "/runtime", cfg)
                           .header(ACCEPT, APPLICATION_JSON)
                           .header(CONTENT_TYPE, APPLICATION_JSON)
                           .loader(loader, "Creating machine from recipe...")
                           .send(newCallback(callback, dtoUnmarshallerFactory.newUnmarshaller(UsersWorkspaceDto.class)));
    }

    @Override
    public Promise<UsersWorkspaceDto> startById(final String id, final String envName) {
        return newPromise(new AsyncPromiseHelper.RequestCall<UsersWorkspaceDto>() {
            @Override
            public void makeCall(AsyncCallback<UsersWorkspaceDto> callback) {
                startById(id, envName, callback);
            }
        });
    }

    private void startById(@Nonnull String id,
                           @Nullable final String envName,
                           @Nonnull AsyncCallback<UsersWorkspaceDto> callback) {
        asyncRequestFactory.createPostRequest(baseHttpUrl + '/' + id + "/runtime?environment=" + envName, null)
                           .header(ACCEPT, APPLICATION_JSON)
                           .loader(loader, "Starting workspace...")
                           .send(newCallback(callback, dtoUnmarshallerFactory.newUnmarshaller(UsersWorkspaceDto.class)));
    }

    @Override
    public Promise<UsersWorkspaceDto> startByName(String name, String envName) {
        return null;
    }

    @Override
    public Promise<Void> stop(String wsId) {
        return null;
    }

    @Override
    public Promise<UsersWorkspaceDto> addCommand(String wsId, CommandDto newCommand) {
        return null;
    }

    @Override
    public Promise<UsersWorkspaceDto> updateCommand(String wsId, CommandDto commandUpdate) {
        return null;
    }

    @Override
    public Promise<UsersWorkspaceDto> deleteCommand(String wsId, String commandName) {
        return null;
    }

    @Override
    public Promise<UsersWorkspaceDto> addEnvironment(String wsId, EnvironmentDto newEnv) {
        return null;
    }

    @Override
    public Promise<UsersWorkspaceDto> updateEnvironment(String wsId, EnvironmentDto environmentUpdate) {
        return null;
    }

    @Override
    public Promise<UsersWorkspaceDto> addEnvironment(String wsId, String envName) {
        return null;
    }

    @Override
    public Promise<UsersWorkspaceDto> addProject(String wsId, ProjectConfigDto newProject) {
        return null;
    }

    @Override
    public Promise<UsersWorkspaceDto> updateProject(String wsId, ProjectConfigDto newEnv) {
        return null;
    }

    @Override
    public Promise<UsersWorkspaceDto> deleteProject(String wsId, String projectName) {
        return null;
    }
}
