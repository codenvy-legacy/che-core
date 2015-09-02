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
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.event.FileEvent;
import org.eclipse.che.ide.api.event.NodeChangedEvent;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.project.tree.TreeNode;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.Unmarshallable;
import org.eclipse.che.test.GwtReflectionUtils;
import com.google.gwt.user.client.rpc.AsyncCallback;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import static org.eclipse.che.ide.api.project.tree.TreeNode.DeleteCallback;
import static org.eclipse.che.ide.api.project.tree.TreeNode.RenameCallback;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testing {@link FileNode} functionality.
 *
 * @author Artem Zatsarynnyy
 */
public class FileNodeTest extends BaseNodeTest {
    private static final String PARENT_PATH       = "/project/folder";
    private static final String ITEM_NAME         = "file_name";
    private static final String ITEM_PATH         = PARENT_PATH + "/" + ITEM_NAME;
    private static final String NEW_NAME          = "new_name";
    private static final String RENAMED_ITEM_PATH = PARENT_PATH + "/" + NEW_NAME;

    private NavigableMap<String, EditorPartPresenter> openedEditorsMap;

    @Mock
    private ItemReference       itemReference;
    @Mock
    private ProjectDescriptor   projectDescriptor;
    @Mock
    private ProjectNode         projectNode;
    @Mock
    private EditorAgent         editorAgent;
    @Mock
    private NotificationManager notificationManager;

    @Mock
    private EditorPartPresenter           editorPartPresenter;
    @Mock
    private Unmarshallable<ItemReference> unmarshaller;
    @Mock
    private ItemReference                 result;
    @Mock
    private Throwable                     throwable;
    @Mock
    private RenameCallback                renameCallback;

    @Mock
    private AsyncCallback<Void>                                 asyncCallback;
    @Captor
    private ArgumentCaptor<AsyncRequestCallback<ItemReference>> itemArgumentCaptor;
    @Captor
    private ArgumentCaptor<AsyncRequestCallback<Void>>          voidArgumentCaptor;

    private FileNode fileNode;

    @Before
    public void setUp() {
        super.setUp();

        when(itemReference.getPath()).thenReturn(ITEM_PATH);
        when(itemReference.getName()).thenReturn(ITEM_NAME);
        fileNode = new FileNode(projectNode,
                                itemReference,
                                treeStructure,
                                eventBus,
                                projectServiceClient,
                                dtoUnmarshallerFactory,
                                editorAgent);

        final List<TreeNode<?>> children = new ArrayList<>();
        when(projectNode.getChildren()).thenReturn(children);
        when(projectNode.getPath()).thenReturn(PARENT_PATH);

        openedEditorsMap = new TreeMap<>();
        openedEditorsMap.put(ITEM_PATH, editorPartPresenter);

        when(dtoUnmarshallerFactory.newUnmarshaller(ItemReference.class)).thenReturn(unmarshaller);

        when(editorAgent.getOpenedEditors()).thenReturn(openedEditorsMap);
    }

    @Test
    public void testGetName() throws Exception {
        assertEquals(ITEM_NAME, fileNode.getName());
    }

    @Test
    public void testGetPath() throws Exception {
        assertEquals(ITEM_PATH, fileNode.getPath());
    }

    @Test
    public void testGetProject() throws Exception {
        assertEquals(projectNode, fileNode.getProject());
    }

    @Test
    public void shouldBeLeaf() throws Exception {
        assertTrue(fileNode.isLeaf());
    }

    @Test
    public void shouldFireFileOpenEventOnProcessNodeAction() throws Exception {
        fileNode.processNodeAction();
        verify(eventBus).fireEvent(Matchers.<FileEvent>anyObject());
    }

    @Test
    public void shouldBeRenemable() throws Exception {
        assertTrue(fileNode.isRenamable());
    }

