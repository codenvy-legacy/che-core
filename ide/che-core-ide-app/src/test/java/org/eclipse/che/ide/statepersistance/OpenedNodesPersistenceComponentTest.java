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

import org.eclipse.che.ide.actions.OpenNodeAction;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.project.tree.TreeNode;
import org.eclipse.che.ide.api.project.tree.generic.FolderNode;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.collections.java.JsonArrayListAdapter;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.part.projectexplorer.ProjectExplorerViewImpl;
import org.eclipse.che.ide.statepersistance.dto.ActionDescriptor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Andrienko Alexander
 */
@RunWith(GwtMockitoTestRunner.class)
public class OpenedNodesPersistenceComponentTest {

    private static final String TEXT1        = "/src/main/java/com";
    private static final String TEXT2        = "/src/main/java/com/ua";
    private static final String PROJECT_PATH = "project";

    @Mock
    private ProjectExplorerViewImpl projectExplorerView;
    @Mock
    private ActionManager           actionManager;
    @Mock
    private OpenNodeAction          openNodeAction;
    @Mock
    private DtoFactory              dtoFactory;

    @Mock
    private FolderNode node1;
    @Mock
    private FolderNode node2;
    @Mock
    private FolderNode node3;

    @Mock
    private ActionDescriptor actionDescriptor1;
    @Mock
    private ActionDescriptor actionDescriptor2;
    @Mock
    private Array<TreeNode<?>>  emptyArray;

    @InjectMocks
    private OpenedNodesPersistenceComponent openedNodesComponent;

    @Before
    public void setUp() {
        List<TreeNode<?>> treeNodeList = new ArrayList<>();
        treeNodeList.add(node1);
        treeNodeList.add(node2);
        treeNodeList.add(node3);
        Array<TreeNode<?>> openedNodes = new JsonArrayListAdapter<>(treeNodeList);

        when(projectExplorerView.getOpenedTreeNodes()).thenReturn(openedNodes);
        when(actionManager.getId(openNodeAction)).thenReturn(TEXT1);

        when(node1.getPath()).thenReturn(PROJECT_PATH + TEXT1);
        when(node2.getPath()).thenReturn(TEXT2);
        when(node3.getPath()).thenReturn(PROJECT_PATH);

        when(dtoFactory.createDto(ActionDescriptor.class)).thenReturn(actionDescriptor1).thenReturn(actionDescriptor2);

        when(actionDescriptor1.withId(TEXT1)).thenReturn(actionDescriptor1);
        when(actionDescriptor2.withId(TEXT1)).thenReturn(actionDescriptor2);

        when(actionDescriptor1.withParameters(Matchers.<Map<String, String>>anyObject())).thenReturn(actionDescriptor1);
        when(actionDescriptor2.withParameters(Matchers.<Map<String, String>>anyObject())).thenReturn(actionDescriptor2);
    }

    @Test
    public void actionsShouldBeReturned() {
        List<ActionDescriptor> result = openedNodesComponent.getActions(PROJECT_PATH);

        verify(projectExplorerView).getOpenedTreeNodes();
        verify(actionManager).getId(openNodeAction);

        verify(node1).getPath();
        verify(node2).getPath();
        verify(node3).getPath();

        verify(dtoFactory, times(2)).createDto(ActionDescriptor.class);
        verify(actionDescriptor1).withId(TEXT1);
        verify(actionDescriptor2).withId(TEXT1);

        verify(actionDescriptor1).withParameters(Matchers.<Map<String, String>>anyObject());
        verify(actionDescriptor2).withParameters(Matchers.<Map<String, String>>anyObject());

        assertThat(result.contains(actionDescriptor1), is(true));
        assertThat(result.contains(actionDescriptor2), is(true));
        assertThat(result.size(), is(2));
    }

    @Test
    public void emptyListActionsShouldBeReturnedIfListOpenedNodesIsNull() {
        when(projectExplorerView.getOpenedTreeNodes()).thenReturn(null);

        List<ActionDescriptor> result = openedNodesComponent.getActions(PROJECT_PATH);

        verify(projectExplorerView).getOpenedTreeNodes();

        assertThat(result.isEmpty(), is(true));
    }

    @Test
    public void emptyListActionsShouldBeReturnedIfListOpenedNodesIsEmpty() {
        when(projectExplorerView.getOpenedTreeNodes()).thenReturn(emptyArray);
        when(emptyArray.isEmpty()).thenReturn(true);

        List<ActionDescriptor> result = openedNodesComponent.getActions(PROJECT_PATH);

        verify(projectExplorerView).getOpenedTreeNodes();

        assertThat(result.size(), is(0));
    }
}
