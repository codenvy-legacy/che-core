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

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test covers {@link OpenedFilesPersistenceComponent} functionality.
 *
 * @author Artem Zatsarynnyy
 */
@RunWith(MockitoJUnitRunner.class)
public class OpenedFilesPersistenceComponentTest {

    private static final String OPEN_FILE_ACTION_ID = "openFile";
    private static final String FILE1_PATH          = "/project/file1";
    private static final String FILE2_PATH          = "/project/file2";

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
    private EditorPartPresenter editorPartPresenter1;

    @Mock
    private EditorPartPresenter editorPartPresenter2;

    @Mock
    private EditorInput editorInput1;

    @Mock
    private EditorInput editorInput2;

    @Mock
    private VirtualFile virtualFile1;

    @Mock
    private VirtualFile virtualFile2;

    @InjectMocks
    private OpenedFilesPersistenceComponent component;

    @Before
    public void setUp() {
        when(editorAgentProvider.get()).thenReturn(editorAgent);
        when(actionManager.getId(eq(openFileAction))).thenReturn(OPEN_FILE_ACTION_ID);

        ActionDescriptor actionDescriptor = mock(ActionDescriptor.class);
        when(actionDescriptor.withId(anyString())).thenReturn(actionDescriptor);
        when(actionDescriptor.withParameters(anyMapOf(String.class, String.class))).thenReturn(actionDescriptor);
        when(dtoFactory.createDto(eq(ActionDescriptor.class))).thenReturn(actionDescriptor);
    }

    @Test
    public void shouldReturnActionsForReopeningFiles() {
        configureOpenedEditors();

        List<ActionDescriptor> actionDescriptors = component.getActions();

        assertEquals(2, actionDescriptors.size());
    }

    @Test
    public void shouldAddActionForActivatingLastFile() {
        configureOpenedEditors();
        when(editorAgent.getActiveEditor()).thenReturn(editorPartPresenter1);
        when(editorAgent.getLastEditor()).thenReturn(editorPartPresenter2);

        List<ActionDescriptor> actionDescriptors = component.getActions();

        assertEquals(3, actionDescriptors.size());
    }

    private void configureOpenedEditors() {
        when(virtualFile1.getPath()).thenReturn(FILE1_PATH);
        when(virtualFile2.getPath()).thenReturn(FILE2_PATH);
        when(editorInput1.getFile()).thenReturn(virtualFile1);
        when(editorInput2.getFile()).thenReturn(virtualFile2);
        when(editorPartPresenter1.getEditorInput()).thenReturn(editorInput1);
        when(editorPartPresenter2.getEditorInput()).thenReturn(editorInput2);

        Map<String, EditorPartPresenter> openedEditors = new TreeMap<>();
        openedEditors.put(FILE1_PATH, editorPartPresenter1);
        openedEditors.put(FILE2_PATH, editorPartPresenter1);

        when(editorAgent.getOpenedEditors()).thenReturn(openedEditors);
    }
}
