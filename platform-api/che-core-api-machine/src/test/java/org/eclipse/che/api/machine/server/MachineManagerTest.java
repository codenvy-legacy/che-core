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

import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;

/**
 * @author Anton Korneta
 */
@Listeners(MockitoTestNGListener.class)
public class MachineManagerTest {
/*    private static final int DEFAULT_MACHINE_MEMORY_SIZE_MB = 1000;

    private MachineManager manager;

    @Mock
    private MachineInstanceProviders machineInstanceProviders;
    @Mock
    private MachineRegistry          machineRegistry;

    @BeforeMethod
    public void setUp() throws Exception {
        final SnapshotDao snapshotDao = mock(SnapshotDao.class);
        final EventService eventService = mock(EventService.class);
        final String machineLogsDir = targetDir().resolve("logs-dir").toString();
        manager = spy(new MachineManager(snapshotDao,
                                         machineRegistry,
                                         machineInstanceProviders,
                                         machineLogsDir,
                                         eventService,
                                         DEFAULT_MACHINE_MEMORY_SIZE_MB,
                                         "apiEndpoint"));

        EnvironmentContext envCont = new EnvironmentContext();
        envCont.setUser(new UserImpl("user", null, null, null, false));
        EnvironmentContext.setCurrent(envCont);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        EnvironmentContext.reset();
    }

    @Test(expectedExceptions = BadRequestException.class, expectedExceptionsMessageRegExp = "Invalid machine name @name!")
    public void shouldThrowExceptionWhileValidatingDockerDisplayName() throws Exception {
        final RecipeMachineCreationMetadata machineCreationMetadata = mock(RecipeMachineCreationMetadata.class);
        final MachineRecipe machineRecipe = mock(MachineRecipe.class);
        final InstanceProvider instanceProvider = mock(InstanceProvider.class);
        final String type = "type";
        final Set<String> recipeTypes = new HashSet<>(Collections.singleton(type));
        when(machineCreationMetadata.getRecipe()).thenReturn(machineRecipe);
        when(machineCreationMetadata.getType()).thenReturn(type);
        when(machineInstanceProviders.getProvider(any())).thenReturn(instanceProvider);
        when(machineRecipe.getType()).thenReturn(type);
        when(instanceProvider.getRecipeTypes()).thenReturn(recipeTypes);
        when(machineCreationMetadata.getName()).thenReturn("@name!");

        manager.create(machineCreationMetadata, false);
    }

    @Test
    public void shouldCreateInstanceWithValidDockerMachineDisplayName() throws Exception {
        final String machineDisplayName = "machineName";
        final RecipeMachineCreationMetadata machineCreationMetadata = mock(RecipeMachineCreationMetadata.class);
        final MachineRecipe machineRecipe = mock(MachineRecipe.class);
        final InstanceProvider instanceProvider = mock(InstanceProvider.class);
        final String type = "type";
        final String outputChannel = "channel";
        final Instance instance = mock(Instance.class);
        final Set<String> recipeTypes = new HashSet<>(Collections.singleton(type));
        final List<MachineImpl> machines = Collections.emptyList();
        when(machineCreationMetadata.getRecipe()).thenReturn(machineRecipe);
        when(machineCreationMetadata.getType()).thenReturn(type);
        when(machineInstanceProviders.getProvider(any())).thenReturn(instanceProvider);
        when(machineRecipe.getType()).thenReturn(type);
        when(instanceProvider.getRecipeTypes()).thenReturn(recipeTypes);
        when(machineCreationMetadata.getName()).thenReturn(machineDisplayName);
        when(machineRegistry.getStates()).thenReturn(machines);
        when(machineCreationMetadata.getOutputChannel()).thenReturn(outputChannel);
        doNothing().when(machineRegistry).add(any(MachineImpl.class));
        when(instanceProvider.createInstance(any(Recipe.class),
                                             anyString(),
                                             anyString(),
                                             anyString(),
                                             anyBoolean(),
                                             anyString(),
                                             anyInt(),
                                             any(LineConsumer.class))).thenReturn(instance);
        doNothing().when(instance).setStatus(any(MachineStatus.class));


        String result = manager.create(machineCreationMetadata, false).getDisplayName();

        assertEquals(machineDisplayName, result);
    }

    private static Path targetDir() throws Exception {
        final URL url = Thread.currentThread().getContextClassLoader().getResource(".");
        assertNotNull(url);
        return Paths.get(url.toURI()).getParent();
    }*/
}
