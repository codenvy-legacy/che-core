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
package org.eclipse.che.api.runner;

import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.core.rest.shared.dto.ServiceDescriptor;
import org.eclipse.che.api.project.shared.dto.RunnerEnvironment;
import org.eclipse.che.api.runner.dto.RunnerDescriptor;
import org.eclipse.che.api.runner.dto.RunnerServer;
import org.eclipse.che.api.runner.dto.ServerState;
import org.eclipse.che.api.runner.internal.Constants;
import org.eclipse.che.dto.server.DtoFactory;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.HttpMethod;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author andrew00x
 */
@Listeners(value = {MockitoTestNGListener.class})
public class RunnerAdminServiceTest {
    private DtoFactory dtoFactory = DtoFactory.getInstance();
    @Mock
    private RunQueue           runQueue;
    @InjectMocks
    private RunnerAdminService service;

    @BeforeMethod
    public void beforeMethod() {
        doNothing().when(runQueue).checkStarted();
    }

    @AfterMethod
    public void afterMethod() {
    }

    @Test
    public void testGetServers() throws Exception {
        String serverUrl = "http://localhost:8080/server1";
        List<RemoteRunnerServer> servers = new ArrayList<>(1);
        RemoteRunnerServer server1 = mock(RemoteRunnerServer.class);
        doReturn(serverUrl).when(server1).getBaseUrl();
        doReturn(dto(ServiceDescriptor.class)).when(server1).getServiceDescriptor();
        servers.add(server1);
        List<RunnerDescriptor> runners = new ArrayList<>(1);
        RunnerDescriptor runner1 = dto(RunnerDescriptor.class).withName("java/web");
        runner1.getEnvironments().add(dto(RunnerEnvironment.class).withId("tomcat7"));
        runner1.getEnvironments().add(dto(RunnerEnvironment.class).withId("jboss7"));
        runners.add(runner1);
        doReturn(runners).when(server1).getRunnerDescriptors();
        doReturn(servers).when(runQueue).getRegisterRunnerServers();

        List<RunnerServer> _servers = service.getRegisteredServers();
        assertEquals(_servers.size(), 1);
        RunnerServer _server = _servers.get(0);
        assertEquals(_server.getUrl(), serverUrl);
    }

    @Test
    public void testGetServersIfOneServerUnavailable() throws Exception {
        String serverUrl1 = "http://localhost:8080/server1";
        List<RemoteRunnerServer> servers = new ArrayList<>(1);
        RemoteRunnerServer server1 = mock(RemoteRunnerServer.class);
        doReturn(serverUrl1).when(server1).getBaseUrl();
        ServiceDescriptor serviceDescriptor = dto(ServiceDescriptor.class);
        Link availableLink = dto(Link.class).withRel(Constants.LINK_REL_AVAILABLE_RUNNERS).withMethod(HttpMethod.GET).withHref(
                serverUrl1 + "/available");
        Link serverStateLink = dto(Link.class).withRel(Constants.LINK_REL_SERVER_STATE).withMethod(HttpMethod.GET).withHref(
                serverUrl1 + "/server-state");
        Link runnerStateLink = dto(Link.class).withRel(Constants.LINK_REL_RUNNER_STATE).withMethod(HttpMethod.GET).withHref(serverUrl1 + "/state");
        serviceDescriptor.getLinks().add(availableLink);
        serviceDescriptor.getLinks().add(serverStateLink);
        serviceDescriptor.getLinks().add(runnerStateLink);
        doReturn(serviceDescriptor).when(server1).getServiceDescriptor();
        doReturn(availableLink).when(server1).getLink(eq(Constants.LINK_REL_AVAILABLE_RUNNERS));
        doReturn(serverStateLink).when(server1).getLink(eq(Constants.LINK_REL_SERVER_STATE));
        doReturn(runnerStateLink).when(server1).getLink(eq(Constants.LINK_REL_RUNNER_STATE));
        doReturn(dto(ServerState.class)).when(server1).getServerState();
        servers.add(server1);
        List<RunnerDescriptor> runners = new ArrayList<>(1);
        RunnerDescriptor runner1 = dto(RunnerDescriptor.class).withName("java/web");
        runner1.getEnvironments().add(dto(RunnerEnvironment.class).withId("tomcat7"));
        runner1.getEnvironments().add(dto(RunnerEnvironment.class).withId("jboss7"));
        runners.add(runner1);
        doReturn(runners).when(server1).getRunnerDescriptors();

        RemoteRunnerServer server2 = mock(RemoteRunnerServer.class);
        String serverUrl2 = "http://localhost:8080/server2";
        doReturn(serverUrl2).when(server2).getBaseUrl();
        doThrow(new RunnerException("Connection refused")).when(server2).getRunnerDescriptors();
        doThrow(new RunnerException("Connection refused")).when(server2).getServiceDescriptor();
        doThrow(new RunnerException("Connection refused")).when(server2).getServerState();
        servers.add(server2);

        doReturn(servers).when(runQueue).getRegisterRunnerServers();

        List<RunnerServer> _servers = service.getRegisteredServers();
        assertEquals(_servers.size(), 2);
        RunnerServer _server1 = _servers.get(0);
        assertEquals(_server1.getUrl(), serverUrl1);
        assertNotNull(_server1.getServerState());
        assertEquals(_server1.getLinks().size(), 3);

        // provide minimal info when something wrong with server
        RunnerServer _server2 = _servers.get(1);
        assertEquals(_server2.getUrl(), serverUrl2);
        assertNull(_server2.getServerState());
        assertTrue(_server2.getLinks().isEmpty());
    }

    private <T> T dto(Class<T> type) {
        return dtoFactory.createDto(type);
    }
}
