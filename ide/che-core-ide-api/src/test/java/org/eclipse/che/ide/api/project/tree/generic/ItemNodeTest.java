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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.ide.api.event.UpdateTreeNodeChildrenEvent;
import org.eclipse.che.ide.api.project.tree.TreeNode;
import org.eclipse.che.ide.api.project.tree.TreeStructure;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.test.GwtReflectionUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 * @author Andrienko Alexander
 */
@RunWith(MockitoJUnitRunner.class)
public class ItemNodeTest {
    private static final String PARENT_PATH             = "/project/test/someParent";
    private static final String RENAMED_SOME_ITEM_PATH1 = "/project/test1";
    private static final String RENAMED_SOME_ITEM_PATH2 = "/project/tes";
    private static final String RENAMED_SOME_ITEM_PATH3 = "/project/tes3";
    private static final String NAME                    = "someFolder";
    private static final String PATH                    = PARENT_PATH + "/" + NAME;
    private static final String NEW_NAME                = "new name";

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
    private ItemReference           newData;
    @Mock
    private ItemReference           childReference1;
    @Mock
    private ItemReference           childReference2;
    @Mock
    private FolderNode              child1;
    @Mock
    private FileNode                child2;
    @Mock
    private TreeNode.RenameCallback renameCallback;
    @Mock
    private Throwable               throwable;
    @Mock
    private AsyncCallback<Void>     asyncCallback;

    @Captor
    private ArgumentCaptor<AsyncRequestCallback<Array<ItemReference>>> itemReferenceCaptor;
    @Captor
    private ArgumentCaptor<AsyncRequestCallback<Void>>                 argumentCaptor;
    @Captor
    private ArgumentCaptor<AsyncRequestCallback<ItemReference>>        asyncRequestCallbackArgumentCaptor;

    private ItemNodeImpl itemNode;

    @Before
    public void setUp() {
        itemNode = new ItemNodeImpl(parent,
                                    data,
                                    treeStructure,
                                    eventBus,
                                    projectServiceClient,
                                    dtoUnmarshallerFactory);

        when(data.getPath()).thenReturn(PATH);
        when(parent.getPath()).thenReturn(PARENT_PATH);

        itemNode.setData(data);
    }

    @Test
    public void nodeShouldBeRenamed() throws Exception {
        itemNode.rename(NEW_NAME, renameCallback);

        verify(projectServiceClient).rename(eq(PATH), eq(NEW_NAME), isNull(String.class), argumentCaptor.capture());

        GwtReflectionUtils.callOnSuccess(argumentCaptor.getValue(), (Void)null);

        verify(parent).getPath();
        verify(projectServiceClient).getItem(eq(PARENT_PATH + "/" + NEW_NAME), asyncRequestCallbackArgumentCaptor.capture());

        ItemReference itemReference = mock(ItemReference.class);

        when(itemReference.getName()).thenReturn(NEW_NAME);

        GwtReflectionUtils.callOnSuccess(asyncRequestCallbackArgumentCaptor.getValue(), newData);

        assertThat(itemNode.getData(), is(newData));
        verify(eventBus).fireEvent(any(UpdateTreeNodeChildrenEvent.class));
    }

    @Test
    public void itemNodeShouldNotBeRenamed() {
        itemNode.rename(NEW_NAME, renameCallback);

        verify(projectServiceClient).rename(eq(PATH), eq(NEW_NAME), isNull(String.class), argumentCaptor.capture());

        GwtReflectionUtils.callOnFailure(argumentCaptor.getValue(), throwable);
        renameCallback.onFailure(throwable);
    }

    @Test
    public void itemNodeShouldNotBeRenamedBecauseFailedGetItemReference() {
        itemNode.rename(NEW_NAME, renameCallback);

        verify(projectServiceClient).rename(eq(PATH), eq(NEW_NAME), isNull(String.class), argumentCaptor.capture());

        GwtReflectionUtils.callOnSuccess(argumentCaptor.getValue(), (Void)null);

        verify(parent).getPath();
        verify(projectServiceClient).getItem(eq(PARENT_PATH + "/" + NEW_NAME), asyncRequestCallbackArgumentCaptor.capture());

        ItemReference itemReference = mock(ItemReference.class);

        when(itemReference.getName()).thenReturn(NEW_NAME);

        GwtReflectionUtils.callOnFailure(asyncRequestCallbackArgumentCaptor.getValue(), throwable);
        renameCallback.onFailure(throwable);
    }

    @Test
    public void dataShouldBeUpdated1() {
        String expectedNewNodePath = RENAMED_SOME_ITEM_PATH1 + "/someParent/" + NAME;
        itemNode.updateData(asyncCallback, RENAMED_SOME_ITEM_PATH1);

        verify(projectServiceClient).getItem(eq(expectedNewNodePath), asyncRequestCallbackArgumentCaptor.capture());
        GwtReflectionUtils.callOnSuccess(asyncRequestCallbackArgumentCaptor.getValue(), newData);

        assertThat(itemNode.getData(), is(newData));
        asyncCallback.onSuccess(null);
    }

    @Test
    public void dataShouldBeUpdated2() {
        String expectedNewNodePath = RENAMED_SOME_ITEM_PATH2 + "/someParent/" + NAME;
        itemNode.updateData(asyncCallback, RENAMED_SOME_ITEM_PATH2);

        verify(projectServiceClient).getItem(eq(expectedNewNodePath), asyncRequestCallbackArgumentCaptor.capture());
        GwtReflectionUtils.callOnSuccess(asyncRequestCallbackArgumentCaptor.getValue(), newData);

        assertThat(itemNode.getData(), is(newData));
        asyncCallback.onSuccess(null);
    }

    @Test
    public void dataShouldBeUpdated3() {
        String expectedNewNodePath = RENAMED_SOME_ITEM_PATH3 + "/someParent/" + NAME;
        itemNode.updateData(asyncCallback, RENAMED_SOME_ITEM_PATH3);

        verify(projectServiceClient).getItem(eq(expectedNewNodePath), asyncRequestCallbackArgumentCaptor.capture());
        GwtReflectionUtils.callOnSuccess(asyncRequestCallbackArgumentCaptor.getValue(), newData);

        assertThat(itemNode.getData(), is(newData));
        asyncCallback.onSuccess(null);
    }

    @Test
    public void dataShouldBeUpdatedFailed() {
        String expectedNewNodePath = RENAMED_SOME_ITEM_PATH3 + "/someParent/" + NAME;
        itemNode.updateData(asyncCallback, RENAMED_SOME_ITEM_PATH3);

        verify(projectServiceClient).getItem(eq(expectedNewNodePath), asyncRequestCallbackArgumentCaptor.capture());
        GwtReflectionUtils.callOnFailure(asyncRequestCallbackArgumentCaptor.getValue(), throwable);

        asyncCallback.onFailure(throwable);
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
