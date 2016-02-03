/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.workspace.server.stack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;

import org.eclipse.che.api.core.model.machine.Command;
import org.eclipse.che.api.core.model.machine.Limits;
import org.eclipse.che.api.core.model.machine.MachineConfig;
import org.eclipse.che.api.core.model.machine.MachineSource;
import org.eclipse.che.api.core.model.machine.Recipe;
import org.eclipse.che.api.core.model.workspace.EnvironmentState;
import org.eclipse.che.api.core.model.workspace.ProjectConfig;
import org.eclipse.che.api.core.model.workspace.WorkspaceConfig;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.machine.shared.dto.CommandDto;
import org.eclipse.che.api.machine.shared.dto.LimitsDto;
import org.eclipse.che.api.machine.shared.dto.MachineConfigDto;
import org.eclipse.che.api.machine.shared.dto.MachineSourceDto;
import org.eclipse.che.api.workspace.server.dao.StackDao;
import org.eclipse.che.api.workspace.server.model.impl.stack.StackComponent;
import org.eclipse.che.api.workspace.server.model.impl.stack.DecoratedStackImpl;
import org.eclipse.che.api.workspace.server.model.impl.stack.StackSource;
import org.eclipse.che.api.workspace.server.stack.adapters.CommandSerializer;
import org.eclipse.che.api.workspace.server.stack.adapters.EnvironmentStateSerializer;
import org.eclipse.che.api.workspace.server.stack.adapters.LimitsSerializer;
import org.eclipse.che.api.workspace.server.stack.adapters.MachineSourceSerializer;
import org.eclipse.che.api.workspace.server.stack.adapters.ProjectConfigSerializer;
import org.eclipse.che.api.workspace.server.stack.adapters.RecipeSerializer;
import org.eclipse.che.api.workspace.server.stack.adapters.StackComponentSerializer;
import org.eclipse.che.api.workspace.server.stack.adapters.StackSourceSerializer;
import org.eclipse.che.api.workspace.server.stack.adapters.WorkspaceConfigSerializer;
import org.eclipse.che.api.workspace.shared.dto.EnvironmentDto;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.api.workspace.shared.dto.ProjectProblemDto;
import org.eclipse.che.api.workspace.shared.dto.RecipeDto;
import org.eclipse.che.api.workspace.shared.dto.SourceStorageDto;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceConfigDto;
import org.eclipse.che.api.workspace.shared.dto.stack.StackComponentDto;
import org.eclipse.che.api.workspace.shared.dto.stack.StackDtoDescriptor;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.che.dto.server.DtoFactory.newDto;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

/**
 * Test for {@link StackLoader}
 *
 * @author Alexander Andrienko
 */
@Listeners(MockitoTestNGListener.class)
public class StackLoaderTest {

    @Mock
    private StackDao stackDao;

    private StackLoader stackLoader;

    @Test
    public void predefinedStackWithValidJsonShouldBeLoaded() throws ServerException, NotFoundException, ConflictException {
        URL url = Thread.currentThread().getContextClassLoader().getResource("stacks.json");
        stackLoader = new StackLoader(Collections.singleton(url.getPath()), stackDao);

        stackLoader.start();
        verify(stackDao, times(2)).update(any());
    }

    @Test
    public void predefinedStackWithValidJsonShouldBeLoaded2() throws ServerException, NotFoundException, ConflictException {
        String resource = "stacks.json";
        stackLoader = new StackLoader(Collections.singleton(resource), stackDao);

        stackLoader.start();
        verify(stackDao, times(2)).update(any());
    }

    @Test(expectedExceptions = ServerException.class, expectedExceptionsMessageRegExp = "Failed to get stacks from specified path.*")
    public void shouldThrowExceptionWhenLoadPredefinedStacksFromInvalidJson() throws ServerException {
        URL url = Thread.currentThread().getContextClassLoader().getResource("invalid-json.json");
        stackLoader = new StackLoader(Collections.singleton(url.getPath()), stackDao);

        stackLoader.start();
    }

    @Test(expectedExceptions = ServerException.class, expectedExceptionsMessageRegExp = "Failed to store stack.*")
    public void shouldThrowExceptionWhenImpossibleToStoreRecipes() throws Exception {
        URL url = Thread.currentThread().getContextClassLoader().getResource("stacks.json");
        if (url != null) {
            stackLoader = new StackLoader(Collections.singleton(url.getPath()), stackDao);
        }
        doThrow(NotFoundException.class).when(stackDao).update(any());
        doThrow(ConflictException.class).when(stackDao).create(any());

        stackLoader.start();
    }

