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
package org.eclipse.che.api.vfs.server.impl.file;

import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.vfs.server.AbstractVirtualFileSystemProvider;
import org.eclipse.che.api.vfs.server.ArchiverFactory;
import org.eclipse.che.api.vfs.server.VirtualFileFilter;
import org.eclipse.che.api.vfs.server.VirtualFileSystem;
import org.eclipse.che.api.vfs.server.search.FSLuceneSearcherProvider;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;

@Singleton
public class LocalVirtualFileSystemProvider extends AbstractVirtualFileSystemProvider {
    private final LocalFileSystemMountStrategy localFileSystemMountStrategy;
    private final File                         indexRootDirectory;

    /**
     * @param fileIndexFilters
     *         set filter for files that should not be indexed
     */
    @Inject
    public LocalVirtualFileSystemProvider(Set<VirtualFileFilter> fileIndexFilters,
                                          LocalFileSystemMountStrategy localFileSystemMountStrategy,
                                          @Named("vfs.local.fs_index_root_dir") File indexRootDirectory) throws IOException {
        super(fileIndexFilters);
        this.localFileSystemMountStrategy = localFileSystemMountStrategy;
        this.indexRootDirectory = indexRootDirectory;
        Files.createDirectories(indexRootDirectory.toPath());

    }

    @Override
    protected VirtualFileSystem createVirtualFileSystem(@Named("vfs.index_filter") String workspaceId,
                                                        VirtualFileSystem.CloseCallback closeCallback) throws ServerException {
        final File indexDirectory;
        try {
            indexDirectory = Files.createTempDirectory(indexRootDirectory.toPath(), null).toFile();
        } catch (IOException e) {
            throw new ServerException(e);
        }
        return new LocalVirtualFileSystem(localFileSystemMountStrategy.getMountPath(workspaceId),
                                          new ArchiverFactory(),
                                          new FSLuceneSearcherProvider(indexDirectory, fileIndexFilter),
                                          closeCallback);
    }
}
