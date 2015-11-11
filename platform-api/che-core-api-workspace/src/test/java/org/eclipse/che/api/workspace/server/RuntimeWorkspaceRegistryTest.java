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

import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.machine.server.MachineManager;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.model.machine.MachineConfig;
import org.eclipse.che.api.core.model.machine.Recipe;
import org.eclipse.che.api.machine.server.model.impl.LimitsImpl;
import org.eclipse.che.api.machine.server.model.impl.MachineImpl;
import org.eclipse.che.api.machine.server.model.impl.MachineStateImpl;
import org.eclipse.che.api.workspace.server.model.impl.EnvironmentStateImpl;
import org.eclipse.che.api.workspace.server.model.impl.RuntimeWorkspaceImpl;
import org.eclipse.che.commons.lang.NameGenerator;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Collections;

import static java.util.Collections.singletonList;
import static org.eclipse.che.api.core.model.workspace.WorkspaceStatus.RUNNING;
import static org.eclipse.che.api.core.model.workspace.WorkspaceStatus.STOPPING;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;

//TODO Cover all methods and cases with tests

/**
 * Tests for {@link RuntimeWorkspaceRegistry}.
 *
 * @author Eugene Voevodin
 */
@Listeners(value = {MockitoTestNGListener.class})
public class RuntimeWorkspaceRegistryTest {

    @Mock
    MachineManager machineManager;

    RuntimeWorkspaceRegistry registry;

    @BeforeMethod
    public void setUp() throws Exception {
        when(machineManager.createMachineSync(any(), any(), any())).thenAnswer(invocation -> {
            MachineConfig cfg = (MachineConfig)invocation.getArguments()[0];
            return MachineImpl.builder()
                              .setId(NameGenerator.generate("machine", 10))
                              .setType(cfg.getType())
                              .setName(cfg.getName())
                              .setDev(cfg.isDev())
                              .setSource(cfg.getSource())
                              .setLimits(new LimitsImpl(cfg.getLimits()))
                              .build();
        });
        registry = new RuntimeWorkspaceRegistry(machineManager);
    }

    @Test(expectedExceptions = ConflictException.class,
          expectedExceptionsMessageRegExp = "Could not start workspace '.*' because its status is '.*'")
    public void shouldNotStartRunningWorkspaces() throws Exception {
        final RuntimeWorkspaceImpl workspace = createWorkspace();

        registry.start(workspace, workspace.getDefaultEnvName());
        registry.start(workspace, workspace.getDefaultEnvName());
    }

    @Test
    public void shouldStartWorkspace() throws Exception {
        final RuntimeWorkspaceImpl workspace = createWorkspace();

        final RuntimeWorkspaceImpl running = registry.start(workspace, workspace.getDefaultEnvName());

        assertEquals(running.getStatus(), RUNNING);
        assertNotNull(running.getDevMachine());
        assertFalse(running.getMachines().isEmpty());
    }

    @Test
    public void shouldUpdateRunningWorkspace() throws Exception {
        final RuntimeWorkspaceImpl workspace = createWorkspace();
        registry.start(workspace, workspace.getDefaultEnvName());

        final RuntimeWorkspaceImpl running = registry.get(workspace.getId());
        running.setStatus(STOPPING);
        final MachineImpl newDev = mock(MachineImpl.class);
        running.getMachines().add(newDev);
        running.setDevMachine(newDev);
        registry.update(running);

        final RuntimeWorkspaceImpl updated = registry.get(workspace.getId());
        assertEquals(updated.getStatus(), STOPPING);
        assertEquals(updated.getMachines().size(), running.getMachines().size());
        assertEquals(updated.getDevMachine().getId(), newDev.getId());
    }

    @Test(expectedExceptions = NotFoundException.class)
    public void shouldNotUpdateWorkspaceWhichIsNotRunning() throws Exception {
        final RuntimeWorkspaceImpl workspace = createWorkspace();

        registry.update(workspace);
    }

    @Test(expectedExceptions = BadRequestException.class, expectedExceptionsMessageRegExp = "Required non-null environment name")
    public void shouldThrowBadRequestExceptionWhenStartingWorkspaceWithNullEnvironment() throws Exception {
        registry.start(createWorkspace(), null);
    }

    private static RuntimeWorkspaceImpl createWorkspace() {
        // prepare default environment
        final EnvironmentStateImpl envState = mock(EnvironmentStateImpl.class, RETURNS_MOCKS);

        final Recipe recipe = mock(Recipe.class);
        when(recipe.getType()).thenReturn("docker");
        when(envState.getRecipe()).thenReturn(recipe);

        final MachineStateImpl cfg = mock(MachineStateImpl.class);
        when(cfg.isDev()).thenReturn(true);
        when(envState.getMachineConfigs()).thenReturn((singletonList(cfg)));

        // prepare workspace
        final RuntimeWorkspaceImpl workspace = mock(RuntimeWorkspaceImpl.class, RETURNS_MOCKS);
        when(workspace.getEnvironments()).thenReturn(Collections.singletonMap("", envState));
        when(workspace.getDevMachine()).thenReturn(mock(MachineImpl.class, RETURNS_MOCKS));
        when(workspace.getId()).thenReturn(NameGenerator.generate("workspace", 10));

        return workspace;
    }
}
