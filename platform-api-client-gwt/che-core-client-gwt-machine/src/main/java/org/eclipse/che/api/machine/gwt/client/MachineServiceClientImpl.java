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
package org.eclipse.che.api.machine.gwt.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.eclipse.che.api.machine.shared.dto.CommandDto;
import org.eclipse.che.api.machine.shared.dto.MachineDto;
import org.eclipse.che.api.machine.shared.dto.MachineProcessDto;
import org.eclipse.che.api.machine.shared.dto.MachineStateDto;
import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.callback.AsyncPromiseHelper.RequestCall;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.rest.AsyncRequestFactory;
import org.eclipse.che.ide.rest.AsyncRequestLoader;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.RestContext;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

import static com.google.gwt.http.client.RequestBuilder.DELETE;
import static org.eclipse.che.api.promises.client.callback.PromiseHelper.newCallback;
import static org.eclipse.che.api.promises.client.callback.PromiseHelper.newPromise;
import static org.eclipse.che.ide.MimeType.APPLICATION_JSON;
import static org.eclipse.che.ide.rest.HTTPHeader.ACCEPT;

/**
 * Implementation for {@link MachineServiceClient}.
 *
 * @author Artem Zatsarynnyy
 * @author Dmitry Shnurenko
 */
public class MachineServiceClientImpl implements MachineServiceClient {
    private final String                 workspaceId;
    private final DtoFactory             dtoFactory;
    private final DtoUnmarshallerFactory dtoUnmarshallerFactory;
    private final AsyncRequestFactory    asyncRequestFactory;
    private final AsyncRequestLoader     loader;
    private final String                 baseHttpUrl;

    @Inject
    protected MachineServiceClientImpl(@RestContext String restContext,
                                       @Named("workspaceId") String workspaceId,
                                       DtoFactory dtoFactory,
                                       DtoUnmarshallerFactory dtoUnmarshallerFactory,
                                       AsyncRequestFactory asyncRequestFactory,
                                       AsyncRequestLoader loader) {
        this.workspaceId = workspaceId;
        this.dtoFactory = dtoFactory;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.asyncRequestFactory = asyncRequestFactory;
        this.loader = loader;
        this.baseHttpUrl = restContext + "/machine";
    }

    @Override
    public Promise<MachineDto> getMachine(@NotNull final String machineId) {
        return newPromise(new RequestCall<MachineDto>() {
            @Override
            public void makeCall(AsyncCallback<MachineDto> callback) {
                getMachine(machineId, callback);
            }
        });
    }

    private void getMachine(@NotNull String machineId, @NotNull AsyncCallback<MachineDto> callback) {
        final String url = baseHttpUrl + '/' + machineId;
        asyncRequestFactory.createGetRequest(url)
                           .header(ACCEPT, APPLICATION_JSON)
                           .loader(loader, "Getting info about machine...")
                           .send(newCallback(callback, dtoUnmarshallerFactory.newUnmarshaller(MachineDto.class)));
    }

    @Override
    public Promise<MachineStateDto> getMachineState(@NotNull final String machineId) {
        return newPromise(new RequestCall<MachineStateDto>() {
            @Override
            public void makeCall(AsyncCallback<MachineStateDto> callback) {
                getMachineState(machineId, callback);
            }
        });
    }

    private void getMachineState(@NotNull String machineId, @NotNull AsyncCallback<MachineStateDto> callback) {
        final String url = baseHttpUrl + '/' + machineId + "/state";
        asyncRequestFactory.createGetRequest(url)
                           .header(ACCEPT, APPLICATION_JSON)
                           .loader(loader, "Getting info about machine...")
                           .send(newCallback(callback, dtoUnmarshallerFactory.newUnmarshaller(MachineStateDto.class)));
    }

