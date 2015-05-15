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
package org.eclipse.che.ide.part.projectexplorer;

import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.event.NodeExpandedEvent;
import org.eclipse.che.ide.api.project.tree.TreeStructure;
import org.eclipse.che.ide.api.project.tree.TreeNode;
import org.eclipse.che.ide.api.project.tree.TreeStructureProviderRegistry;
import org.eclipse.che.ide.api.project.tree.generic.ProjectNode;
import org.eclipse.che.ide.api.project.tree.generic.StorableNode;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.collections.java.JsonArrayListAdapter;
import org.eclipse.che.ide.menu.ContextMenu;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.ui.tree.SelectionModel;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.web.bindery.event.shared.EventBus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testing {@link ProjectExplorerPartPresenter} functionality.
 *
 * @author Artem Zatsarynnyy
 */
@RunWith(MockitoJUnitRunner.class)
public class ProjectExplorerPartPresenterTest {
    @Mock
    private ProjectExplorerView           view;
    @Mock
    private EventBus                      eventBus;
    @Mock
    private ContextMenu                   contextMenu;
    @Mock
    private ProjectServiceClient          projectServiceClient;
    @Mock
    private DtoUnmarshallerFactory        dtoUnmarshallerFactory;
    @Mock
    private CoreLocalizationConstant      coreLocalizationConstant;
    @Mock
    private AppContext                    appContext;
    @Mock
    private TreeStructureProviderRegistry treeStructureProviderRegistry;
    @Mock
    private TreeStructure                 currentTreeStructure;
    @Mock
    private TreeNode<?>                   selectedNode;
    @Mock
    private DeleteNodeHandler             deleteNodeHandler;
    @Mock
    private CurrentProject                currentProject;
    @InjectMocks
    private ProjectExplorerPartPresenter  presenter;

    @Before
    public void setUp() throws Exception {
        when(appContext.getCurrentProject()).thenReturn(currentProject);
    }

    @Test
    public void shouldSetWidget() throws Exception {
        AcceptsOneWidget container = mock(AcceptsOneWidget.class);
        presenter.go(container);
        verify(container).setWidget(view);
    }

    @Test
    public void shouldSetActionDelegate() throws Exception {
        verify(view).setDelegate(presenter);
    }

    @Test
    public void shouldSetSelectionFirstElement() throws Exception {
        final TreeNode node = mock(TreeNode.class);
        final SelectionModel selectionModel = mock(SelectionModel.class);
        final Array selection = new JsonArrayListAdapter(Collections.singletonList(node));
        when(selectionModel.getSelectedNodes()).thenReturn(selection);
        presenter.onNodeSelected(node, selectionModel);

        assertEquals(node, presenter.getSelection().getFirstElement());
    }

    @Test
    public void shouldSetSelectinoFirstElementMultiple() throws Exception {
        final TreeNode node1 = mock(TreeNode.class);
        final TreeNode node2 = mock(TreeNode.class);
        final SelectionModel selectionModel = mock(SelectionModel.class);
        final Array selection = new JsonArrayListAdapter(new ArrayList<Object>() {{
            add(node1);
            add(node2);
        }});
        when(selectionModel.getSelectedNodes()).thenReturn(selection);
        presenter.onNodeSelected(node1, selectionModel);

        assertEquals(node1, presenter.getSelection().getFirstElement());
    }

    @Test
    public void shouldSetSelectionHead() throws Exception {
        final TreeNode node = mock(TreeNode.class);
        final SelectionModel selectionModel = mock(SelectionModel.class);
        final Array selection = new JsonArrayListAdapter(Collections.singletonList(node));
        when(selectionModel.getSelectedNodes()).thenReturn(selection);
        presenter.onNodeSelected(node, selectionModel);

        assertEquals(node, presenter.getSelection().getHeadElement());
    }

    @Test
    public void shouldUpdateCurrentProjectOnNodeSelection() throws Exception {
        ProjectDescriptor projectDescriptor = mock(ProjectDescriptor.class);
        ProjectNode project = mock(ProjectNode.class);
        when(project.getData()).thenReturn(projectDescriptor);
        StorableNode node = mock(StorableNode.class);
        when(node.getProject()).thenReturn(project);

        final SelectionModel selectionModel = mock(SelectionModel.class);
        final Array selection = new JsonArrayListAdapter(Collections.singletonList(node));
        when(selectionModel.getSelectedNodes()).thenReturn(selection);

        presenter.onNodeSelected(node, selectionModel);

        verify(currentProject).setProjectDescription(projectDescriptor);
    }

    @Test
    public void testOnNodeAction() throws Exception {
        TreeNode node = mock(TreeNode.class);
        presenter.onNodeAction(node);

        verify(node).processNodeAction();
    }

    @Test
    public void shouldShowContextMenu() throws Exception {
        int x = 1;
        int y = 1;
        presenter.onContextMenu(x, y);

        verify(contextMenu).show(x, y);
    }

    @Test
    public void testOnDeleteKey() throws Exception {
        StorableNode firstNode = mock(StorableNode.class);
        StorableNode secondNode = mock(StorableNode.class);
        Array<?> array = new JsonArrayListAdapter<>(Arrays.asList(firstNode, secondNode));
        doReturn(array).when(view).getSelectedNodes();
        presenter.onDeleteKey();

        Class<List<StorableNode>> listClass = (Class) List.class;
        ArgumentCaptor<List<StorableNode>> captor = ArgumentCaptor.forClass(listClass);
        verify(deleteNodeHandler).deleteNodes(captor.capture());
        assertTrue(captor.getValue().contains(firstNode));
        assertTrue(captor.getValue().contains(secondNode));
    }

    @Test
    public void testOnEnterKey() throws Exception {
        TreeNode node = mock(TreeNode.class);
        doReturn(node).when(view).getSelectedNode();
        presenter.onEnterKey();

        verify(node).processNodeAction();
    }

    @Test
    public void nodeShouldBeExpanded() {
        presenter.expandNode(selectedNode);

        verify(view).expandAndSelectNode(selectedNode);
        verify(view).updateNode(selectedNode, selectedNode);

        verify(eventBus).fireEvent(any(NodeExpandedEvent.class));
    }
}
