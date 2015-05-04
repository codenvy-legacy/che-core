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
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.event.OpenProjectEvent;
import org.eclipse.che.ide.api.event.ProjectActionEvent;
import org.eclipse.che.ide.api.event.WindowActionEvent;
import org.eclipse.che.ide.api.preferences.PreferencesManager;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.statepersistance.dto.AppState;
import org.eclipse.che.ide.statepersistance.dto.ProjectState;
import org.eclipse.che.ide.ui.toolbar.PresentationFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test covers {@link AppStateManager} functionality.
 *
 * @author Artem Zatsarynnyy
 */
@RunWith(MockitoJUnitRunner.class)
public class AppStateManagerTest {

    private static final String SERIALIZED_STATE = "text";

    @Mock
    private Set<PersistenceComponent> persistenceComponents;

    @Mock
    private EventBus eventBus;

    @Mock
    private PreferencesManager preferencesManager;

    @Mock
    private AppContext appContext;

    @Mock
    private DtoFactory dtoFactory;

    @Mock
    private ActionManager actionManager;

    @Mock
    private PresentationFactory presentationFactory;

    @Mock
    private AppState appState;

    @InjectMocks
    private AppStateManager appStateManager;

    @Before
    public void setUp() {
        when(preferencesManager.getValue(anyString())).thenReturn(SERIALIZED_STATE);
        when(dtoFactory.createDtoFromJson(anyString(), Matchers.<Class<AppState>>anyObject())).thenReturn(appState);
        when(appState.getLastProjectPath()).thenReturn("/project");

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
    public void shouldOpenLastProjectOnStart() {
        appStateManager.start(true);

        verify(eventBus).fireEvent(any(OpenProjectEvent.class));
    }

    @Test
    public void shouldEraseLastProjectPathOnWindowClosingWhenNoOpenedProject() {
        when(dtoFactory.toJson(eq(appState))).thenReturn(SERIALIZED_STATE);

        appStateManager.onWindowClosing(mock(WindowActionEvent.class));

        verify(appState).setLastProjectPath(eq(""));

        verify(preferencesManager).setValue(anyString(), eq(SERIALIZED_STATE));
        verify(preferencesManager).flushPreferences(any(AsyncCallback.class));
    }

    @Test
    public void shouldCallAllRegisteredComponents() {
        CurrentProject currentProject = mock(CurrentProject.class);
        when(appContext.getCurrentProject()).thenReturn(currentProject);

        ProjectDescriptor rootProject = mock(ProjectDescriptor.class);
        when(currentProject.getRootProject()).thenReturn(rootProject);
        final String projectPath = "/project";
        when(rootProject.getPath()).thenReturn(projectPath);

        when(dtoFactory.toJson(eq(appState))).thenReturn(SERIALIZED_STATE);

        ProjectState projectState = mock(ProjectState.class);
        when(dtoFactory.createDto(eq(ProjectState.class))).thenReturn(projectState);

        PersistenceComponent c1 = mock(PersistenceComponent.class);
        PersistenceComponent c2 = mock(PersistenceComponent.class);
        List<PersistenceComponent> componentList = new ArrayList<>();
        Collections.addAll(componentList, c1, c2);
        when(persistenceComponents.iterator()).thenReturn(componentList.iterator());

        appStateManager.onWindowClosing(mock(WindowActionEvent.class));

        verify(c1).getActions(eq(projectPath));
        verify(c2).getActions(eq(projectPath));
    }
}