    @Test
    public void dtoShouldBeSerialized() {
        StackDtoDescriptor stackDtoDescriptor = newDto(StackDtoDescriptor.class).withName("nameWorkspaceConfig");
        StackComponentDto stackComponentDto = newDto(StackComponentDto.class)
                                                        .withName("java")
                                                        .withVersion("1.8");
        stackDtoDescriptor.setComponents(Collections.singletonList(stackComponentDto));
        stackDtoDescriptor.setTags(Arrays.asList("some teg1", "some teg2"));
        stackDtoDescriptor.setDescription("description");
        stackDtoDescriptor.setId("someId");
        stackDtoDescriptor.setScope("scope");
        stackDtoDescriptor.setCreator("Created in Codenvy");

        Map<String, String> attributes = new HashMap<>();
        attributes.put("attribute1", "valute attribute1");
        Link link = newDto(Link.class).withHref("some url")
                                      .withMethod("get")
                                      .withRel("someRel")
                                      .withConsumes("consumes")
                                      .withProduces("produces");


        HashMap<String, List<String>> projectMap = new HashMap<>();
        projectMap.put("test", Arrays.asList("test", "test2"));

        ProjectProblemDto projectProblem = newDto(ProjectProblemDto.class).withCode(100).withMessage("message");
        SourceStorageDto sourceStorageDto = newDto(SourceStorageDto.class).withType("some type")
                                                                          .withParameters(attributes)
                                                                          .withLocation("location");

        ProjectConfigDto moduleConfigDto = newDto(ProjectConfigDto.class).withName("module")
                                                                          .withPath("somePath")
                                                                          .withAttributes(projectMap)
                                                                          .withType("maven type")
                                                                          .withContentRoot("contentRoot")
                                                                          .withDescription("some project description")
                                                                          .withLinks(Collections.singletonList(link))
                                                                          .withMixins(Collections.singletonList("mixin time"))
                                                                          .withProblems(Collections.singletonList(projectProblem))
                                                                          .withSource(sourceStorageDto);

        ProjectConfigDto projectConfigDto = newDto(ProjectConfigDto.class).withName("project")
                                                                          .withPath("somePath")
                                                                          .withAttributes(projectMap)
                                                                          .withType("maven type")
                                                                          .withContentRoot("contentRoot")
                                                                          .withDescription("some project description")
                                                                          .withLinks(Collections.singletonList(link))
                                                                          .withMixins(Collections.singletonList("mixin time"))
                                                                          .withProblems(Collections.singletonList(projectProblem))
                                                                          .withSource(sourceStorageDto)
                                                                          .withModules(Collections.singletonList(moduleConfigDto));


        RecipeDto recipeDto = newDto(RecipeDto.class).withType("type").withScript("script");

        LimitsDto limitsDto = newDto(LimitsDto.class).withMemory(100);

        MachineSourceDto machineSourceDto = newDto(MachineSourceDto.class).withLocation("location").withType("type");

        MachineConfigDto machineConfig = newDto(MachineConfigDto.class).withDev(true)
                                                                       .withName("machine config name")
                                                                       .withType("type")
                                                                       .withLimits(limitsDto)
                                                                       .withSource(machineSourceDto);

        EnvironmentDto environmentDto = newDto(EnvironmentDto.class).withName("name")
                                                                    .withRecipe(recipeDto)
                                                                    .withMachineConfigs(Collections.singletonList(machineConfig));

        CommandDto commandDto = newDto(CommandDto.class).withType("command type")
                                                        .withName("command name")
                                                        .withCommandLine("command line");

        WorkspaceConfigDto workspaceConfigDto = newDto(WorkspaceConfigDto.class).withName("SomeWorkspaceConfig")
                                                                                .withAttributes(attributes)
                                                                                .withDescription("some workspace")
                                                                                .withLinks(Collections.singletonList(link))
                                                                                .withDefaultEnvName("some Default Env name")
                                                                                .withProjects(Collections.singletonList(projectConfigDto))
                                                                                .withEnvironments(Collections.singletonMap("test", environmentDto))
                                                                                .withCommands(Collections.singletonList(commandDto));

        stackDtoDescriptor.setWorkspaceConfig(workspaceConfigDto);
        Gson GSON = new GsonBuilder().registerTypeAdapter(StackComponent.class, new StackComponentSerializer())
                                     .registerTypeAdapter(WorkspaceConfig.class, new WorkspaceConfigSerializer())
                                     .registerTypeAdapter(ProjectConfig.class, new ProjectConfigSerializer())
                                     .registerTypeAdapter(EnvironmentState.class, new EnvironmentStateSerializer())
                                     .registerTypeAdapter(Command.class, new CommandSerializer())
                                     .registerTypeAdapter(Recipe.class, new RecipeSerializer())
                                     .registerTypeAdapter(Limits.class, new LimitsSerializer())
                                     .registerTypeAdapter(MachineSource.class, new MachineSourceSerializer())
                                     .registerTypeAdapter(MachineConfig.class, new MachineSourceSerializer())
                                     .registerTypeAdapter(StackSource.class, new StackSourceSerializer())
                                     .create();

        GSON.fromJson(stackDtoDescriptor.toString(), DecoratedStackImpl.class);
    }
}
