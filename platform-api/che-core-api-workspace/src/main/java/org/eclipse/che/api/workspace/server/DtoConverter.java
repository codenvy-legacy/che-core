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
import org.eclipse.che.api.core.model.project.SourceStorage;
import org.eclipse.che.api.core.model.workspace.Environment;
import org.eclipse.che.api.core.model.workspace.EnvironmentState;
import org.eclipse.che.api.core.model.workspace.ModuleConfig;
import org.eclipse.che.api.core.model.workspace.ProjectConfig;
import org.eclipse.che.api.core.model.workspace.RuntimeWorkspace;
import org.eclipse.che.api.core.model.workspace.UsersWorkspace;
import org.eclipse.che.api.core.model.workspace.WorkspaceConfig;
import org.eclipse.che.api.machine.shared.dto.CommandDto;
import org.eclipse.che.api.machine.shared.dto.MachineConfigDto;
import org.eclipse.che.api.machine.shared.dto.MachineDto;
import org.eclipse.che.api.machine.shared.dto.MachineStateDto;
import org.eclipse.che.api.workspace.shared.dto.EnvironmentDto;
import org.eclipse.che.api.workspace.shared.dto.EnvironmentStateDto;
import org.eclipse.che.api.workspace.shared.dto.ModuleConfigDto;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.api.workspace.shared.dto.RuntimeWorkspaceDto;
import org.eclipse.che.api.workspace.shared.dto.SourceStorageDto;
import org.eclipse.che.api.workspace.shared.dto.UsersWorkspaceDto;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceConfigDto;

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
        final Map<String, EnvironmentStateDto> environments = workspace.getEnvironments()
                                                                       .values()
                                                                       .stream()
                                                                       .collect(toMap(EnvironmentState::getName, DtoConverter::asDto));

        return newDto(UsersWorkspaceDto.class).withId(workspace.getId())
                                              .withStatus(workspace.getStatus())
                                              .withName(workspace.getName())
                                              .withOwner(workspace.getOwner())
                                              .withDefaultEnvName(workspace.getDefaultEnvName())
                                              .withCommands(commands)
                                              .withProjects(projects)
                                              .withEnvironments(environments)
                                              .withDescription(workspace.getDescription())
                                              .withAttributes(workspace.getAttributes());
    }

    /**
     * Converts {@link WorkspaceConfig} to {@link WorkspaceConfigDto}.
     */
    public static WorkspaceConfigDto asDto(WorkspaceConfig workspace) {
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

        return newDto(WorkspaceConfigDto.class).withName(workspace.getName())
                                               .withDefaultEnvName(workspace.getDefaultEnvName())
                                               .withCommands(commands)
                                               .withProjects(projects)
                                               .withEnvironments(environments)
                                               .withDescription(workspace.getDescription())
                                               .withAttributes(workspace.getAttributes());
    }

    /**
     * Converts {@link Command} to {@link CommandDto}.
     */
    public static CommandDto asDto(Command command) {
        return newDto(CommandDto.class).withName(command.getName())
                                       .withCommandLine(command.getCommandLine())
                                       .withType(command.getType());
    }

    /**
     * Converts {@link ProjectConfig} to {@link ProjectConfigDto}.
     */
    public static ProjectConfigDto asDto(ProjectConfig projectCfg) {
        final ProjectConfigDto projectConfigDto = newDto(ProjectConfigDto.class).withName(projectCfg.getName())
                                                                                .withDescription(projectCfg.getDescription())
                                                                                .withPath(projectCfg.getPath())
                                                                                .withType(projectCfg.getType())
                                                                                .withAttributes(projectCfg.getAttributes())
                                                                                .withMixins(projectCfg.getMixins());
        if (projectCfg.getModules() != null) {
            final List<ModuleConfigDto> modules = projectCfg.getModules()
                                                            .stream()
                                                            .map(DtoConverter::asDto)
                                                            .collect(toList());
            projectConfigDto.withModules(modules);
        }
        final SourceStorage source = projectCfg.getSource();
        if (source != null) {
            projectConfigDto.withSource(newDto(SourceStorageDto.class).withLocation(source.getLocation())
                                                                      .withType(source.getType())
                                                                      .withParameters(source.getParameters()));
        }
        return projectConfigDto;
    }


    /**
     * Converts {@link ProjectConfig} to {@link ProjectConfigDto}.
     */
    public static ModuleConfigDto asDto(ModuleConfig moduleConfig) {
        final ModuleConfigDto moduleConfigDto = newDto(ModuleConfigDto.class).withName(moduleConfig.getName())
                                                                                .withDescription(moduleConfig.getDescription())
                                                                                .withPath(moduleConfig.getPath())
                                                                                .withType(moduleConfig.getType())
                                                                                .withAttributes(moduleConfig.getAttributes())
                                                                                .withMixins(moduleConfig.getMixins());
        if (moduleConfig.getModules() != null) {
            final List<ModuleConfigDto> modules = moduleConfig.getModules()
                                                              .stream()
                                                              .map(DtoConverter::asDto)
                                                              .collect(toList());
            moduleConfigDto.withModules(modules);
        }
        return moduleConfigDto;
    }

    //TODO add recipe

    /**
     * Converts {@link Environment} to {@link EnvironmentDto}.
     */
    public static EnvironmentDto asDto(Environment environment) {
        final List<MachineConfigDto> machineConfigs = environment.getMachineConfigs()
                                                                 .stream()
                                                                 .map(org.eclipse.che.api.machine.server.DtoConverter::asDto)
                                                                 .collect(toList());
        return newDto(EnvironmentDto.class).withName(environment.getName()).withMachineConfigs(machineConfigs);
    }

    /**
     * Converts {@link EnvironmentState} to {@link EnvironmentStateDto}.
     */
    public static EnvironmentStateDto asDto(EnvironmentState environment) {
        final List<MachineStateDto> machineConfigs = environment.getMachineConfigs()
                                                                .stream()
                                                                .map(org.eclipse.che.api.machine.server.DtoConverter::asDto)
                                                                .collect(toList());
        return newDto(EnvironmentStateDto.class).withName(environment.getName()).withMachineConfigs(machineConfigs);
    }

    /**
     * Converts {@link RuntimeWorkspace} to {@link RuntimeWorkspaceDto}.
     */
    public static RuntimeWorkspaceDto asDto(RuntimeWorkspace workspace) {
        final List<MachineDto> machines = workspace.getMachines()
                                                   .stream()
                                                   .map(org.eclipse.che.api.machine.server.DtoConverter::asDto)
                                                   .collect(toList());
        final List<CommandDto> commands = workspace.getCommands()
                                                   .stream()
                                                   .map(DtoConverter::asDto)
                                                   .collect(toList());
        final List<ProjectConfigDto> projects = workspace.getProjects()
                                                         .stream()
                                                         .map(DtoConverter::asDto)
                                                         .collect(toList());
        final Map<String, EnvironmentStateDto> environments = workspace.getEnvironments()
                                                                       .values()
                                                                       .stream()
                                                                       .collect(toMap(EnvironmentState::getName, DtoConverter::asDto));

        return newDto(RuntimeWorkspaceDto.class).withId(workspace.getId())
                                                .withName(workspace.getName())
                                                .withStatus(workspace.getStatus())
                                                .withOwner(workspace.getOwner())
                                                .withActiveEnvName(workspace.getActiveEnvName())
                                                .withDefaultEnvName(workspace.getDefaultEnvName())
                                                .withCommands(commands)
                                                .withProjects(projects)
                                                .withEnvironments(environments)
                                                .withAttributes(workspace.getAttributes())
                                                .withDevMachine(
                                                        org.eclipse.che.api.machine.server.DtoConverter.asDto(workspace.getDevMachine()))
                                                .withRootFolder(workspace.getRootFolder())
                                                .withMachines(machines)
                                                .withDescription(workspace.getDescription());
    }

    private DtoConverter() {
    }
}
