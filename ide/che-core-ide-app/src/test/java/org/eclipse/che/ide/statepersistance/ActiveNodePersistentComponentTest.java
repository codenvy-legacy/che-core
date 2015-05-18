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

import org.eclipse.che.ide.actions.SelectNodeAction;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorInput;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.project.tree.VirtualFile;
import org.eclipse.che.ide.api.project.tree.generic.FileNode;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.collections.java.JsonArrayListAdapter;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.part.projectexplorer.ProjectExplorerViewImpl;
import org.eclipse.che.ide.api.project.tree.TreeNode;
import org.eclipse.che.ide.statepersistance.dto.ActionDescriptor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.hamcrest.core.Is.is;

/**
 * @author Andrienko Alexander
 */
@RunWith(GwtMockitoTestRunner.class)
public class ActiveNodePersistentComponentTest {

    private static final String PATH = "/Spring";
    private static final String TEXT = PATH + "/src/main/java/somepackage";

    @Mock
    private Provider<EditorAgent>   editorAgentProvider;
    @Mock
    private DtoFactory              dtoFactory;
    @Mock
    private ActionManager           actionManager;
    @Mock
    private SelectNodeAction selectNodeAction;
    @Mock
    private ProjectExplorerViewImpl projectExplorerView;

    @Mock
    private EditorAgent         editorAgent;
    @Mock
    private EditorPartPresenter activeEditor;
    @Mock
    private EditorInput         editorInput;
    @Mock
    private TreeNode<?>         parentNode;
    @Mock
    private TreeNode<?>         treeNode;
    @Mock
    private ActionDescriptor    actionDescriptor;
    @Mock
    private Array<TreeNode<?>>  emptyArray;
    private VirtualFile         virtualFile;

    @InjectMocks
    private ActiveNodePersistentComponent persistentComponent;

    @Before
    public void setUp() {
        virtualFile = mock(FileNode.class);
        List<TreeNode<?>> treeNodeList = new ArrayList<>();
        treeNodeList.add(parentNode);
        treeNodeList.add(treeNode);
        Array<TreeNode<?>> openedNodes = new JsonArrayListAdapter<>(treeNodeList);

        when(editorAgentProvider.get()).thenReturn(editorAgent);
        when(editorAgent.getActiveEditor()).thenReturn(activeEditor);
        when(activeEditor.getEditorInput()).thenReturn(editorInput);
        when(editorInput.getFile()).thenReturn(virtualFile);
        Mockito.<TreeNode<?>>when(((FileNode)virtualFile).getParent()).thenReturn(parentNode);
        when(virtualFile.getPath()).thenReturn(TEXT);
        when(actionManager.getId(selectNodeAction)).thenReturn(TEXT);

        when(dtoFactory.createDto(ActionDescriptor.class)).thenReturn(actionDescriptor);
        when(actionDescriptor.withId(TEXT)).thenReturn(actionDescriptor);
        when(actionDescriptor.withParameters(Matchers.<Map<String, String>>anyObject())).thenReturn(actionDescriptor);

        when(projectExplorerView.getOpenedTreeNodes()).thenReturn(openedNodes);
    }

    @Test
    public void listActionsShouldBeReturned() {
        List<ActionDescriptor> result = persistentComponent.getActions(PATH);

        verify(editorAgentProvider).get();
        verify(editorAgent).getActiveEditor();
        verify(activeEditor).getEditorInput();
        verify(editorInput).getFile();
        verify(((FileNode)virtualFile)).getParent();
        verify(projectExplorerView).getOpenedTreeNodes();
        verify(virtualFile).getPath();
        verify(actionManager).getId(selectNodeAction);
        verify(dtoFactory).createDto(ActionDescriptor.class);
        verify(actionDescriptor).withId(TEXT);
        verify(actionDescriptor).withParameters(Matchers.<Map<String, String>>anyObject());

        assertThat(result.contains(actionDescriptor), is(true));
        assertThat(result.size(), is(1));
    }

    @Test
    public void emptyListActionsShouldBeReturnedIfEditorIsNull() {
        when(editorAgent.getActiveEditor()).thenReturn(null);

        List<ActionDescriptor> result = persistentComponent.getActions(PATH);

        verify(editorAgentProvider).get();
        verify(editorAgent).getActiveEditor();

        assertThat(result.contains(actionDescriptor), is(false));
        assertThat(result.size(), is(0));
    }

    @Test
    public void emptyListActionsShouldBeReturnedIfActivePartIsNoteFileNode() {
        virtualFile = mock(VirtualFile.class);
        when(editorInput.getFile()).thenReturn(virtualFile);

        List<ActionDescriptor> result = persistentComponent.getActions(PATH);

        verify(editorAgentProvider).get();
        verify(editorAgent).getActiveEditor();
        verify(activeEditor).getEditorInput();
        verify(editorInput).getFile();

        assertThat(result.contains(actionDescriptor), is(false));
        assertThat(result.size(), is(0));
    }

    @Test
    public void emptyListActionsShouldBeReturnedWhenListOpenedNodesIsNull() {
        when(projectExplorerView.getOpenedTreeNodes()).thenReturn(null);

        List<ActionDescriptor> result = persistentComponent.getActions(PATH);

        verify(editorAgentProvider).get();
        verify(editorAgent).getActiveEditor();
        verify(activeEditor).getEditorInput();
        verify(editorInput).getFile();
        verify(((FileNode)virtualFile)).getParent();
        verify(projectExplorerView).getOpenedTreeNodes();

        assertThat(result.contains(actionDescriptor), is(false));
        assertThat(result.size(), is(0));
    }

    @Test
    public void emptyListActionsShouldBeReturnedWhenListOpenedNodesIsNotContainsParentNodeOfActivePart() {
        when(projectExplorerView.getOpenedTreeNodes()).thenReturn(emptyArray);

        List<ActionDescriptor> result = persistentComponent.getActions(PATH);

        verify(editorAgentProvider).get();
        verify(editorAgent).getActiveEditor();
        verify(activeEditor).getEditorInput();
        verify(editorInput).getFile();
        verify(((FileNode)virtualFile)).getParent();
        verify(projectExplorerView).getOpenedTreeNodes();

        assertThat(result.contains(actionDescriptor), is(false));
        assertThat(result.size(), is(0));
    }
}
