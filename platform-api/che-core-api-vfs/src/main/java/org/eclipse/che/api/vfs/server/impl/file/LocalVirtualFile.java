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

import com.google.common.io.ByteStreams;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.vfs.server.Path;
import org.eclipse.che.api.vfs.server.VirtualFile;
import org.eclipse.che.api.vfs.server.VirtualFileFilter;
import org.eclipse.che.api.vfs.server.VirtualFileVisitor;
import org.eclipse.che.commons.lang.Pair;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Implementation of VirtualFile which uses java.io.File.
 *
 * @author andrew00x
 */
public class LocalVirtualFile implements VirtualFile {
    private final java.io.File    ioFile;
    private final Path            path;
    private final LocalMountPoint mountPoint;

    LocalVirtualFile(java.io.File ioFile, Path path, LocalMountPoint mountPoint) {
        this.ioFile = ioFile;
        this.path = path;
        this.mountPoint = mountPoint;
    }

    @Override
    public String getName() {
        return path.getName();
    }

    @Override
    public Path getPath() {
        return path;
    }

    @Override
    public boolean exists() {
        return getIoFile().exists();
    }

    @Override
    public boolean isRoot() {
        return path.isRoot();
    }

    @Override
    public boolean isFile() {
        return getIoFile().isFile();
    }

    @Override
    public boolean isFolder() {
        return getIoFile().isDirectory();
    }

    @Override
    public VirtualFile getParent() {
        return mountPoint.getParent(this);
    }

    @Override
    public List<VirtualFile> getChildren(VirtualFileFilter filter) throws ServerException {
        return mountPoint.getChildren(this, filter);
    }

    @Override
    public List<VirtualFile> getChildren() throws ServerException {
        return mountPoint.getChildren(this, VirtualFileFilter.ALL);
    }

    @Override
    public VirtualFile getChild(Path path) throws ServerException {
        return mountPoint.getChild(this, path);
    }

    @Override
    public InputStream getContent() throws ForbiddenException, ServerException {
        return mountPoint.getContent(this);
    }

    @Override
    public byte[] getContentAsBytes() throws ForbiddenException, ServerException {
        try {
            return ByteStreams.toByteArray(getContent());
        } catch (IOException e) {
            throw new ServerException(e); // TODO
        }
    }

    @Override
    public String getContentAsString() throws ForbiddenException, ServerException {
        return new String(getContentAsBytes());
    }

    @Override
    public VirtualFile updateContent(InputStream content, String lockToken) throws ForbiddenException, ServerException {
        mountPoint.updateContent(this, content, lockToken);
        return this;
    }

    @Override
    public VirtualFile updateContent(InputStream content) throws ForbiddenException, ServerException {
        return null;
    }

    @Override
    public VirtualFile updateContent(byte[] content, String lockToken) throws ForbiddenException, ServerException {
        return null;
    }

    @Override
    public VirtualFile updateContent(byte[] content) throws ForbiddenException, ServerException {
        return null;
    }

    @Override
    public VirtualFile updateContent(String content, String lockToken) throws ForbiddenException, ServerException {
        return null;
    }

    @Override
    public VirtualFile updateContent(String content) throws ForbiddenException, ServerException {
        return null;
    }

//    @Override
//    public long getCreationDate() {
//        // Creation date may not be available from underlying file system.
//        return -1;
//    }

    @Override
    public long getLastModificationDate() {
        return getIoFile().lastModified();
    }

    @Override
    public long getLength() throws ServerException {
        return getIoFile().length();
    }

    @Override
    public Map<String, List<String>> getProperties() throws ServerException {
        return null;
    }

    @Override
    public List<String> getProperties(String name) throws ServerException {
        return null;
    }

    @Override
    public String getProperty(String name) throws ServerException {
        return null;
    }

    @Override
    public VirtualFile updateProperties(Map<String, List<String>> properties, String lockToken) throws ForbiddenException, ServerException {
        return null;
    }

    @Override
    public VirtualFile updateProperties(Map<String, List<String>> properties) throws ForbiddenException, ServerException {
        return null;
    }

    @Override
    public VirtualFile setProperties(String name, List<String> values, String lockToken) throws ForbiddenException, ServerException {
        return null;
    }

    @Override
    public VirtualFile setProperties(String name, List<String> values) throws ForbiddenException, ServerException {
        return null;
    }