    @Test
    public void fileShouldBeRenamedSuccessfully() throws Exception {
        openedEditorsMap.put(RENAMED_ITEM_PATH, editorPartPresenter);

        fileNode.rename(NEW_NAME, renameCallback);

        verify(projectServiceClient).rename(eq(ITEM_PATH), eq(NEW_NAME), isNull(String.class), voidArgumentCaptor.capture());
        GwtReflectionUtils.callOnSuccess(voidArgumentCaptor.getValue(), (Void)null);

        verify(projectServiceClient).getItem(eq(RENAMED_ITEM_PATH), itemArgumentCaptor.capture());
        GwtReflectionUtils.callOnSuccess(itemArgumentCaptor.getValue(), result);

        assertThat(fileNode.getData(), is(result));
        verify(editorAgent).getOpenedEditors();
        verify(editorAgent).updateEditorNode(ITEM_PATH, fileNode);

        verify(eventBus).fireEvent(any(NodeChangedEvent.class));
    }

    @Test
    public void fileShouldBeRenamedFailed() throws Exception {
        openedEditorsMap.put(RENAMED_ITEM_PATH, editorPartPresenter);

        fileNode.rename(NEW_NAME, renameCallback);

        verify(projectServiceClient).rename(eq(ITEM_PATH), eq(NEW_NAME), isNull(String.class), voidArgumentCaptor.capture());
        GwtReflectionUtils.callOnFailure(voidArgumentCaptor.getValue(), throwable);

        verify(renameCallback).onFailure(throwable);
    }

    @Test
    public void fileShouldBeRenamedSuccessButGettingItemFailed() {
        openedEditorsMap.put(RENAMED_ITEM_PATH, editorPartPresenter);

        fileNode.rename(NEW_NAME, renameCallback);

        verify(projectServiceClient).rename(eq(ITEM_PATH), eq(NEW_NAME), isNull(String.class), voidArgumentCaptor.capture());
        GwtReflectionUtils.callOnSuccess(voidArgumentCaptor.getValue(), (Void)null);

        verify(projectServiceClient).getItem(eq(RENAMED_ITEM_PATH), itemArgumentCaptor.capture());
        GwtReflectionUtils.callOnFailure(itemArgumentCaptor.getValue(), throwable);

        renameCallback.onFailure(throwable);
    }

    @Test
    public void shouldBeDeletable() throws Exception {
        assertTrue(fileNode.isDeletable());
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
        DeleteCallback callback = mock(DeleteCallback.class);

        fileNode.delete(callback);

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
        DeleteCallback callback = mock(DeleteCallback.class);

        fileNode.delete(callback);

        verify(projectServiceClient).delete(eq(ITEM_PATH), Matchers.<AsyncRequestCallback<Void>>anyObject());
        verify(callback).onFailure(Matchers.<Throwable>anyObject());
    }

