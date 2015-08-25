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
import org.eclipse.che.api.core.model.workspace.Machine;
import org.eclipse.che.api.core.util.FileCleaner;
import org.eclipse.che.api.machine.server.exception.MachineException;
import org.eclipse.che.api.machine.server.impl.MachineImpl;
import org.eclipse.che.api.machine.shared.dto.RecipeMachineCreationMetadata;
import org.eclipse.che.api.machine.shared.dto.SnapshotMachineCreationMetadata;
import org.eclipse.che.api.machine.shared.dto.recipe.MachineRecipe;
import org.eclipse.che.api.workspace.server.MachineClient;
import org.eclipse.che.api.workspace.shared.dto.MachineDto;
import org.eclipse.che.api.workspace.shared.dto.MachineSourceDto;
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
    public Machine start(MachineConfig machineConfig, String workspaceId)
            throws ServerException, BadRequestException, NotFoundException, ConflictException {
        final String machineSourceType = machineConfig.getSource().getType();
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
                                                                                          .withOutputChannel(machineConfig.getName())
                                                                                          .withRecipe(recipe)
                                                                                          .withWorkspaceId(workspaceId);
            final MachineImpl machine = machineManager.create(creationMetadata, false);

            return DtoFactory.newDto(MachineDto.class)
                             .withId(machine.getId())
                             .withType(machine.getType())
                             .withName(machine.getDisplayName())
                             .withDev(machine.isDev())
                             .withSource(DtoFactory.newDto(MachineSourceDto.class)
                                                   .withType(machineSourceType)
                                                   .withLocation(machineConfig.getSource().getLocation()))
                             .withMemorySize(machine.getMemorySize());
        } else if ("Snapshot".equalsIgnoreCase(machineSourceType)) {
            final SnapshotMachineCreationMetadata snapshotMetadata = DtoFactory.newDto(SnapshotMachineCreationMetadata.class)
                                                                               .withSnapshotId(machineConfig.getSource().getLocation())
                                                                               .withDisplayName(machineConfig.getName())
                                                                               .withMemorySize(machineConfig.getMemorySize())
                                                                               .withOutputChannel(machineConfig.getName());

            final MachineImpl machine = machineManager.create(snapshotMetadata, false);

            return DtoFactory.newDto(MachineDto.class)
                             .withId(machine.getId())
                             .withType(machine.getType())
                             .withName(machine.getDisplayName())
                             .withDev(machine.isDev())
                             .withSource(DtoFactory.newDto(MachineSourceDto.class)
                                                   .withType(machineSourceType)
                                                   .withLocation(machineConfig.getSource().getLocation()))
                             .withMemorySize(machine.getMemorySize());
        } else {
            throw new BadRequestException("Machine source type is unknown " + machineSourceType);
        }
    }

    @Override
    public void destroy(String machineId) throws NotFoundException, MachineException {
        machineManager.destroy(machineId, false);
    }
}
