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
package org.eclipse.che.api.vfs.server;

import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.vfs.server.VirtualFileSystem.CloseCallback;
import org.eclipse.che.api.vfs.server.search.MediaTypeFilter;
import org.eclipse.che.commons.env.EnvironmentContext;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.google.common.collect.Lists.newArrayList;

public abstract class AbstractVirtualFileSystemProvider implements VirtualFileSystemProvider {
    protected final VirtualFileFilter                        fileIndexFilter;
    protected final ConcurrentMap<String, VirtualFileSystem> fileSystems;

    /**
     * @param fileIndexFilters
     *         set filter for files that should not be indexed
     */
    public AbstractVirtualFileSystemProvider(Set<VirtualFileFilter> fileIndexFilters) {
        fileIndexFilter = mergeFileIndexFilters(fileIndexFilters);
        fileSystems = new ConcurrentHashMap<>();
    }

    private VirtualFileFilter mergeFileIndexFilters(Set<VirtualFileFilter> fileIndexFilters) {
        final VirtualFileFilter filter;
        if (fileIndexFilters.isEmpty()) {
            filter = new MediaTypeFilter();
        } else {
            final List<VirtualFileFilter> myFilters = newArrayList(new MediaTypeFilter());
            myFilters.addAll(fileIndexFilters);
            filter = VirtualFileFilters.createAndFilter(myFilters);
        }
        return filter;
    }

    @Override
    public VirtualFileSystem getVirtualFileSystem(String workspaceId) throws ServerException {
        VirtualFileSystem fileSystem = fileSystems.get(workspaceId);
        if (fileSystem == null) {
            VirtualFileSystem newFileSystem = createVirtualFileSystem(workspaceId, () -> fileSystems.remove(workspaceId));
            fileSystem = fileSystems.putIfAbsent(workspaceId, newFileSystem);
            if (fileSystem == null) {
                fileSystem = newFileSystem;
            }
        }
        return fileSystem;
    }

    @Override
    public VirtualFileSystem getVirtualFileSystem() throws ServerException {
        return getVirtualFileSystem(EnvironmentContext.getCurrent().getWorkspaceId());
    }

    protected abstract VirtualFileSystem createVirtualFileSystem(String  workspaceId, CloseCallback closeCallback) throws ServerException;

    @Override
    public void close() throws ServerException {
        for (VirtualFileSystem fileSystem : fileSystems.values()) {
            fileSystem.close();
        }
    }
}
