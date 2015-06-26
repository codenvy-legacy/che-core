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

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.project.shared.dto.RunnerEnvironment;
import org.eclipse.che.api.project.shared.dto.RunnerEnvironmentLeaf;
import org.eclipse.che.api.project.shared.dto.RunnerEnvironmentTree;
import org.eclipse.che.api.runner.dto.ApplicationProcessDescriptor;
import org.eclipse.che.api.runner.dto.RunRequest;
import org.eclipse.che.api.runner.dto.RunnerDescriptor;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.user.User;
import org.eclipse.che.commons.user.UserImpl;
import org.eclipse.che.dto.server.DtoFactory;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author andrew00x
 */
@Listeners(value = {MockitoTestNGListener.class})
public class RunnerServiceTest {
    private DtoFactory dtoFactory = DtoFactory.getInstance();
    @Mock
    private RunQueue runQueue;
    @InjectMocks
    private RunnerService service;

    @BeforeMethod
    public void beforeMethod() {
        // define environment
        EnvironmentContext.reset();

        doNothing().when(runQueue).checkStarted();
    }

    @AfterMethod
    public void afterMethod() {
        // cleanup environment
        EnvironmentContext.reset();
    }

    @Test
    public void testGetRunnerEnvironments() throws Exception {
        List<RemoteRunnerServer> servers = new ArrayList<>(1);
        RemoteRunnerServer server1 = mock(RemoteRunnerServer.class);
        doReturn(true).when(server1).isAvailable();
        servers.add(server1);
        List<RunnerDescriptor> runners1 = new ArrayList<>(1);
        RunnerDescriptor runner1 = dto(RunnerDescriptor.class).withName("java/web");
        runner1.getEnvironments().add(dto(RunnerEnvironment.class).withId("tomcat7"));
        runner1.getEnvironments().add(dto(RunnerEnvironment.class).withId("jboss7"));
        runners1.add(runner1);

        RemoteRunnerServer server2 = mock(RemoteRunnerServer.class);
        doReturn(true).when(server2).isAvailable();
        servers.add(server2);
        List<RunnerDescriptor> runners2 = new ArrayList<>(1);
        RunnerDescriptor runner2 = dto(RunnerDescriptor.class).withName("java/web");
        runner2.getEnvironments().add(dto(RunnerEnvironment.class).withId("tomcat7"));
        runner2.getEnvironments().add(dto(RunnerEnvironment.class).withId("jboss7"));
        runners2.add(runner2);

        doReturn(runners1).when(server1).getRunnerDescriptors();
        doReturn(runners2).when(server2).getRunnerDescriptors();
        doReturn(servers).when(runQueue).getRegisterRunnerServers();

        RunnerEnvironmentTree system = service.getRunnerEnvironments(null, null);
        assertEquals(system.getDisplayName(), "system");
        assertEquals(system.getLeaves().size(), 0);
        List<RunnerEnvironmentTree> nodes = system.getNodes();
        assertEquals(nodes.size(), 1);

        RunnerEnvironmentTree java = system.getNode("java");
        assertNotNull(java);
        assertEquals(java.getDisplayName(), "java");
        nodes = java.getNodes();
        assertEquals(nodes.size(), 1);
        assertEquals(java.getLeaves().size(), 0);

        RunnerEnvironmentTree web = java.getNode("web");
        assertNotNull(web);
        assertEquals(web.getNodes().size(), 0);
        assertEquals(web.getLeaves().size(), 2);
        RunnerEnvironmentLeaf tomcat7 = web.getEnvironment("tomcat7");
        assertNotNull(tomcat7);
        RunnerEnvironmentLeaf jboss7 = web.getEnvironment("jboss7");
        assertNotNull(jboss7);
        RunnerEnvironment tomcat7Environment = tomcat7.getEnvironment();
        RunnerEnvironment jboss7Environment = jboss7.getEnvironment();
        assertNotNull(tomcat7Environment);
        assertNotNull(jboss7Environment);
        assertEquals(tomcat7Environment.getId(), "system:/java/web/tomcat7");
        assertEquals(jboss7Environment.getId(), "system:/java/web/jboss7");
    }

