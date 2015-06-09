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
import org.eclipse.che.api.machine.shared.dto.MachineFromRecipeMetadata;
import org.eclipse.che.api.machine.shared.dto.MachineFromSnapshotMetadata;
import org.eclipse.che.api.machine.shared.dto.ProcessDescriptor;
import org.eclipse.che.api.machine.shared.dto.recipe.RecipeDescriptor;
import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.callback.AsyncPromiseHelper.RequestCall;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.rest.AsyncRequestFactory;
import org.eclipse.che.ide.rest.AsyncRequestLoader;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.RestContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static com.google.gwt.http.client.RequestBuilder.DELETE;
import static org.eclipse.che.api.machine.gwt.client.Utils.newCallback;
import static org.eclipse.che.api.machine.gwt.client.Utils.newPromise;
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
    public Promise<MachineDescriptor> createMachineFromRecipe(@Nonnull final String machineType,
                                                              @Nonnull final String recipeType,
                                                              @Nonnull final String recipeScript,
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

    private void createMachineFromRecipe(@Nonnull String workspaceId,
                                         @Nonnull String machineType,
                                         @Nonnull String recipeType,
                                         @Nonnull String recipeScript,
                                         @Nullable final String displayName,
                                         boolean bindWorkspace,
                                         @Nullable String outputChannel,
                                         @Nonnull AsyncCallback<MachineDescriptor> callback) {
        final RecipeDescriptor recipeDescriptor = dtoFactory.createDto(RecipeDescriptor.class)
                                                            .withType(recipeType)
                                                            .withScript(recipeScript);

        final MachineFromRecipeMetadata request = dtoFactory.createDto(MachineFromRecipeMetadata.class)
                                                            .withWorkspaceId(workspaceId)
                                                            .withType(machineType)
                                                            .withRecipeDescriptor(recipeDescriptor)
                                                            .withDisplayName(displayName)
                                                            .withBindWorkspace(bindWorkspace)
                                                            .withOutputChannel(outputChannel);

        asyncRequestFactory.createPostRequest(baseHttpUrl + "/recipe", request)
                           .header(ACCEPT, APPLICATION_JSON)
                           .header(CONTENT_TYPE, APPLICATION_JSON)
                           .loader(loader, "Creating machine from recipe...")
                           .send(newCallback(callback, dtoUnmarshallerFactory.newUnmarshaller(MachineDescriptor.class)));
    }

    @Override
    public Promise<MachineDescriptor> createMachineFromSnapshot(@Nonnull final String snapshotId,
                                                                @Nullable final String displayName,
                                                                @Nullable final String outputChannel) {
        return newPromise(new RequestCall<MachineDescriptor>() {
            @Override
            public void makeCall(AsyncCallback<MachineDescriptor> callback) {
                createMachineFromSnapshot(snapshotId, displayName, outputChannel, callback);
            }
        });
    }

    private void createMachineFromSnapshot(@Nonnull String snapshotId,
                                           @Nullable final String displayName,
                                           @Nullable String outputChannel,
                                           @Nonnull AsyncCallback<MachineDescriptor> callback) {
        final MachineFromSnapshotMetadata request = dtoFactory.createDto(MachineFromSnapshotMetadata.class)
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
    public Promise<MachineDescriptor> getMachine(@Nonnull final String machineId) {
        return newPromise(new RequestCall<MachineDescriptor>() {
            @Override
            public void makeCall(AsyncCallback<MachineDescriptor> callback) {
                getMachine(machineId, callback);
            }
        });
    }

    private void getMachine(@Nonnull String machineId, @Nonnull AsyncCallback<MachineDescriptor> callback) {
        final String url = baseHttpUrl + '/' + machineId;
        asyncRequestFactory.createGetRequest(url)
                           .header(ACCEPT, APPLICATION_JSON)
                           .loader(loader, "Getting info about machine...")
                           .send(newCallback(callback, dtoUnmarshallerFactory.newUnmarshaller(MachineDescriptor.class)));
    }

    @Override
    public Promise<List<MachineDescriptor>> getMachines(@Nullable final String projectPath) {
        return newPromise(new RequestCall<Array<MachineDescriptor>>() {
            @Override
            public void makeCall(AsyncCallback<Array<MachineDescriptor>> callback) {
                getMachines(workspaceId, projectPath, callback);
            }
        }).then(new Function<Array<MachineDescriptor>, List<MachineDescriptor>>() {
            @Override
            public List<MachineDescriptor> apply(Array<MachineDescriptor> arg) throws FunctionException {
                final List<MachineDescriptor> descriptors = new ArrayList<>();
                for (MachineDescriptor descriptor : arg.asIterable()) {
                    descriptors.add(descriptor);
                }
                return descriptors;
            }
        });
    }

    private void getMachines(@Nonnull String workspaceId, @Nullable String projectPath,
                             @Nonnull AsyncCallback<Array<MachineDescriptor>> callback) {
        final String url = baseHttpUrl + "?workspace=" + workspaceId + (projectPath != null ? "&project=" + projectPath : "");
        asyncRequestFactory.createGetRequest(url)
                           .header(ACCEPT, APPLICATION_JSON)
                           .loader(loader, "Getting info about bound machines...")
                           .send(newCallback(callback, dtoUnmarshallerFactory.newArrayUnmarshaller(MachineDescriptor.class)));
    }

    @Override
    public Promise<Void> destroyMachine(@Nonnull final String machineId) {
        return newPromise(new RequestCall<Void>() {
            @Override
            public void makeCall(AsyncCallback<Void> callback) {
                destroyMachine(machineId, callback);
            }
        });
    }

    private void destroyMachine(@Nonnull String machineId, @Nonnull AsyncCallback<Void> callback) {
        asyncRequestFactory.createRequest(DELETE, baseHttpUrl + '/' + machineId, null, false)
                           .loader(loader, "Destroying machine...")
                           .send(newCallback(callback));
    }

    @Override
    public Promise<ProcessDescriptor> executeCommand(@Nonnull final String machineId,
                                                     @Nonnull final String commandLine,
                                                     @Nullable final String outputChannel) {
        return newPromise(new RequestCall<ProcessDescriptor>() {
            @Override
            public void makeCall(AsyncCallback<ProcessDescriptor> callback) {
                executeCommand(machineId, commandLine, outputChannel, callback);
            }
        });
    }

    private void executeCommand(@Nonnull String machineId,
                                @Nonnull String commandLine,
                                @Nullable String outputChannel,
                                @Nonnull AsyncCallback<ProcessDescriptor> callback) {
        final CommandDescriptor request = dtoFactory.createDto(CommandDescriptor.class)
                                                    .withCommandLine(commandLine)
                                                    .withOutputChannel(outputChannel);

        asyncRequestFactory.createPostRequest(baseHttpUrl + '/' + machineId + "/command", request)
                           .header(ACCEPT, APPLICATION_JSON)
                           .loader(loader, "Executing command...")
                           .send(newCallback(callback, dtoUnmarshallerFactory.newUnmarshaller(ProcessDescriptor.class)));
    }

    @Override
    public Promise<List<ProcessDescriptor>> getProcesses(@Nonnull final String machineId) {
        return newPromise(new RequestCall<Array<ProcessDescriptor>>() {
            @Override
            public void makeCall(AsyncCallback<Array<ProcessDescriptor>> callback) {
                final String url = baseHttpUrl + "/" + machineId + "/process";
                asyncRequestFactory.createGetRequest(url)
                                   .header(ACCEPT, APPLICATION_JSON)
                                   .loader(loader, "Getting machine processes...")
                                   .send(newCallback(callback, dtoUnmarshallerFactory.newArrayUnmarshaller(ProcessDescriptor.class)));
            }
        }).then(new Function<Array<ProcessDescriptor>, List<ProcessDescriptor>>() {
            @Override
            public List<ProcessDescriptor> apply(Array<ProcessDescriptor> arg) throws FunctionException {
                final List<ProcessDescriptor> descriptors = new ArrayList<>();
                for (ProcessDescriptor descriptor : arg.asIterable()) {
                    descriptors.add(descriptor);
                }
                return descriptors;
            }
        });
    }

    @Override
    public Promise<Void> bindProject(@Nonnull final String machineId, @Nonnull final String projectPath) {
        return newPromise(new RequestCall<Void>() {
            @Override
            public void makeCall(AsyncCallback<Void> callback) {
                bindProject(machineId, projectPath, callback);
            }
        });
    }

    private void bindProject(@Nonnull String machineId, @Nonnull String projectPath, @Nonnull AsyncCallback<Void> callback) {
        asyncRequestFactory.createPostRequest(baseHttpUrl + '/' + machineId + "/binding/" + (projectPath), null)
                           .loader(loader, "Binding project to machine...")
                           .send(newCallback(callback));
    }

    @Override
    public Promise<Void> unbindProject(@Nonnull final String machineId, @Nonnull final String projectPath) {
        return newPromise(new RequestCall<Void>() {
            @Override
            public void makeCall(AsyncCallback<Void> callback) {
                unbindProject(machineId, projectPath, callback);
            }
        });
    }

    private void unbindProject(@Nonnull String machineId, @Nonnull String projectPath, @Nonnull AsyncCallback<Void> callback) {
        asyncRequestFactory.createDeleteRequest(baseHttpUrl + '/' + machineId + "/binding/" + (projectPath))
                           .loader(loader, "Unbinding project from machine...")
                           .send(newCallback(callback));
    }
}
