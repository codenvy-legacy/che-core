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

import org.eclipse.che.api.vfs.server.VirtualFile;
import org.eclipse.che.api.vfs.server.VirtualFileSystem;
import org.junit.Before;
import org.junit.Test;

import static org.eclipse.che.api.vfs.server.VirtualFileFilter.ACCEPT_ALL;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MemoryLuceneSearcherProviderTest {
    private MemoryLuceneSearcherProvider memoryLuceneSearcherProvider;

    @Before
    public void setUp() throws Exception {
        memoryLuceneSearcherProvider = new MemoryLuceneSearcherProvider(ACCEPT_ALL);
    }

    @Test
    public void doesNotCreateSearcherWhenItIsNotCreatedYetAndCreationIsDisabled() throws Exception {
        VirtualFileSystem virtualFileSystem = mockVirtualFileSystem();
        assertNull(memoryLuceneSearcherProvider.getSearcher(virtualFileSystem, false));
    }

    @Test
    public void createsAndInitializeSearcherWhenCreationRequested() throws Exception {
        VirtualFileSystem virtualFileSystem = mockVirtualFileSystem();
        assertNotNull(memoryLuceneSearcherProvider.getSearcher(virtualFileSystem, true));
    }

    @Test
    public void returnsSameInstanceOfSearcherOnceItWasCreated() throws Exception {
        VirtualFileSystem virtualFileSystem = mockVirtualFileSystem();

        Searcher searcher = memoryLuceneSearcherProvider.getSearcher(virtualFileSystem, true);
        assertNotNull(searcher);
        assertSame(searcher, memoryLuceneSearcherProvider.getSearcher(virtualFileSystem, false));
    }

    @Test
    public void searcherIsResetInProviderAfterClosingSearcher() throws Exception {
        VirtualFileSystem virtualFileSystem = mockVirtualFileSystem();
        Searcher searcher = memoryLuceneSearcherProvider.getSearcher(virtualFileSystem, true);
        assertNotNull(searcher);
        searcher.close();
        assertNull(memoryLuceneSearcherProvider.getSearcher(virtualFileSystem, false));
    }

    private VirtualFileSystem mockVirtualFileSystem() {
        VirtualFileSystem virtualFileSystem = mock(VirtualFileSystem.class);
        VirtualFile root = mock(VirtualFile.class);
        when(virtualFileSystem.getRoot()).thenReturn(root);
        return virtualFileSystem;
    }
}