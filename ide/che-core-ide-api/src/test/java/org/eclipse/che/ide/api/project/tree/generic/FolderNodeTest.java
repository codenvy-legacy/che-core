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

import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.project.tree.TreeNode;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.test.GwtReflectionUtils;
import com.google.gwt.user.client.rpc.AsyncCallback;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testing {@link FolderNode} functionality.
 *
 * @author Artem Zatsarynnyy
 */
public class FolderNodeTest extends BaseNodeTest {
    private static final String ITEM_PATH = "/project/folder/folder_name";
    private static final String ITEM_NAME = "folder_name";
    @Captor
    private ArgumentCaptor<AsyncRequestCallback<List<ItemReference>>> asyncRequestCallbackCaptor;
    @Captor
    private ArgumentCaptor<List<ItemReference>>                       ListCaptor;
    @Mock
    private ItemReference                                              itemReference;
    @Mock
    private ProjectDescriptor                                          projectDescriptor;
    @Mock
    private ProjectNode                                                projectNode;
    @InjectMocks
    private FolderNode                                                 folderNode;

    @Before
    public void setUp() {
        super.setUp();

        when(itemReference.getPath()).thenReturn(ITEM_PATH);
        when(itemReference.getName()).thenReturn(ITEM_NAME);

        final List<TreeNode<?>> children = new ArrayList<>();
        when(projectNode.getChildren()).thenReturn(children);
    }

    @Test
    public void testGetName() throws Exception {
        assertEquals(ITEM_NAME, folderNode.getName());
    }

    @Test
    public void testGetPath() throws Exception {
        assertEquals(ITEM_PATH, folderNode.getPath());
    }

    @Test
    public void testGetProject() throws Exception {
        assertEquals(projectNode, folderNode.getProject());
    }

    @Test
    public void shouldNotBeLeaf() throws Exception {
        assertFalse(folderNode.isLeaf());
    }

    @Test
    public void shouldBeRenemable() throws Exception {
        assertTrue(folderNode.isRenamable());
    }

    @Test
    public void testRenameWhenRenameIsSuccessful() throws Exception {
        final String newName = "new_name";

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<Void> callback = (AsyncRequestCallback<Void>)arguments[3];
                GwtReflectionUtils.callOnSuccess(callback, (Void)null);
                return callback;
            }
        }).when(projectServiceClient).rename(anyString(), anyString(), anyString(), (AsyncRequestCallback<Void>)anyObject());
        TreeNode.RenameCallback callback = mock(TreeNode.RenameCallback.class);

        folderNode.rename(newName, callback);

        verify(projectServiceClient).rename(eq(ITEM_PATH), eq(newName), anyString(), Matchers.<AsyncRequestCallback<Void>>anyObject());