    @Test
    public void testGetRunnerEnvironmentsIfOneServerUnavailable() throws Exception {
        List<RemoteRunnerServer> servers = new ArrayList<>(2);
        RemoteRunnerServer server1 = mock(RemoteRunnerServer.class);
        doReturn(true).when(server1).isAvailable();
        servers.add(server1);
        List<RunnerDescriptor> runners = new ArrayList<>(1);
        RunnerDescriptor runner1 = dto(RunnerDescriptor.class).withName("java/web");
        runner1.getEnvironments().add(dto(RunnerEnvironment.class).withId("tomcat7"));
        runner1.getEnvironments().add(dto(RunnerEnvironment.class).withId("jboss7"));
        runners.add(runner1);
        doReturn(runners).when(server1).getRunnerDescriptors();

        RemoteRunnerServer server2 = mock(RemoteRunnerServer.class);
        doReturn(true).when(server2).isAvailable();
        doThrow(new RunnerException("Connection refused")).when(server2).getRunnerDescriptors();
        servers.add(server2);

        doReturn(servers).when(runQueue).getRegisterRunnerServers();

        RunnerEnvironmentTree system = service.getRunnerEnvironments(null, null);

        assertEquals(system.getDisplayName(), "system");
        assertEquals(system.getLeaves().size(), 0);
        List<RunnerEnvironmentTree> nodes = system.getNodes();
        assertEquals(nodes.size(), 1);

        RunnerEnvironmentTree java = system.getNode("java");
        assertNotNull(java);
        assertEquals(java.getDisplayName(), "java");
        nodes = java.getNodes();
        assertEquals(nodes.size(), 1);
        assertEquals(java.getLeaves().size(), 0);

        RunnerEnvironmentTree web = java.getNode("web");
        assertNotNull(web);
        assertEquals(web.getNodes().size(), 0);
        assertEquals(web.getLeaves().size(), 2);
        RunnerEnvironmentLeaf tomcat7 = web.getEnvironment("tomcat7");
        assertNotNull(tomcat7);
        RunnerEnvironmentLeaf jboss7 = web.getEnvironment("jboss7");
        assertNotNull(jboss7);
        RunnerEnvironment tomcat7Environment = tomcat7.getEnvironment();
        RunnerEnvironment jboss7Environment = jboss7.getEnvironment();
        assertNotNull(tomcat7Environment);
        assertNotNull(jboss7Environment);
        assertEquals(tomcat7Environment.getId(), "system:/java/web/tomcat7");
        assertEquals(jboss7Environment.getId(), "system:/java/web/jboss7");
    }

    @Test
    public void testFilerEnvironmentsByWorkspaceGetAll() throws Exception {
        createRunners();
        RunnerEnvironmentTree system = service.getRunnerEnvironments(null, null);
        assertEquals(system.getDisplayName(), "system");
        assertEquals(system.getLeaves().size(), 0);
        List<RunnerEnvironmentTree> nodes = system.getNodes();
        assertEquals(nodes.size(), 1);
        RunnerEnvironmentTree java = system.getNode("java");
        assertNotNull(java);
        RunnerEnvironmentTree web = java.getNode("web");
        assertNotNull(web);
        assertEquals(web.getNodes().size(), 0);
        assertEquals(web.getLeaves().size(), 4);
        RunnerEnvironmentLeaf tomcat7 = web.getEnvironment("tomcat7");
        assertNotNull(tomcat7);
        RunnerEnvironmentLeaf jboss7 = web.getEnvironment("jboss7");
        assertNotNull(jboss7);
        RunnerEnvironmentLeaf _tomcat7 = web.getEnvironment("my_tomcat7");
        assertNotNull(_tomcat7);
        RunnerEnvironmentLeaf _jboss7 = web.getEnvironment("my_jboss7");
        assertNotNull(_jboss7);
    }

    @Test
    public void testFilerEnvironmentsByWorkspaceRestricted() throws Exception {
        List<RemoteRunnerServer> servers = createRunners();
        // first server isn't accessible
        doReturn("my").when(servers.get(0)).getAssignedWorkspace();
        RunnerEnvironmentTree system = service.getRunnerEnvironments(null, null);
        assertEquals(system.getDisplayName(), "system");
        assertEquals(system.getLeaves().size(), 0);
        List<RunnerEnvironmentTree> nodes = system.getNodes();
        assertEquals(nodes.size(), 1);
        RunnerEnvironmentTree java = system.getNode("java");
        assertNotNull(java);
        RunnerEnvironmentTree web = java.getNode("web");
        assertNotNull(web);
        assertEquals(web.getNodes().size(), 0);
        // from private shouldn't be visible
        assertEquals(web.getLeaves().size(), 2);
        RunnerEnvironmentLeaf tomcat7 = web.getEnvironment("tomcat7");
        assertNotNull(tomcat7);
        RunnerEnvironmentLeaf jboss7 = web.getEnvironment("jboss7");
        assertNotNull(jboss7);
    }