    @Override
    public Promise<List<MachineDto>> getMachines(@Nullable final String projectPath) {
        return newPromise(new RequestCall<List<MachineDto>>() {
            @Override
            public void makeCall(AsyncCallback<List<MachineDto>> callback) {
                getMachines(workspaceId, projectPath, callback);
            }
        }).then(new Function<List<MachineDto>, List<MachineDto>>() {
            @Override
            public List<MachineDto> apply(List<MachineDto> arg) throws FunctionException {
                final List<MachineDto> descriptors = new ArrayList<>();
                for (MachineDto descriptor : arg) {
                    descriptors.add(descriptor);
                }
                return descriptors;
            }
        });
    }

    @Override
    public Promise<List<MachineDto>> getWorkspaceMachines(final String workspaceId) {
        return newPromise(new RequestCall<List<MachineDto>>() {
            @Override
            public void makeCall(AsyncCallback<List<MachineDto>> callback) {
                getMachines(workspaceId, null, callback);
            }
        }).then(new Function<List<MachineDto>, List<MachineDto>>() {
            @Override
            public List<MachineDto> apply(List<MachineDto> arg) throws FunctionException {
                final List<MachineDto> descriptors = new ArrayList<>();
                for (MachineDto descriptor : arg) {
                    descriptors.add(descriptor);
                }
                return descriptors;
            }
        });
    }

    private void getMachines(@NotNull String workspaceId, @Nullable String projectPath,
                             @NotNull AsyncCallback<List<MachineDto>> callback) {
        final String url = baseHttpUrl + "?workspace=" + workspaceId + (projectPath != null ? "&project=" + projectPath : "");
        asyncRequestFactory.createGetRequest(url)
                           .header(ACCEPT, APPLICATION_JSON)
                           .loader(loader, "Getting info about bound machines...")
                           .send(newCallback(callback, dtoUnmarshallerFactory.newListUnmarshaller(MachineDto.class)));
    }

    @Override
    public Promise<List<MachineStateDto>> getMachinesStates(@Nullable final String projectPath) {
        return newPromise(new RequestCall<List<MachineStateDto>>() {
            @Override
            public void makeCall(AsyncCallback<List<MachineStateDto>> callback) {
                getMachinesStates(workspaceId, projectPath, callback);
            }
        }).then(new Function<List<MachineStateDto>, List<MachineStateDto>>() {
            @Override
            public List<MachineStateDto> apply(List<MachineStateDto> arg) throws FunctionException {
                final List<MachineStateDto> descriptors = new ArrayList<>();
                for (MachineStateDto descriptor : arg) {
                    descriptors.add(descriptor);
                }
                return descriptors;
            }
        });
    }

    private void getMachinesStates(@NotNull String workspaceId, @Nullable String projectPath,
                                   @NotNull AsyncCallback<List<MachineStateDto>> callback) {
        final String url = baseHttpUrl + "/state" + "?workspace=" + workspaceId + (projectPath != null ? "&project=" + projectPath : "");
        asyncRequestFactory.createGetRequest(url)
                           .header(ACCEPT, APPLICATION_JSON)
                           .loader(loader, "Getting info about bound machines...")
                           .send(newCallback(callback, dtoUnmarshallerFactory.newListUnmarshaller(MachineStateDto.class)));
    }

    @Override
    public Promise<Void> destroyMachine(@NotNull final String machineId) {
        return newPromise(new RequestCall<Void>() {
            @Override
            public void makeCall(AsyncCallback<Void> callback) {
                destroyMachine(machineId, callback);
            }
        });
    }

    private void destroyMachine(@NotNull String machineId, @NotNull AsyncCallback<Void> callback) {
        asyncRequestFactory.createRequest(DELETE, baseHttpUrl + '/' + machineId, null, false)
                           .loader(loader, "Destroying machine...")
                           .send(newCallback(callback));
    }

