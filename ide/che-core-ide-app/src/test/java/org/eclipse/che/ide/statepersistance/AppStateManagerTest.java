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
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.user.shared.dto.ProfileDescriptor;
import org.eclipse.che.ide.api.action.Action;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.action.Presentation;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.event.OpenProjectEvent;
import org.eclipse.che.ide.api.event.ProjectActionEvent;
import org.eclipse.che.ide.api.event.WindowActionEvent;
import org.eclipse.che.ide.api.preferences.PreferencesManager;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.statepersistance.dto.ActionDescriptor;
import org.eclipse.che.ide.statepersistance.dto.AppState;
import org.eclipse.che.ide.statepersistance.dto.ProjectState;
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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.eclipse.che.ide.statepersistance.AppStateManager.PREFERENCE_PROPERTY_NAME;

/**
 * Test covers {@link AppStateManager} functionality.
 *
 * @author Artem Zatsarynnyy
 */
@RunWith(GwtMockitoTestRunner.class)
public class AppStateManagerTest {

    private static final String SERIALIZED_STATE  = "text";
    private static final String PROJECT_PATH      = "/project";
    private static final String WORKSPACE_NAME    = "someWorkspace";
    private static final String FULL_PROJECT_PATH = "/" + WORKSPACE_NAME + PROJECT_PATH;
    private List<ActionDescriptor>    actions;
    private Map<String, ProjectState> stateMap;

    @Mock
    private Set<PersistenceComponent> persistenceComponents;
    @Mock
    private EventBus                  eventBus;
    @Mock
    private PreferencesManager        preferencesManager;
    @Mock
    private AppContext                appContext;
    @Mock
    private DtoFactory                dtoFactory;
    @Mock
    private ActionManager             actionManager;
    @Mock
    private PresentationFactory       presentationFactory;
    @Mock
    private ProjectServiceClient      projectServiceClient;

    @Mock
    private AppState             appState;
    @Mock
    private ProjectDescriptor    rootProject;
    @Mock
    private CurrentProject       currentProject;
    @Mock
    private ProjectActionEvent   event;
    @Mock
    private ActionDescriptor     actionDescriptor;
    @Mock
    private ProjectState         projectState;
    @Mock
    private PersistenceComponent persistenceComponent;
    @Mock
    private PersistenceComponent c1;
    @Mock
    private PersistenceComponent c2;
    @Mock
    private Promise<Void>        promise;
    @Mock
    private Action               action;
    @Mock
    private Presentation         presentation;

    @Captor
    private ArgumentCaptor<AsyncRequestCallback<ProjectDescriptor>> projectDescriptorCaptor;

    private AppStateManager appStateManager;

    @Before
    public void setUp() {
        actions = new ArrayList<>();
        actions.add(actionDescriptor);
        stateMap = new HashMap<>();
        stateMap.put(FULL_PROJECT_PATH, projectState);

        List<PersistenceComponent> componentList = new ArrayList<>();
        Collections.addAll(componentList, c1, c2);
        when(persistenceComponents.iterator()).thenReturn(componentList.iterator());

        when(preferencesManager.getValue(anyString())).thenReturn(SERIALIZED_STATE);
        when(dtoFactory.createDtoFromJson(anyString(), Matchers.<Class<AppState>>anyObject())).thenReturn(appState);
        when(appState.getLastProjectPath()).thenReturn("/project");
        when(appContext.getCurrentProject()).thenReturn(currentProject);
        when(currentProject.getRootProject()).thenReturn(rootProject);
        when(rootProject.getPath()).thenReturn(PROJECT_PATH);
        when(rootProject.getWorkspaceName()).thenReturn(WORKSPACE_NAME);
        when(event.getProject()).thenReturn(rootProject);
        when(rootProject.getPath()).thenReturn(PROJECT_PATH);
        when(dtoFactory.createDto(ProjectState.class)).thenReturn(projectState);
        when(projectState.getActions()).thenReturn(actions);
        when(appState.getProjects()).thenReturn(stateMap);
        when(actionManager.performActions(Matchers.<List<Pair<Action, ActionEvent>>>anyObject(), eq(false))).thenReturn(promise);
        when(actionDescriptor.getId()).thenReturn(SERIALIZED_STATE);
        when(actionManager.getAction(SERIALIZED_STATE)).thenReturn(action);
        when(presentationFactory.getPresentation(action)).thenReturn(presentation);
        when(dtoFactory.toJson(eq(appState))).thenReturn(SERIALIZED_STATE);

        appStateManager = new AppStateManager(persistenceComponents,
                                              eventBus,
                                              preferencesManager,
                                              appContext,
                                              dtoFactory,
                                              actionManager,
                                              presentationFactory,
                                              projectServiceClient);
        appStateManager.start(false);
    }

