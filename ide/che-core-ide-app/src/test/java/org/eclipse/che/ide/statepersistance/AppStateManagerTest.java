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
package org.eclipse.che.ide.statepersistance;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwtmockito.GwtMockitoTestRunner;
import com.google.inject.Provider;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.api.user.shared.dto.ProfileDescriptor;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceDescriptor;
import org.eclipse.che.ide.api.action.Action;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.action.Presentation;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.event.OpenProjectEvent;
import org.eclipse.che.ide.api.event.PersistProjectTreeStateEvent;
import org.eclipse.che.ide.api.event.PersistProjectTreeStateHandler;
import org.eclipse.che.ide.api.event.ProjectActionEvent;
import org.eclipse.che.ide.api.event.RestoreProjectTreeStateEvent;
import org.eclipse.che.ide.api.event.RestoreProjectTreeStateHandler;
import org.eclipse.che.ide.api.event.WindowActionEvent;
import org.eclipse.che.ide.api.parts.PerspectiveManager;
import org.eclipse.che.ide.api.preferences.PreferencesManager;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.statepersistance.dto.ActionDescriptor;
import org.eclipse.che.ide.statepersistance.dto.AppState;
import org.eclipse.che.ide.statepersistance.dto.ProjectState;
import org.eclipse.che.ide.statepersistance.dto.RecentProject;
import org.eclipse.che.ide.ui.toolbar.PresentationFactory;
import org.eclipse.che.ide.util.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.Mock;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.eclipse.che.ide.statepersistance.AppStateManager.PREFERENCE_PROPERTY_NAME;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Test covers {@link AppStateManager} functionality.
 *
 * @author Artem Zatsarynnyy
 * @author Dmitry Shnurenko
 */
@RunWith(GwtMockitoTestRunner.class)
public class AppStateManagerTest {

    private static final String SERIALIZED_STATE  = "text";
    private static final String PROJECT_PATH      = "/project";
    private static final String WORKSPACE_NAME    = "someWorkspace";
    private static final String WORKSPACE_ID      = "workspaceg13rj45zkespwtxm";
    private static final String FULL_PROJECT_PATH = "/" + WORKSPACE_NAME + PROJECT_PATH;

    @Mock
    private Set<PersistenceComponent>         persistenceComponents;

    private Map<String, PersistenceComponent> projectTreePersistenceComponents;
    @Mock
    private EventBus                          eventBus;
    @Mock
    private PreferencesManager                preferencesManager;
    @Mock
    private AppContext                        appContext;
    @Mock
    private DtoFactory                        dtoFactory;
    @Mock
    private ActionManager                     actionManager;
    @Mock
    private PresentationFactory               presentationFactory;
    @Mock
    private ProjectServiceClient              projectServiceClient;

    @Mock
    private AppState                     appState;
    @Mock
    private WorkspaceDescriptor          workspaceDescriptor;
    @Mock
    private RecentProject                recentProject;
    @Mock
    private ProjectDescriptor            rootProject;
    @Mock
    private CurrentProject               currentProject;
    @Mock
    private ProjectActionEvent           event;
    @Mock
    private ActionDescriptor             actionDescriptor;
    @Mock
    private ProjectState                 projectState;
    @Mock
    private PersistenceComponent         persistenceComponent;
    @Mock
    private PersistenceComponent         c1;
    @Mock
    private PersistenceComponent         c2;
    @Mock
    private Promise<Void>                promise;
    @Mock
    private Action                       action;
    @Mock
    private Presentation                 presentation;
    @Mock
    private Provider<PerspectiveManager> managerProvider;
    @Mock
    private PerspectiveManager           perspectiveManager;

    @Captor
    private ArgumentCaptor<AsyncRequestCallback<ProjectDescriptor>> projectDescriptorCaptor;
    @Captor
    private ArgumentCaptor<PersistProjectTreeStateHandler> projectTreeStateHandlerArgCaptor;
    @Captor
    private ArgumentCaptor<RestoreProjectTreeStateHandler> treeStateHandlerArgumentCaptor;

    private AppStateManager appStateManager;

