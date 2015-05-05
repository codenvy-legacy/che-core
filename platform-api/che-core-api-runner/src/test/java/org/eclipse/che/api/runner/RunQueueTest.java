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

import org.eclipse.che.api.builder.BuildStatus;
import org.eclipse.che.api.builder.RemoteBuilderServer;
import org.eclipse.che.api.builder.dto.BuildOptions;
import org.eclipse.che.api.builder.dto.BuildTaskDescriptor;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.core.rest.HttpJsonHelper;
import org.eclipse.che.api.core.rest.RemoteServiceDescriptor;
import org.eclipse.che.api.core.rest.ServiceContext;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.core.util.ValueHolder;
import org.eclipse.che.api.project.shared.dto.BuildersDescriptor;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.project.shared.dto.RunnerEnvironment;
import org.eclipse.che.api.project.shared.dto.RunnersDescriptor;
import org.eclipse.che.api.runner.dto.RunOptions;
import org.eclipse.che.api.runner.dto.RunRequest;
import org.eclipse.che.api.runner.dto.RunnerDescriptor;
import org.eclipse.che.api.runner.dto.RunnerMetric;
import org.eclipse.che.api.runner.dto.RunnerServerAccessCriteria;
import org.eclipse.che.api.runner.dto.RunnerServerDescriptor;
import org.eclipse.che.api.runner.dto.RunnerServerLocation;
import org.eclipse.che.api.runner.dto.RunnerServerRegistration;
import org.eclipse.che.api.runner.dto.RunnerState;
import org.eclipse.che.api.runner.dto.ServerState;
import org.eclipse.che.api.runner.internal.Constants;
import org.eclipse.che.api.runner.internal.RunnerEvent;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceDescriptor;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.user.User;
import org.eclipse.che.dto.server.DtoFactory;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import javax.ws.rs.core.UriBuilder;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * @author andrew00x
 */
@Listeners(value = {MockitoTestNGListener.class})
public class RunQueueTest {
    private DtoFactory dtoFactory = DtoFactory.getInstance();
    private HttpJsonHelper.HttpJsonHelperImpl httpJsonHelper;
    private RunQueue                          runQueue;
    private String wsId   = "my_ws";
    private String wsName = wsId;
    private String pName  = "my_project";
    private String pPath  = "/" + pName;
    private ProjectDescriptor   project;
    private WorkspaceDescriptor workspace;
    private EnvironmentContext  codenvyContext;

    private List<RunnerEvent> events = new CopyOnWriteArrayList<>();

