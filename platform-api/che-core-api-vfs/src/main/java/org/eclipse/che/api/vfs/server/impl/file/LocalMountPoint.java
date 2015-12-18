/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * <p/>
 * Contributors:
 * Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.vfs.server.impl.file;

import com.google.common.annotations.Beta;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.core.util.ValueHolder;
import org.eclipse.che.api.vfs.server.MountPoint;
import org.eclipse.che.api.vfs.server.Path;
import org.eclipse.che.api.vfs.server.PathLockFactory;
import org.eclipse.che.api.vfs.server.VirtualFile;
import org.eclipse.che.api.vfs.server.VirtualFileFilter;
import org.eclipse.che.api.vfs.server.VirtualFileVisitor;
import org.eclipse.che.api.vfs.server.observation.CreateEvent;
import org.eclipse.che.api.vfs.server.observation.UpdateContentEvent;
import org.eclipse.che.api.vfs.server.search.SearcherProvider;
import org.eclipse.che.api.vfs.server.util.DeleteOnCloseFileInputStream;
import org.eclipse.che.api.vfs.server.util.NotClosableInputStream;
import org.eclipse.che.api.vfs.server.util.ZipContent;
import org.eclipse.che.commons.lang.NameGenerator;
import org.eclipse.che.commons.lang.Pair;
import org.eclipse.che.commons.lang.cache.Cache;
import org.eclipse.che.commons.lang.cache.LoadingValueSLRUCache;
import org.eclipse.che.commons.lang.cache.SynchronizedCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.eclipse.che.commons.lang.IoUtil.GIT_FILTER;
import static org.eclipse.che.commons.lang.IoUtil.deleteRecursive;
import static org.eclipse.che.commons.lang.IoUtil.nioCopy;
import static org.eclipse.che.commons.lang.Strings.nullToEmpty;

/**
 * Local filesystem implementation of MountPoint.
 *
 * @author andrew00x
 */
public class LocalMountPoint implements MountPoint {
    private static final Logger LOG = LoggerFactory.getLogger(LocalMountPoint.class);

    /*
     * Configuration parameters for caches.
     * Caches are split to the few partitions to reduce lock contention.
     * Use SLRU cache algorithm here.
     * This is required some additional parameters, e.g. protected and probationary size.
     * See details about SLRU algorithm: http://en.wikipedia.org/wiki/Cache_algorithms#Segmented_LRU
     */
    private static final int CACHE_PARTITIONS_NUM        = 1 << 3;
    private static final int CACHE_PROTECTED_SIZE        = 100;
    private static final int CACHE_PROBATIONARY_SIZE     = 200;
    private static final int MASK                        = CACHE_PARTITIONS_NUM - 1;
    private static final int PARTITION_PROTECTED_SIZE    = CACHE_PROTECTED_SIZE / CACHE_PARTITIONS_NUM;
    private static final int PARTITION_PROBATIONARY_SIZE = CACHE_PROBATIONARY_SIZE / CACHE_PARTITIONS_NUM;
    // end cache parameters

    private static final int MAX_BUFFER_SIZE  = 200 * 1024; // 200k
    private static final int COPY_BUFFER_SIZE = 8 * 1024; // 8k

    private static final long LOCK_FILE_TIMEOUT     = 60000; // 60 seconds
    private static final int  FILE_LOCK_MAX_THREADS = 1024;

    static final String SERVICE_DIR = ".vfs";

    static final String ACL_DIR         = SERVICE_DIR + File.separatorChar + "acl";
    static final String ACL_FILE_SUFFIX = "_acl";

    static final String LOCKS_DIR        = SERVICE_DIR + File.separatorChar + "locks";
    static final String LOCK_FILE_SUFFIX = "_lock";

    static final String PROPS_DIR              = SERVICE_DIR + File.separatorChar + "props";
    static final String PROPERTIES_FILE_SUFFIX = "_props";