    @Override
    public Promise<MachineProcessDto> executeCommand(@NotNull final String machineId,
                                                     @NotNull final String commandLine,
                                                     @Nullable final String outputChannel) {
        return newPromise(new RequestCall<MachineProcessDto>() {
            @Override
            public void makeCall(AsyncCallback<MachineProcessDto> callback) {
                executeCommand(machineId, commandLine, outputChannel, callback);
            }
        });
    }

    private void executeCommand(@NotNull String machineId,
                                @NotNull String commandLine,
                                @Nullable String outputChannel,
                                @NotNull AsyncCallback<MachineProcessDto> callback) {
        final CommandDto request = dtoFactory.createDto(CommandDto.class)
                                             .withCommandLine(commandLine);

        asyncRequestFactory.createPostRequest(baseHttpUrl + '/' + machineId + "/command?outputChannel=" + outputChannel, request)
                           .header(ACCEPT, APPLICATION_JSON)
                           .loader(loader, "Executing command...")
                           .send(newCallback(callback, dtoUnmarshallerFactory.newUnmarshaller(MachineProcessDto.class)));
    }

    @Override
    public Promise<List<MachineProcessDto>> getProcesses(@NotNull final String machineId) {
        return newPromise(new RequestCall<List<MachineProcessDto>>() {
            @Override
            public void makeCall(AsyncCallback<List<MachineProcessDto>> callback) {
                final String url = baseHttpUrl + "/" + machineId + "/process";
                asyncRequestFactory.createGetRequest(url)
                                   .header(ACCEPT, APPLICATION_JSON)
                                   .loader(loader, "Getting machine processes...")
                                   .send(newCallback(callback, dtoUnmarshallerFactory.newListUnmarshaller(MachineProcessDto.class)));
            }
        }).then(new Function<List<MachineProcessDto>, List<MachineProcessDto>>() {
            @Override
            public List<MachineProcessDto> apply(List<MachineProcessDto> arg) throws FunctionException {
                final List<MachineProcessDto> descriptors = new ArrayList<>();
                for (MachineProcessDto descriptor : arg) {
                    descriptors.add(descriptor);
                }
                return descriptors;
            }
        });
    }

    @Override
    public Promise<Void> stopProcess(@NotNull final String machineId, final int processId) {
        return newPromise(new RequestCall<Void>() {
            @Override
            public void makeCall(AsyncCallback<Void> callback) {
                stopProcess(machineId, processId, callback);
            }
        });
    }

    private void stopProcess(@NotNull String machineId, int processId, @NotNull AsyncCallback<Void> callback) {
        asyncRequestFactory.createDeleteRequest(baseHttpUrl + '/' + machineId + "/process/" + processId)
                           .loader(loader, "Stopping process...")
                           .send(newCallback(callback));
    }

    @Override
    public Promise<Void> bindProject(@NotNull final String machineId, @NotNull final String projectPath) {
        return newPromise(new RequestCall<Void>() {
            @Override
            public void makeCall(AsyncCallback<Void> callback) {
                bindProject(machineId, projectPath, callback);
            }
        });
    }

    private void bindProject(@NotNull String machineId, @NotNull String projectPath, @NotNull AsyncCallback<Void> callback) {
        asyncRequestFactory.createPostRequest(baseHttpUrl + '/' + machineId + "/binding/" + projectPath, null)
                           .loader(loader, "Binding project to machine...")
                           .send(newCallback(callback));
    }

    @Override
    public Promise<Void> unbindProject(@NotNull final String machineId, @NotNull final String projectPath) {
        return newPromise(new RequestCall<Void>() {
            @Override
            public void makeCall(AsyncCallback<Void> callback) {
                unbindProject(machineId, projectPath, callback);
            }
        });
    }

    private void unbindProject(@NotNull String machineId, @NotNull String projectPath, @NotNull AsyncCallback<Void> callback) {
        asyncRequestFactory.createDeleteRequest(baseHttpUrl + '/' + machineId + "/binding/" + projectPath)
                           .loader(loader, "Unbinding project from machine...")
                           .send(newCallback(callback));
    }
}
