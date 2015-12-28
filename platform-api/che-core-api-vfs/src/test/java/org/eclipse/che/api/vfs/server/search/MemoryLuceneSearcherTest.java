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
package org.eclipse.che.api.vfs.server.search;

import org.eclipse.che.api.vfs.server.ArchiverFactory;
import org.eclipse.che.api.vfs.server.VirtualFile;
import org.eclipse.che.api.vfs.server.VirtualFileFilter;
import org.eclipse.che.api.vfs.server.VirtualFileSystem;
import org.eclipse.che.api.vfs.server.impl.memory.MemoryVirtualFileSystem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MemoryLuceneSearcherTest {
    private static final String TEST_CONTENT_ONE = "to be or not to be";
    private static final String TEST_CONTENT_TWO = "maybe you should think twice";

    private MemoryLuceneSearcher   searcher;
    private VirtualFileFilter      filter;
    private Searcher.CloseCallback closeCallback;

    @Before
    public void setUp() throws Exception {
        filter = mock(VirtualFileFilter.class);
        when(filter.accept(any(VirtualFile.class))).thenReturn(true);
        closeCallback = mock(Searcher.CloseCallback.class);
        searcher = new MemoryLuceneSearcher(filter, closeCallback);
    }

    @After
    public void tearDown() throws Exception {
        searcher.close();
    }

    @Test
    public void initializesIndexForExistedFiles() throws Exception {
        VirtualFileSystem virtualFileSystem = virtualFileSystem();
        addTestData(virtualFileSystem.getRoot());
        searcher.init(virtualFileSystem);

        String[] searchResult = searcher.search(new QueryExpression().setText("think"));
        assertEquals(newArrayList("/folder/zzz.txt"), newArrayList(searchResult));
    }

    @Test
    public void addsSingleFileInIndex() throws Exception {
        VirtualFileSystem virtualFileSystem = virtualFileSystem();
        searcher.init(virtualFileSystem);
        VirtualFile file = virtualFileSystem.getRoot().createFolder("aaa").createFile("aaa.txt", TEST_CONTENT_TWO);

        searcher.add(file);

        String[] searchResult = searcher.search(new QueryExpression().setText("should"));
        assertEquals(newArrayList(file.getPath().toString()), newArrayList(searchResult));
    }

    @Test
    public void addsFileTreeInIndex() throws Exception {
        VirtualFileSystem virtualFileSystem = virtualFileSystem();
        searcher.init(virtualFileSystem);
        addTestData(virtualFileSystem.getRoot());

        searcher.add(virtualFileSystem.getRoot());

        String[] searchResult = searcher.search(new QueryExpression().setText("be"));
        assertEquals(newArrayList("/folder/xxx.txt"), newArrayList(searchResult));
        searchResult = searcher.search(new QueryExpression().setText("should"));
        assertEquals(newArrayList("/folder/zzz.txt"), newArrayList(searchResult));
    }

    @Test
    public void updatesSingleFileInIndex() throws Exception {
        VirtualFileSystem virtualFileSystem = virtualFileSystem();
        VirtualFile file = virtualFileSystem.getRoot().createFolder("aaa").createFile("aaa.txt", TEST_CONTENT_ONE);
        searcher.init(virtualFileSystem);

        String[] searchResult = searcher.search(new QueryExpression().setText("should"));
        assertEquals(0, searchResult.length);

        file.updateContent(TEST_CONTENT_TWO);
        searcher.update(file);

        searchResult = searcher.search(new QueryExpression().setText("should"));

        assertEquals(newArrayList(file.getPath().toString()), newArrayList(searchResult));
    }

    @Test
    public void deletesSingleFileFromIndex() throws Exception {
        VirtualFileSystem virtualFileSystem = virtualFileSystem();
        VirtualFile file = virtualFileSystem.getRoot().createFolder("aaa").createFile("aaa.txt", TEST_CONTENT_ONE);
        searcher.init(virtualFileSystem);

        String[] searchResult = searcher.search(new QueryExpression().setText("be"));
        assertEquals(newArrayList(file.getPath().toString()), newArrayList(searchResult));

        searcher.delete(file.getPath().toString(), file.isFile());

        searchResult = searcher.search(new QueryExpression().setText("be"));
        assertEquals(0, searchResult.length);
    }

    @Test
    public void deletesFileTreeFromIndex() throws Exception {
        VirtualFileSystem virtualFileSystem = virtualFileSystem();
        addTestData(virtualFileSystem.getRoot());
        searcher.init(virtualFileSystem);

        String[] searchResult = searcher.search(new QueryExpression().setText("be"));
        assertEquals(newArrayList("/folder/xxx.txt"), newArrayList(searchResult));
        searchResult = searcher.search(new QueryExpression().setText("should"));
        assertEquals(newArrayList("/folder/zzz.txt"), newArrayList(searchResult));

        searcher.delete("/folder", false);

        searchResult = searcher.search(new QueryExpression().setText("be"));
        assertEquals(0, searchResult.length);
        searchResult = searcher.search(new QueryExpression().setText("should"));
        assertEquals(0, searchResult.length);
    }

    @Test
    public void searchesByTextAndFileName() throws Exception {
        VirtualFileSystem virtualFileSystem = virtualFileSystem();
        VirtualFile folder = virtualFileSystem.getRoot().createFolder("folder");
        folder.createFile("xxx.txt", TEST_CONTENT_ONE);
        folder.createFile("zzz.txt", TEST_CONTENT_ONE);
        searcher.init(virtualFileSystem);

        String[] searchResult = searcher.search(new QueryExpression().setText("be").setName("xxx.txt"));
        assertEquals(newArrayList("/folder/xxx.txt"), newArrayList(searchResult));
    }

    @Test
    public void searchesByTextAndPath() throws Exception {
        VirtualFileSystem virtualFileSystem = virtualFileSystem();
        VirtualFile folder1 = virtualFileSystem.getRoot().createFolder("folder1/a/b");
        VirtualFile folder2 = virtualFileSystem.getRoot().createFolder("folder2");
        folder1.createFile("xxx.txt", TEST_CONTENT_ONE);
        folder2.createFile("zzz.txt", TEST_CONTENT_ONE);
        searcher.init(virtualFileSystem);

        String[] searchResult = searcher.search(new QueryExpression().setText("be").setPath("/folder1"));
        assertEquals(newArrayList("/folder1/a/b/xxx.txt"), newArrayList(searchResult));
    }

    @Test
    public void searchesByTextAndPathAndFileName() throws Exception {
        VirtualFileSystem virtualFileSystem = virtualFileSystem();
        VirtualFile folder1 = virtualFileSystem.getRoot().createFolder("folder1/a/b");
        VirtualFile folder2 = virtualFileSystem.getRoot().createFolder("folder2/a/b");
        folder1.createFile("xxx.txt", TEST_CONTENT_ONE);
        folder1.createFile("yyy.txt", TEST_CONTENT_ONE);
        folder2.createFile("zzz.txt", TEST_CONTENT_ONE);
        searcher.init(virtualFileSystem);

        String[] searchResult = searcher.search(new QueryExpression().setText("be").setPath("/folder1").setName("xxx.txt"));
        assertEquals(newArrayList("/folder1/a/b/xxx.txt"), newArrayList(searchResult));
    }

    @Test
    public void closesLuceneIndexWriterWhenSearcherClosed() throws Exception {
        VirtualFileSystem virtualFileSystem = virtualFileSystem();
        searcher.init(virtualFileSystem);

        searcher.close();

        assertTrue(searcher.isClosed());
        assertFalse(searcher.getIndexWriter().isOpen());
    }

    @Test
    public void notifiesCallbackWhenSearcherClosed() throws Exception {
        VirtualFileSystem virtualFileSystem = virtualFileSystem();
        searcher.init(virtualFileSystem);

        searcher.close();
        verify(closeCallback).onClose();
    }

    @Test
    public void excludesFilesFromIndexWithFilter() throws Exception {
        VirtualFileSystem virtualFileSystem = virtualFileSystem();
        VirtualFile folder = virtualFileSystem.getRoot().createFolder("folder");
        folder.createFile("xxx.txt", TEST_CONTENT_ONE);
        folder.createFile("yyy.txt", TEST_CONTENT_ONE);
        folder.createFile("zzz.txt", TEST_CONTENT_ONE);

        when(filter.accept(withName("yyy.txt"))).thenReturn(false);
        searcher.init(virtualFileSystem);

        String[] searchResult = searcher.search(new QueryExpression().setText("be"));
        assertEquals(newArrayList("/folder/xxx.txt", "/folder/zzz.txt"), newArrayList(searchResult));
    }

    private VirtualFileSystem virtualFileSystem() throws Exception {
        return new MemoryVirtualFileSystem(mock(ArchiverFactory.class), null);
    }

    private void addTestData(VirtualFile vfsRoot) throws Exception {
        VirtualFile folder = vfsRoot.createFolder("folder");
        folder.createFile("xxx.txt", TEST_CONTENT_ONE);
        folder.createFile("zzz.txt", TEST_CONTENT_TWO);
    }

    private static VirtualFile withName(String name) {
        return argThat(new ArgumentMatcher<VirtualFile>() {
            @Override
            public boolean matches(Object argument) {
                return name.equals(((VirtualFile)argument).getName());
            }
        });
    }
}