    /** Hide .vfs directory. */
    private static final java.io.FilenameFilter SERVICE_DIR_FILTER = new java.io.FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return !(SERVICE_DIR.equals(name));
        }
    };

    /** Hide .vfs and .git directories. */
    private static final java.io.FilenameFilter SERVICE_GIT_DIR_FILTER = new OrFileNameFilter(SERVICE_DIR_FILTER, GIT_FILTER);

    private static class OrFileNameFilter implements java.io.FilenameFilter {
        private final java.io.FilenameFilter[] filters;

        private OrFileNameFilter(java.io.FilenameFilter... filters) {
            this.filters = filters;
        }

        @Override
        public boolean accept(File dir, String name) {
            for (java.io.FilenameFilter filter : filters) {
                if (!filter.accept(dir, name)) {
                    return false;
                }
            }

            return true;
        }
    }

    private static final FileLock NO_LOCK = new FileLock("no_lock", 0);

    private class FileLockCache extends LoadingValueSLRUCache<Path, FileLock> {
        FileLockCache() {
            super(PARTITION_PROTECTED_SIZE, PARTITION_PROBATIONARY_SIZE);
        }

        @Override
        protected FileLock loadValue(Path key) {
            DataInputStream dis = null;

            try {
                final Path lockFilePath = getLockFilePath(key);
                final File lockIoFile = new File(ioRoot, toIoPath(lockFilePath));
                if (lockIoFile.exists()) {
                    final PathLockFactory.PathLock lockFilePathLock =
                            pathLockFactory.getLock(lockFilePath, false).acquire(LOCK_FILE_TIMEOUT);
                    try {
                        dis = new DataInputStream(new BufferedInputStream(new FileInputStream(lockIoFile)));
                        return locksSerializer.read(dis);
                    } finally {
                        lockFilePathLock.release();
                    }
                }
                return NO_LOCK;
            } catch (IOException e) {
                String msg = String.format("Unable read lock for '%s'. ", key);
                LOG.error(msg + e.getMessage(), e); // More details in log but do not show internal error to caller.
                throw new RuntimeException(msg);
            } finally {
                closeQuietly(dis);
            }
        }
    }


    private class FileMetadataCache extends LoadingValueSLRUCache<Path, Map<String, String[]>> {
        FileMetadataCache() {
            super(PARTITION_PROTECTED_SIZE, PARTITION_PROBATIONARY_SIZE);
        }

        @Override
        protected Map<String, String[]> loadValue(Path key) {
            DataInputStream dis = null;
            try {
                final Path metadataFilePath = getMetadataFilePath(key);
                File metadataIoFile = new File(ioRoot, toIoPath(metadataFilePath));
                if (metadataIoFile.exists()) {
                    final PathLockFactory.PathLock metadataFilePathLock =
                            pathLockFactory.getLock(metadataFilePath, false).acquire(LOCK_FILE_TIMEOUT);
                    try {
                        dis = new DataInputStream(new BufferedInputStream(new FileInputStream(metadataIoFile)));
                        return metadataSerializer.read(dis);
                    } finally {
                        metadataFilePathLock.release();
                    }
                }
                return Collections.emptyMap();
            } catch (IOException e) {
                String msg = String.format("Unable read properties for '%s'. ", key);
                LOG.error(msg + e.getMessage(), e); // More details in log but do not show internal error to caller.
                throw new RuntimeException(msg);
            } finally {
                closeQuietly(dis);
            }
        }
    }


    private final File             ioRoot;
    private final EventService     eventService;
    private final SearcherProvider searcherProvider;

    /* NOTE -- This does not related to virtual file system locking in any kind. -- */
    private final PathLockFactory pathLockFactory;

    private final LocalVirtualFile root;

    /* ----- Virtual file system lock feature. ----- */
    private final FileLockSerializer      locksSerializer;
    private final Cache<Path, FileLock>[] lockTokensCache;

    /* ----- File metadata. ----- */
    private final FileMetadataSerializer               metadataSerializer;
    private final Cache<Path, Map<String, String[]>>[] metadataCache;

    @SuppressWarnings("unchecked")
    LocalMountPoint(File ioRoot, EventService eventService, SearcherProvider searcherProvider) {
        this.ioRoot = ioRoot;
        this.eventService = eventService;
        this.searcherProvider = searcherProvider;

        root = new LocalVirtualFile(ioRoot, Path.ROOT, this);
        pathLockFactory = new PathLockFactory(FILE_LOCK_MAX_THREADS);

        locksSerializer = new FileLockSerializer();
        lockTokensCache = new Cache[CACHE_PARTITIONS_NUM];

        metadataSerializer = new FileMetadataSerializer();
        metadataCache = new Cache[CACHE_PARTITIONS_NUM];

        for (int i = 0; i < CACHE_PARTITIONS_NUM; i++) {
            lockTokensCache[i] = new SynchronizedCache(new FileLockCache());
            metadataCache[i] = new SynchronizedCache(new FileMetadataCache());
        }
    }

    @Override
    public LocalVirtualFile getRoot() {
        return root;
    }

    @Override
    public SearcherProvider getSearcherProvider() {
        return searcherProvider;
    }

    @Override
    public EventService getEventService() {
        return eventService;
    }

    public LocalVirtualFile getVirtualFile(String path) throws NotFoundException, ForbiddenException, ServerException {
        if (path == null || path.isEmpty() || "/".equals(path)) {
            return getRoot();
        }
        return doGetVirtualFile(Path.fromString(path));
    }

    private LocalVirtualFile doGetVirtualFile(Path vfsPath) throws NotFoundException, ForbiddenException, ServerException {
        final LocalVirtualFile virtualFile =
                new LocalVirtualFile(new File(ioRoot, toIoPath(vfsPath)), vfsPath, this);
        if (virtualFile.exists()) {
            return virtualFile;
        }
        throw new NotFoundException(String.format("Object '%s' does not exists. ", vfsPath));
    }

    /** Call after unmount this MountPoint. Clear all caches. */
    public void reset() {
        clearMetadataCache();
        clearLockTokensCache();
    }

    // Used in tests. Need this to check state of PathLockFactory.
    // All locks MUST be released at the end of request lifecycle.
    PathLockFactory getPathLockFactory() {
        return pathLockFactory;
    }

   /* =================================== INTERNAL =================================== */

    // All methods below designed to be used from VirtualFileImpl ONLY.

    LocalVirtualFile getParent(LocalVirtualFile virtualFile) {
        if (virtualFile.isRoot()) {
            return null;
        }
        final Path parentPath = virtualFile.getPath().getParent();
        return new LocalVirtualFile(new File(ioRoot, toIoPath(parentPath)), parentPath, this);
    }


    LocalVirtualFile getChild(LocalVirtualFile parent, Path path) {
        if (parent.isFile()) {
            return null;
        }
        final LocalVirtualFile child = new LocalVirtualFile(new File(parent.getIoFile(), path.toString()), path, this);
        if (child.exists()) {
            return child;
        }

        return null;
    }


    List<VirtualFile> getChildren(LocalVirtualFile parent, VirtualFileFilter filter) throws ServerException {
        if (!parent.isFolder()) {
            return Collections.emptyList();
        }

        final List<VirtualFile> children = doGetChildren(parent, SERVICE_DIR_FILTER);
        Collections.sort(children);
        return children;
    }


    private List<VirtualFile> doGetChildren(LocalVirtualFile virtualFile, java.io.FilenameFilter filter) throws ServerException {
        final String[] names = virtualFile.getIoFile().list(filter);
        if (names == null) {
            // Something wrong. According to java docs may be null only if i/o error occurs.
            throw new ServerException(String.format("Unable get children '%s'. ", virtualFile.getPath()));
        }
        final List<VirtualFile> children = new ArrayList<>(names.length);
        for (String name : names) {
            final Path childPath = virtualFile.getPath().newPath(name);
            children.add(new LocalVirtualFile(new File(ioRoot, toIoPath(childPath)), childPath, this));
        }
        return children;
    }


    LocalVirtualFile createFile(LocalVirtualFile parent, String name, InputStream content)
            throws ForbiddenException, ConflictException, ServerException {
        checkName(name);

        if (parent.isFolder()) {
            final Path newPath = parent.getPath().newPath(name);
            final File newIoFile = new File(ioRoot, toIoPath(newPath));
            try {
                if (!newIoFile.createNewFile()) {
                    throw new ConflictException(String.format("Item '%s' already exists. ", newPath));
                }
            } catch (IOException e) {
                String msg = String.format("Unable create new file '%s'. ", newPath);
                LOG.error(msg + e.getMessage(), e);
                throw new ServerException(msg);
            }

            final LocalVirtualFile newVirtualFile = new LocalVirtualFile(newIoFile, newPath, this);
            if (content != null) {
                doUpdateContent(newVirtualFile, content);
            }

            if (searcherProvider != null) {
                try {
                    searcherProvider.getSearcher(this, true).add(newVirtualFile);
                } catch (ServerException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
            return newVirtualFile;
        } else {
            throw new ForbiddenException("Unable create new file. Item specified as parent is not a folder. ");
        }
    }


    LocalVirtualFile createFolder(LocalVirtualFile parent, String name) throws ForbiddenException, ConflictException, ServerException {
        checkName(name);

        if (!parent.isFolder()) {
            throw new ForbiddenException("Unable create folder. Item specified as parent is not a folder. ");
        }

        // Name may be hierarchical, e.g. folder1/folder2/folder3.
        // Some folder in hierarchy may already exists but at least one folder must be created.
        // If no one folder created then ItemAlreadyExistException is thrown.
        Path currentPath = parent.getPath();
        Path newPath = null;
        File newIoFile = null;
        for (String element : Path.fromString(name).elements()) {
            currentPath = currentPath.newPath(element);
            File currentIoFile = new File(ioRoot, toIoPath(currentPath));
            if (currentIoFile.mkdir()) {
                newPath = currentPath;
                newIoFile = currentIoFile;
            }
        }

        if (newPath == null) {
            // Folder or folder hierarchy already exists.
            throw new ConflictException(String.format("Item '%s' already exists. ", parent.getPath().newPath(name)));
        }

        // Return first created folder, e.g. assume we need create: folder1/folder2/folder3 in specified folder.
        // If folder1 already exists then return folder2 as first created in hierarchy.
        final LocalVirtualFile newVirtualFile = new LocalVirtualFile(newIoFile, newPath, this);
//        eventService.publish(new CreateEvent(workspaceId, newVirtualFile.getPath(), true));
        return newVirtualFile;
    }

    LocalVirtualFile copy(LocalVirtualFile source, LocalVirtualFile parent) throws ForbiddenException, ConflictException, ServerException {
        return copy(source, parent, null, false);
    }

    /**
     * Copy a VirtualFileImpl to a given location
     *
     * @param source the VirtualFileImpl instance to copy
     * @param parent the VirtualFileImpl (must be a folder) which will become
     * the parent of the source
     * @param name the name of the copy, can be left {@code null} or empty
     * {@code String} for current source name
     * @param overWrite should the destination be overwritten, set to true to
     * overwrite, false otherwise
     * @return an instance of VirtualFileImpl, which is the actual copy of
     * source under parent
     * @throws ForbiddenException
     * @throws ConflictException
     * @throws ServerException
     */
    @Beta
    public LocalVirtualFile copy(LocalVirtualFile source, LocalVirtualFile parent, String name, boolean overWrite)
            throws ForbiddenException, ConflictException, ServerException {
        if (source.getPath().equals(parent.getPath())) {
            throw new ForbiddenException("Item cannot be copied to itself. ");
        }
        if (!parent.isFolder()) {
            throw new ForbiddenException("Unable copy item. Item specified as parent is not a folder. ");
        }
        String newName = nullToEmpty(name).trim().isEmpty() ? source.getName() : name;
        final Path newPath = parent.getPath().newPath(newName); // TODO: change name here
        final File theFile = new File(ioRoot, toIoPath(newPath));
        final LocalVirtualFile destination = new LocalVirtualFile(theFile, newPath, this);

        // checking override
        if (destination.exists()) {
            doOverWrite(overWrite, destination, newPath);
        }

        doCopy(source, destination);
//        eventService.publish(new CreateEvent(workspaceId, destination.getPath(), source.isFolder()));
        return destination;
    }


    private void doCopy(LocalVirtualFile source, LocalVirtualFile destination) throws ServerException {
        try {
            // First copy metadata (properties) for source.
            // If we do in this way and fail cause to any i/o or
            // other error client will see error and may try to copy again.
            // But if we successfully copy tree (or single file) and then
            // fail to copy metadata client may not try to copy again
            // because copy destination already exists.

            // NOTE: Don't copy locks, just files itself and metadata files.

            final File sourceMetadataFile = new File(ioRoot, toIoPath(getMetadataFilePath(source.getPath())));
            final File destinationMetadataFile = new File(ioRoot, toIoPath(getMetadataFilePath(destination.getPath())));
            if (sourceMetadataFile.exists()) {
                nioCopy(sourceMetadataFile, destinationMetadataFile, null);
            }
            nioCopy(source.getIoFile(), destination.getIoFile(), null);

            if (searcherProvider != null) {
                try {
                    searcherProvider.getSearcher(this, true).add(destination);
                } catch (ServerException e) {
                    LOG.error(e.getMessage(), e); // just log about i/o error in index
                }
            }
        } catch (IOException e) {
            // Do nothing for file tree. Let client side decide what to do.
            // User may delete copied files (if any) and try copy again.
            String msg = String.format("Unable copy '%s' to '%s'. ", source, destination);
            LOG.error(msg + e.getMessage(), e); // More details in log but do not show internal error to caller.
            throw new ServerException(msg);
        }
    }


    LocalVirtualFile rename(LocalVirtualFile virtualFile, String newName, String lockToken)
            throws ForbiddenException, ConflictException, ServerException {
        if (virtualFile.isRoot()) {
            throw new ForbiddenException("Unable rename root folder. ");
        }
        final Path sourcePath = virtualFile.getPath();
        if (virtualFile.isFile() && !validateLockTokenIfLocked(virtualFile, lockToken)) {
            throw new ForbiddenException(String.format("Unable rename file '%s'. File is locked. ", sourcePath));
        }
        final String name = virtualFile.getName();
        final LocalVirtualFile renamed;
        if (!(newName == null || name.equals(newName))) {
            final Path newPath = virtualFile.getPath().getParent().newPath(newName);
            renamed = new LocalVirtualFile(new File(ioRoot, toIoPath(newPath)), newPath, this);
            if (renamed.exists()) {
                throw new ConflictException(String.format("Item '%s' already exists. ", renamed.getName()));
            }
            doCopy(virtualFile, renamed);
            doDelete(virtualFile, lockToken);
        } else {
            renamed = virtualFile;
        }

//        eventService.publish(new RenameEvent(workspaceId, renamed.getPath(), sourcePath, renamed.isFolder()));
        return renamed;
    }


    LocalVirtualFile move(LocalVirtualFile source, LocalVirtualFile parent, String lockToken)
            throws ForbiddenException, ConflictException, ServerException {
        return move(source, parent, null, false, lockToken);
    }

    /**
     * Move a VirtualFileImpl to a given location
     *
     * @param source the VirtualFileImpl instance to move
     * @param parent the VirtualFileImpl (must be a folder) which will become
     * the parent of the source
     * @param name a new name for the moved source, can be left {@code null} or
     * empty {@code String} for current source name
     * @param overWrite should the destination be overwritten, set to true to
     * overwrite, false otherwise
     * @return an instance of VirtualFileImpl, source under parent
     * @throws ForbiddenException
     * @throws ConflictException
     * @throws ServerException
     */
    @Beta
    LocalVirtualFile move(LocalVirtualFile source, LocalVirtualFile parent, String name, boolean overWrite, String lockToken)
            throws ForbiddenException, ConflictException, ServerException {
        final Path sourcePath = source.getPath();
        final Path parentPath = parent.getPath();
        if (source.isRoot()) {
            throw new ForbiddenException("Unable move root folder. ");
        }
        if (source.getPath().equals(parent.getPath())) {
            throw new ForbiddenException("Item cannot be moved to itself. ");
        }
        if (!parent.isFolder()) {
            throw new ForbiddenException("Unable move. Item specified as parent is not a folder. ");
        }
        if (source.isFolder() && parent.getPath().isChild(source.getPath())) {
            throw new ForbiddenException(String.format("Unable move item '%s' to '%s'. Item may not have itself as parent. ",
                                                       sourcePath, parentPath));
        }

        // Even we check lock before delete original file check it here also to have better behaviour.
        // Prevent even copy original file if we already know it is locked.
        if (source.isFile() && !validateLockTokenIfLocked(source, lockToken)) {
            throw new ForbiddenException(String.format("Unable move file '%s'. File is locked. ", sourcePath));
        }

        String newName = nullToEmpty(name).trim().isEmpty() ? source.getName() : name;
        final Path newPath = parent.getPath().newPath(newName);
        LocalVirtualFile destination
                = new LocalVirtualFile(new File(ioRoot, toIoPath(newPath)), newPath, this);

        // checking override
        if (destination.exists()) {
            doOverWrite(overWrite, destination, newPath);
        }

        // use copy and delete
        doCopy(source, destination);
        doDelete(source, lockToken);
//        eventService.publish(new MoveEvent(workspaceId, destination.getPath(), sourcePath, destination.isFolder()));
        return destination;
    }

    private void doOverWrite(boolean overWrite, LocalVirtualFile destination, final Path newPath)
            throws ForbiddenException, ConflictException, ServerException {
        // if we override, then dest needs to be erased before proceeding with copy
        if (overWrite) {
            String token = null;
            if (destination.isFile()) {
                token = destination.lock(0);
            }
            destination.delete(token);
        } else {
            throw new ConflictException(String.format("Item '%s' already exists. ", newPath));
        }
    }

    InputStream getContent(LocalVirtualFile virtualFile) throws ForbiddenException, ServerException {
        if (!virtualFile.isFile()) {
            throw new ForbiddenException(String.format("Unable get content. Item '%s' is not a file. ", virtualFile.getPath()));
        }

        final PathLockFactory.PathLock lock = pathLockFactory.getLock(virtualFile.getPath(), false).acquire(LOCK_FILE_TIMEOUT);
        try {
            final File ioFile = virtualFile.getIoFile();
            FileInputStream fIn = null;
            try {
                final long fLength = ioFile.length();
                if (fLength <= MAX_BUFFER_SIZE) {
                    // If file small enough save its content in memory.
                    fIn = new FileInputStream(ioFile);
                    final byte[] buff = new byte[(int)fLength];
                    int offset = 0;
                    int len = buff.length;
                    int r;
                    while ((r = fIn.read(buff, offset, len)) > 0) {
                        offset += r;
                        len -= r;
                    }
                    return new ByteArrayInputStream(buff);
                }

                // Otherwise copy this file to be able release the file lock before leave this method.
                final File f = File.createTempFile("spool_file", null);
                nioCopy(ioFile, f, null);
                return new DeleteOnCloseFileInputStream(f);
            } catch (IOException e) {
                String msg = String.format("Unable get content of '%s'. ", virtualFile.getPath());
                LOG.error(msg + e.getMessage(), e); // More details in log but do not show internal error to caller.
                throw new ServerException(msg);
            } finally {
                closeQuietly(fIn);
            }
        } finally {
            lock.release();
        }
    }


    void updateContent(LocalVirtualFile virtualFile, InputStream content, String lockToken) throws ForbiddenException, ServerException {
        if (!virtualFile.isFile()) {
            throw new ForbiddenException(String.format("Unable update content. Item '%s' is not file. ", virtualFile.getPath()));
        }

        if (!validateLockTokenIfLocked(virtualFile, lockToken)) {
            throw new ForbiddenException(String.format("Unable update content of file '%s'. File is locked. ", virtualFile.getPath()));
        }

        doUpdateContent(virtualFile, content);

        if (searcherProvider != null) {
            try {
                searcherProvider.getSearcher(this, true).update(virtualFile);
            } catch (ServerException e) {
                LOG.error(e.getMessage(), e);
            }
        }
//        eventService.publish(new UpdateContentEvent(workspaceId, virtualFile.getPath()));
    }


    private void doUpdateContent(LocalVirtualFile virtualFile, String mediaType, InputStream content) throws ServerException {
        final PathLockFactory.PathLock lock = pathLockFactory.getLock(virtualFile.getPath(), true).acquire(LOCK_FILE_TIMEOUT);
        try {
            _doUpdateContent(virtualFile, content);
            setProperty(virtualFile, "vfs:mimeType", mediaType);
        } finally {
            lock.release();
        }
    }

    private void doUpdateContent(LocalVirtualFile virtualFile, InputStream content) throws ServerException {
        final PathLockFactory.PathLock lock = pathLockFactory.getLock(virtualFile.getPath(), true).acquire(LOCK_FILE_TIMEOUT);
        try {
            _doUpdateContent(virtualFile, content);
        } finally {
            lock.release();
        }
    }

    // UNDER LOCK
    private void _doUpdateContent(LocalVirtualFile virtualFile, InputStream content) throws ServerException {
        FileOutputStream fOut = null;
        try {
            fOut = new FileOutputStream(virtualFile.getIoFile());
            final byte[] buff = new byte[COPY_BUFFER_SIZE];
            int r;
            while ((r = content.read(buff)) != -1) {
                fOut.write(buff, 0, r);
            }
        } catch (IOException e) {
            String msg = String.format("Unable set content of '%s'. ", virtualFile.getPath());
            LOG.error(msg + e.getMessage(), e); // More details in log but do not show internal error to caller.
            throw new ServerException(msg);
        } finally {
            closeQuietly(fOut);
        }
    }


    void delete(LocalVirtualFile virtualFile, String lockToken) throws ForbiddenException, ServerException {
        if (virtualFile.isRoot()) {
            throw new ForbiddenException("Unable delete root folder. ");
        }
        final Path myPath = virtualFile.getPath();
        final boolean folder = virtualFile.isFolder();
        if (virtualFile.isFile() && !validateLockTokenIfLocked(virtualFile, lockToken)) {
            throw new ForbiddenException(String.format("Unable delete item '%s'. Item is locked. ", myPath));
        }

        doDelete(virtualFile, lockToken);
//        eventService.publish(new DeleteEvent(workspaceId, myPath, folder));
    }

    private void doDelete(LocalVirtualFile virtualFile, String lockToken) throws ForbiddenException, ServerException {
        if (virtualFile.isFolder()) {
            final LinkedList<VirtualFile> q = new LinkedList<>();
            q.add(virtualFile);
            while (!q.isEmpty()) {
                for (VirtualFile child : doGetChildren((LocalVirtualFile)q.pop(), SERVICE_GIT_DIR_FILTER)) {
                    if (child.isFolder()) {
                        q.push(child);
                    } else if (isLocked((LocalVirtualFile)child)) {
                        // Do not check lock token here. It checked only when remove file directly.
                        // If folder contains locked children it may not be deleted.
                        throw new ForbiddenException(String.format("Unable delete item '%s'. Child item '%s' is locked. ",
                                                                   virtualFile.getPath(), child.getPath()));
                    }
                }
            }
        }

        // unlock file
        if (virtualFile.isFile()) {
            final FileLock fileLock = checkIsLockValidAndGet(virtualFile);
            if (NO_LOCK != fileLock) {
                doUnlock(virtualFile, fileLock, lockToken);
            }
        }

        // clear caches
        clearLockTokensCache();
        clearMetadataCache();

        final Path path = virtualFile.getPath();
        boolean isFile = virtualFile.isFile();
        if (!deleteRecursive(virtualFile.getIoFile())) {
            LOG.error("Unable delete file {}", virtualFile.getIoFile());
            throw new ServerException(String.format("Unable delete item '%s'. ", path));
        }

        // delete metadata file
        final File metadataFile = new File(ioRoot, toIoPath(getMetadataFilePath(virtualFile.getPath())));
        if (metadataFile.delete()) {
            if (metadataFile.exists()) {
                LOG.error("Unable delete file metadata {}", metadataFile);
                throw new ServerException(String.format("Unable delete item '%s'. ", path));
            }
        }

        if (searcherProvider != null) {
            try {
                searcherProvider.getSearcher(this, true).delete(path.toString(), isFile);
            } catch (ServerException e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }


    private void clearLockTokensCache() {
        for (Cache<Path, FileLock> cache : lockTokensCache) {
            cache.clear();
        }
    }


    private void clearMetadataCache() {
        for (Cache<Path, Map<String, String[]>> cache : metadataCache) {
            cache.clear();
        }
    }


    InputStream zip(LocalVirtualFile virtualFile) throws ForbiddenException, ServerException {
        if (!virtualFile.isFolder()) {
            throw new ForbiddenException(String.format("Unable export to zip. Item '%s' is not a folder. ", virtualFile.getPath()));
        }
        File zipFile = null;
        FileOutputStream out = null;
        try {
            zipFile = File.createTempFile("export", ".zip");
            out = new FileOutputStream(zipFile);
            final ZipOutputStream zipOut = new ZipOutputStream(out);
            final LinkedList<VirtualFile> q = new LinkedList<>();
            q.add(virtualFile);
            final int zipEntryNameTrim = virtualFile.getPath().length();
            final byte[] buff = new byte[COPY_BUFFER_SIZE];
            while (!q.isEmpty()) {
                for (VirtualFile current : doGetChildren((LocalVirtualFile)q.pop(), SERVICE_GIT_DIR_FILTER)) {
                    final String zipEntryName = current.getPath().subPath(zipEntryNameTrim).toString().substring(1);
                    if (current.isFile()) {
                        final ZipEntry zipEntry = new ZipEntry(zipEntryName);
                        zipOut.putNextEntry(zipEntry);
                        InputStream in = null;
                        final PathLockFactory.PathLock lock =
                                pathLockFactory.getLock(current.getPath(), false).acquire(LOCK_FILE_TIMEOUT);
                        try {
                            zipEntry.setTime(virtualFile.getLastModificationDate());
                            in = new FileInputStream(((LocalVirtualFile)current).getIoFile());
                            int r;
                            while ((r = in.read(buff)) != -1) {
                                zipOut.write(buff, 0, r);
                            }
                        } finally {
                            closeQuietly(in);
                            lock.release();
                        }
                        zipOut.closeEntry();
                    } else if (current.isFolder()) {
                        final ZipEntry zipEntry = new ZipEntry(zipEntryName + '/');
                        zipEntry.setTime(0);
                        zipOut.putNextEntry(zipEntry);
                        q.add(current);
                        zipOut.closeEntry();
                    }
                }
            }
            closeQuietly(zipOut);
            final String name = virtualFile.getName() + ".zip";
            return new DeleteOnCloseFileInputStream(zipFile);
        } catch (IOException | RuntimeException ioe) {
            if (zipFile != null) {
                zipFile.delete();
            }
            throw new ServerException(ioe.getMessage(), ioe);
        } finally {
            closeQuietly(out);
        }
    }


    void unzip(LocalVirtualFile parent, InputStream zipped, boolean overwrite, int stripNumber)
            throws ForbiddenException, ConflictException, ServerException {
        if (!parent.isFolder()) {
            throw new ForbiddenException(String.format("Unable import zip content. Item '%s' is not a folder. ", parent.getPath()));
        }
        final ZipContent zipContent;
        try {
            zipContent = ZipContent.newInstance(zipped);
        } catch (IOException e) {
            throw new ServerException(e.getMessage(), e);
        }

        ZipInputStream zip = null;
        try {
            zip = new ZipInputStream(zipContent.zippedData);
            // Wrap zip stream to prevent close it. We can pass stream to other method and it can read content of current
            // ZipEntry but not able to close original stream of ZIPed data.
            InputStream noCloseZip = new NotClosableInputStream(zip);
            ZipEntry zipEntry;
            while ((zipEntry = zip.getNextEntry()) != null) {
                LocalVirtualFile current = parent;
                Path relPath = Path.fromString(zipEntry.getName());

                if (stripNumber > 0) {
                    int currentLevel = relPath.elements().length;
                    if (currentLevel <= stripNumber) {
                        continue;
                    }
                    relPath = relPath.subPath(stripNumber);
                }

                final String name = relPath.getName();
                if (relPath.length() > 1) {
                    // create all required parent directories
                    final Path parentPath = parent.getPath().newPath(relPath.subPath(0, relPath.length() - 1));
                    current = new LocalVirtualFile(new File(ioRoot, toIoPath(parentPath)), parentPath, this);
                    if (!(current.exists() || current.getIoFile().mkdirs())) {
                        throw new ServerException(String.format("Unable create directory '%s' ", parentPath));
                    }
                }
                final Path newPath = current.getPath().newPath(name);
                if (zipEntry.isDirectory()) {
                    final File dir = new File(current.getIoFile(), name);
                    if (!dir.exists()) {
                        if (dir.mkdir()) {
//                            eventService.publish(new CreateEvent(workspaceId, newPath.toString(), true));
                        } else {
                            throw new ServerException(String.format("Unable create directory '%s' ", newPath));
                        }
                    }
                } else {
                    final LocalVirtualFile file =
                            new LocalVirtualFile(new File(current.getIoFile(), name), newPath, this);
                    if (file.exists()) {
                        if (isLocked(file)) {
                            throw new ForbiddenException(String.format("File '%s' already exists and locked. ", file.getPath()));
                        }
                    }

                    boolean newFile;
                    try {
                        if (!(newFile = file.getIoFile().createNewFile())) { // atomic
                            if (!overwrite) {
                                throw new ConflictException(String.format("File '%s' already exists. ", file.getPath()));
                            }
                        }
                    } catch (IOException e) {
                        String msg = String.format("Unable create new file '%s'. ", newPath);
                        LOG.error(msg + e.getMessage(), e); // More details in log but do not show internal error to caller.
                        throw new ServerException(msg);
                    }

                    doUpdateContent(file, noCloseZip);
                    if (newFile) {
//                        eventService.publish(new CreateEvent(workspaceId, newPath.toString(), false));
                    } else {
//                        eventService.publish(new UpdateContentEvent(workspaceId, newPath.toString()));
                    }
                }
                zip.closeEntry();
            }
            if (searcherProvider != null) {
                try {
                    searcherProvider.getSearcher(this, true).add(parent);
                } catch (ServerException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        } catch (IOException e) {
            throw new ServerException(e.getMessage(), e);
        } finally {
            closeQuietly(zip);
        }
    }

   /* ============ LOCKING ============ */

    String lock(LocalVirtualFile virtualFile, long timeout) throws ForbiddenException, ConflictException, ServerException {
        if (!virtualFile.isFile()) {
            throw new ForbiddenException(String.format("Unable lock '%s'. Locking allowed for files only. ", virtualFile.getPath()));
        }

        return doLock(virtualFile, timeout);
    }


    private String doLock(LocalVirtualFile virtualFile, long timeout) throws ConflictException, ServerException {
        final int index = virtualFile.getPath().hashCode() & MASK;
        if (NO_LOCK == lockTokensCache[index].get(virtualFile.getPath())) // causes read from file if need.
        {
            final String lockToken = NameGenerator.generate(null, 16);
            final long expired = timeout > 0 ? (System.currentTimeMillis() + timeout) : Long.MAX_VALUE;
            final FileLock fileLock = new FileLock(lockToken, expired);
            DataOutputStream dos = null;
            try {
                final Path lockFilePath = getLockFilePath(virtualFile.getPath());
                final File lockIoFile = new File(ioRoot, toIoPath(lockFilePath));
                lockIoFile.getParentFile().mkdirs(); // Ignore result of 'mkdirs' here. If we are failed to create
                // directory we will get FileNotFoundException at the next line when try to create FileOutputStream.
                final PathLockFactory.PathLock lockFilePathLock = pathLockFactory.getLock(lockFilePath, true).acquire(LOCK_FILE_TIMEOUT);
                try {
                    dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(lockIoFile)));
                    locksSerializer.write(dos, fileLock);
                } finally {
                    lockFilePathLock.release();
                }
            } catch (IOException e) {
                String msg = String.format("Unable lock file '%s'. ", virtualFile.getPath());
                LOG.error(msg + e.getMessage(), e); // More details in log but do not show internal error to caller.
                throw new ServerException(msg);
            } finally {
                closeQuietly(dos);
            }

            // Save lock token in cache if lock successful.
            lockTokensCache[index].put(virtualFile.getPath(), fileLock);
            return lockToken;
        }

        throw new ConflictException(String.format("Unable lock file '%s'. File already locked. ", virtualFile.getPath()));
    }


    void unlock(LocalVirtualFile virtualFile, String lockToken) throws ForbiddenException, ConflictException, ServerException {
        if (lockToken == null) {
            throw new ForbiddenException("Null lock token. ");
        }
        if (!virtualFile.isFile()) {
            // Locks available for files only.
            throw new ConflictException(String.format("Item '%s' is not locked. ", virtualFile.getPath()));
        }
        final FileLock fileLock = checkIsLockValidAndGet(virtualFile);
        if (NO_LOCK == fileLock) {
            throw new ConflictException(String.format("File '%s' is not locked. ", virtualFile.getPath()));
        }
        doUnlock(virtualFile, fileLock, lockToken);
    }

    private void doUnlock(LocalVirtualFile virtualFile, FileLock lock, String lockToken) throws ForbiddenException, ServerException {
        final int index = virtualFile.getPath().hashCode() & MASK;
        try {
            if (!lock.getLockToken().equals(lockToken)) {
                throw new ForbiddenException(String.format("Unable unlock file '%s'. Lock token does not match. ", virtualFile.getPath()));
            }
            final File lockIoFile = new File(ioRoot, toIoPath(getLockFilePath(virtualFile.getPath())));
            if (!lockIoFile.delete()) {
                throw new IOException(String.format("Unable delete lock file %s. ", lockIoFile));
            }
            // Mark as unlocked in cache.
            lockTokensCache[index].put(virtualFile.getPath(), NO_LOCK);
        } catch (IOException e) {
            String msg = String.format("Unable unlock file '%s'. ", virtualFile.getPath());
            LOG.error(msg + e.getMessage(), e); // More details in log but do not show internal error to caller.
            throw new ServerException(msg);
        }
    }


    boolean isLocked(LocalVirtualFile virtualFile) {
        return virtualFile.isFile() && NO_LOCK != checkIsLockValidAndGet(virtualFile);
    }

    private FileLock checkIsLockValidAndGet(LocalVirtualFile virtualFile) {
        final int index = virtualFile.getPath().hashCode() & MASK;
        // causes read from file if need
        final FileLock lock = lockTokensCache[index].get(virtualFile.getPath());
        if (NO_LOCK == lock) {
            return NO_LOCK;
        }
        if (lock.getExpired() < System.currentTimeMillis()) {
            final File lockIoFile = new File(ioRoot, toIoPath(getLockFilePath(virtualFile.getPath())));
            if (!lockIoFile.delete()) {
                if (lockIoFile.exists()) {
                    // just warn here
                    LOG.warn("Unable delete lock file %s. ", lockIoFile);
                }
            }
            lockTokensCache[index].put(virtualFile.getPath(), NO_LOCK);
            return NO_LOCK;
        }
        return lock;
    }


    private boolean validateLockTokenIfLocked(LocalVirtualFile virtualFile, String checkLockToken) {
        final FileLock lock = checkIsLockValidAndGet(virtualFile);
        return NO_LOCK == lock || lock.getLockToken().equals(checkLockToken);
    }


    private Path getLockFilePath(Path virtualFilePath) {
        return virtualFilePath.isRoot()
               ? virtualFilePath.newPath(LOCKS_DIR, virtualFilePath.getName() + LOCK_FILE_SUFFIX)
               : virtualFilePath.getParent().newPath(LOCKS_DIR, virtualFilePath.getName() + LOCK_FILE_SUFFIX);
    }

   /* ============ METADATA  ============ */

    Map<String, List<String>> getProperties(LocalVirtualFile virtualFile) {
        // Do not check permission here. We already check 'read' permission when get VirtualFile.
//        final Map<String, String[]> metadata = getFileMetadata(virtualFile);
//        final List<Property> result = new ArrayList<>(metadata.size());
//        for (Map.Entry<String, String[]> e : metadata.entrySet()) {
//            final String name = e.getKey();
//            if (filter.accept(name)) {
//                final Property property = DtoFactory.getInstance().createDto(Property.class).withName(name);
//                if (e.getValue() != null) {
//                    List<String> list = new ArrayList<>(e.getValue().length);
//                    Collections.addAll(list, e.getValue());
//                    property.setValue(list);
//                }
//                result.add(property);
//            }
//        }
        return null;
    }


    void updateProperties(LocalVirtualFile virtualFile, Map<String, List<String>> properties, String lockToken)
            throws ForbiddenException, ServerException {
//        final int index = virtualFile.getPath().hashCode() & MASK;
//
//        if (virtualFile.isFile() && !validateLockTokenIfLocked(virtualFile, lockToken)) {
//            throw new ForbiddenException(
//                    String.format("Unable update properties of item '%s'. Item is locked. ", virtualFile.getPath()));
//        }
//
//        // 1. make copy of properties
//        final Map<String, String[]> metadata = copyMetadataMap(metadataCache[index].get(virtualFile.getPath()));
//        // 2. update
//        for (Property property : properties) {
//            final String name = property.getName();
//            final List<String> value = property.getValue();
//            if (value != null) {
//                metadata.put(name, value.toArray(new String[value.size()]));
//            } else {
//                metadata.remove(name);
//            }
//        }
//
//        // 3. save in file
//        saveFileMetadata(virtualFile, metadata);
//        // 4. update cache
//        metadataCache[index].put(virtualFile.getPath(), metadata);
//        // 5. update last modification time
//        if (!virtualFile.getIoFile().setLastModified(System.currentTimeMillis())) {
//            LOG.warn("Unable to set timestamp to '{}'. ", virtualFile.getIoFile());
//        }
//        eventService.publish(new UpdatePropertiesEvent(workspaceId, virtualFile.getPath(), virtualFile.isFolder()));
    }


    private Map<String, String[]> getFileMetadata(LocalVirtualFile virtualFile) {
        final int index = virtualFile.getPath().hashCode() & MASK;
        return copyMetadataMap(metadataCache[index].get(virtualFile.getPath()));
    }


    String getPropertyValue(LocalVirtualFile virtualFile, String name) {
        // Do not check permission here. We already check 'read' permission when get VirtualFile.
        final int index = virtualFile.getPath().hashCode() & MASK;
        final String[] value = metadataCache[index].get(virtualFile.getPath()).get(name);
        return value == null || value.length == 0 ? null : value[0];
    }


    String[] getPropertyValues(LocalVirtualFile virtualFile, String name) {
        // Do not check permission here. We already check 'read' permission when get VirtualFile.
        final int index = virtualFile.getPath().hashCode() & MASK;
        final String[] value = metadataCache[index].get(virtualFile.getPath()).get(name);
        final String[] copyValue = new String[value.length];
        System.arraycopy(value, 0, copyValue, 0, value.length);
        return copyValue;
    }


    void setProperty(LocalVirtualFile virtualFile, String name, String value) throws ServerException {
        setProperty(virtualFile, name, value == null ? null : new String[]{value});
    }


    void setProperty(LocalVirtualFile virtualFile, String name, String... value) throws ServerException {
        final int index = virtualFile.getPath().hashCode() & MASK;
        // 1. make copy of properties
        final Map<String, String[]> metadata = copyMetadataMap(metadataCache[index].get(virtualFile.getPath()));
        // 2. update
        if (value != null) {
            String[] copyValue = new String[value.length];
            System.arraycopy(value, 0, copyValue, 0, value.length);
            metadata.put(name, copyValue);
        } else {
            metadata.remove(name);
        }
        // 3. save in file
        saveFileMetadata(virtualFile, metadata);
        // 4. update cache
        metadataCache[index].put(virtualFile.getPath(), metadata);
    }


    private void saveFileMetadata(LocalVirtualFile virtualFile, Map<String, String[]> properties) throws ServerException {
        DataOutputStream dos = null;

        try {
            final Path metadataFilePath = getMetadataFilePath(virtualFile.getPath());
            final File metadataFile = new File(ioRoot, toIoPath(metadataFilePath));
            if (properties.isEmpty()) {
                if (!metadataFile.delete()) {
                    if (metadataFile.exists()) {
                        throw new IOException(String.format("Unable delete file '%s'. ", metadataFile));
                    }
                }
            } else {
                metadataFile.getParentFile().mkdirs(); // Ignore result of 'mkdirs' here. If we are failed to create
                // directory we will get FileNotFoundException at the next line when try to create FileOutputStream.
                final PathLockFactory.PathLock lock = pathLockFactory.getLock(metadataFilePath, true).acquire(LOCK_FILE_TIMEOUT);
                try {
                    dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(metadataFile)));
                    metadataSerializer.write(dos, properties);
                } finally {
                    lock.release();
                }
            }
        } catch (IOException e) {
            String msg = String.format("Unable save properties for '%s'. ", virtualFile.getPath());
            LOG.error(msg + e.getMessage(), e); // More details in log but do not show internal error to caller.
            throw new ServerException(msg);
        } finally {
            closeQuietly(dos);
        }
    }


    private Path getMetadataFilePath(Path virtualFilePath) {
        return virtualFilePath.isRoot()
               ? virtualFilePath.newPath(PROPS_DIR, virtualFilePath.getName() + PROPERTIES_FILE_SUFFIX)
               : virtualFilePath.getParent().newPath(PROPS_DIR, virtualFilePath.getName() + PROPERTIES_FILE_SUFFIX);
    }

   /* ==================================== */

    List<Pair<String, String>> countMd5Sums(LocalVirtualFile virtualFile) throws ServerException {
        if (!virtualFile.isFolder()) {
            return Collections.emptyList();
        }
        final List<Pair<String, String>> hashes = new ArrayList<>();
        final int trimPathLength = virtualFile.getPath().length() + 1;
        final HashFunction hashFunction = Hashing.md5();
        final ValueHolder<ServerException> errorHolder = new ValueHolder<>();
        virtualFile.accept(new VirtualFileVisitor() {
            @Override
            public void visit(final VirtualFile virtualFile) {
                try {
                    if (virtualFile.isFile()) {
                        hashes.add(
                                Pair.of(countHashSum(virtualFile, hashFunction), virtualFile.getPath().subPath(trimPathLength).toString()));
                    } else {
                        for (VirtualFile file : virtualFile.getChildren(VirtualFileFilter.ALL)) {
                            file.accept(this);
                        }
                    }
                } catch (ServerException e) {
                    errorHolder.set(e);
                }
            }
        });
        return hashes;
    }


    private String countHashSum(VirtualFile virtualFile, HashFunction hashFunction) throws ServerException {
        // TODO
        final PathLockFactory.PathLock lock = pathLockFactory.getLock(virtualFile.getPath(), false).acquire(LOCK_FILE_TIMEOUT);
        try (InputStream contentStream = virtualFile.getContent()) {
            return ByteSource.wrap(ByteStreams.toByteArray(contentStream)).hash(hashFunction).toString();
        } catch (ForbiddenException e) {
            throw new ServerException(e.getServiceError());
        } catch (IOException e) {
            throw new ServerException(e);
        } finally {
            lock.release();
        }
    }

   /* ============ HELPERS  ============ */

    /* Relative system path */
    private String toIoPath(Path vfsPath) {
        if (vfsPath.isRoot()) {
            return "";
        }
        if ('/' == File.separatorChar) {
            // Unix like system. Use vfs path as relative i/o path.
            return vfsPath.toString();
        }
        return vfsPath.join(File.separatorChar);
    }

    private Map<String, String[]> copyMetadataMap(Map<String, String[]> source) {
        final Map<String, String[]> copyMap = new HashMap<>(source.size());
        for (Map.Entry<String, String[]> e : source.entrySet()) {
            String[] value = e.getValue();
            String[] copyValue = new String[value.length];
            System.arraycopy(value, 0, copyValue, 0, value.length);
            copyMap.put(e.getKey(), copyValue);
        }
        return copyMap;
    }


    private void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {
            }
        }
    }


    private void checkName(String name) throws ServerException {
        if (name == null || name.trim().isEmpty()) {
            throw new ServerException("Item's name is not set. ");
        }
    }
}
