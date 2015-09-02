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
package org.eclipse.che.api.machine.server;

import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.machine.MachineConfig;
import org.eclipse.che.api.core.util.FileCleaner;
import org.eclipse.che.api.machine.server.exception.MachineException;
import org.eclipse.che.api.machine.shared.dto.RecipeMachineCreationMetadata;
import org.eclipse.che.api.machine.shared.dto.SnapshotMachineCreationMetadata;
import org.eclipse.che.api.machine.shared.dto.recipe.MachineRecipe;
import org.eclipse.che.api.workspace.server.MachineClient;
import org.eclipse.che.api.workspace.server.model.impl.MachineImpl;
import org.eclipse.che.api.workspace.server.model.impl.MachineSourceImpl;
import org.eclipse.che.commons.lang.IoUtil;
import org.eclipse.che.dto.server.DtoFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;

/**
 * @author Alexander Garagatyi
 */
public class MachineClientImpl implements MachineClient {
    private final MachineManager machineManager;

    @Inject
    public MachineClientImpl(MachineManager machineManager) {
        this.machineManager = machineManager;
    }

    @Override
    public org.eclipse.che.api.workspace.server.model.impl.MachineImpl start(MachineConfig machineConfig, String workspaceId,
                                                                             String envName)
            throws ServerException, BadRequestException, NotFoundException, ConflictException {
        final String machineSourceType = machineConfig.getSource().getType();
        final String outputChannel = workspaceId + ':' + envName + ':' + machineConfig.getName();
        if ("Recipe".equalsIgnoreCase(machineSourceType)) {
            String recipeContent;
            File file = null;
            try {
                file = IoUtil.downloadFile(null, "recipe", null, new URL(machineConfig.getSource().getLocation()));
                recipeContent = IoUtil.readAndCloseQuietly(new FileInputStream(file));
            } catch (IOException e) {
                throw new ServerException("Can't start machine " + machineConfig.getName() + ". " + e.getLocalizedMessage());
            } finally {
                if (file != null) {
                    FileCleaner.addFile(file);
                }
            }

            final MachineRecipe recipe = DtoFactory.newDto(MachineRecipe.class).withType("Dockerfile").withScript(recipeContent);

            // TODO do we need output channel?
            final RecipeMachineCreationMetadata creationMetadata = DtoFactory.newDto(RecipeMachineCreationMetadata.class)
                                                                             .withType(machineConfig.getType())
                                                                             .withDev(machineConfig.isDev())
                                                                             .withDisplayName(machineConfig.getName())
                                                                             .withMemorySize(machineConfig.getMemorySize())
                                                                             .withOutputChannel(outputChannel)
                                                                             .withRecipe(recipe)
                                                                             .withWorkspaceId(workspaceId);
            final org.eclipse.che.api.machine.server.impl.MachineImpl machine = machineManager.create(creationMetadata, false);

            return MachineImpl.builder()
                              .setId(machine.getId())
                              .setType(machine.getType())
                              .setName(machine.getDisplayName())
                              .setDev(machine.isDev())
                              .setSource(new MachineSourceImpl(machineSourceType, machineConfig.getSource().getLocation()))
                              .setMemorySize(machine.getMemorySize())
                              .setOutputChannel(outputChannel)
                              .build();
        } else if ("Snapshot".equalsIgnoreCase(machineSourceType)) {
            final SnapshotMachineCreationMetadata snapshotMetadata = DtoFactory.newDto(SnapshotMachineCreationMetadata.class)
                                                                               .withSnapshotId(machineConfig.getSource().getLocation())
                                                                               .withDisplayName(machineConfig.getName())
                                                                               .withMemorySize(machineConfig.getMemorySize())
                                                                               .withOutputChannel(outputChannel);

            final org.eclipse.che.api.machine.server.impl.MachineImpl machine = machineManager.create(snapshotMetadata, false);

            return MachineImpl.builder()
                              .setId(machine.getId())
                              .setType(machine.getType())
                              .setName(machine.getDisplayName())
                              .setDev(machine.isDev())
                              .setSource(new MachineSourceImpl(machineSourceType, machineConfig.getSource().getLocation()))
                              .setMemorySize(machine.getMemorySize())
                              .setOutputChannel(outputChannel)
                              .build();
        } else {
            throw new BadRequestException("Machine source type is unknown " + machineSourceType);
        }
    }

    @Override
    public void destroy(String machineId) throws NotFoundException, MachineException {
        machineManager.destroy(machineId, false);
    }
}
