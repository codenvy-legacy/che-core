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
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.model.machine.Channels;
import org.eclipse.che.api.core.model.machine.Limits;
import org.eclipse.che.api.core.model.machine.MachineConfig;
import org.eclipse.che.api.core.model.machine.MachineMetadata;
import org.eclipse.che.api.core.model.machine.MachineSource;
import org.eclipse.che.api.core.model.machine.MachineState;
import org.eclipse.che.api.core.model.machine.MachineStatus;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.core.util.LineConsumer;
import org.eclipse.che.api.machine.server.dao.SnapshotDao;
import org.eclipse.che.api.machine.server.exception.MachineException;
import org.eclipse.che.api.machine.server.impl.AbstractInstance;
import org.eclipse.che.api.machine.server.impl.SnapshotImpl;
import org.eclipse.che.api.machine.server.model.impl.ChannelsImpl;
import org.eclipse.che.api.machine.server.model.impl.LimitsImpl;
import org.eclipse.che.api.machine.server.model.impl.MachineConfigImpl;
import org.eclipse.che.api.machine.server.model.impl.MachineImpl;
import org.eclipse.che.api.machine.server.model.impl.MachineSourceImpl;
import org.eclipse.che.api.machine.server.recipe.RecipeImpl;
import org.eclipse.che.api.machine.server.spi.Instance;
import org.eclipse.che.api.machine.server.spi.InstanceKey;
import org.eclipse.che.api.machine.server.spi.InstanceNode;
import org.eclipse.che.api.machine.server.spi.InstanceProcess;
import org.eclipse.che.api.machine.server.spi.InstanceProvider;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.user.UserImpl;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author Anton Korneta
 */
@Listeners(MockitoTestNGListener.class)
public class MachineManagerTest {
    private static final int DEFAULT_MACHINE_MEMORY_SIZE_MB = 1000;

    private MachineManager manager;

    @Mock
    private MachineInstanceProviders machineInstanceProviders;
    @Mock
    private InstanceProvider         instanceProvider;
    @Mock
    private MachineRegistry          machineRegistry;
    @Mock
    private Instance                 instance;

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
    public void shouldThrowExceptionOnMachineCreationIfMachineNameIsInvalid() throws Exception {
        doReturn(new RecipeImpl().withScript("script").withType("Dockerfile"))
                .when(manager).getRecipeByLocation(any(MachineConfig.class));

        MachineConfig machineConfig = new MachineConfigImpl(false,
                                                            "@name!",
                                                            "machineType",
                                                            new MachineSourceImpl("Recipe", "location"),
                                                            new LimitsImpl(1024));
        String workspaceId = "wsId";
        String environmentName = "env1";

        manager.createMachineSync(machineConfig, workspaceId, environmentName);
    }

    @Test
    public void shouldBeAbleToCreateMachineWithValidName() throws Exception {
        String expectedName = "MachineName";
        String workspaceId = "wsId";
        String environmentName = "env1";
        RecipeImpl recipe = new RecipeImpl().withScript("script").withType("Dockerfile");
        MachineConfig machineConfig = new MachineConfigImpl(false,
                                                            expectedName,
                                                            "docker",
                                                            new MachineSourceImpl("Recipe", "location"),
                                                            new LimitsImpl(1024));
        final NoOpInstanceImpl noOpInstance = new NoOpInstanceImpl("machineId",
                                                                   machineConfig.getType(),
                                                                   workspaceId,
                                                                   "owner",
                                                                   machineConfig.isDev(),
                                                                   machineConfig.getName(),
                                                                   new ChannelsImpl("chan1", "chan2"),
                                                                   machineConfig.getLimits(),
                                                                   machineConfig.getSource(),
                                                                   MachineStatus.CREATING,
                                                                   environmentName);
        doReturn(recipe).when(manager).getRecipeByLocation(any(MachineConfig.class));
        when(machineInstanceProviders.getProvider(anyString())).thenReturn(instanceProvider);
        when(instanceProvider.createInstance(eq(recipe), any(MachineState.class), any(LineConsumer.class))).thenReturn(noOpInstance);
        when(machineRegistry.get(anyString())).thenReturn(noOpInstance);

        final MachineImpl machine = manager.createMachineSync(machineConfig, workspaceId, environmentName);

        assertEquals(machine.getName(), expectedName);
    }

    private static Path targetDir() throws Exception {
        final URL url = Thread.currentThread().getContextClassLoader().getResource(".");
        assertNotNull(url);
        return Paths.get(url.toURI()).getParent();
    }

    private static class NoOpInstanceImpl extends AbstractInstance {

        public NoOpInstanceImpl(String id,
                                String type,
                                String workspaceId,
                                String owner,
                                boolean isDev,
                                String displayName,
                                Channels channels,
                                Limits limits,
                                MachineSource source,
                                MachineStatus machineStatus,
                                String envName) {
            super(id, type, workspaceId, owner, isDev, displayName, channels, limits, source, machineStatus, envName);
        }

        public NoOpInstanceImpl(MachineState machineState) {
            super(machineState);
        }

        @Override
        public LineConsumer getLogger() {
            return null;
        }

        @Override
        public InstanceProcess getProcess(int pid) throws NotFoundException, MachineException {
            return null;
        }

        @Override
        public List<InstanceProcess> getProcesses() throws MachineException {
            return null;
        }

        @Override
        public InstanceProcess createProcess(String commandLine) throws MachineException {
            return null;
        }

        @Override
        public InstanceKey saveToSnapshot(String owner) throws MachineException {
            return null;
        }

        @Override
        public void destroy() throws MachineException {

        }

        @Override
        public InstanceNode getNode() {
            return null;
        }

        @Override
        public String readFileContent(String filePath, int startFrom, int limit) throws MachineException {
            return null;
        }

        @Override
        public void copy(Instance sourceMachine, String sourcePath, String targetPath, boolean overwrite) throws MachineException {

        }

        @Override
        public MachineMetadata getMetadata() {
            return null;
        }
    }
}
