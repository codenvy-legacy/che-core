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
package org.eclipse.che.ide.core.editor;

import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorInput;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.editor.EditorProvider;
import org.eclipse.che.ide.api.editor.EditorRegistry;
import org.eclipse.che.ide.api.filetypes.FileType;
import org.eclipse.che.ide.api.filetypes.FileTypeRegistry;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.parts.WorkspaceAgent;
import org.eclipse.che.ide.api.project.tree.VirtualFile;
import org.eclipse.che.ide.api.project.tree.generic.FileNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 *
 * @author Alexander Andrienko
 */
@RunWith(MockitoJUnitRunner.class)
public class EditorAgentImplTest {

    private final String TEXT = "some text";

    //mocks for constructor
    @Mock
    private EventBus                 eventBus;
    @Mock
    private FileTypeRegistry         fileTypeRegistry;
    @Mock
    private EditorRegistry           editorRegistry;
    @Mock
    private WorkspaceAgent           workspace;
    @Mock
    private NotificationManager      notificationManager;
    @Mock
    private CoreLocalizationConstant coreLocalizationConstant;

    @Mock
    private VirtualFile                    file;
    @Mock
    private EditorAgent.OpenEditorCallback callback;
    @Mock
    private FileType                       fileType;
    @Mock
    private FileNode                       newFileNode;
    @Mock
    private FileNode                       fileNode2;
    @Mock
    private EditorProvider                 editorProvider;
    @Mock
    private EditorPartPresenter            editor;
    @Mock
    private EditorInput                    editorInput;

//    @Captor
//    private ArgumentCaptor<DeleteModuleEventHandler> deleteModuleHandlerCaptor;

    @InjectMocks
    private EditorAgentImpl editorAgent;

    @Before
    public void setUp() {
        when(fileTypeRegistry.getFileTypeByFile(file)).thenReturn(fileType);
        when(fileTypeRegistry.getFileTypeByFile(newFileNode)).thenReturn(fileType);
        when(fileTypeRegistry.getFileTypeByFile(fileNode2)).thenReturn(fileType);
        when(editorRegistry.getEditor(fileType)).thenReturn(editorProvider);
        when(editorProvider.getEditor()).thenReturn(editor);
        when(file.getPath()).thenReturn(TEXT);
        when(newFileNode.getPath()).thenReturn(TEXT + 1);

        when(editor.getEditorInput()).thenReturn(editorInput);
    }

    @Test
    public void editorNodeShouldBeUpdated() {
        editorAgent.openEditor(file, callback);

        editorAgent.updateEditorNode(TEXT, newFileNode);

        verify(editor).getEditorInput();
        verify(editorInput).setFile(newFileNode);
        verify(editor).onFileChanged();

        assertThat(editorAgent.getOpenedEditors().containsKey(TEXT + 1), is(true));
        assertThat(editorAgent.getOpenedEditors().get(TEXT + 1), is(editor));
    }

    @Test
    public void editorNodeShouldNotBeUpdatedBecauseFileIsNotOpened() {
        editorAgent.updateEditorNode(TEXT, newFileNode);

        verifyNoMoreInteractions(editor, newFileNode, editorInput);
    }
}