//        verify(callback).onRenamed();
    }

    @Test
    public void testRenameWhenRenameIsFailed() throws Exception {
        final String newName = "new_name";

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<Void> callback = (AsyncRequestCallback<Void>)arguments[3];
                GwtReflectionUtils.callOnFailure(callback, mock(Throwable.class));
                return callback;
            }
        }).when(projectServiceClient).rename(anyString(), anyString(), anyString(), (AsyncRequestCallback<Void>)anyObject());
        TreeNode.RenameCallback callback = mock(TreeNode.RenameCallback.class);

        folderNode.rename(newName, callback);

        verify(projectServiceClient).rename(eq(ITEM_PATH), eq(newName), anyString(), Matchers.<AsyncRequestCallback<Void>>anyObject());
        verify(callback).onFailure(Matchers.<Throwable>anyObject());
    }

    @Test
    public void shouldBeDeletable() throws Exception {
        assertTrue(folderNode.isDeletable());
    }

    @Test
    public void testDeleteWhenDeleteIsSuccessful() throws Exception {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<Void> callback = (AsyncRequestCallback<Void>)arguments[1];
                GwtReflectionUtils.callOnSuccess(callback, (Void)null);
                return callback;
            }
        }).when(projectServiceClient).delete(anyString(), (AsyncRequestCallback<Void>)anyObject());
        TreeNode.DeleteCallback callback = mock(TreeNode.DeleteCallback.class);

        folderNode.delete(callback);

        verify(projectServiceClient).delete(eq(ITEM_PATH), Matchers.<AsyncRequestCallback<Void>>anyObject());
        verify(callback).onDeleted();
    }

    @Test
    public void testDeleteWhenDeleteIsFailed() throws Exception {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<Void> callback = (AsyncRequestCallback<Void>)arguments[1];
                GwtReflectionUtils.callOnFailure(callback, mock(Throwable.class));
                return callback;
            }
        }).when(projectServiceClient).delete(anyString(), (AsyncRequestCallback<Void>)anyObject());
        TreeNode.DeleteCallback callback = mock(TreeNode.DeleteCallback.class);

        folderNode.delete(callback);

        verify(projectServiceClient).delete(eq(ITEM_PATH), Matchers.<AsyncRequestCallback<Void>>anyObject());
        verify(callback).onFailure(Matchers.<Throwable>anyObject());
    }

    @Test
    public void shouldCreateChildFileNode() throws Exception {
        ItemReference fileItem = mock(ItemReference.class);
        when(fileItem.getType()).thenReturn("file");

        folderNode.createChildNode(fileItem);

        verify(treeStructure).newFileNode(eq(folderNode), eq(fileItem));
    }

    @Test
    public void shouldCreateChildFolderNodeForFolderItem() {
        ItemReference folderItem = mock(ItemReference.class);
        when(folderItem.getType()).thenReturn("folder");

        folderNode.createChildNode(folderItem);

        verify(treeStructure).newFolderNode(eq(folderNode), eq(folderItem));
    }

    @Test
    public void shouldCreateChildFolderNodeForProjectItem() {
        ItemReference folderItem = mock(ItemReference.class);
        when(folderItem.getType()).thenReturn("project");

        folderNode.createChildNode(folderItem);

        verify(treeStructure).newFolderNode(eq(folderNode), eq(folderItem));
    }

    @Test
    public void testGetChildrenWhenHiddenItemsAreShown() throws Exception {
        when(treeSettings.isShowHiddenItems()).thenReturn(true);

        String path = "path";
        AsyncCallback asyncCallback = mock(AsyncCallback.class);
        List<ItemReference> children = new ArrayList<>();

        ItemReference item = mock(ItemReference.class);
        when(item.getName()).thenReturn("item");
        ItemReference hiddenItem = mock(ItemReference.class);
        when(hiddenItem.getName()).thenReturn(".item");

        children.add(item);
        children.add(hiddenItem);

        folderNode.getChildren(path, asyncCallback);

        verify(projectServiceClient).getChildren(eq(path), asyncRequestCallbackCaptor.capture());
        AsyncRequestCallback<List<ItemReference>> requestCallback = asyncRequestCallbackCaptor.getValue();
        GwtReflectionUtils.callOnSuccess(requestCallback, children);

        verify(asyncCallback).onSuccess(ListCaptor.capture());

        List<ItemReference> list = ListCaptor.getValue();
        assertEquals(children.size(), list.size());
        assertTrue(list.contains(item));
        assertTrue(list.contains(hiddenItem));
    }

    @Test
    public void testGetChildrenWhenHiddenItemsAreNotShown() throws Exception {
        when(treeSettings.isShowHiddenItems()).thenReturn(false);

        String path = "path";
        AsyncCallback asyncCallback = mock(AsyncCallback.class);
        List<ItemReference> children = new ArrayList<>();

        ItemReference item = mock(ItemReference.class);
        when(item.getName()).thenReturn("item");
        ItemReference hiddenItem = mock(ItemReference.class);
        when(hiddenItem.getName()).thenReturn(".item");

        children.add(item);
        children.add(hiddenItem);

        folderNode.getChildren(path, asyncCallback);

        verify(projectServiceClient).getChildren(eq(path), asyncRequestCallbackCaptor.capture());
        AsyncRequestCallback<List<ItemReference>> requestCallback = asyncRequestCallbackCaptor.getValue();
        GwtReflectionUtils.callOnSuccess(requestCallback, children);

        verify(asyncCallback).onSuccess(ListCaptor.capture());

        List<ItemReference> list = ListCaptor.getValue();
        assertEquals(1, list.size());
        assertTrue(list.contains(item));
        assertFalse(list.contains(hiddenItem));
    }

    @Test
    public void testGetChildrenWhenRequestFailure() throws Exception {
        String path = "path";
        AsyncCallback asyncCallback = mock(AsyncCallback.class);

        folderNode.getChildren(path, asyncCallback);

        verify(projectServiceClient).getChildren(eq(path), asyncRequestCallbackCaptor.capture());
        AsyncRequestCallback<List<ItemReference>> requestCallback = asyncRequestCallbackCaptor.getValue();
        GwtReflectionUtils.callOnFailure(requestCallback, mock(Throwable.class));

        verify(asyncCallback).onFailure(Matchers.<Throwable>anyObject());
    }
}
