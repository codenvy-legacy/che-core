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


import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.model.workspace.UsersWorkspace;
import org.eclipse.che.api.core.model.workspace.WorkspaceConfig;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.machine.shared.dto.MachineConfigDto;
import org.eclipse.che.api.machine.shared.dto.MachineSourceDto;
import org.eclipse.che.api.workspace.server.model.impl.UsersWorkspaceImpl;
import org.eclipse.che.api.workspace.server.spi.WorkspaceDao;
import org.eclipse.che.api.workspace.shared.dto.EnvironmentDto;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceConfigDto;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.user.User;
import org.eclipse.che.commons.user.UserImpl;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.eclipse.che.api.core.model.workspace.WorkspaceStatus.RUNNING;
import static org.eclipse.che.api.core.model.workspace.WorkspaceStatus.STARTING;
import static org.eclipse.che.api.core.model.workspace.WorkspaceStatus.STOPPED;
import static org.eclipse.che.dto.server.DtoFactory.newDto;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;

//TODO cover WorkspaceManager with tests when API is established

/**
 * Covers main cases of {@link WorkspaceManager}.
 *
 * @author Eugene Voevodin
 */
@Listeners(value = {MockitoTestNGListener.class})
public class WorkspaceManagerTest {

    @Mock
    EventService             eventService;
    @Mock
    WorkspaceDao             workspaceDao;
    @Mock
    WorkspaceHooks           workspaceHooks;
    @Mock
    RuntimeWorkspaceRegistry registry;

    WorkspaceManager   manager;
    EnvironmentContext environmentContext;

    @BeforeMethod
    public void setUpManager() throws Exception {
        manager = spy(new WorkspaceManager(workspaceDao, registry, eventService));
        manager.setHooks(workspaceHooks);

        when(workspaceDao.create(any(UsersWorkspaceImpl.class))).thenAnswer(invocation -> invocation.getArguments()[0]);
        when(workspaceDao.update(any(UsersWorkspaceImpl.class))).thenAnswer(invocation -> invocation.getArguments()[0]);

        EnvironmentContext.setCurrent(environmentContext = new EnvironmentContext() {
            @Override
            public User getUser() {
                return new UserImpl("name", "id", "token", new ArrayList<>(), false);
            }
        });
    }

    @Test
    public void shouldBeAbleToCreateWorkspace() throws Exception {
        final WorkspaceConfig cfg = createConfig();

        final UsersWorkspaceImpl workspace = manager.createWorkspace(cfg, "user123", "account");

        assertNotNull(workspace);
        assertFalse(isNullOrEmpty(workspace.getId()));
        assertEquals(workspace.getOwner(), "user123");
        assertEquals(workspace.getName(), cfg.getName());
        assertEquals(workspace.getStatus(), STOPPED);
        verify(workspaceHooks).beforeCreate(workspace, "account");
        verify(workspaceHooks).afterCreate(workspace, "account");
        verify(workspaceDao).create(workspace);
    }

    @Test
    public void shouldBeAbleToUpdateWorkspace() throws Exception {
        final UsersWorkspaceImpl workspace = manager.createWorkspace(createConfig(), "user123", "account");
        when(workspaceDao.get(workspace.getId())).thenReturn(workspace);
        when(registry.get(any())).thenThrow(new NotFoundException(""));
        final WorkspaceConfig update = createConfig();

        UsersWorkspace updated = manager.updateWorkspace(workspace.getId(), update);

        verify(workspaceDao).update(any(UsersWorkspaceImpl.class));
        assertNotNull(updated.getStatus());
    }

    @Test
    public void shouldBeAbleToDeleteWorkspace() throws Exception {
        manager.removeWorkspace("workspace123");

        verify(workspaceDao).remove("workspace123");
        verify(workspaceHooks).afterRemove("workspace123");
    }

    @Test
    public void shouldBeAbleToStartWorkspace() throws Exception {
        final UsersWorkspaceImpl workspace = manager.createWorkspace(createConfig(), "user123", "account");
        when(workspaceDao.get(workspace.getId())).thenReturn(workspace);
        doNothing().when(manager).startWorkspaceAsync(workspace, null);

        final UsersWorkspace workspace2 = manager.startWorkspaceById(workspace.getId(), null, null);

        assertEquals(workspace2.getStatus(), STARTING);
        verify(manager).startWorkspaceAsync(workspace, null);
        verify(workspaceHooks).beforeStart(workspace, null);
    }

    @Test(enabled = false)
    public void shouldBeAbleToStartTemporaryWorkspace() throws Exception {
        final WorkspaceConfig config = createConfig();
        final UsersWorkspace workspace = manager.createWorkspace(config, "user123", "account");
        doReturn(workspace).when(manager).fromConfig(config);

        final UsersWorkspaceImpl workspace2 = manager.startTemporaryWorkspace(config, "account");

        assertEquals(workspace2.getStatus(), RUNNING);
        verify(manager).startWorkspaceSync(workspace2, null);
        verify(workspaceHooks).beforeStart(workspace, "account");
    }

    @Test
    public void shouldBeAbleToStopWorkspace() throws Exception {
        doNothing().when(manager).stopWorkspaceAsync("workspace123");

        manager.stopWorkspace("workspace123");

        verify(manager).stopWorkspaceAsync("workspace123");
    }

    private static WorkspaceConfig createConfig() {
        MachineConfigDto devMachine = newDto(MachineConfigDto.class).withDev(true)
                                                                    .withName("dev-machine")
                                                                    .withType("docker")
                                                                    .withSource(newDto(MachineSourceDto.class).withLocation("location")
                                                                                                              .withType("recipe"));
        EnvironmentDto devEnv = newDto(EnvironmentDto.class).withName("dev-env")
                                                            .withMachineConfigs(new ArrayList<>(singletonList(devMachine)))
                                                            .withRecipe(null);
        return newDto(WorkspaceConfigDto.class).withName("dev-workspace")
                                               .withEnvironments(new HashMap<>(singletonMap("dev-env", devEnv)))
                                               .withDefaultEnvName("dev-env");
    }
}
