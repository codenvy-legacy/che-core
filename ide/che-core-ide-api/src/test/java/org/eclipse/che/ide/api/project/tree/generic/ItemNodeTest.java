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
package org.eclipse.che.ide.api.project.tree.generic;

import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.ide.api.project.tree.TreeNode;
import org.eclipse.che.ide.api.project.tree.TreeStructure;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.collections.java.JsonArrayListAdapter;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.Unmarshallable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 * @author Andrienko Alexander
 */
@RunWith(MockitoJUnitRunner.class)
public class ItemNodeTest {
    private static final String TEXT     = "some text";
    private static final String NEW_NAME = "new name";
    private Array<ItemReference> result;
    private Array<TreeNode<?>>   children;

    @Mock
    private StorableNode           parent;
    @Mock
    private ItemReference          data;
    @Mock
    private TreeStructure          treeStructure;
    @Mock
    private EventBus               eventBus;
    @Mock
    private ProjectServiceClient   projectServiceClient;
    @Mock
    private DtoUnmarshallerFactory dtoUnmarshallerFactory;

    @Mock
    private Unmarshallable<Array<ItemReference>> unmarshaller;
    @Mock
    private ItemReference                        itemParent;
    @Mock
    private ItemReference                        childReference1;
    @Mock
    private ItemReference                        childReference2;
    @Mock
    private FolderNode                           child1;
    @Mock
    private FileNode                             child2;
    @Mock
    private TreeNode.RenameCallback              renameCallback;

    @Captor
    private ArgumentCaptor<AsyncRequestCallback<Array<ItemReference>>> itemReferenceCaptor;
    @Captor
    private ArgumentCaptor<AsyncRequestCallback<Void>>                 argumentCaptor;
    @Captor
    private ArgumentCaptor<AsyncRequestCallback<Array<ItemReference>>> asyncRequestCallbackArgumentCaptor;

    private ItemNodeImpl itemNode;

    @Before
    public void setUp() {
        itemNode = new ItemNodeImpl(parent,
                                    data,
                                    treeStructure,
                                    eventBus,
                                    projectServiceClient,
                                    dtoUnmarshallerFactory);

        when(dtoUnmarshallerFactory.newArrayUnmarshaller(ItemReference.class)).thenReturn(unmarshaller);
        when(parent.getPath()).thenReturn(TEXT);
        when(itemParent.getPath()).thenReturn(TEXT);
        when(child1.getName()).thenReturn(TEXT + 1);
        when(child2.getName()).thenReturn(TEXT + 1);
        when(childReference1.getName()).thenReturn(TEXT + 1);
        when(childReference2.getName()).thenReturn(TEXT + 1);
        itemNode.setData(itemParent);

        List<TreeNode<?>> list = new ArrayList<>();
        list.add(child1);
        list.add(child2);
        children = new JsonArrayListAdapter<TreeNode<?>>(list);
        itemNode.setChildren(children);
        result = new JsonArrayListAdapter<>(Arrays.asList(childReference1, childReference2));
    }

    @Test
    public void actionShouldBePerformedWhenNodeRenamed() throws Exception {
        itemNode.onNodeRenamed(TEXT);

        verify(dtoUnmarshallerFactory).newArrayUnmarshaller(ItemReference.class);
        verify(projectServiceClient).getChildren(eq(TEXT), itemReferenceCaptor.capture());

        AsyncRequestCallback<Array<ItemReference>> callback = itemReferenceCaptor.getValue();

        Method method = callback.getClass().getDeclaredMethod("onSuccess", Object.class);
        method.setAccessible(true);
        method.invoke(callback, result);

        verify(child1, times(2)).getName();
        verify(child2, times(2)).getName();

        verify(child1, times(2)).getPath();
        verify(child2, times(2)).getPath();

        verify(child1).setData(childReference1);
        verify(child2).setData(childReference2);
    }

    @Test
    public void nodeShouldBeRenamed() throws Exception {
        itemNode.rename(NEW_NAME, renameCallback);

        verify(projectServiceClient).rename(eq(TEXT), eq(NEW_NAME), isNull(String.class), argumentCaptor.capture());
        AsyncRequestCallback<Void> callback = argumentCaptor.getValue();

        Method method = callback.getClass().getDeclaredMethod("onSuccess", Object.class);
        method.setAccessible(true);
        method.invoke(callback, (Void)null);

        verify(parent).getPath();
        verify(dtoUnmarshallerFactory).newArrayUnmarshaller(ItemReference.class);
        verify(projectServiceClient).getChildren(eq(TEXT), asyncRequestCallbackArgumentCaptor.capture());
        AsyncRequestCallback<Array<ItemReference>> callback2 = asyncRequestCallbackArgumentCaptor.getValue();

        ItemReference itemReference = mock(ItemReference.class);
        result = new JsonArrayListAdapter<>(Arrays.asList(itemReference, childReference1));
        when(itemReference.getName()).thenReturn(NEW_NAME);

        method = callback2.getClass().getDeclaredMethod("onSuccess", Object.class);
        method.setAccessible(true);
        method.invoke(callback2, result);

        verify(itemReference).getName();
        assertThat(itemNode.getData(), is(itemReference));
    }

    class ItemNodeImpl extends ItemNode {

        public ItemNodeImpl(TreeNode<?> parent, ItemReference data, TreeStructure treeStructure,
                            EventBus eventBus, ProjectServiceClient projectServiceClient,
                            DtoUnmarshallerFactory dtoUnmarshallerFactory) {
            super(parent, data, treeStructure, eventBus, projectServiceClient, dtoUnmarshallerFactory);
        }

        @Override
        public boolean canContainsFolder() {
            return false;
        }

        @Override
        public boolean isLeaf() {
            return false;
        }
    }
}
