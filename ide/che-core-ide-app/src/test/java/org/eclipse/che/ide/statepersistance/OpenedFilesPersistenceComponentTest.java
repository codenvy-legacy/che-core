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

import com.google.inject.Provider;

import org.eclipse.che.ide.actions.OpenFileAction;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorInput;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.project.tree.VirtualFile;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.statepersistance.dto.ActionDescriptor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import static org.eclipse.che.ide.actions.OpenFileAction.FILE_PARAM_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test covers {@link OpenedFilesPersistenceComponent} functionality.
 *
 * @author Artem Zatsarynnyy
 */
@RunWith(MockitoJUnitRunner.class)
public class OpenedFilesPersistenceComponentTest {

    private static final String OPEN_FILE_ACTION_ID = "openFile";
    private static final String PROJECT_PATH        = "/project";
    private static final String FILE_PATH           = PROJECT_PATH + "/file1";

    @Mock
    private Provider<EditorAgent> editorAgentProvider;

    @Mock
    private EditorAgent editorAgent;

    @Mock
    private ActionManager actionManager;

    @Mock
    private OpenFileAction openFileAction;

    @Mock
    private DtoFactory dtoFactory;

    @Mock
    private EditorPartPresenter editorPartPresenter;

    @Mock
    private EditorInput editorInput;

    @Mock
    private VirtualFile virtualFile;

    @InjectMocks
    private OpenedFilesPersistenceComponent component;

    @Before
    public void setUp() {
        when(editorAgentProvider.get()).thenReturn(editorAgent);
        when(actionManager.getId(eq(openFileAction))).thenReturn(OPEN_FILE_ACTION_ID);
    }

    @Test
    public void shouldGetActions() {
        configureOpenedEditors();

        ActionDescriptor action = mock(ActionDescriptor.class);
        when(action.withId(anyString())).thenReturn(action);
        when(action.withParameters(anyMapOf(String.class, String.class))).thenReturn(action);
        when(dtoFactory.createDto(eq(ActionDescriptor.class))).thenReturn(action);
        NavigableMap<String, EditorPartPresenter> editors = new TreeMap<>();
        editors.put("/foo", editorPartPresenter);
        when(editorAgent.getOpenedEditors()).thenReturn(editors);
        when(editorPartPresenter.getEditorInput()).thenReturn(editorInput);
        when(editorInput.getFile()).thenReturn(virtualFile);
        when(virtualFile.getPath()).thenReturn(FILE_PATH);

        List<ActionDescriptor> actionDescriptors = component.getActions(PROJECT_PATH);

        verify(action).withId(anyString());
        verify(action).withParameters(eq(Collections.singletonMap(FILE_PARAM_ID, FILE_PATH)));
        assertEquals(1, actionDescriptors.size());
        assertTrue(actionDescriptors.contains(action));
    }

    private void configureOpenedEditors() {
        NavigableMap<String, EditorPartPresenter> openedEditors = new TreeMap<>();
        when(editorAgent.getOpenedEditors()).thenReturn(openedEditors);

        EditorPartPresenter editorPartPresenter = mock(EditorPartPresenter.class);
        openedEditors.put(FILE_PATH, editorPartPresenter);
    }
}
