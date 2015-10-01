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

import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.callback.AsyncPromiseHelper.RequestCall;
import org.eclipse.che.api.workspace.shared.dto.CommandDto;
import org.eclipse.che.api.workspace.shared.dto.EnvironmentDto;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.api.workspace.shared.dto.RuntimeWorkspaceDto;
import org.eclipse.che.api.workspace.shared.dto.UsersWorkspaceDto;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceConfigDto;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.ide.rest.AsyncRequestFactory;
import org.eclipse.che.ide.rest.AsyncRequestLoader;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.RestContext;

import javax.validation.constraints.NotNull;
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
 * @author Artem Zatsarynnyy
 */
public class WorkspaceServiceClientImpl implements WorkspaceServiceClient {

    private final DtoUnmarshallerFactory dtoUnmarshallerFactory;
    private final AsyncRequestFactory    asyncRequestFactory;
    private final AsyncRequestLoader     loader;
    private final String                 baseHttpUrl;

    @Inject
    private WorkspaceServiceClientImpl(@RestContext String restContext,
                                       DtoUnmarshallerFactory dtoUnmarshallerFactory,
                                       AsyncRequestFactory asyncRequestFactory,
                                       AsyncRequestLoader loader) {
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.asyncRequestFactory = asyncRequestFactory;
        this.loader = loader;
        this.baseHttpUrl = restContext + "/workspace";
    }

    @Override
    public Promise<UsersWorkspaceDto> create(final UsersWorkspaceDto newWorkspace, final String accountId) {
        return newPromise(new RequestCall<UsersWorkspaceDto>() {
            @Override
            public void makeCall(AsyncCallback<UsersWorkspaceDto> callback) {
                create(newWorkspace, accountId, callback);
            }
        });
    }

    private void create(@NotNull UsersWorkspaceDto newWorkspace,
                        String accountId,
                        @NotNull AsyncCallback<UsersWorkspaceDto> callback) {
        String url = baseHttpUrl + "/config?account=" + accountId;
        asyncRequestFactory.createPostRequest(url, newWorkspace)
                           .header(ACCEPT, APPLICATION_JSON)
                           .header(CONTENT_TYPE, APPLICATION_JSON)
                           .loader(loader, "Creating workspace...")
                           .send(newCallback(callback, dtoUnmarshallerFactory.newUnmarshaller(UsersWorkspaceDto.class)));
    }

    @Override
    public Promise<UsersWorkspaceDto> getUsersWorkspace(final String wsId) {
        return newPromise(new RequestCall<UsersWorkspaceDto>() {
            @Override
            public void makeCall(AsyncCallback<UsersWorkspaceDto> callback) {
                getUsersWorkspace(wsId, callback);
            }
        });
    }

    private void getUsersWorkspace(@NotNull String wsId, @NotNull AsyncCallback<UsersWorkspaceDto> callback) {
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
    public Promise<List<UsersWorkspaceDto>> getWorkspaces(int skip, int limit) {
        return newPromise(new RequestCall<List<UsersWorkspaceDto>>() {
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

    private void getWorkspaces(@NotNull AsyncCallback<List<UsersWorkspaceDto>> callback) {
        final String url = baseHttpUrl;
        asyncRequestFactory.createGetRequest(url)
                           .header(ACCEPT, APPLICATION_JSON)
                           .loader(loader, "Getting info about workspaces...")
                           .send(newCallback(callback, dtoUnmarshallerFactory.newListUnmarshaller(UsersWorkspaceDto.class)));
    }

    @Override
    public Promise<List<RuntimeWorkspaceDto>> getRuntimeWorkspaces(int skip, int limit) {
        return null;
    }

    @Override
    public Promise<UsersWorkspaceDto> update(String wsId, WorkspaceConfigDto newCfg) {
        return null;
    }

    @Override
    public Promise<Void> delete(String wsId) {
        return null;
    }

    @Override
    public Promise<UsersWorkspaceDto> startTemporary(final WorkspaceConfigDto cfg, final String accountId) {
        return newPromise(new RequestCall<UsersWorkspaceDto>() {
            @Override
            public void makeCall(AsyncCallback<UsersWorkspaceDto> callback) {
                startTemporary(cfg, accountId, callback);
            }
        });
    }

    private void startTemporary(@NotNull WorkspaceConfigDto cfg,
                                @NotNull String accountId,
                                @NotNull AsyncCallback<UsersWorkspaceDto> callback) {
        asyncRequestFactory.createPostRequest(baseHttpUrl + "/runtime", cfg)
                           .header(ACCEPT, APPLICATION_JSON)
                           .header(CONTENT_TYPE, APPLICATION_JSON)
                           .loader(loader, "Creating machine from recipe...")
                           .send(newCallback(callback, dtoUnmarshallerFactory.newUnmarshaller(UsersWorkspaceDto.class)));
    }

    @Override
    public Promise<UsersWorkspaceDto> startById(@NotNull final String id, final String envName) {
        return newPromise(new RequestCall<UsersWorkspaceDto>() {
            @Override
            public void makeCall(AsyncCallback<UsersWorkspaceDto> callback) {
                startById(id, envName, callback);
            }
        });
    }

    private void startById(@NotNull String workspaceId,
                           @Nullable String envName,
                           @NotNull AsyncCallback<UsersWorkspaceDto> callback) {
        String url = baseHttpUrl + "/" + workspaceId + "/runtime?environment=" + envName;
        asyncRequestFactory.createPostRequest(url, null)
                           .header(ACCEPT, APPLICATION_JSON)
                           .header(CONTENT_TYPE, APPLICATION_JSON)
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
    public Promise<UsersWorkspaceDto> deleteEnvironment(String wsId, String envName) {
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