    @Override
    public VirtualFile setProperty(String name, String value, String lockToken) throws ForbiddenException, ServerException {
        return null;
    }

    @Override
    public VirtualFile setProperty(String name, String value) throws ForbiddenException, ServerException {
        return null;
    }

    @Override
    public LocalVirtualFile copyTo(VirtualFile parent) throws ForbiddenException, ConflictException, ServerException {
        return copyTo(parent, null, false);
    }

    public LocalVirtualFile copyTo(VirtualFile parent, String name, boolean overwrite)
            throws ForbiddenException, ConflictException, ServerException {
        return mountPoint.copy(this, (LocalVirtualFile)parent, name, overwrite);
    }

    @Override
    public VirtualFile moveTo(VirtualFile parent) throws ForbiddenException, ConflictException, ServerException {
        return moveTo(parent, null, false, null);
    }

    public LocalVirtualFile moveTo(VirtualFile parent, String name, boolean overwrite, String lockToken)
            throws ForbiddenException, ConflictException, ServerException {
        return mountPoint.move(this, (LocalVirtualFile)parent, name, overwrite, lockToken);
    }

    @Override
    public VirtualFile rename(String newName, String lockToken)
            throws ForbiddenException, ConflictException, ServerException {
        return mountPoint.rename(this, newName, lockToken);
    }

    @Override
    public VirtualFile rename(String newName) throws ForbiddenException, ConflictException, ServerException {
        return null;
    }

    @Override
    public void delete(String lockToken) throws ForbiddenException, ServerException {
        mountPoint.delete(this, lockToken);
    }

    @Override
    public void delete() throws ForbiddenException, ServerException {
        delete(null);
    }

    //

    @Override
    public InputStream zip() throws ForbiddenException, ServerException {
        return mountPoint.zip(this);
    }

    @Override
    public void unzip(InputStream zipped, boolean overwrite, int stripNumber)
            throws ForbiddenException, ConflictException, ServerException {
        mountPoint.unzip(this, zipped, overwrite, stripNumber);
    }

    //

    @Override
    public String lock(long timeout) throws ForbiddenException, ConflictException, ServerException {
        return mountPoint.lock(this, timeout);
    }

    @Override
    public VirtualFile unlock(String lockToken) throws ForbiddenException, ConflictException, ServerException {
        mountPoint.unlock(this, lockToken);
        return this;
    }

    @Override
    public boolean isLocked() throws ServerException {
        return mountPoint.isLocked(this);
    }

    @Override
    public VirtualFile createFile(String name, InputStream content) throws ForbiddenException, ConflictException, ServerException {
        return mountPoint.createFile(this, name, content);
    }

    @Override
    public VirtualFile createFile(String name, byte[] content) throws ForbiddenException, ConflictException, ServerException {
        return createFile(name, content == null ? null : new ByteArrayInputStream(content));
    }

    @Override
    public VirtualFile createFile(String name, String content) throws ForbiddenException, ConflictException, ServerException {
        return createFile(name, content == null ? null : content.getBytes());
    }

    @Override
    public VirtualFile createFolder(String name) throws ForbiddenException, ConflictException, ServerException {
        return mountPoint.createFolder(this, name);
    }

    //

    @Override
    public LocalMountPoint getMountPoint() {
        return mountPoint;
    }

    @Override
    public void accept(VirtualFileVisitor visitor) throws ServerException {
        visitor.visit(this);
    }

    @Override
    public List<Pair<String, String>> countMd5Sums() throws ServerException {
        return mountPoint.countMd5Sums(this);
    }

    @Override
    public File toIoFile() {
        return ioFile;
    }

    @Override
    public int compareTo(VirtualFile other) {
        // To get nice order of items:
        // 1. Regular folders
        // 2. Files
        if (other == null) {
            throw new NullPointerException();
        }
        if (isFolder()) {
            return other.isFolder() ? getName().compareTo(other.getName()) : -1;
        } else if (other.isFolder()) {
            return 1;
        }
        return getName().compareTo(other.getName());
    }

   /* =================== */

    public final java.io.File getIoFile() {
        return ioFile;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o instanceof LocalVirtualFile)) {
            LocalVirtualFile other = (LocalVirtualFile)o;
            return Objects.equals(path, other.path)
                   && Objects.equals(mountPoint, other.mountPoint);
        }
        return false;

    }

    @Override
    public int hashCode() {
        return Objects.hash(path, mountPoint);
    }
}