    @Test
    public void shouldAddEventHandlers() {
        verify(eventBus).addHandler(eq(WindowActionEvent.TYPE), eq(appStateManager));
        verify(eventBus).addHandler(eq(ProjectActionEvent.TYPE), eq(appStateManager));
    }

    @Test
    public void shouldDeserializeStateOnStart() {
        verify(dtoFactory).createDtoFromJson(eq(SERIALIZED_STATE), Matchers.<Class<AppState>>anyObject());
    }

    @Test
    public void shouldOpenLastProjectOnStart() throws Exception {
        appStateManager.start(true);

        ProjectDescriptor result = mock(ProjectDescriptor.class);

        verify(projectServiceClient, times(2)).getProject(anyString(), projectDescriptorCaptor.capture());
        AsyncRequestCallback<ProjectDescriptor> asyncRequestCallback = projectDescriptorCaptor.getValue();

        Method method = asyncRequestCallback.getClass().getDeclaredMethod("onSuccess", Object.class);
        method.setAccessible(true);
        method.invoke(asyncRequestCallback, result);

        verify(eventBus).fireEvent(any(OpenProjectEvent.class));
    }

    @Test
    public void userPreferenceShouldBeCleanedUp() throws Exception {
        appStateManager.start(true);

        Throwable exception = mock(Throwable.class);

        verify(projectServiceClient, times(2)).getProject(anyString(), projectDescriptorCaptor.capture());
        AsyncRequestCallback<ProjectDescriptor> asyncRequestCallback = projectDescriptorCaptor.getValue();

        Method method = asyncRequestCallback.getClass().getDeclaredMethod("onFailure", Throwable.class);
        method.setAccessible(true);
        method.invoke(asyncRequestCallback, exception);

        verify(appState).getProjects();
        verify(appState).setLastProjectPath("");
        verify(dtoFactory).toJson(appState);
        preferencesManager.setValue(PREFERENCE_PROPERTY_NAME, SERIALIZED_STATE);
        verify(preferencesManager).flushPreferences(Matchers.<AsyncCallback<ProfileDescriptor>>anyObject());
    }

    @Test
    public void shouldEraseLastProjectPathOnWindowClosingWhenNoOpenedProject() {
        when(appContext.getCurrentProject()).thenReturn(null);

        appStateManager.onWindowClosing(mock(WindowActionEvent.class));

        verify(appState).setLastProjectPath(eq(""));

        verify(preferencesManager).setValue(anyString(), eq(SERIALIZED_STATE));
        verify(preferencesManager).flushPreferences(Matchers.<AsyncCallback<ProfileDescriptor>>anyObject());
    }

    @Test
    public void shouldCallAllRegisteredComponents() {
        when(dtoFactory.toJson(appState)).thenReturn(SERIALIZED_STATE);
        when(dtoFactory.toJson(eq(appState))).thenReturn(SERIALIZED_STATE);

        ProjectState projectState = mock(ProjectState.class);
        when(dtoFactory.createDto(eq(ProjectState.class))).thenReturn(projectState);

        appStateManager.onWindowClosing(mock(WindowActionEvent.class));

        verify(appContext, times(2)).getCurrentProject();
        verify(currentProject, times(2)).getRootProject();
        verify(rootProject, times(2)).getPath();
        verify(rootProject, times(2)).getWorkspaceName();
        verify(appState).setLastProjectPath(FULL_PROJECT_PATH);
        verify(dtoFactory).createDto(ProjectState.class);
        verify(appState).getProjects();
        verify(projectState).getActions();
        verify(currentProject, times(2)).getRootProject();
        verify(dtoFactory).toJson(appState);
        verify(preferencesManager).setValue(PREFERENCE_PROPERTY_NAME, SERIALIZED_STATE);
        verify(preferencesManager).flushPreferences(Matchers.<AsyncCallback<ProfileDescriptor>>anyObject());

        verify(c1).getActions(eq(PROJECT_PATH));
        verify(c2).getActions(eq(PROJECT_PATH));
    }

    @Test
    public void projectShouldNotBeOpenedBecauseCurrentProjectIsNull() {
        when(appContext.getCurrentProject()).thenReturn(null);

        appStateManager.onProjectOpened(event);

        verify(appContext).getCurrentProject();
        verifyNoMoreInteractions(rootProject, event);
    }

    @Test
    public void projectShouldBeOpenedAndRestored() {
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
}
