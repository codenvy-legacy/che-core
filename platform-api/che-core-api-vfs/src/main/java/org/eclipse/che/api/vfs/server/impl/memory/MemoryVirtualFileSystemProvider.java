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
package org.eclipse.che.api.vfs.server.impl.memory;

import org.eclipse.che.api.vfs.server.AbstractVirtualFileSystemProvider;
import org.eclipse.che.api.vfs.server.ArchiverFactory;
import org.eclipse.che.api.vfs.server.VirtualFileFilter;
import org.eclipse.che.api.vfs.server.VirtualFileSystem;
import org.eclipse.che.api.vfs.server.search.MemoryLuceneSearcherProvider;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Set;

@Singleton
public class MemoryVirtualFileSystemProvider extends AbstractVirtualFileSystemProvider {
    /**
     * @param fileIndexFilters
     *         set filter for files that should not be indexed
     */
    @Inject
    public MemoryVirtualFileSystemProvider(@Named("vfs.index_filter") Set<VirtualFileFilter> fileIndexFilters) {
        super(fileIndexFilters);
    }

    @Override
    protected VirtualFileSystem createVirtualFileSystem(String  workspaceId, VirtualFileSystem.CloseCallback closeCallback) {
        return new MemoryVirtualFileSystem(new ArchiverFactory(),
                                           new MemoryLuceneSearcherProvider(fileIndexFilter),
                                           closeCallback);
    }
}
