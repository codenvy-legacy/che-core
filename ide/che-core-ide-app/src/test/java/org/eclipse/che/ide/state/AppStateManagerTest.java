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
package org.eclipse.che.ide.state;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Provider;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.actions.OpenFileAction;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorInput;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.event.OpenProjectEvent;
import org.eclipse.che.ide.api.event.WindowActionEvent;
import org.eclipse.che.ide.api.preferences.PreferencesManager;
import org.eclipse.che.ide.api.project.tree.generic.FileNode;
import org.eclipse.che.ide.collections.Collections;
import org.eclipse.che.ide.collections.StringMap;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.state.dto.ActionDescriptor;
import org.eclipse.che.ide.state.dto.AppState;
import org.eclipse.che.ide.state.dto.ProjectState;
import org.eclipse.che.ide.toolbar.PresentationFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMapOf;
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
    private EventBus eventBus;

    @Mock
    private PreferencesManager preferencesManager;

    @Mock
    private AppContext appContext;

    @Mock
    private Provider<EditorAgent> editorAgentProvider;

    @Mock
    private EditorAgent editorAgent;

    @Mock
    private DtoFactory dtoFactory;

    @Mock
    private ActionManager actionManager;

    @Mock
    private PresentationFactory presentationFactory;

    @Mock
    private OpenFileAction openFileAction;

    @Mock
    private AppState appState;

    @InjectMocks
    private AppStateManager appStateManager;

    @Before
    public void setUp() {
        when(editorAgentProvider.get()).thenReturn(editorAgent);
        when(preferencesManager.getValue(anyString())).thenReturn(SERIALIZED_STATE);
        when(dtoFactory.createDtoFromJson(anyString(), Matchers.<Class<AppState>>anyObject())).thenReturn(appState);
        when(appState.getLastProjectPath()).thenReturn("/project");

        appStateManager.start(false);
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
    public void shouldPersistStateOnWindowClosingWhenNoOpenedProject() {
        when(dtoFactory.toJson(eq(appState))).thenReturn(SERIALIZED_STATE);

        appStateManager.onWindowClosing(mock(WindowActionEvent.class));

        verify(appState).setLastProjectPath(eq(""));

        verify(preferencesManager).setValue(anyString(), eq(SERIALIZED_STATE));
        verify(preferencesManager).flushPreferences(any(AsyncCallback.class));
    }

    @Test
    public void shouldPersistStateOnWindowClosingWhenProjectOpened() {
        CurrentProject currentProject = mock(CurrentProject.class);
        when(appContext.getCurrentProject()).thenReturn(currentProject);

        ProjectDescriptor rootProject = mock(ProjectDescriptor.class);
        when(currentProject.getRootProject()).thenReturn(rootProject);
        when(rootProject.getPath()).thenReturn("/project");

        when(dtoFactory.toJson(eq(appState))).thenReturn(SERIALIZED_STATE);

        ProjectState projectState = mock(ProjectState.class);
        when(dtoFactory.createDto(eq(ProjectState.class))).thenReturn(projectState);

        StringMap<EditorPartPresenter> openedEditors = Collections.createStringMap();
        EditorPartPresenter editorPartPresenter = mock(EditorPartPresenter.class);
        openedEditors.put("/project/file", editorPartPresenter);
        EditorInput editorInput = mock(EditorInput.class);
        FileNode file = mock(FileNode.class);
        when(editorAgent.getOpenedEditors()).thenReturn(openedEditors);
        when(editorPartPresenter.getEditorInput()).thenReturn(editorInput);
        when(editorInput.getFile()).thenReturn(file);
        when(file.getPath()).thenReturn("/project/file");

        ActionDescriptor action = mock(ActionDescriptor.class);
        when(action.withId(anyString())).thenReturn(action);
        when(action.withParameters(anyMapOf(String.class, String.class))).thenReturn(action);
        when(dtoFactory.createDto(eq(ActionDescriptor.class))).thenReturn(action);


        appStateManager.onWindowClosing(mock(WindowActionEvent.class));


        verify(appState).setLastProjectPath(eq("/project"));

        verify(action).withId(anyString());
        verify(action).withParameters(anyMapOf(String.class, String.class));

        verify(preferencesManager).setValue(anyString(), eq(SERIALIZED_STATE));
        verify(preferencesManager).flushPreferences(any(AsyncCallback.class));
    }
}
