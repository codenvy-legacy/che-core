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

import com.google.gwtmockito.GwtMockitoTestRunner;
import com.google.inject.Provider;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.workspace.shared.dto.UsersWorkspaceDto;
import org.eclipse.che.ide.api.action.Action;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.action.Presentation;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.eclipse.che.ide.statepersistance.AppStateManager.PREFERENCE_PROPERTY_NAME;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
    private Set<PersistenceComponent>    persistenceComponents;
    @Mock
    private EventBus                     eventBus;
    @Mock
    private PreferencesManager           preferencesManager;
    @Mock
    private AppContext                   appContext;
    @Mock
    private DtoFactory                   dtoFactory;
    @Mock
    private ActionManager                actionManager;
    @Mock
    private PresentationFactory          presentationFactory;
    @Mock
    private AppState                     appState;
    @Mock
    private UsersWorkspaceDto            workspaceDescriptor;
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
    private ArgumentCaptor<PersistProjectTreeStateHandler>          projectTreeStateHandlerArgCaptor;
    @Captor
    private ArgumentCaptor<RestoreProjectTreeStateHandler>          treeStateHandlerArgumentCaptor;

    private AppStateManager appStateManager;

    @Before
    public void setUp() {
        List<ActionDescriptor> actions = new ArrayList<>();
        actions.add(actionDescriptor);
        Map<String, ProjectState> stateMap = new HashMap<>();
        stateMap.put(FULL_PROJECT_PATH, projectState);

        Map<String, PersistenceComponent> projectTreePersistenceComponents = new HashMap<>();
        projectTreePersistenceComponents.put(PROJECT_PATH, persistenceComponent);
        List<ActionDescriptor> actionDescriptors = new ArrayList<>();
        actionDescriptors.add(actionDescriptor);
        when(persistenceComponent.getActions(PROJECT_PATH)).thenReturn(actionDescriptors);

        when(appState.getProjects()).thenReturn(new HashMap<>());

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
        when(appContext.getOpenedProjects()).thenReturn(Arrays.asList(rootProject));
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
                                              managerProvider);
    }

    @Test
    public void shouldAddEventHandlers() {
        appStateManager.start();

        verify(eventBus).addHandler(eq(WindowActionEvent.TYPE), eq(appStateManager));
        verify(eventBus).addHandler(eq(PersistProjectTreeStateEvent.TYPE), anyObject());
        verify(eventBus).addHandler(eq(RestoreProjectTreeStateEvent.TYPE), anyObject());
    }

    @Test
    public void shouldDeserializeStateOnStart() {
        appStateManager.start();

        verify(dtoFactory).createDtoFromJson(eq(SERIALIZED_STATE), Matchers.<Class<AppState>>anyObject());
    }

    @Test
    public void userPreferenceShouldBeCleanedUp() throws Exception {
        when(preferencesManager.getValue(PREFERENCE_PROPERTY_NAME)).thenReturn(null);

        appStateManager.start();

        verify(preferencesManager).getValue(PREFERENCE_PROPERTY_NAME);
        verify(dtoFactory).createDto(AppState.class);
        verify(dtoFactory).createDto(RecentProject.class);
        verify(recentProject).setPath("");
        verify(recentProject).setWorkspaceId("");
        verify(appState).setRecentProject(recentProject);
    }

    @Test
    public void projectTreeShouldBePersisted() {
        appStateManager.start();

        PersistProjectTreeStateEvent projectTreeStateEvent = mock(PersistProjectTreeStateEvent.class);

        verify(eventBus).addHandler(eq(PersistProjectTreeStateEvent.TYPE), projectTreeStateHandlerArgCaptor.capture());
        projectTreeStateHandlerArgCaptor.getValue().onPersist(projectTreeStateEvent);

        verify(appContext).getOpenedProjects();
        verify(rootProject).getPath();
        verify(dtoFactory).createDto(ProjectState.class);
        verify(appState).getProjects();
        verify(projectState).getActions();
        verify(persistenceComponent).getActions(PROJECT_PATH);
    }

    @Test
    public void preferenceShouldBeCleanBecauseAppStateJsonIsNull() {
        when(preferencesManager.getValue(PREFERENCE_PROPERTY_NAME)).thenReturn(null);
        appStateManager.start();

        verify(eventBus).addHandler(WindowActionEvent.TYPE, appStateManager);

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
        appStateManager.start();

        verify(eventBus).addHandler(WindowActionEvent.TYPE, appStateManager);

        verify(preferencesManager).getValue(PREFERENCE_PROPERTY_NAME);
        verify(dtoFactory).createDtoFromJson(anyString(), eq(AppState.class));
        verify(dtoFactory).createDto(RecentProject.class);
        verify(appState).getRecentProject();

        verify(recentProject).setPath("");
        verify(recentProject).setWorkspaceId("");
        verify(appState).setRecentProject(recentProject);
    }

}
