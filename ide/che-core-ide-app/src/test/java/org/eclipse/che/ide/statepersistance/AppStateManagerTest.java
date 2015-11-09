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

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.workspace.shared.dto.UsersWorkspaceDto;
import org.eclipse.che.ide.api.action.Action;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.event.WindowActionEvent;
import org.eclipse.che.ide.api.event.project.OpenProjectEvent;
import org.eclipse.che.ide.api.parts.PerspectiveManager;
import org.eclipse.che.ide.api.preferences.PreferencesManager;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.statepersistance.dto.ActionDescriptor;
import org.eclipse.che.ide.statepersistance.dto.AppState;
import org.eclipse.che.ide.statepersistance.dto.ProjectState;
import org.eclipse.che.ide.statepersistance.dto.RecentProject;
import org.eclipse.che.ide.ui.toolbar.PresentationFactory;
import org.eclipse.che.ide.util.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.eclipse.che.ide.statepersistance.AppStateManager.PREFERENCE_PROPERTY_NAME;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

    private static final String JSON      = "json";
    private static final String PATH      = "path";
    private static final String NAME      = "name";
    private static final String SOME_TEXT = "someText";

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
    private Provider<PerspectiveManager> managerProvider;
    @Mock
    private EventBus                     eventBus;

    //additional mocks
    @Mock
    private AppState             appState;
    @Mock
    private RecentProject        recentProject;
    @Mock
    private UsersWorkspaceDto    workspace;
    @Mock
    private ProjectDescriptor    projectDescriptor;
    @Mock
    private ProjectState         projectState;
    @Mock
    private ActionDescriptor     actionDescriptor;
    @Mock
    private Action               action;
    @Mock
    private Promise<Void>        voidPromise;
    @Mock
    private ProjectDescriptor    currentProject;
    @Mock
    private PersistenceComponent persistenceComponent;

    private AppStateManager appStateManager;

    @Before
    public void setUp() {
        when(preferencesManager.getValue(PREFERENCE_PROPERTY_NAME)).thenReturn(JSON);
        when(dtoFactory.createDtoFromJson(JSON, AppState.class)).thenReturn(appState);

        when(dtoFactory.createDto(RecentProject.class)).thenReturn(recentProject);

        when(projectDescriptor.getWorkspaceId()).thenReturn(NAME);
        when(projectDescriptor.getPath()).thenReturn(PATH);

        Map<String, ProjectState> projectStates = new HashMap<>();

        projectStates.put("/" + NAME + PATH, projectState);

        when(appState.getProjects()).thenReturn(projectStates);

        Set<PersistenceComponent> persistenceComponents = new HashSet<>(Arrays.asList(persistenceComponent));

        appStateManager = new AppStateManager(persistenceComponents,
                                              preferencesManager,
                                              appContext,
                                              dtoFactory,
                                              actionManager,
                                              presentationFactory,
                                              managerProvider,
                                              eventBus);
    }

    @Test
    public void stateFromPreferencesShouldBeRead() {
        verify(preferencesManager).getValue(PREFERENCE_PROPERTY_NAME);
        verify(dtoFactory).createDtoFromJson(JSON, AppState.class);

        verifyInitClearRecentProject();
    }

    private void verifyInitClearRecentProject() {
        verify(dtoFactory).createDto(RecentProject.class);
        verify(recentProject).setPath("");
        verify(recentProject).setWorkspaceId("");
        verify(appState).setRecentProject(recentProject);
    }

    @Test
    public void projectStateShouldNotBeRestoredWhenProjectStateIsNull() {
        OpenProjectEvent openProjectEvent = mock(OpenProjectEvent.class);
        when(openProjectEvent.getDescriptor()).thenReturn(projectDescriptor);

        when(appState.getProjects()).thenReturn(new HashMap<>());

        appStateManager.onProjectOpened(openProjectEvent);

        verify(projectState, never()).getActions();
    }

    @Test
    public void projectStateShouldBeRestored() {
        OpenProjectEvent openProjectEvent = mock(OpenProjectEvent.class);
        when(openProjectEvent.getDescriptor()).thenReturn(projectDescriptor);

        when(projectState.getActions()).thenReturn(Arrays.asList(actionDescriptor));
        when(actionDescriptor.getId()).thenReturn(SOME_TEXT);
        when(actionManager.getAction(anyString())).thenReturn(action);
        when(actionManager.performActions(Matchers.<List<Pair<Action, ActionEvent>>>anyObject(), eq(false))).thenReturn(voidPromise);

        appStateManager.onProjectOpened(openProjectEvent);

        verify(projectDescriptor).getWorkspaceId();
        verify(projectDescriptor).getPath();
        verify(appState).getProjects();

        verify(projectState).getActions();

        verify(actionManager).getAction(SOME_TEXT);

        verify(presentationFactory).getPresentation(action);
        verify(managerProvider).get();

        verify(actionManager).performActions(Matchers.<List<Pair<Action, ActionEvent>>>anyObject(), eq(false));
    }
}