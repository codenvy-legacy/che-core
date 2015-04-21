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

import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.eclipse.che.api.machine.shared.dto.CommandDescriptor;
import org.eclipse.che.api.machine.shared.dto.CreateMachineFromRecipe;
import org.eclipse.che.api.machine.shared.dto.CreateMachineFromSnapshot;
import org.eclipse.che.api.machine.shared.dto.MachineDescriptor;
import org.eclipse.che.api.machine.shared.dto.RecipeDescriptor;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.AsyncRequestFactory;
import org.eclipse.che.ide.rest.AsyncRequestLoader;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.google.gwt.http.client.RequestBuilder.DELETE;
import static org.eclipse.che.ide.MimeType.APPLICATION_JSON;
import static org.eclipse.che.ide.rest.HTTPHeader.ACCEPT;
import static org.eclipse.che.ide.rest.HTTPHeader.CONTENT_TYPE;

/**
 * Implementation for {@link MachineServiceClient}.
 *
 * @author Artem Zatsarynnyy
 */
public class MachineServiceClientImpl implements MachineServiceClient {
    private final DtoFactory          dtoFactory;
    private final AsyncRequestFactory asyncRequestFactory;
    private final AsyncRequestLoader  loader;
    private final String              baseHttpUrl;

    @Inject
    protected MachineServiceClientImpl(@Named("restContext") String restContext,
                                       DtoFactory dtoFactory,
                                       AsyncRequestFactory asyncRequestFactory,
                                       AsyncRequestLoader loader) {
        this.dtoFactory = dtoFactory;
        this.asyncRequestFactory = asyncRequestFactory;
        this.loader = loader;
        this.baseHttpUrl = restContext + "/machine";
    }

    /** {@inheritDoc} */
    @Override
    public void createMachineFromRecipe(@Nonnull String workspaceId,
                                        @Nonnull String machineType,
                                        @Nonnull String recipeType,
                                        @Nonnull String recipeScript,
                                        @Nullable String outputChannel,
                                        @Nonnull AsyncRequestCallback<MachineDescriptor> callback) {
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
                           .send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void createMachineFromSnapshot(@Nonnull String snapshotId,
                                          @Nullable String outputChannel,
                                          @Nonnull AsyncRequestCallback<MachineDescriptor> callback) {
        final CreateMachineFromSnapshot request = dtoFactory.createDto(CreateMachineFromSnapshot.class)
                                                            .withSnapshotId(snapshotId)
                                                            .withOutputChannel(outputChannel);

        asyncRequestFactory.createPostRequest(baseHttpUrl + "/snapshot", request)
                           .header(ACCEPT, APPLICATION_JSON)
                           .header(CONTENT_TYPE, APPLICATION_JSON)
                           .loader(loader, "Creating machine from snapshot...")
                           .send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void destroyMachine(@Nonnull String machineId, @Nonnull AsyncRequestCallback<Void> callback) {
        asyncRequestFactory.createRequest(DELETE, baseHttpUrl + '/' + machineId, null, false)
                           .loader(loader, "Destroying machine...")
                           .send(callback);
    }

    /** {@inheritDoc} */
    @Override
    public void executeCommandInMachine(@Nonnull String machineId,
                                        @Nonnull String commandLine,
                                        @Nullable String outputChannel,
                                        @Nonnull AsyncRequestCallback<Void> callback) {
        final CommandDescriptor request = dtoFactory.createDto(CommandDescriptor.class)
                                                    .withCommandLine(commandLine)
                                                    .withOutputChannel(outputChannel);

        asyncRequestFactory.createPostRequest(baseHttpUrl + '/' + machineId + "/command", request)
                           .header(CONTENT_TYPE, APPLICATION_JSON)
                           .loader(loader, "Executing command...")
                           .send(callback);
    }
}
