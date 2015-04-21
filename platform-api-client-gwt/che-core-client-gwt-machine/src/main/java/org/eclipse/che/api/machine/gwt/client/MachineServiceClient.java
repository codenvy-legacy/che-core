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

import org.eclipse.che.api.machine.shared.dto.MachineDescriptor;
import org.eclipse.che.ide.rest.AsyncRequestCallback;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Client for Machine service.
 *
 * @author Artem Zatsarynnyy
 */
public interface MachineServiceClient {

    /**
     * Create and start machine from scratch using recipe.
     *
     * @param workspaceId
     *         ID of a workspace machine should be bound to
     * @param machineType
     *         type of machine
     * @param recipeType
     *         type of recipe
     * @param recipeScript
     *         recipe script
     * @param outputChannel
     *         websocket chanel where machine logs should be put
     * @param callback
     *         the callback to use for the response
     */
    void createMachineFromRecipe(@Nonnull String workspaceId,
                                 @Nonnull String machineType,
                                 @Nonnull String recipeType,
                                 @Nonnull String recipeScript,
                                 @Nullable String outputChannel,
                                 @Nonnull AsyncRequestCallback<MachineDescriptor> callback);

    /**
     * Restore and start machine from snapshot.
     *
     * @param snapshotId
     *         ID of snapshot machine should be restored from
     * @param outputChannel
     *         websocket chanel where machine logs should be put
     * @param callback
     *         the callback to use for the response
     */
    void createMachineFromSnapshot(@Nonnull String snapshotId,
                                   @Nullable String outputChannel,
                                   @Nonnull AsyncRequestCallback<MachineDescriptor> callback);

    /**
     * Destroy machine with the specified ID.
     *
     * @param machineId
     *         ID of machine that should be destroyed
     * @param callback
     *         the callback to use for the response
     */
    void destroyMachine(@Nonnull String machineId, @Nonnull AsyncRequestCallback<Void> callback);

    /**
     * Execute a command in machine.
     *
     * @param machineId
     *         ID of the machine where command should be executed
     * @param commandLine
     *         command line that should be executed in the machine
     * @param outputChannel
     *         websocket chanel for execution logs
     * @param callback
     *         the callback to use for the response
     */
    void executeCommandInMachine(@Nonnull String machineId,
                                 @Nonnull String commandLine,
                                 @Nullable String outputChannel,
                                 @Nonnull AsyncRequestCallback<Void> callback);
}