    @Before
    public void setUp() {
        List<ActionDescriptor> actions = new ArrayList<>();
        actions.add(actionDescriptor);
        Map<String, ProjectState> stateMap = new HashMap<>();
        stateMap.put(FULL_PROJECT_PATH, projectState);

        projectTreePersistenceComponents = new HashMap<>();
        projectTreePersistenceComponents.put(PROJECT_PATH, persistenceComponent);
        List<ActionDescriptor> actionDescriptors = new ArrayList<>();
        actionDescriptors.add(actionDescriptor);
        when(persistenceComponent.getActions(PROJECT_PATH)).thenReturn(actionDescriptors);

        when(appState.getProjects()).thenReturn(new HashMap<String, ProjectState>());

        List<PersistenceComponent> componentList = new ArrayList<>();
        Collections.addAll(componentList, c1, c2);
        when(persistenceComponents.iterator()).thenReturn(componentList.iterator());

        when(preferencesManager.getValue(anyString())).thenReturn(SERIALIZED_STATE);
        when(dtoFactory.createDtoFromJson(anyString(), Matchers.<Class<AppState>>anyObject())).thenReturn(appState);
        when(appState.getRecentProject()).thenReturn(recentProject);
        when(recentProject.getPath()).thenReturn("/project");
        when(recentProject.getWorkspaceId()).thenReturn(WORKSPACE_ID);
        when(dtoFactory.createDto(RecentProject.class)).thenReturn(recentProject);
        when(appContext.getCurrentProject()).thenReturn(currentProject);
        when(appContext.getWorkspace()).thenReturn(workspaceDescriptor);
        when(workspaceDescriptor.getId()).thenReturn(WORKSPACE_ID);
        when(currentProject.getRootProject()).thenReturn(rootProject);
        when(rootProject.getPath()).thenReturn(PROJECT_PATH);
        when(rootProject.getWorkspaceName()).thenReturn(WORKSPACE_NAME);
        when(event.getProject()).thenReturn(rootProject);
        when(rootProject.getPath()).thenReturn(PROJECT_PATH);
        when(dtoFactory.createDto(ProjectState.class)).thenReturn(projectState);
        when(dtoFactory.createDto(AppState.class)).thenReturn(appState);

        when(projectState.getActions()).thenReturn(actions);
        when(appState.getProjects()).thenReturn(stateMap);
        when(actionManager.performActions(Matchers.<List<Pair<Action, ActionEvent>>>anyObject(), eq(false))).thenReturn(promise);
        when(actionDescriptor.getId()).thenReturn(SERIALIZED_STATE);
        when(actionManager.getAction(SERIALIZED_STATE)).thenReturn(action);
        when(presentationFactory.getPresentation(action)).thenReturn(presentation);
        when(dtoFactory.toJson(eq(appState))).thenReturn(SERIALIZED_STATE);
        when(managerProvider.get()).thenReturn(perspectiveManager);

        appStateManager = new AppStateManager(persistenceComponents,
                                              projectTreePersistenceComponents,
                                              eventBus,
                                              preferencesManager,
                                              appContext,
                                              dtoFactory,
                                              actionManager,
                                              presentationFactory,
                                              projectServiceClient,
                                              managerProvider);
    }

    @Test
    public void shouldAddEventHandlers() {
        appStateManager.start(false);

        verify(eventBus).addHandler(eq(WindowActionEvent.TYPE), eq(appStateManager));
        verify(eventBus).addHandler(eq(ProjectActionEvent.TYPE), eq(appStateManager));
        verify(eventBus).addHandler(eq(PersistProjectTreeStateEvent.TYPE), (PersistProjectTreeStateHandler) anyObject());
        verify(eventBus).addHandler(eq(RestoreProjectTreeStateEvent.TYPE), (RestoreProjectTreeStateHandler) anyObject());
    }

    @Test
    public void shouldDeserializeStateOnStart() {
        appStateManager.start(false);

        verify(dtoFactory).createDtoFromJson(eq(SERIALIZED_STATE), Matchers.<Class<AppState>>anyObject());
    }

    @Test
    public void shouldOpenRecentlyProjectOnStart() throws Exception {
        appStateManager.start(true);

        ProjectDescriptor result = mock(ProjectDescriptor.class);

        verify(projectServiceClient).getProject(anyString(), projectDescriptorCaptor.capture());
        AsyncRequestCallback<ProjectDescriptor> asyncRequestCallback = projectDescriptorCaptor.getValue();

        //noinspection NonJREEmulationClassesInClientCode
        Method method = asyncRequestCallback.getClass().getDeclaredMethod("onSuccess", Object.class);
        method.setAccessible(true);
        method.invoke(asyncRequestCallback, result);

        verify(eventBus).fireEvent(any(OpenProjectEvent.class));
    }

