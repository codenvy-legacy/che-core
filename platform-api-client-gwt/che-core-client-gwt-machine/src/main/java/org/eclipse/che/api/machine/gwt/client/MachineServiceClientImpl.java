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

import org.eclipse.che.api.machine.shared.dto.CommandDescriptor;
import org.eclipse.che.api.machine.shared.dto.MachineDescriptor;
import org.eclipse.che.api.machine.shared.dto.MachineStateDescriptor;
import org.eclipse.che.api.machine.shared.dto.ProcessDescriptor;
import org.eclipse.che.api.machine.shared.dto.RecipeMachineCreationMetadata;
import org.eclipse.che.api.machine.shared.dto.SnapshotMachineCreationMetadata;
import org.eclipse.che.api.machine.shared.dto.recipe.MachineRecipe;
import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.callback.AsyncPromiseHelper.RequestCall;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.rest.AsyncRequestFactory;
import org.eclipse.che.ide.rest.AsyncRequestLoader;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.RestContext;

import javax.validation.constraints.NotNull;
import org.eclipse.che.commons.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static com.google.gwt.http.client.RequestBuilder.DELETE;
import static org.eclipse.che.api.promises.client.callback.PromiseHelper.newCallback;
import static org.eclipse.che.api.promises.client.callback.PromiseHelper.newPromise;
import static org.eclipse.che.ide.MimeType.APPLICATION_JSON;
import static org.eclipse.che.ide.rest.HTTPHeader.ACCEPT;
import static org.eclipse.che.ide.rest.HTTPHeader.CONTENT_TYPE;

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
    public Promise<MachineDescriptor> createMachineFromRecipe(@NotNull final String machineType,
                                                              @NotNull final String recipeType,
                                                              @NotNull final String recipeScript,
                                                              @Nullable final String displayName,
                                                              final boolean bindWorkspace,
                                                              @Nullable final String outputChannel) {
        return newPromise(new RequestCall<MachineDescriptor>() {
            @Override
            public void makeCall(AsyncCallback<MachineDescriptor> callback) {
                createMachineFromRecipe(workspaceId, machineType, recipeType, recipeScript, displayName, bindWorkspace, outputChannel,
                                        callback);
            }
        });
    }

    private void createMachineFromRecipe(@NotNull String workspaceId,
                                         @NotNull String machineType,
                                         @NotNull String recipeType,
                                         @NotNull String recipeScript,
                                         @Nullable final String displayName,
                                         boolean bindWorkspace,
                                         @Nullable String outputChannel,
                                         @NotNull AsyncCallback<MachineDescriptor> callback) {
        final MachineRecipe machineRecipe = dtoFactory.createDto(MachineRecipe.class)
                                                      .withType(recipeType)
                                                      .withScript(recipeScript);

        final RecipeMachineCreationMetadata request = dtoFactory.createDto(RecipeMachineCreationMetadata.class)
                                                                .withWorkspaceId(workspaceId)
                                                                .withType(machineType)
                                                                .withRecipe(machineRecipe)
                                                                .withDisplayName(displayName)
                                                                .withDev(bindWorkspace)
                                                                .withOutputChannel(outputChannel);

        asyncRequestFactory.createPostRequest(baseHttpUrl + "/recipe", request)
                           .header(ACCEPT, APPLICATION_JSON)
                           .header(CONTENT_TYPE, APPLICATION_JSON)
                           .loader(loader, "Creating machine from recipe...")
                           .send(newCallback(callback, dtoUnmarshallerFactory.newUnmarshaller(MachineDescriptor.class)));
    }

    @Override
    public Promise<MachineDescriptor> createMachineFromSnapshot(@NotNull final String snapshotId,
                                                                @Nullable final String displayName,
                                                                @Nullable final String outputChannel) {
        return newPromise(new RequestCall<MachineDescriptor>() {
            @Override
            public void makeCall(AsyncCallback<MachineDescriptor> callback) {
                createMachineFromSnapshot(snapshotId, displayName, outputChannel, callback);
            }
        });
    }

    private void createMachineFromSnapshot(@NotNull String snapshotId,
                                           @Nullable final String displayName,
                                           @Nullable String outputChannel,
                                           @NotNull AsyncCallback<MachineDescriptor> callback) {
        final SnapshotMachineCreationMetadata request = dtoFactory.createDto(SnapshotMachineCreationMetadata.class)
                                                                  .withSnapshotId(snapshotId)
                                                                  .withDisplayName(displayName)
                                                                  .withOutputChannel(outputChannel);

        asyncRequestFactory.createPostRequest(baseHttpUrl + "/snapshot", request)
                           .header(ACCEPT, APPLICATION_JSON)
                           .header(CONTENT_TYPE, APPLICATION_JSON)
                           .loader(loader, "Creating machine from snapshot...")
                           .send(newCallback(callback, dtoUnmarshallerFactory.newUnmarshaller(MachineDescriptor.class)));
    }

    @Override
    public Promise<MachineDescriptor> getMachine(@NotNull final String machineId) {
        return newPromise(new RequestCall<MachineDescriptor>() {
            @Override
            public void makeCall(AsyncCallback<MachineDescriptor> callback) {
                getMachine(machineId, callback);
            }
        });
    }

    private void getMachine(@NotNull String machineId, @NotNull AsyncCallback<MachineDescriptor> callback) {
        final String url = baseHttpUrl + '/' + machineId;
        asyncRequestFactory.createGetRequest(url)
                           .header(ACCEPT, APPLICATION_JSON)
                           .loader(loader, "Getting info about machine...")
                           .send(newCallback(callback, dtoUnmarshallerFactory.newUnmarshaller(MachineDescriptor.class)));
    }

    @Override
    public Promise<MachineStateDescriptor> getMachineState(@NotNull final String machineId) {
        return newPromise(new RequestCall<MachineStateDescriptor>() {
            @Override
            public void makeCall(AsyncCallback<MachineStateDescriptor> callback) {
                getMachineState(machineId, callback);
            }
        });
    }

    private void getMachineState(@NotNull String machineId, @NotNull AsyncCallback<MachineStateDescriptor> callback) {
        final String url = baseHttpUrl + '/' + machineId + "/state";
        asyncRequestFactory.createGetRequest(url)
                           .header(ACCEPT, APPLICATION_JSON)
                           .loader(loader, "Getting info about machine...")
                           .send(newCallback(callback, dtoUnmarshallerFactory.newUnmarshaller(MachineStateDescriptor.class)));
    }

    @Override
    public Promise<List<MachineDescriptor>> getMachines(@Nullable final String projectPath) {
        return newPromise(new RequestCall<List<MachineDescriptor>>() {
            @Override
            public void makeCall(AsyncCallback<List<MachineDescriptor>> callback) {
                getMachines(workspaceId, projectPath, callback);
            }
        }).then(new Function<List<MachineDescriptor>, List<MachineDescriptor>>() {
            @Override
            public List<MachineDescriptor> apply(List<MachineDescriptor> arg) throws FunctionException {
                final List<MachineDescriptor> descriptors = new ArrayList<>();
                for (MachineDescriptor descriptor : arg) {
                    descriptors.add(descriptor);
                }
                return descriptors;
            }
        });
    }

    private void getMachines(@NotNull String workspaceId, @Nullable String projectPath,
                             @NotNull AsyncCallback<List<MachineDescriptor>> callback) {
        final String url = baseHttpUrl + "?workspace=" + workspaceId + (projectPath != null ? "&project=" + projectPath : "");
        asyncRequestFactory.createGetRequest(url)
                           .header(ACCEPT, APPLICATION_JSON)
                           .loader(loader, "Getting info about bound machines...")
                           .send(newCallback(callback, dtoUnmarshallerFactory.newListUnmarshaller(MachineDescriptor.class)));
    }

    @Override
    public Promise<List<MachineStateDescriptor>> getMachinesStates(@Nullable final String projectPath) {
        return newPromise(new RequestCall<List<MachineStateDescriptor>>() {
            @Override
            public void makeCall(AsyncCallback<List<MachineStateDescriptor>> callback) {
                getMachinesStates(workspaceId, projectPath, callback);
            }
        }).then(new Function<List<MachineStateDescriptor>, List<MachineStateDescriptor>>() {
            @Override
            public List<MachineStateDescriptor> apply(List<MachineStateDescriptor> arg) throws FunctionException {
                final List<MachineStateDescriptor> descriptors = new ArrayList<>();
                for (MachineStateDescriptor descriptor : arg) {
                    descriptors.add(descriptor);
                }
                return descriptors;
            }
        });
    }

    private void getMachinesStates(@NotNull String workspaceId, @Nullable String projectPath,
                                   @NotNull AsyncCallback<List<MachineStateDescriptor>> callback) {
        final String url = baseHttpUrl + "/state" + "?workspace=" + workspaceId + (projectPath != null ? "&project=" + projectPath : "");
        asyncRequestFactory.createGetRequest(url)
                           .header(ACCEPT, APPLICATION_JSON)
                           .loader(loader, "Getting info about bound machines...")
                           .send(newCallback(callback, dtoUnmarshallerFactory.newListUnmarshaller(MachineStateDescriptor.class)));
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
    public Promise<ProcessDescriptor> executeCommand(@NotNull final String machineId,
                                                     @NotNull final String commandLine,
                                                     @Nullable final String outputChannel) {
        return newPromise(new RequestCall<ProcessDescriptor>() {
            @Override
            public void makeCall(AsyncCallback<ProcessDescriptor> callback) {
                executeCommand(machineId, commandLine, outputChannel, callback);
            }
        });
    }

    private void executeCommand(@NotNull String machineId,
                                @NotNull String commandLine,
                                @Nullable String outputChannel,
                                @NotNull AsyncCallback<ProcessDescriptor> callback) {
        final CommandDescriptor request = dtoFactory.createDto(CommandDescriptor.class)
                                                    .withCommandLine(commandLine)
                                                    .withOutputChannel(outputChannel);

        asyncRequestFactory.createPostRequest(baseHttpUrl + '/' + machineId + "/command", request)
                           .header(ACCEPT, APPLICATION_JSON)
                           .loader(loader, "Executing command...")
                           .send(newCallback(callback, dtoUnmarshallerFactory.newUnmarshaller(ProcessDescriptor.class)));
    }

    @Override
    public Promise<List<ProcessDescriptor>> getProcesses(@NotNull final String machineId) {
        return newPromise(new RequestCall<List<ProcessDescriptor>>() {
            @Override
            public void makeCall(AsyncCallback<List<ProcessDescriptor>> callback) {
                final String url = baseHttpUrl + "/" + machineId + "/process";
                asyncRequestFactory.createGetRequest(url)
                                   .header(ACCEPT, APPLICATION_JSON)
                                   .loader(loader, "Getting machine processes...")
                                   .send(newCallback(callback, dtoUnmarshallerFactory.newListUnmarshaller(ProcessDescriptor.class)));
            }
        }).then(new Function<List<ProcessDescriptor>, List<ProcessDescriptor>>() {
            @Override
            public List<ProcessDescriptor> apply(List<ProcessDescriptor> arg) throws FunctionException {
                final List<ProcessDescriptor> descriptors = new ArrayList<>();
                for (ProcessDescriptor descriptor : arg) {
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