    @Test
    public void testGettingContentWhenGetContentIsSuccessful() throws Exception {
        final String content = "content";

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<String> callback = (AsyncRequestCallback<String>)arguments[1];
                GwtReflectionUtils.callOnSuccess(callback, content);
                return callback;
            }
        }).when(projectServiceClient).getFileContent(anyString(), (AsyncRequestCallback<String>)anyObject());
        AsyncCallback<String> callback = mock(AsyncCallback.class);

        fileNode.getContent(callback);

        verify(projectServiceClient).getFileContent(eq(ITEM_PATH), Matchers.<AsyncRequestCallback<String>>anyObject());
        verify(callback).onSuccess(eq(content));
    }

    @Test
    public void testGettingContentWhenGetContentIsFailed() throws Exception {
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<String> callback = (AsyncRequestCallback<String>)arguments[1];
                GwtReflectionUtils.callOnFailure(callback, mock(Throwable.class));
                return callback;
            }
        }).when(projectServiceClient).getFileContent(anyString(), (AsyncRequestCallback<String>)anyObject());
        AsyncCallback<String> callback = mock(AsyncCallback.class);

        fileNode.getContent(callback);

        verify(projectServiceClient).getFileContent(eq(ITEM_PATH), Matchers.<AsyncRequestCallback<String>>anyObject());
        verify(callback).onFailure(Matchers.<Throwable>anyObject());
    }

    @Test
    public void testUpdatingContentWhenUpdateContentIsSuccessful() throws Exception {
        final String newContent = "new content";

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<Void> callback = (AsyncRequestCallback<Void>)arguments[3];
                GwtReflectionUtils.callOnSuccess(callback, (Void)null);
                return callback;
            }
        }).when(projectServiceClient).updateFile(anyString(), anyString(), anyString(), (AsyncRequestCallback<Void>)anyObject());
        AsyncCallback<Void> callback = mock(AsyncCallback.class);

        fileNode.updateContent(newContent, callback);

        verify(projectServiceClient).updateFile(eq(ITEM_PATH), eq(newContent), anyString(),
                                                Matchers.<AsyncRequestCallback<Void>>anyObject());
        verify(callback).onSuccess(Matchers.<Void>anyObject());
    }

    @Test
    public void testUpdatingContentWhenUpdateContentIsFailed() throws Exception {
        final String newContent = "new content";

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                AsyncRequestCallback<Void> callback = (AsyncRequestCallback<Void>)arguments[3];
                GwtReflectionUtils.callOnFailure(callback, mock(Throwable.class));
                return callback;
            }
        }).when(projectServiceClient).updateFile(anyString(), anyString(), anyString(), (AsyncRequestCallback<Void>)anyObject());
        AsyncCallback<Void> callback = mock(AsyncCallback.class);

        fileNode.updateContent(newContent, callback);

        verify(projectServiceClient).updateFile(eq(ITEM_PATH), eq(newContent), anyString(),
                                                Matchers.<AsyncRequestCallback<Void>>anyObject());
        verify(callback).onFailure(Matchers.<Throwable>anyObject());
    }

    @Test
    public void dataShouldBeUpdated() {
        String expectedPath = "/project/folderRenamed/file_name";
        openedEditorsMap.put(expectedPath, editorPartPresenter);

        fileNode.updateData(asyncCallback, PARENT_PATH + "/" + NEW_NAME);

        verify(projectServiceClient).getItem(eq(PARENT_PATH + "/" + NEW_NAME), itemArgumentCaptor.capture());
        GwtReflectionUtils.callOnSuccess(itemArgumentCaptor.getValue(), result);

        assertThat(fileNode.getData(), is(result));
        verify(asyncCallback).onSuccess(null);
        verify(editorAgent).getOpenedEditors();
        verify(editorAgent).updateEditorNode(ITEM_PATH, fileNode);
    }

    @Test
    public void dataShouldBeUpdatedButEditorReferenceShouldNotBeUpdatedBecauseThisFileIsNotOpened() {
        openedEditorsMap = new TreeMap<>();
        when(editorAgent.getOpenedEditors()).thenReturn(openedEditorsMap);

        fileNode.updateData(asyncCallback, PARENT_PATH + "/" + NEW_NAME);

        verify(projectServiceClient).getItem(eq(PARENT_PATH + "/" + NEW_NAME), itemArgumentCaptor.capture());
        GwtReflectionUtils.callOnSuccess(itemArgumentCaptor.getValue(), result);

        assertThat(fileNode.getData(), is(result));
        verify(asyncCallback).onSuccess(null);
        verify(editorAgent).getOpenedEditors();
        verify(editorAgent, never()).updateEditorNode(anyString(), eq(fileNode));
    }

    @Test
    public void dataShouldBeUpdatedFailed() {
        String expectedPath = "/project/folderRenamed/file_name";
        openedEditorsMap.put(expectedPath, editorPartPresenter);

        fileNode.updateData(asyncCallback, PARENT_PATH + "/" + NEW_NAME);

        verify(projectServiceClient).getItem(eq(PARENT_PATH + "/" + NEW_NAME), itemArgumentCaptor.capture());
        GwtReflectionUtils.callOnFailure(itemArgumentCaptor.getValue(), throwable);

        verify(asyncCallback).onFailure(throwable);
        verify(editorAgent, never()).getOpenedEditors();
    }
}