    @Test
    public void userPreferenceShouldBeCleanedUp() throws Exception {
        appStateManager.start(true);

        Throwable exception = mock(Throwable.class);

        verify(projectServiceClient).getProject(anyString(), projectDescriptorCaptor.capture());
        AsyncRequestCallback<ProjectDescriptor> asyncRequestCallback = projectDescriptorCaptor.getValue();

        //noinspection NonJREEmulationClassesInClientCode
        Method method = asyncRequestCallback.getClass().getDeclaredMethod("onFailure", Throwable.class);
        method.setAccessible(true);
        method.invoke(asyncRequestCallback, exception);

        verify(appState).getProjects();
        verify(appState, times(2)).getRecentProject();
        verify(recentProject).setPath("");

        verify(dtoFactory).toJson(appState);
        preferencesManager.setValue(PREFERENCE_PROPERTY_NAME, SERIALIZED_STATE);
        verify(preferencesManager).flushPreferences(Matchers.<AsyncCallback<ProfileDescriptor>>anyObject());
    }

    @Test
    public void shouldEraseRecentlyProjectPathOnWindowClosingWhenNoOpenedProject() {
        appStateManager.start(false);
        when(appContext.getCurrentProject()).thenReturn(null);

        appStateManager.onWindowClosing(mock(WindowActionEvent.class));

        verify(recentProject).setWorkspaceId("");
        verify(appState, times(2)).getRecentProject();
        verify(recentProject).setPath("");

        verify(dtoFactory).toJson(appState);
        verify(preferencesManager).setValue(anyString(), eq(SERIALIZED_STATE));
        verify(preferencesManager).flushPreferences(Matchers.<AsyncCallback<ProfileDescriptor>>anyObject());
    }

    @Test
    public void shouldCallAllRegisteredComponents() {
        appStateManager.start(true);

        when(dtoFactory.toJson(appState)).thenReturn(SERIALIZED_STATE);
        when(dtoFactory.toJson(eq(appState))).thenReturn(SERIALIZED_STATE);

        ProjectState projectState = mock(ProjectState.class);
        when(dtoFactory.createDto(eq(ProjectState.class))).thenReturn(projectState);

        appStateManager.onWindowClosing(mock(WindowActionEvent.class));

        verify(appContext, times(2)).getCurrentProject();
        verify(appState, times(3)).getRecentProject();
        verify(appContext).getWorkspace();
        verify(workspaceDescriptor).getId();
        verify(recentProject).setWorkspaceId(WORKSPACE_ID);
        verify(currentProject, times(2)).getRootProject();
        verify(rootProject, times(2)).getPath();
        verify(rootProject, times(2)).getWorkspaceName();
        verify(appState, times(3)).getRecentProject();
        verify(recentProject).setPath(FULL_PROJECT_PATH);
        verify(dtoFactory).createDto(ProjectState.class);
        verify(appState).getProjects();
        verify(projectState).getActions();
        verify(currentProject, times(2)).getRootProject();
        verify(dtoFactory).toJson(appState);
        verify(preferencesManager).setValue(PREFERENCE_PROPERTY_NAME, SERIALIZED_STATE);
        verify(preferencesManager).flushPreferences(Matchers.<AsyncCallback<ProfileDescriptor>>anyObject());

        verify(c1).getActions(eq(PROJECT_PATH));
        verify(c2).getActions(eq(PROJECT_PATH));

        assertThat(appState.getRecentProject().getPath(), is(PROJECT_PATH));
        assertThat(appState.getRecentProject().getWorkspaceId(), is(WORKSPACE_ID));
    }

    @Test
    public void projectShouldNotBeOpenedBecauseCurrentProjectIsNull() {
        appStateManager.start(false);

        when(appContext.getCurrentProject()).thenReturn(null);

        appStateManager.onProjectOpened(event);

        verify(appContext).getCurrentProject();
        verifyNoMoreInteractions(rootProject, event);
    }

    @Test
    public void projectShouldBeOpenedAndRestored() {
        appStateManager.start(false);

        appStateManager.onProjectOpened(event);

        verify(appContext).getCurrentProject();
        verify(currentProject).getRootProject();
        verify(rootProject).getWorkspaceName();
        verify(event).getProject();
        verify(rootProject).getPath();
        verify(appState).getProjects();

        verify(projectState).getActions();
        verify(presentationFactory).getPresentation(action);
        verify(actionDescriptor).getParameters();
    }