    /**
     * Creates a task for given set of user, workspace, project's name
     *
     * @return the created mock object
     */
    private RunQueueTask createTask(User user, String workspace, String projectName) throws NotFoundException, RunnerException {
        RunQueueTask runQueueTask = mock(RunQueueTask.class);
        RunRequest runRequest = mock(RunRequest.class);
        doReturn(runRequest).when(runQueueTask).getRequest();
        doReturn(user.getId()).when(runRequest).getUserId();
        doReturn(workspace).when(runRequest).getWorkspace();
        doReturn(projectName).when(runRequest).getProject();


        ApplicationProcessDescriptor descriptor = mock(ApplicationProcessDescriptor.class);
        doReturn(descriptor).when(runQueueTask).getDescriptor();
        doReturn(workspace).when(descriptor).getWorkspace();
        doReturn(projectName).when(descriptor).getProject();

        return runQueueTask;
    }

    /**
     * Check running processes
     *
     * @throws Exception
     */
    @Test
    public void testGetRunningProcesses() throws Exception {

        // two users with tasks, only florent is the current user
        User user = new UserImpl("florent", "id-2314", "token-2323", Collections.<String>emptyList(), false);
        User dummy = new UserImpl("dummy", "id-2315", "token-2324", Collections.<String>emptyList(), false);
        EnvironmentContext context = EnvironmentContext.getCurrent();
        context.setUser(user);

        // add 5 tasks for 4 projects with Florent and one for dummy user
        String workspaceFlorent = "workspaceFlorent";
        String workspaceCodenvy = "workspaceCodenvy";
        String projectA = "/projectA";
        String projectFlorentA = projectA;
        String projectFlorentB = "projectB";
        String projectCodenvyA = projectA;

        List<RunQueueTask> tasks = new ArrayList<>(4);
        tasks.add(createTask(user, workspaceFlorent, projectFlorentA));
        tasks.add(createTask(user, workspaceFlorent, projectFlorentB));
        tasks.add(createTask(user, workspaceCodenvy, projectCodenvyA));
        tasks.add(createTask(dummy, "dummyWorkspace", "dummyProject"));
        doReturn(tasks).when(runQueue).getTasks();

        // we should have all the processes without specifying anything
        List<ApplicationProcessDescriptor> processes = service.getRunningProcesses(null, null);
        // list is 3 as we don't have task for dummy user
        assertEquals(processes.size(), 3);

        // we should have all the processes without specifying anything
        processes = service.getRunningProcesses(workspaceCodenvy, null);
        // list is 1 for this workspace
        assertEquals(processes.size(), 1);
        ApplicationProcessDescriptor process = processes.get(0);
        assertEquals(process.getWorkspace(), workspaceCodenvy);
        assertEquals(process.getProject(), projectCodenvyA);


        // we should have all the processes without specifying anything
        processes = service.getRunningProcesses(workspaceCodenvy, projectA);
        // list is 1 as we only have one matching workspace + project
        assertEquals(processes.size(), 1);


        // we should have all the processes without specifying anything
        processes = service.getRunningProcesses(null, projectA);
        // list is 2 as we have two workspaces with this project name
        assertEquals(processes.size(), 2);

        // no workspace with that name
        processes = service.getRunningProcesses("dummy", null);
        assertEquals(processes.size(), 0);

        // no project with that name
        processes = service.getRunningProcesses(null, "project");
        assertEquals(processes.size(), 0);

        // no project with that workspace + name
        processes = service.getRunningProcesses("dummy", "project");
        assertEquals(processes.size(), 0);
    }

    private List<RemoteRunnerServer> createRunners() throws Exception {
        List<RemoteRunnerServer> servers = new ArrayList<>(1);
        RemoteRunnerServer server1 = mock(RemoteRunnerServer.class);
        doReturn(true).when(server1).isAvailable();
        servers.add(server1);
        List<RunnerDescriptor> runners1 = new ArrayList<>(1);
        RunnerDescriptor runner1 = dto(RunnerDescriptor.class).withName("java/web");
        runner1.getEnvironments().add(dto(RunnerEnvironment.class).withId("my_tomcat7"));
        runner1.getEnvironments().add(dto(RunnerEnvironment.class).withId("my_jboss7"));
        runners1.add(runner1);

        RemoteRunnerServer server2 = mock(RemoteRunnerServer.class);
        doReturn(true).when(server2).isAvailable();
        servers.add(server2);
        List<RunnerDescriptor> runners2 = new ArrayList<>(1);
        RunnerDescriptor runner2 = dto(RunnerDescriptor.class).withName("java/web");
        runner2.getEnvironments().add(dto(RunnerEnvironment.class).withId("tomcat7"));
        runner2.getEnvironments().add(dto(RunnerEnvironment.class).withId("jboss7"));
        runners2.add(runner2);

        doReturn(runners1).when(server1).getRunnerDescriptors();
        doReturn(runners2).when(server2).getRunnerDescriptors();
        doReturn(servers).when(runQueue).getRegisterRunnerServers();
        return servers;
    }

    private <T> T dto(Class<T> type) {
        return dtoFactory.createDto(type);
    }
}

