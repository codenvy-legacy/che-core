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
import org.eclipse.che.api.machine.shared.dto.CreateMachineFromRecipe;
import org.eclipse.che.api.machine.shared.dto.CreateMachineFromSnapshot;
import org.eclipse.che.api.machine.shared.dto.MachineDescriptor;
import org.eclipse.che.api.machine.shared.dto.ProcessDescriptor;
import org.eclipse.che.api.machine.shared.dto.RecipeDescriptor;
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
 */
public class MachineServiceClientImpl implements MachineServiceClient {
    private final String workspaceId;
    private final DtoFactory dtoFactory;
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
                                                              @Nullable final String outputChannel) {
        return newPromise(new RequestCall<MachineDescriptor>() {
            @Override
            public void makeCall(AsyncCallback<MachineDescriptor> callback) {
                createMachineFromRecipe(workspaceId, machineType, recipeType, recipeScript, outputChannel, callback);
            }
        });
    }

    private void createMachineFromRecipe(@Nonnull String workspaceId,
                                         @Nonnull String machineType,
                                         @Nonnull String recipeType,
                                         @Nonnull String recipeScript,
                                         @Nullable String outputChannel,
                                         @Nonnull AsyncCallback<MachineDescriptor> callback) {
        final RecipeDescriptor recipeDescriptor = dtoFactory.createDto(RecipeDescriptor.class)
                                                            .withType(recipeType)
                                                            .withScript(recipeScript);

        final CreateMachineFromRecipe request = dtoFactory.createDto(CreateMachineFromRecipe.class)
                                                          .withWorkspaceId(workspaceId)
                                                          .withType(machineType)
                                                          .withRecipeDescriptor(recipeDescriptor)
                                                          .withOutputChannel(outputChannel);

        asyncRequestFactory.createPostRequest(baseHttpUrl + "/recipe", request)
                           .header(ACCEPT, APPLICATION_JSON)
                           .header(CONTENT_TYPE, APPLICATION_JSON)
                           .loader(loader, "Creating machine from recipe...")
                           .send(newCallback(callback, dtoUnmarshallerFactory.newUnmarshaller(MachineDescriptor.class)));
    }

    @Override
    public Promise<MachineDescriptor> createMachineFromSnapshot(@Nonnull final String snapshotId, @Nullable final String outputChannel) {
        return newPromise(new RequestCall<MachineDescriptor>() {
            @Override
            public void makeCall(AsyncCallback<MachineDescriptor> callback) {
                createMachineFromSnapshot(snapshotId, outputChannel, callback);
            }
        });
    }

    private void createMachineFromSnapshot(@Nonnull String snapshotId,
                                           @Nullable String outputChannel,
                                           @Nonnull AsyncCallback<MachineDescriptor> callback) {
        final CreateMachineFromSnapshot request = dtoFactory.createDto(CreateMachineFromSnapshot.class)
                                                            .withSnapshotId(snapshotId)
                                                            .withOutputChannel(outputChannel);

        asyncRequestFactory.createPostRequest(baseHttpUrl + "/snapshot", request)
                           .header(ACCEPT, APPLICATION_JSON)
                           .header(CONTENT_TYPE, APPLICATION_JSON)
                           .loader(loader, "Creating machine from snapshot...")
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