    @Test
    public void projectTreeShouldBePersisted() {
        appStateManager.start(false);

        PersistProjectTreeStateEvent projectTreeStateEvent = mock(PersistProjectTreeStateEvent.class);

        verify(eventBus).addHandler(eq(PersistProjectTreeStateEvent.TYPE), projectTreeStateHandlerArgCaptor.capture());
        projectTreeStateHandlerArgCaptor.getValue().onPersist(projectTreeStateEvent);

        verify(appContext).getCurrentProject();
        verify(currentProject).getRootProject();
        verify(rootProject).getPath();
        verify(rootProject).getWorkspaceName();
        verify(dtoFactory).createDto(ProjectState.class);
        verify(appState).getProjects();
        verify(projectState).getActions();
        verify(persistenceComponent).getActions(PROJECT_PATH);
    }

    @Test
    public void projectTreeStateShouldBeRestored() {
        RestoreProjectTreeStateEvent event = mock(RestoreProjectTreeStateEvent.class);
        when(event.getProjectPath()).thenReturn("/" + WORKSPACE_NAME + PROJECT_PATH);
        appStateManager.start(false);

        verify(eventBus).addHandler(eq(RestoreProjectTreeStateEvent.TYPE), treeStateHandlerArgumentCaptor.capture());
        treeStateHandlerArgumentCaptor.getValue().onRestore(event);

        verify(event).getProjectPath();
        verify(appState).getProjects();
        verify(projectState).getActions();
        verify(actionManager).getAction(SERIALIZED_STATE);
        verify(presentationFactory).getPresentation(action);
        verify(actionDescriptor).getParameters();

        verify(actionManager).performActions(Matchers.<List<Pair<Action, ActionEvent>>>anyObject(), eq(false));
        verify(promise).catchError(Matchers.<Operation<PromiseError>>anyObject());
    }

    @Test
    public void preferenceShouldBeCleanBecauseAppStateJsonIsNull() {
        when(preferencesManager.getValue(PREFERENCE_PROPERTY_NAME)).thenReturn(null);
        appStateManager.start(false);

        verify(eventBus).addHandler(WindowActionEvent.TYPE, appStateManager);
        verify(eventBus).addHandler(ProjectActionEvent.TYPE, appStateManager);

        verify(preferencesManager).getValue(PREFERENCE_PROPERTY_NAME);
        verify(dtoFactory).createDto(AppState.class);
        verify(dtoFactory).createDto(RecentProject.class);

        verify(recentProject).setPath("");
        verify(recentProject).setWorkspaceId("");
        verify(appState).setRecentProject(recentProject);
    }

    @Test
    public void preferenceShouldBeCleanBecauseAppStateJsonIsNull2() {
        when(appState.getRecentProject()).thenReturn(null);
        appStateManager.start(false);

        verify(eventBus).addHandler(WindowActionEvent.TYPE, appStateManager);
        verify(eventBus).addHandler(ProjectActionEvent.TYPE, appStateManager);

        verify(preferencesManager).getValue(PREFERENCE_PROPERTY_NAME);
        verify(dtoFactory).createDtoFromJson(anyString(), eq(AppState.class));
        verify(dtoFactory).createDto(RecentProject.class);
        verify(appState).getRecentProject();

        verify(recentProject).setPath("");
        verify(recentProject).setWorkspaceId("");
        verify(appState).setRecentProject(recentProject);
    }

    @Test
    public void shouldPersistStateWhenProjectIsClosing() {
        ProjectActionEvent projectActionEvent = mock(ProjectActionEvent.class);
        appStateManager.start(true);

        appStateManager.onProjectClosing(projectActionEvent);

        verify(appContext).getCurrentProject();
        verify(currentProject).getRootProject();
        verify(rootProject).getPath();
        verify(rootProject).getWorkspaceName();
        verify(dtoFactory).createDto(ProjectState.class);
        verify(appState).getProjects();
        verify(projectState).getActions();
        verify(c1).getActions(PROJECT_PATH);
        verify(c2).getActions(PROJECT_PATH);

        verify(dtoFactory).toJson(appState);
        verify(preferencesManager).setValue(eq(PREFERENCE_PROPERTY_NAME), anyString());
        verify( preferencesManager).flushPreferences(Matchers.<AsyncCallback<ProfileDescriptor>>anyObject());
    }
}
