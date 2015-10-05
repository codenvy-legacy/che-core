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
package org.eclipse.che.api.workspace.server;

import org.eclipse.che.api.core.model.machine.Command;
import org.eclipse.che.api.core.model.machine.MachineConfig;
import org.eclipse.che.api.core.model.machine.MachineSource;
import org.eclipse.che.api.core.model.workspace.Environment;
import org.eclipse.che.api.core.model.workspace.ProjectConfig;
import org.eclipse.che.api.core.model.workspace.UsersWorkspace;
import org.eclipse.che.api.workspace.shared.dto.CommandDto;
import org.eclipse.che.api.workspace.shared.dto.EnvironmentDto;
import org.eclipse.che.api.workspace.shared.dto.MachineConfigDto;
import org.eclipse.che.api.workspace.shared.dto.MachineSourceDto;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.api.workspace.shared.dto.SourceStorageDto;
import org.eclipse.che.api.workspace.shared.dto.UsersWorkspaceDto;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.eclipse.che.dto.server.DtoFactory.newDto;

// TODO! use global registry for DTO converters

/**
 * Helps to convert to/from DTOs related to workspace.
 *
 * @author Eugene Voevodin
 */
public final class DtoConverter {

    /**
     * Converts {@link UsersWorkspace} to {@link UsersWorkspaceDto}.
     */
    public static UsersWorkspaceDto asDto(UsersWorkspace workspace) {
        final List<CommandDto> commands = workspace.getCommands()
                                                   .stream()
                                                   .map(DtoConverter::asDto)
                                                   .collect(toList());
        final List<ProjectConfigDto> projects = workspace.getProjects()
                                                         .stream()
                                                         .map(DtoConverter::asDto)
                                                         .collect(toList());
        final Map<String, EnvironmentDto> environments = workspace.getEnvironments()
                                                                  .values()
                                                                  .stream()
                                                                  .collect(toMap(Environment::getName, DtoConverter::asDto));

        return newDto(UsersWorkspaceDto.class).withId(workspace.getId())
                                              .withStatus(workspace.getStatus())
                                              .withName(workspace.getName())
                                              .withOwner(workspace.getOwner())
                                              .withDefaultEnvName(workspace.getDefaultEnvName())
                                              .withCommands(commands)
                                              .withProjects(projects)
                                              .withEnvironments(environments)
                                              .withAttributes(workspace.getAttributes());
    }

    /**
     * Converts {@link Command} to {@link CommandDto}.
     */
    public static CommandDto asDto(Command command) {
        return newDto(CommandDto.class).withName(command.getName())
                                       .withCommandLine(command.getCommandLine())
                                       .withType(command.getType())
                                       .withWorkingDir(command.getWorkingDir());
    }

    /**
     * Converts {@link ProjectConfig} to {@link ProjectConfigDto}.
     */
    public static ProjectConfigDto asDto(ProjectConfig projectCfg) {
        return newDto(ProjectConfigDto.class)
                .withName(projectCfg.getName())
                .withDescription(projectCfg.getDescription())
                .withPath(projectCfg.getPath())
                .withType(projectCfg.getType())
                .withAttributes(projectCfg.getAttributes())
                .withMixinTypes(projectCfg.getMixinTypes())
                .withStorage(newDto(SourceStorageDto.class)
                                           .withLocation(projectCfg.getStorage().getLocation())
                                           .withType(projectCfg.getStorage().getType())
                                           .withParameters(projectCfg.getStorage().getParameters()));
    }

    //TODO add recipe

    /**
     * Converts {@link Environment} to {@link EnvironmentDto}.
     */
    public static EnvironmentDto asDto(Environment environment) {
        final List<MachineConfigDto> machineConfigs = environment.getMachineConfigs()
                                                                 .stream()
                                                                 .map(DtoConverter::asDto)
                                                                 .collect(toList());
        return newDto(EnvironmentDto.class).withName(environment.getName()).withMachineConfigs(machineConfigs);
    }

    /**
     * Converts {@link MachineConfig} to {@link MachineConfigDto}.
     */
    public static MachineConfigDto asDto(MachineConfig config) {
        return newDto(MachineConfigDto.class).withName(config.getName())
                                             .withType(config.getType())
                                             .withDev(config.isDev())
                                             .withOutputChannel(config.getOutputChannel())
                                             .withStatusChannel(config.getStatusChannel())
                                             .withSource(asDto(config.getSource()));
    }

    /**
     * Converts {@link MachineSource} to {@link MachineSourceDto}.
     */
    public static MachineSourceDto asDto(MachineSource source) {
        return newDto(MachineSourceDto.class).withType(source.getType()).withLocation(source.getLocation());
    }

    private DtoConverter() {}
}