    @BeforeMethod
    public void beforeMethod() throws Exception {
        httpJsonHelper = mock(HttpJsonHelper.HttpJsonHelperImpl.class);
        Field field = HttpJsonHelper.class.getDeclaredField("httpJsonHelperImpl");
        field.setAccessible(true);
        field.set(null, httpJsonHelper);

        EventService eventService = mock(EventService.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                events.add((RunnerEvent)invocation.getArguments()[0]);
                return null;
            }
        }).when(eventService).publish(any(RunnerEvent.class));
        RunnerSelectionStrategy selectionStrategy = new LastInUseRunnerSelectionStrategy();
        runQueue = spy(new RunQueue("http://localhost:8080/api/workspace",
                                    "http://localhost:8080/api/project",
                                    "http://localhost:8080/api/builder",
                                    256,
                                    5,
                                    5,
                                    5,
                                    selectionStrategy,
                                    eventService));
        runQueue.cleanerPeriod = 1000; // run cleaner every second
        runQueue.checkAvailableRunnerPeriod = 1000;
        runQueue.checkBuildResultPeriod = 1000;
        runQueue.start();
        verify(runQueue, timeout(1000).times(1)).start();

        project = dto(ProjectDescriptor.class).withName(pName).withPath(pPath);
        project.getLinks().add(dto(Link.class).withMethod("GET")
                                              .withHref(String.format("http://localhost:8080/api/project/%s/%s", wsId, pPath))
                                              .withRel(org.eclipse.che.api.project.server.Constants.LINK_REL_EXPORT_ZIP));
        workspace = dto(WorkspaceDescriptor.class).withId(wsId).withName(wsName).withTemporary(false).withAccountId("my_account");

        events.clear();

        codenvyContext = mock(EnvironmentContext.class);
        User user = mock(User.class);
        doReturn(user).when(codenvyContext).getUser();
        String secureToken = "secret";
        doReturn(secureToken).when(user).getToken();
        EnvironmentContext.setCurrent(codenvyContext);
    }

    @AfterMethod
    public void afterMethod() {
        runQueue.stop();
    }

    @Test
    public void testRegisterSlaveRunner() throws Exception {
        String remoteUrl = "http://localhost:8080/api/internal/runner";

        RunnerDescriptor runnerDescriptor = dto(RunnerDescriptor.class).withName("java/web").withDescription("test description");
        runnerDescriptor.getEnvironments().add(dto(RunnerEnvironment.class).withId("tomcat7"));

        RemoteRunnerServer runnerServer = registerRunnerServer(remoteUrl, runnerDescriptor, null);
        RemoteRunner runner = runnerServer.getRemoteRunner("java/web");

        verify(runQueue, times(1)).createRemoteRunnerServer(eq(remoteUrl));
        verify(runQueue, times(1)).doRegisterRunnerServer(eq(runnerServer));

        List<RemoteRunnerServer> registerRunnerServers = runQueue.getRegisterRunnerServers();
        assertEquals(registerRunnerServers.size(), 1);
        assertEquals(registerRunnerServers.get(0), runnerServer);

        Set<RemoteRunner> runnerList = runQueue.getRunnerList("community", null, null);
        assertNotNull(runnerList);
        assertEquals(runnerList.size(), 1);
        assertTrue(runnerList.contains(runner));

        runnerList = runQueue.getRunnerList("community", wsId, null);
        assertNotNull(runnerList);
        assertEquals(runnerList.size(), 1);
        assertTrue(runnerList.contains(runner));

        runnerList = runQueue.getRunnerList("community", wsId, pPath);
        assertNotNull(runnerList);
        assertEquals(runnerList.size(), 1);
        assertTrue(runnerList.contains(runner));

        assertNull(runQueue.getRunnerList("paid", null, null));
        assertNull(runQueue.getRunnerList("paid", wsId, null));
        assertNull(runQueue.getRunnerList("paid", wsId, pPath));
    }

    @Test
    public void testRegisterSlaveRunnerWithRunnerServerAccessCriteria() throws Exception {
        String remoteUrl = "http://localhost:8080/api/internal/runner";
        RunnerDescriptor runnerDescriptor = dto(RunnerDescriptor.class).withName("java/web").withDescription("test description");
        runnerDescriptor.getEnvironments().add(dto(RunnerEnvironment.class).withId("tomcat7"));
        RunnerServerAccessCriteria accessRules = dto(RunnerServerAccessCriteria.class)
                .withWorkspace(wsId)
                .withProject(pPath)
                .withInfra("paid");
        RemoteRunnerServer runnerServer = registerRunnerServer(remoteUrl, runnerDescriptor, accessRules);
        RemoteRunner runner = runnerServer.getRemoteRunner("java/web");

        verify(runnerServer, times(1)).setAssignedWorkspace(wsId);
        verify(runnerServer, times(1)).setAssignedProject(pPath);
        verify(runnerServer, times(1)).setInfra("paid");
        verify(runQueue, times(1)).createRemoteRunnerServer(eq(remoteUrl));
        verify(runQueue, times(1)).doRegisterRunnerServer(eq(runnerServer));

        List<RemoteRunnerServer> registerRunnerServers = runQueue.getRegisterRunnerServers();
        assertEquals(registerRunnerServers.size(), 1);
        assertEquals(registerRunnerServers.get(0), runnerServer);

        Set<RemoteRunner> runnerList = runQueue.getRunnerList("paid", wsId, pPath);
        assertNotNull(runnerList);
        assertTrue(runnerList.contains(runner));

        assertNull(runQueue.getRunnerList("paid", wsId, null));
        assertNull(runQueue.getRunnerList("paid", null, null));
        assertNull(runQueue.getRunnerList("community", wsId, pPath));
        assertNull(runQueue.getRunnerList("community", wsId, null));
        assertNull(runQueue.getRunnerList("community", null, null));
    }

    @Test
    public void testRegisterSlaveRunnerWithMoreThenOneInfra() throws Exception {
        String remoteUrl = "http://localhost:8080/api/internal/runner";
        RunnerDescriptor runnerDescriptor = dto(RunnerDescriptor.class).withName("java/web").withDescription("test description");
        runnerDescriptor.getEnvironments().add(dto(RunnerEnvironment.class).withId("tomcat7"));
        registerRunnerServer(remoteUrl, runnerDescriptor, dto(RunnerServerAccessCriteria.class).withInfra("paid"));
        registerRunnerServer(remoteUrl, runnerDescriptor, dto(RunnerServerAccessCriteria.class).withInfra("community"));

        assertNotNull(runQueue.getRunnerList("paid", wsId, pPath));
        assertNotNull(runQueue.getRunnerList("paid", wsId, null));
        assertNotNull(runQueue.getRunnerList("paid", null, null));
        assertNotNull(runQueue.getRunnerList("community", wsId, pPath));
        assertNotNull(runQueue.getRunnerList("community", wsId, null));
        assertNotNull(runQueue.getRunnerList("community", null, null));

        assertEquals(runQueue.getRunnerList("paid", wsId, pPath).size(), 1);
        assertEquals(runQueue.getRunnerList("paid", wsId, null).size(), 1);
        assertEquals(runQueue.getRunnerList("paid", null, null).size(), 1);
        assertEquals(runQueue.getRunnerList("community", wsId, pPath).size(), 1);
        assertEquals(runQueue.getRunnerList("community", wsId, null).size(), 1);
        assertEquals(runQueue.getRunnerList("community", null, null).size(), 1);
    }

    @Test
    public void testUnregisterSlaveRunner() throws Exception {
        RemoteRunnerServer runnerServer = registerDefaultRunnerServer();
        runQueue.unregisterRunnerServer(dto(RunnerServerLocation.class).withUrl(runnerServer.getBaseUrl()));

        verify(runQueue, times(1)).doUnregisterRunners(eq(runnerServer.getBaseUrl()));

        List<RemoteRunnerServer> registerRunnerServers = runQueue.getRegisterRunnerServers();
        assertEquals(registerRunnerServers.size(), 0);

        assertNull(runQueue.getRunnerList("community", null, null));
    }

    @Test(expectedExceptions = {RunnerException.class},
            expectedExceptionsMessageRegExp = "Runner environment 'system:/java/web/jboss7' is not available for workspace 'my_ws' on infra 'community'.")
    public void testRunWhenReadRunnerConfigurationFromProject_RunnerIsNotAvailable() throws Exception {
        registerDefaultRunnerServer(); // doesn't have what we need
        ServiceContext serviceContext = newServiceContext();
        RunOptions runOptions = mock(RunOptions.class);
        project.withRunners(dto(RunnersDescriptor.class).withDefault("system:/java/web/jboss7"));

        doReturn(workspace).when(runQueue).getWorkspaceDescriptor(wsId, serviceContext);
        doReturn(project).when(runQueue).getProjectDescriptor(wsId, pPath, serviceContext);

        runQueue.run(wsId, pPath, serviceContext, runOptions);
    }

    @Test
    public void testRunWhenReadRunnerConfigurationFromProject() throws Exception {
        RemoteRunnerServer runnerServer = registerDefaultRunnerServer();
        RemoteRunner runner = runnerServer.getRemoteRunner("java/web");
        // Free memory should be more than 256.
        doReturn(dto(RunnerState.class).withServerState(dto(ServerState.class).withFreeMemory(512))).when(runner).getRemoteRunnerState();
        RemoteRunnerProcess process = spy(new RemoteRunnerProcess(runnerServer.getBaseUrl(), runner.getName(), 1l));
        doReturn(process).when(runner).run(any(RunRequest.class));

        ServiceContext serviceContext = newServiceContext();
        project.withRunners(dto(RunnersDescriptor.class).withDefault("system:/java/web/tomcat7"));

        doReturn(project).when(runQueue).getProjectDescriptor(wsId, pPath, serviceContext);
        doReturn(workspace).when(runQueue).getWorkspaceDescriptor(wsId, serviceContext);
        doNothing().when(runQueue).checkResources(eq(workspace), any(RunRequest.class));

        runQueue.run(wsId, pPath, serviceContext, null);

        ArgumentCaptor<RunRequest> runRequestCaptor = ArgumentCaptor.forClass(RunRequest.class);
        // need timeout for executor
        verify(runner, timeout(1000)).run(runRequestCaptor.capture());

        RunRequest request = runRequestCaptor.getValue();
        assertEquals(request.getMemorySize(), 256); // default mem size
        assertEquals(request.getEnvironmentId(), "tomcat7");
        assertEquals(request.getRunner(), "java/web");
        assertTrue(request.getOptions().isEmpty());
        assertTrue(request.getVariables().isEmpty());
        assertEquals(request.getUserToken(), codenvyContext.getUser().getToken());

        checkEvents(RunnerEvent.EventType.RUN_TASK_ADDED_IN_QUEUE);
    }

    @Test
    public void testRunWithRunOptions() throws Exception {
        RemoteRunnerServer runnerServer = registerDefaultRunnerServer();
        RemoteRunner runner = runnerServer.getRemoteRunner("java/web");
        // Free memory should be more than 256.
        doReturn(dto(RunnerState.class).withServerState(dto(ServerState.class).withFreeMemory(512))).when(runner).getRemoteRunnerState();
        RemoteRunnerProcess process = spy(new RemoteRunnerProcess(runnerServer.getBaseUrl(), runner.getName(), 1l));
        doReturn(process).when(runner).run(any(RunRequest.class));

        ServiceContext serviceContext = newServiceContext();
        RunOptions runOptions = mock(RunOptions.class);
        doReturn("system:/java/web/tomcat7").when(runOptions).getEnvironmentId();
        doReturn(384).when(runOptions).getMemorySize();
        Map<String, String> options = new HashMap<>(4);
        options.put("jpda", "");
        options.put("run", "");
        doReturn(options).when(runOptions).getOptions();
        Map<String, String> envVar = new HashMap<>(4);
        envVar.put("jpda", "");
        envVar.put("run", "");
        doReturn(envVar).when(runOptions).getVariables();

        doReturn(project).when(runQueue).getProjectDescriptor(wsId, pPath, serviceContext);
        doReturn(workspace).when(runQueue).getWorkspaceDescriptor(wsId, serviceContext);
        doNothing().when(runQueue).checkResources(eq(workspace), any(RunRequest.class));

        runQueue.run(wsId, pPath, serviceContext, runOptions);

        ArgumentCaptor<RunRequest> runRequestCaptor = ArgumentCaptor.forClass(RunRequest.class);
        // need timeout for executor
        verify(runner, timeout(1000)).run(runRequestCaptor.capture());

        // check was RunOptions in use.
        verify(runOptions, times(1)).getMemorySize();
        verify(runOptions, times(1)).getEnvironmentId();
        verify(runOptions, times(1)).getOptions();
        verify(runOptions, times(1)).getVariables();

        RunRequest request = runRequestCaptor.getValue();
        assertEquals(request.getMemorySize(), 384);
        assertEquals(request.getEnvironmentId(), "tomcat7");
        assertEquals(request.getRunner(), "java/web");
        assertEquals(request.getOptions(), options);
        assertEquals(request.getVariables(), envVar);
        assertEquals(request.getUserToken(), codenvyContext.getUser().getToken());

        checkEvents(RunnerEvent.EventType.RUN_TASK_ADDED_IN_QUEUE);
    }

    @Test
    public void testRunWithCustomEnvironment() throws Exception {
        String remoteUrl = "http://localhost:8080/api/internal/runner";
        RunnerDescriptor runnerDescriptor = dto(RunnerDescriptor.class).withName("docker");
        RemoteRunnerServer runnerServer = registerRunnerServer(remoteUrl, runnerDescriptor, null);
        RemoteRunner runner = runnerServer.getRemoteRunner(runnerDescriptor.getName());
        doReturn(dto(RunnerState.class).withServerState(dto(ServerState.class).withFreeMemory(512))).when(runner).getRemoteRunnerState();
        RemoteRunnerProcess process = spy(new RemoteRunnerProcess(runnerServer.getBaseUrl(), runner.getName(), 1l));
        doReturn(process).when(runner).run(any(RunRequest.class));

        ServiceContext serviceContext = newServiceContext();
        String envName = "my_env_1";
        RunOptions runOptions = mock(RunOptions.class);
        doReturn("project://" + envName).when(runOptions).getEnvironmentId();

        doReturn(project).when(runQueue).getProjectDescriptor(wsId, pPath, serviceContext);
        doReturn(workspace).when(runQueue).getWorkspaceDescriptor(wsId, serviceContext);
        doNothing().when(runQueue).checkResources(eq(workspace), any(RunRequest.class));

        ItemReference recipe = dto(ItemReference.class).withName("Dockerfile").withType("file");
        String recipeUrl = String.format("http://localhost:8080/api/project/%s/.codenvy/runners/environments/%s", wsId,
                                         envName);
        recipe.getLinks()
              .add(dto(Link.class).withRel(org.eclipse.che.api.project.server.Constants.LINK_REL_GET_CONTENT).withHref(recipeUrl));
        doReturn(Arrays.asList(recipe)).when(runQueue).getProjectRunnerRecipes(eq(project), eq(envName));

        runQueue.run(wsId, pPath, serviceContext, runOptions);

        ArgumentCaptor<RunRequest> runRequestCaptor = ArgumentCaptor.forClass(RunRequest.class);
        // need timeout for executor
        verify(runner, timeout(1000)).run(runRequestCaptor.capture());

        RunRequest request = runRequestCaptor.getValue();
        assertNull(request.getEnvironmentId());
        assertEquals(request.getRunner(), "docker");
        List<String> recipeUrls = request.getRecipeUrls();
        assertEquals(recipeUrls.size(), 1);
        assertEquals(request.getUserToken(), codenvyContext.getUser().getToken());

        checkEvents(RunnerEvent.EventType.RUN_TASK_ADDED_IN_QUEUE);
    }

    @Test
    public void testTimeOutWhileWaitingForRunner() throws Exception {
        RemoteRunnerServer runnerServer = registerDefaultRunnerServer();
        RemoteRunner runner = runnerServer.getRemoteRunner("java/web");
        // Free memory should be less (!!!) than 256.
        doReturn(dto(RunnerState.class).withServerState(dto(ServerState.class).withFreeMemory(128))).when(runner).getRemoteRunnerState();

        ServiceContext serviceContext = newServiceContext();
        project.withRunners(dto(RunnersDescriptor.class).withDefault("system:/java/web/tomcat7"));

        doReturn(project).when(runQueue).getProjectDescriptor(wsId, pPath, serviceContext);
        doReturn(workspace).when(runQueue).getWorkspaceDescriptor(wsId, serviceContext);
        doNothing().when(runQueue).checkResources(eq(workspace), any(RunRequest.class));

        RunQueueTask task = runQueue.run(wsId, pPath, serviceContext, null);

        assertTrue(task.isWaiting());
        // sleep - max waiting time + 2 sec
        TimeUnit.SECONDS.sleep(7);
        verify(runner, never()).run(any(RunRequest.class));
        assertFalse(task.isWaiting());
        assertTrue(task.isCancelled());
        checkEvents(RunnerEvent.EventType.RUN_TASK_ADDED_IN_QUEUE, RunnerEvent.EventType.RUN_TASK_QUEUE_TIME_EXCEEDED);
    }

    @Test
    public void testRunWithBuildBefore() throws Exception {
        RemoteRunnerServer runnerServer = registerDefaultRunnerServer();
        RemoteRunner runner = runnerServer.getRemoteRunner("java/web");
        // Free memory should be more than 256.
        doReturn(dto(RunnerState.class).withServerState(dto(ServerState.class).withFreeMemory(512))).when(runner).getRemoteRunnerState();
        RemoteRunnerProcess process = spy(new RemoteRunnerProcess(runnerServer.getBaseUrl(), runner.getName(), 1l));
        doReturn(process).when(runner).run(any(RunRequest.class));

        ServiceContext serviceContext = newServiceContext();
        project.withBuilders(dto(BuildersDescriptor.class).withDefault("maven"))
               .withRunners(dto(RunnersDescriptor.class).withDefault("system:/java/web/tomcat7"));

        doReturn(project).when(runQueue).getProjectDescriptor(wsId, pPath, serviceContext);
        doReturn(workspace).when(runQueue).getWorkspaceDescriptor(wsId, serviceContext);
        doNothing().when(runQueue).checkResources(eq(workspace), any(RunRequest.class));

        mockBuilderApi(3);

        runQueue.run(wsId, pPath, serviceContext, null);

        ArgumentCaptor<RunRequest> runRequestCaptor = ArgumentCaptor.forClass(RunRequest.class);
        // timeout - max waiting time + 1 sec
        verify(runner, timeout(6000)).run(runRequestCaptor.capture());

        RunRequest request = runRequestCaptor.getValue();
        assertEquals(request.getMemorySize(), 256); // default mem size
        assertEquals(request.getEnvironmentId(), "tomcat7");
        assertEquals(request.getRunner(), "java/web");
        assertTrue(request.getOptions().isEmpty());
        assertTrue(request.getVariables().isEmpty());
        assertEquals(request.getUserToken(), codenvyContext.getUser().getToken());

        checkEvents(RunnerEvent.EventType.RUN_TASK_ADDED_IN_QUEUE);
    }

    @Test
    public void testOverrideBuilderWithRunOptions() throws Exception {
        RemoteRunnerServer runnerServer = registerDefaultRunnerServer();
        RemoteRunner runner = runnerServer.getRemoteRunner("java/web");
        // Free memory should be more than 256.
        doReturn(dto(RunnerState.class).withServerState(dto(ServerState.class).withFreeMemory(512))).when(runner).getRemoteRunnerState();
        RemoteRunnerProcess process = spy(new RemoteRunnerProcess(runnerServer.getBaseUrl(), runner.getName(), 1l));
        doReturn(process).when(runner).run(any(RunRequest.class));

        ServiceContext serviceContext = newServiceContext();
        project.withBuilders(dto(BuildersDescriptor.class).withDefault("maven"))
               .withRunners(dto(RunnersDescriptor.class).withDefault("system:/java/web/tomcat7"));

        doReturn(project).when(runQueue).getProjectDescriptor(wsId, pPath, serviceContext);
        doReturn(workspace).when(runQueue).getWorkspaceDescriptor(wsId, serviceContext);
        doNothing().when(runQueue).checkResources(eq(workspace), any(RunRequest.class));

        mockBuilderApi(1);

        runQueue.run(wsId, pPath, serviceContext, dto(RunOptions.class).withBuildOptions(dto(BuildOptions.class).withBuilderName("ant")));

        // timeout for start executor and check build result once (checked every seconds)
        verify(runner, timeout(3000)).run(any(RunRequest.class));
        ArgumentCaptor<BuildOptions> buildOptionsCaptor = ArgumentCaptor.forClass(BuildOptions.class);
        verify(runQueue, times(1)).startBuild(any(RemoteBuilderServer.class), eq(pPath), buildOptionsCaptor.capture());

        BuildOptions buildOptions = buildOptionsCaptor.getValue();
        assertEquals(buildOptions.getBuilderName(), "ant"); // overridden with options even maven is set in project configuration
    }

    @Test
    public void testSkipBuildNoBuilderName() throws Exception {
        RemoteRunnerServer runnerServer = registerDefaultRunnerServer();
        RemoteRunner runner = runnerServer.getRemoteRunner("java/web");
        // Free memory should be more than 256.
        doReturn(dto(RunnerState.class).withServerState(dto(ServerState.class).withFreeMemory(512))).when(runner).getRemoteRunnerState();
        RemoteRunnerProcess process = spy(new RemoteRunnerProcess(runnerServer.getBaseUrl(), runner.getName(), 1l));
        doReturn(process).when(runner).run(any(RunRequest.class));

        ServiceContext serviceContext = newServiceContext();
        project.withRunners(dto(RunnersDescriptor.class).withDefault("system:/java/web/tomcat7"))
               .withBuilders(dto(BuildersDescriptor.class)); // set builders model but don't set any builder name

        doReturn(project).when(runQueue).getProjectDescriptor(wsId, pPath, serviceContext);
        doReturn(workspace).when(runQueue).getWorkspaceDescriptor(wsId, serviceContext);
        doNothing().when(runQueue).checkResources(eq(workspace), any(RunRequest.class));

        runQueue.run(wsId, pPath, serviceContext, null);

        verify(runner, timeout(1000)).run(any(RunRequest.class));
        verify(runQueue, never()).getBuilderServiceDescriptor(eq(wsId), eq(serviceContext));
        verify(runQueue, never()).startBuild(any(RemoteBuilderServer.class), eq(pPath), any(BuildOptions.class));
    }

    @Test
    public void testSkipBuildWithSkipBuildOptions() throws Exception {
        RemoteRunnerServer runnerServer = registerDefaultRunnerServer();
        RemoteRunner runner = runnerServer.getRemoteRunner("java/web");
        // Free memory should be more than 256.
        doReturn(dto(RunnerState.class).withServerState(dto(ServerState.class).withFreeMemory(512))).when(runner).getRemoteRunnerState();
        RemoteRunnerProcess process = spy(new RemoteRunnerProcess(runnerServer.getBaseUrl(), runner.getName(), 1l));
        doReturn(process).when(runner).run(any(RunRequest.class));

        ServiceContext serviceContext = newServiceContext();
        project.withRunners(dto(RunnersDescriptor.class).withDefault("system:/java/web/tomcat7"))
               .withBuilders(dto(BuildersDescriptor.class).withDefault("maven")); // set builders fully

        doReturn(project).when(runQueue).getProjectDescriptor(wsId, pPath, serviceContext);
        doReturn(workspace).when(runQueue).getWorkspaceDescriptor(wsId, serviceContext);
        doNothing().when(runQueue).checkResources(eq(workspace), any(RunRequest.class));

        runQueue.run(wsId, pPath, serviceContext, dto(RunOptions.class).withSkipBuild(true));

        verify(runner, timeout(1000)).run(any(RunRequest.class));
        verify(runQueue, never()).getBuilderServiceDescriptor(eq(wsId), eq(serviceContext));
        verify(runQueue, never()).startBuild(any(RemoteBuilderServer.class), eq(pPath), any(BuildOptions.class));
    }

    @Test
    public void testTimeOutWhileWaitingForLongBuildProcess() throws Exception {
        RemoteRunnerServer runnerServer = registerDefaultRunnerServer();
        RemoteRunner runner = runnerServer.getRemoteRunner("java/web");
        // Free memory should be more than 256.
        doReturn(dto(RunnerState.class).withServerState(dto(ServerState.class).withFreeMemory(512))).when(runner).getRemoteRunnerState();

        ServiceContext serviceContext = newServiceContext();
        project.withBuilders(dto(BuildersDescriptor.class).withDefault("maven"))
               .withRunners(dto(RunnersDescriptor.class).withDefault("system:/java/web/tomcat7"));

        doReturn(project).when(runQueue).getProjectDescriptor(wsId, pPath, serviceContext);
        doReturn(workspace).when(runQueue).getWorkspaceDescriptor(wsId, serviceContext);
        doNothing().when(runQueue).checkResources(eq(workspace), any(RunRequest.class));

        mockBuilderApi(7);

        RunQueueTask task = runQueue.run(wsId, pPath, serviceContext, null);

        assertTrue(task.isWaiting());
        // sleep - max waiting time + 2 sec
        TimeUnit.SECONDS.sleep(7);
        verify(runner, never()).run(any(RunRequest.class));
        assertFalse(task.isWaiting());
        assertTrue(task.isCancelled());

        checkEvents(RunnerEvent.EventType.RUN_TASK_ADDED_IN_QUEUE, RunnerEvent.EventType.RUN_TASK_QUEUE_TIME_EXCEEDED);
    }

    @Test(expectedExceptions = {RunnerException.class},
            expectedExceptionsMessageRegExp = "Not enough resources to start application. Available memory 128M but 256M required.")
    public void testErrorWhenNotEnoughMemoryAssignedToWorkspace() throws Exception {
        RemoteRunnerServer runnerServer = registerDefaultRunnerServer();
        RemoteRunner runner = runnerServer.getRemoteRunner("java/web");
        // Free memory should be more than 256.
        doReturn(dto(RunnerState.class).withServerState(dto(ServerState.class).withFreeMemory(512))).when(runner).getRemoteRunnerState();

        ServiceContext serviceContext = newServiceContext();
        project.withRunners(dto(RunnersDescriptor.class).withDefault("system:/java/web/tomcat7"));

        doReturn(project).when(runQueue).getProjectDescriptor(wsId, pPath, serviceContext);
        // limit memory
        workspace.getAttributes().put(Constants.RUNNER_MAX_MEMORY_SIZE, "128");
        doReturn(workspace).when(runQueue).getWorkspaceDescriptor(wsId, serviceContext);

        try {
            runQueue.run(wsId, pPath, serviceContext, null);
        } catch (RunnerException e) {
            verify(runQueue, never()).checkMemory(eq(wsId), anyInt(), anyInt());
            throw e;
        }
    }

    @Test(expectedExceptions = {RunnerException.class},
            expectedExceptionsMessageRegExp = "Run action for this workspace is locked")
    public void testErrorWhenRunIsBlockedForWorkspace() throws Exception {
        RemoteRunnerServer runnerServer = registerDefaultRunnerServer();
        RemoteRunner runner = runnerServer.getRemoteRunner("java/web");
        // Free memory should be more than 256.
        doReturn(dto(RunnerState.class).withServerState(dto(ServerState.class).withFreeMemory(512))).when(runner).getRemoteRunnerState();

        ServiceContext serviceContext = newServiceContext();
        project.withRunners(dto(RunnersDescriptor.class).withDefault("system:/java/web/tomcat7"));

        doReturn(project).when(runQueue).getProjectDescriptor(wsId, pPath, serviceContext);
        // limit memory
        workspace.getAttributes().put(Constants.RUNNER_MAX_MEMORY_SIZE, "1024");
        workspace.getAttributes().put(org.eclipse.che.api.account.server.Constants.RESOURCES_LOCKED_PROPERTY, "true");
        doReturn(workspace).when(runQueue).getWorkspaceDescriptor(wsId, serviceContext);

        try {
            runQueue.run(wsId, pPath, serviceContext, null);
        } catch (RunnerException e) {
            verify(runQueue, never()).checkMemory(eq(wsId), anyInt(), anyInt());
            throw e;
        }
    }

    @Test(expectedExceptions = {RunnerException.class},
            expectedExceptionsMessageRegExp = "Not enough resources to start application. Available memory 128M but 129M required.")
    public void testErrorWhenNotEnoughMemoryToRunNewApplication() throws Exception {
        RemoteRunnerServer runnerServer = registerDefaultRunnerServer();
        RemoteRunner runner = runnerServer.getRemoteRunner("java/web");
        doReturn(dto(RunnerState.class).withServerState(dto(ServerState.class).withFreeMemory(256))).when(runner).getRemoteRunnerState();

        ServiceContext serviceContext = newServiceContext();
        project.withRunners(dto(RunnersDescriptor.class).withDefault("system:/java/web/tomcat7"));

        doReturn(project).when(runQueue).getProjectDescriptor(wsId, pPath, serviceContext);
        // limit memory
        workspace.getAttributes().put(Constants.RUNNER_MAX_MEMORY_SIZE, "256");
        doReturn(workspace).when(runQueue).getWorkspaceDescriptor(wsId, serviceContext);

        doReturn(new Callable<RemoteRunnerProcess>() {
            @Override
            public RemoteRunnerProcess call() throws Exception {
                Thread.sleep(5000); // need to have first task in waiting status
                return null;
            }
        }).when(runQueue).createTaskFor(anyListOf(RemoteRunner.class), any(RunRequest.class), any(ValueHolder.class));

        runQueue.run(wsId, pPath, serviceContext, dto(RunOptions.class).withMemorySize(128));
        runQueue.run(wsId, pPath, serviceContext, dto(RunOptions.class).withMemorySize(129));
    }

    @Test
    public void testWhenRunningOutOfDiskSpace() throws Exception {
        RemoteRunnerServer runnerServer = registerDefaultRunnerServer();
        RemoteRunner runner = runnerServer.getRemoteRunner("java/web");
        List<RunnerMetric> metrics = new ArrayList<>(2);
        metrics.add(dto(RunnerMetric.class).withName(RunnerMetric.DISK_SPACE_TOTAL).withValue("1000000"));
        metrics.add(dto(RunnerMetric.class).withName(RunnerMetric.DISK_SPACE_USED).withValue("980000"));
        doReturn(dto(RunnerState.class).withServerState(dto(ServerState.class).withFreeMemory(256)).withStats(metrics))
                .when(runner).getRemoteRunnerState();

        ServiceContext serviceContext = newServiceContext();
        project.withRunners(dto(RunnersDescriptor.class).withDefault("system:/java/web/tomcat7"));

        doReturn(project).when(runQueue).getProjectDescriptor(wsId, pPath, serviceContext);
        doReturn(workspace).when(runQueue).getWorkspaceDescriptor(wsId, serviceContext);
        doNothing().when(runQueue).checkResources(eq(workspace), any(RunRequest.class));

        RunQueueTask task = runQueue.run(wsId, pPath, serviceContext, null);

        assertTrue(task.isWaiting());
        // sleep - max waiting time + 2 sec
        TimeUnit.SECONDS.sleep(7);
        verify(runner, never()).run(any(RunRequest.class));
        assertFalse(task.isWaiting());
        assertTrue(task.isCancelled());
        checkEvents(RunnerEvent.EventType.RUN_TASK_ADDED_IN_QUEUE, RunnerEvent.EventType.RUN_TASK_QUEUE_TIME_EXCEEDED);
    }

    private String mockBuilderApi(final int inProgressNum) throws Exception {
        assertTrue(inProgressNum >= 1);
        final BuildTaskDescriptor buildTaskQueue = dto(BuildTaskDescriptor.class).withStatus(BuildStatus.IN_QUEUE);
        String statusLink = String.format("http://localhost:8080/api/builder/%s/status/%d", wsId, 1);
        buildTaskQueue.getLinks().add(dto(Link.class).withMethod("GET")
                                                     .withHref(statusLink)
                                                     .withRel(org.eclipse.che.api.builder.internal.Constants.LINK_REL_GET_STATUS));
        doReturn(buildTaskQueue).when(runQueue).startBuild(any(RemoteServiceDescriptor.class), eq(pPath), any(BuildOptions.class));

        final BuildTaskDescriptor buildTaskProgress = dtoFactory.clone(buildTaskQueue).withStatus(BuildStatus.IN_PROGRESS);
        String cancelLink = String.format("http://localhost:8080/api/builder/%s/cancel/%d", wsId, 1);
        buildTaskProgress.getLinks().add(dto(Link.class).withMethod("GET")
                                                        .withHref(cancelLink)
                                                        .withRel(org.eclipse.che.api.builder.internal.Constants.LINK_REL_CANCEL));

        final BuildTaskDescriptor buildTaskDone = dtoFactory.clone(buildTaskQueue).withStatus(BuildStatus.SUCCESSFUL);
        String downloadLink = String.format("http://localhost:8080/api/builder/%s/download/%d?path=artifact", wsId, 1);
        buildTaskDone.getLinks().add(dto(Link.class).withMethod("GET")
                                                    .withHref(downloadLink)
                                                    .withRel(org.eclipse.che.api.builder.internal.Constants.LINK_REL_DOWNLOAD_RESULT));
        doAnswer(new Answer<BuildTaskDescriptor>() {
            int tick = 0;

            @Override
            public BuildTaskDescriptor answer(InvocationOnMock invocation) throws Throwable {
                if (tick == 0) {
                    ++tick;
                    return buildTaskQueue;
                } else if (tick < inProgressNum) {
                    ++tick;
                    return buildTaskProgress;
                }
                return buildTaskDone;
            }
        }).when(httpJsonHelper).request(eq(BuildTaskDescriptor.class), eq(statusLink), eq("GET"), any());
        return downloadLink;
    }

    private void checkEvents(RunnerEvent.EventType... expected) {
        List<RunnerEvent.EventType> list = new ArrayList<>(expected.length);
        java.util.Collections.addAll(list, expected);
        for (RunnerEvent event : events) {
            for (Iterator<RunnerEvent.EventType> iterator = list.iterator(); iterator.hasNext(); ) {
                RunnerEvent.EventType eventType = iterator.next();
                if (eventType.equals(event.getType())) {
                    iterator.remove();
                }
            }
        }

        if (!list.isEmpty()) {
            fail("Missing events: " + list);
        }
    }

    //

    private ServiceContext newServiceContext() {
        ServiceContext sc = mock(ServiceContext.class);
        doReturn(UriBuilder.fromUri("http://localhost:8080/api")).when(sc).getBaseUriBuilder();
        doReturn(UriBuilder.fromUri("http://localhost:8080/api/runner/" + wsId)).when(sc).getServiceUriBuilder();
        return sc;
    }

    private <T> T dto(Class<T> type) {
        return dtoFactory.createDto(type);
    }

    private RemoteRunnerServer registerDefaultRunnerServer() throws Exception {
        String remoteUrl = "http://localhost:8080/api/internal/runner";
        RunnerDescriptor runnerDescriptor = dto(RunnerDescriptor.class).withName("java/web").withDescription("test description");
        runnerDescriptor.getEnvironments().add(dto(RunnerEnvironment.class).withId("tomcat7"));
        return registerRunnerServer(remoteUrl, runnerDescriptor, null);
    }

    private RemoteRunnerServer registerRunnerServer(String remoteUrl, RunnerDescriptor runnerDescriptor,
                                                    RunnerServerAccessCriteria accessRules) throws Exception {
        RemoteRunnerServer runnerServer = spy(new RemoteRunnerServer(remoteUrl));
        doReturn(dto(RunnerServerDescriptor.class)).when(runnerServer).getServiceDescriptor();
        doReturn(Arrays.asList(runnerDescriptor)).when(runnerServer).getRunnerDescriptors();
        RemoteRunner runner = spy(new RemoteRunner(remoteUrl, runnerDescriptor.getName(), new ArrayList<Link>()));
        doReturn(runnerDescriptor.getEnvironments()).when(runner).getEnvironments();
        doReturn(Arrays.asList(runner)).when(runnerServer).getRemoteRunners();
        doReturn(runner).when(runnerServer).getRemoteRunner(eq(runnerDescriptor.getName()));
        when(runnerServer.isAvailable()).thenReturn(true);
        doReturn(dto(RunnerState.class)).when(runner).getRemoteRunnerState();
        when(runQueue.createRemoteRunnerServer(remoteUrl)).thenReturn(runnerServer);
        RunnerServerRegistration registration = dto(RunnerServerRegistration.class)
                .withRunnerServerLocation(dto(RunnerServerLocation.class).withUrl(remoteUrl))
                .withRunnerServerAccessCriteria(accessRules);
        runQueue.registerRunnerServer(registration);
        return runnerServer;
    }
}
