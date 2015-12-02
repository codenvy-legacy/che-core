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

import com.google.common.hash.Hashing;
import com.google.common.io.ByteStreams;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.vfs.server.HashSumsCounter;
import org.eclipse.che.api.vfs.server.LockedFileFinder;
import org.eclipse.che.api.vfs.server.MountPoint;
import org.eclipse.che.api.vfs.server.Path;
import org.eclipse.che.api.vfs.server.VirtualFile;
import org.eclipse.che.api.vfs.server.VirtualFileFilter;
import org.eclipse.che.api.vfs.server.VirtualFileVisitor;
import org.eclipse.che.api.vfs.server.search.SearcherProvider;
import org.eclipse.che.api.vfs.server.util.NotClosableInputStream;
import org.eclipse.che.api.vfs.server.util.ZipContent;
import org.eclipse.che.commons.lang.NameGenerator;
import org.eclipse.che.commons.lang.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;

/**
 * In-memory implementation of VirtualFile.
 * <p/>
 * NOTE: This implementation is not thread safe.
 *
 * @author andrew00x
 */
public class MemoryVirtualFile implements VirtualFile {
    private static final Logger  LOG    = LoggerFactory.getLogger(MemoryVirtualFile.class);
    private static final boolean FILE   = false;
    private static final boolean FOLDER = true;

    static MemoryVirtualFile newFile(MemoryVirtualFile parent, String name, InputStream content) throws IOException {
        return new MemoryVirtualFile(parent, name, content == null ? new byte[0] : ByteStreams.toByteArray(content));
    }

    static MemoryVirtualFile newFile(MemoryVirtualFile parent, String name, byte[] content) {
        return new MemoryVirtualFile(parent, name, content == null ? new byte[0] : Arrays.copyOf(content, content.length));
    }

    static MemoryVirtualFile newFile(MemoryVirtualFile parent, String name, String content) {
        return new MemoryVirtualFile(parent, name, content == null ? new byte[0] : content.getBytes());
    }

    static MemoryVirtualFile newFolder(MemoryVirtualFile parent, String name) {
        return new MemoryVirtualFile(parent, name);
    }

    //

    private final boolean                        type;
    private final Map<String, List<String>>      properties;
    //private final long                           creationDate;
    private final Map<String, MemoryVirtualFile> children;
    private final MemoryMountPoint               mountPoint;

    private String            name;
    private MemoryVirtualFile parent;
    private Path              path;
    private byte[]            content;
    private long              lastModificationDate;
    private LockHolder        lock;

    private boolean exists = true;

    // --- File ---
    private MemoryVirtualFile(MemoryVirtualFile parent, String name, byte[] content) {
        this.mountPoint = (MemoryMountPoint)parent.getMountPoint();
        this.parent = parent;
        this.type = FILE;
        this.name = name;
        this.properties = newHashMap();
        //this.creationDate = this.lastModificationDate = System.currentTimeMillis();
        this.content = content;
        children = Collections.emptyMap();
    }

    // --- Folder ---
    private MemoryVirtualFile(MemoryVirtualFile parent, String name) {
        this.mountPoint = (MemoryMountPoint)parent.getMountPoint();
        this.parent = parent;
        this.type = FOLDER;
        this.name = name;
        this.properties = newHashMap();
        //this.creationDate = this.lastModificationDate = System.currentTimeMillis();
        children = newHashMap();
    }

    // --- Root folder ---
    MemoryVirtualFile(MountPoint mountPoint) {
        this.mountPoint = (MemoryMountPoint)mountPoint;
        this.type = FOLDER;
        this.name = "";
        this.properties = newHashMap();
        //this.creationDate = this.lastModificationDate = System.currentTimeMillis();
        children = newHashMap();
    }

    @Override
    public String getName() {
        checkExist();
        return name;
    }

    @Override
    public Path getPath() {
        checkExist();
        MemoryVirtualFile parent = this.parent;
        if (parent == null) {
            return Path.ROOT;
        }
        if (path != null) {
            return path;
        }
        Path parentPath = parent.getPath();
        path = parentPath.newPath(getName());
        return path;
    }

    @Override
    public boolean isFile() {
        checkExist();
        return type == FILE;
    }

    @Override
    public boolean isFolder() {
        checkExist();
        return type == FOLDER;
    }

    @Override
    public boolean exists() {
        return exists;
    }

    @Override
    public boolean isRoot() {
        checkExist();
        return parent == null;
    }

//    @Override
//    public long getCreationDate() {
//        checkExist();
//        return creationDate;
//    }

    @Override
    public long getLastModificationDate() {
        checkExist();
        return lastModificationDate;
    }

    @Override
    public VirtualFile getParent() {
        checkExist();
        return parent;
    }

    @Override
    public Map<String, List<String>> getProperties() {
        checkExist();
        final Map<String, List<String>> result = newHashMap();
        for (Map.Entry<String, List<String>> e : properties.entrySet()) {
            final String name = e.getKey();
            final List<String> values = e.getValue();
            if (values != null) {
                result.put(name, newArrayList(values));
            }
        }
        return result;
    }

    @Override
    public String getProperty(String name) {
        checkExist();
        final List<String> values = properties.get(name);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }

    @Override
    public List<String> getProperties(String name) {
        checkExist();
        final List<String> values = properties.get(name);
        if (values == null || values.isEmpty()) {
            return newArrayList();
        }
        return newArrayList(values);
    }

    @Override
    public VirtualFile updateProperties(Map<String, List<String>> update, String lockToken) throws ForbiddenException {
        checkExist();
        if (isFile() && fileIsLockedAndLockTokenIsInvalid(lockToken)) {
            throw new ForbiddenException(String.format("Unable update properties of item '%s'. Item is locked. ", getPath()));
        }
        for (Map.Entry<String, List<String>> entry : update.entrySet()) {
            String name = entry.getKey();
            List<String> value = entry.getValue();
            if (value == null || value.isEmpty()) {
                properties.remove(name);
            } else {
                properties.put(name, newArrayList(value));
            }
        }
        lastModificationDate = System.currentTimeMillis();
        //mountPoint.getEventService().publish(new UpdatePropertiesEvent(mountPoint.getWorkspaceId(), getPath(), isFolder()));
        return this;
    }

    @Override
    public VirtualFile updateProperties(Map<String, List<String>> properties) throws ForbiddenException, ServerException {
        return updateProperties(properties, null);
    }

    @Override
    public VirtualFile setProperties(String name, List<String> values, String lockToken) throws ForbiddenException, ServerException {
        checkExist();
        if (isFile() && fileIsLockedAndLockTokenIsInvalid(lockToken)) {
            throw new ForbiddenException(String.format("Unable update properties of item '%s'. Item is locked. ", getPath()));
        }
        if (values == null || values.isEmpty()) {
            properties.remove(name);
        } else {
            properties.put(name, newArrayList(values));
        }
        return this;
    }

    @Override
    public VirtualFile setProperties(String name, List<String> values) throws ForbiddenException, ServerException {
        return setProperties(name, values, null);
    }

    @Override
    public VirtualFile setProperty(String name, String value, String lockToken) throws ForbiddenException, ServerException {
        return setProperties(name, value == null ? null : newArrayList(value), lockToken);
    }

    @Override
    public VirtualFile setProperty(String name, String value) throws ForbiddenException, ServerException {
        return setProperty(name, value, null);
    }

    @Override
    public void accept(VirtualFileVisitor visitor) throws ServerException {
        checkExist();
        visitor.visit(this);
    }

    @Override
    public List<Pair<String, String>> countMd5Sums() throws ServerException {
        checkExist();
        if (isFile()) {
            return newArrayList();
        }

        return new HashSumsCounter(this, Hashing.md5()).countHashSums();
    }

    @Override
    public List<VirtualFile> getChildren(VirtualFileFilter filter) {
        checkExist();
        if (isFolder()) {
            return doGetChildren(this).stream().filter(filter::accept).sorted().collect(Collectors.toList());
        }
        return newArrayList();
    }

    @Override
    public List<VirtualFile> getChildren() {
        checkExist();
        if (isFolder()) {
            List<VirtualFile> children = doGetChildren(this);
            if (children.size() > 1) {
                Collections.sort(children);
            }
            return children;
        }
        return newArrayList();
    }

    private List<VirtualFile> doGetChildren(VirtualFile folder) {
        return newArrayList(((MemoryVirtualFile)folder).children.values());
    }

    @Override
    public VirtualFile getChild(Path path) throws ServerException {
        checkExist();
        String[] elements = path.elements();
        MemoryVirtualFile child = children.get(elements[0]);
        if (child != null && elements.length > 1) {
            for (int i = 1; i < elements.length && child != null; i++) {
                if (child.isFolder()) {
                    child = child.children.get(elements[i]);
                }
            }
        }
        return child;
    }

    boolean addChild(MemoryVirtualFile child) {
        checkExist();
        final String childName = child.getName();
        if (children.get(childName) == null) {
            children.put(childName, child);
            return true;
        }
        return false;
    }

    @Override
    public InputStream getContent() throws ForbiddenException {
        return new ByteArrayInputStream(getContentAsBytes());
    }

    @Override
    public byte[] getContentAsBytes() throws ForbiddenException {
        checkExist();
        if (isFile()) {
            if (content == null) {
                content = new byte[0];
            }
            return Arrays.copyOf(content, content.length);
        }

        throw new ForbiddenException(String.format("We were unable to retrieve the content. Item '%s' is not a file. ", getPath()));
    }

    @Override
    public String getContentAsString() throws ForbiddenException {
        return new String(getContentAsBytes());
    }

    @Override
    public VirtualFile updateContent(InputStream content, String lockToken) throws ForbiddenException, ServerException {
        byte[] bytes;
        try {
            bytes = ByteStreams.toByteArray(content);
        } catch (IOException e) {
            throw new ServerException(String.format("We were unable to set the content of '%s'. Error: %s", getPath(), e.getMessage()));
        }
        doUpdateContent(bytes, lockToken);
        return this;
    }

    @Override
    public VirtualFile updateContent(byte[] content, String lockToken) throws ForbiddenException, ServerException {
        doUpdateContent(content, lockToken);
        return this;
    }

    @Override
    public VirtualFile updateContent(String content, String lockToken) throws ForbiddenException, ServerException {
        return updateContent(content.getBytes(), lockToken);
    }

    @Override
    public VirtualFile updateContent(byte[] content) throws ForbiddenException, ServerException {
        return updateContent(content, null);
    }

    @Override
    public VirtualFile updateContent(InputStream content) throws ForbiddenException, ServerException {
        return updateContent(content, null);
    }

    @Override
    public VirtualFile updateContent(String content) throws ForbiddenException, ServerException {
        return updateContent(content, null);
    }

    private void doUpdateContent(byte[] content, String lockToken) throws ForbiddenException, ServerException {
        checkExist();

        if (isFile()) {
            if (fileIsLockedAndLockTokenIsInvalid(lockToken)) {
                throw new ForbiddenException(
                        String.format("We were unable to update the content of file '%s'. The file is locked. ", getPath()));
            }

            this.content = Arrays.copyOf(content, content.length);

            SearcherProvider searcherProvider = mountPoint.getSearcherProvider();
            if (searcherProvider != null) {
                try {
                    searcherProvider.getSearcher(mountPoint, true).update(this);
                } catch (ServerException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
            lastModificationDate = System.currentTimeMillis();
        //mountPoint.getEventService().publish(new UpdateContentEvent(mountPoint.getWorkspaceId(), getPath()));
        } else {
            throw new ForbiddenException(String.format("We were unable to update the content. Item '%s' is not a file. ", getPath()));
        }
    }

    @Override
    public long getLength() {
        checkExist();
        if (isFile()) {
            return content.length;
        }
        return 0;
    }

    @Override
    public VirtualFile copyTo(VirtualFile parent) throws ForbiddenException, ConflictException, ServerException {
        return copyTo(parent, null, false);
    }

    @Override
    public VirtualFile copyTo(VirtualFile parent, String newName, boolean overwrite)
            throws ForbiddenException, ConflictException, ServerException {
        checkExist();
        ((MemoryVirtualFile)parent).checkExist();
        if (isRoot()) {
            throw new ServerException("Unable copy root folder. ");
        }
        if (newName == null || newName.trim().isEmpty()) {
            newName = this.getName();
        }
        if (parent.isFolder()) {
            VirtualFile copy = doCopy((MemoryVirtualFile)parent, newName, overwrite);
            SearcherProvider searcherProvider = mountPoint.getSearcherProvider();
            if (searcherProvider != null) {
                try {
                    searcherProvider.getSearcher(mountPoint, true).add(parent);
                } catch (ServerException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
            //mountPoint.getEventService().publish(new CreateEvent(mountPoint.getWorkspaceId(), copy.getPath(), copy.isFolder()));
            return copy;
        } else {
            throw new ForbiddenException(String.format("Unable create copy of '%s'. Item '%s' specified as parent is not a folder.",
                                                       getPath(), parent.getPath()));
        }
    }

    private VirtualFile doCopy(MemoryVirtualFile parent) throws ConflictException {
        return doCopy(parent, null, false);
    }

    private VirtualFile doCopy(MemoryVirtualFile parent, String targetName, boolean overWrite) throws ConflictException {
        if (overWrite) {
            parent.children.remove(targetName);
        }

        VirtualFile virtualFile;
        if (isFile()) {
            virtualFile = newFile(parent, targetName, Arrays.copyOf(content, content.length));
        } else {
            virtualFile = newFolder(parent, targetName);
            for (VirtualFile child : getChildren()) {
                ((MemoryVirtualFile)child).doCopy((MemoryVirtualFile)virtualFile);
            }
        }
        for (Map.Entry<String, List<String>> e : properties.entrySet()) {
            String name = e.getKey();
            List<String> value = e.getValue();
            if (value != null) {
                ((MemoryVirtualFile)virtualFile).properties.put(name, newArrayList(value));
            }
        }
        if (parent.addChild((MemoryVirtualFile)virtualFile)) {
            return virtualFile;
        }
        throw new ConflictException(String.format("Item '%s' already exists. ", parent.getPath().newPath(name)));
    }

    @Override
    public VirtualFile moveTo(VirtualFile parent) throws ForbiddenException, ConflictException, ServerException {
        return moveTo(parent, null, false, null);
    }

    @Override
    public VirtualFile moveTo(VirtualFile parent, String newName, boolean overwrite, String lockToken)
            throws ForbiddenException, ConflictException, ServerException {
        checkExist();
        ((MemoryVirtualFile)parent).checkExist();
        if (isRoot()) {
            throw new ForbiddenException("Unable move root folder. ");
        }
        if (!parent.isFolder()) {
            throw new ForbiddenException("Unable move item. Item specified as parent is not a folder. ");
        }
        if (newName == null || newName.trim().isEmpty()) {
            newName = this.getName();
        }
        final boolean isFile = isFile();
        final Path myPath = getPath();
        final Path newParentPath = parent.getPath();

        final boolean folder = isFolder();
        if (folder) {
            if (newParentPath.isChild(myPath)) {
                throw new ForbiddenException(
                        String.format("Unable move item %s to %s. Item may not have itself as parent. ", myPath, newParentPath));
            }
            final List<VirtualFile> lockedFiles = new LockedFileFinder(this).findLockedFiles();
            if (!lockedFiles.isEmpty()) {
                throw new ForbiddenException(
                        String.format("Unable move item '%s'. Child items '%s' are locked. ", getName(), lockedFiles));
            }
        } else {
            if (fileIsLockedAndLockTokenIsInvalid(lockToken)) {
                throw new ForbiddenException(String.format("Unable move item %s. Item is locked. ", myPath));
            }
        }

        if (overwrite) {
            ((MemoryVirtualFile)parent).children.remove(newName);
        }

        if (newName.equals(getName())) {
            if (((MemoryVirtualFile)parent).addChild(this)) {
                this.parent.children.remove(getName());
                this.parent = (MemoryVirtualFile)parent;
            } else {
                throw new ConflictException(String.format("Item '%s' already exists. ", parent.getPath().newPath(name)));
            }
        } else {
            if (((MemoryVirtualFile)parent).children.containsKey(newName)) {
                throw new ConflictException(String.format("Item '%s' already exists. ", parent.getPath().newPath(newName)));
            }
            this.parent.children.remove(getName());
            this.parent = (MemoryVirtualFile)parent;
            this.parent.children.put(newName, this);
            this.name = newName;
        }
        this.path = null;

        SearcherProvider searcherProvider = mountPoint.getSearcherProvider();
        if (searcherProvider != null) {
            try {
                searcherProvider.getSearcher(mountPoint, true).delete(myPath.toString(), isFile);
            } catch (ServerException e) {
                LOG.error(e.getMessage(), e);
            }
            try {
                searcherProvider.getSearcher(mountPoint, true).add(parent);
            } catch (ServerException e) {
                LOG.error(e.getMessage(), e);
            }
        }
        //mountPoint.getEventService().publish(new MoveEvent(mountPoint.getWorkspaceId(), getPath(), myPath, folder));
        return this;
    }

    @Override
    public VirtualFile rename(String newName, String lockToken)
            throws ForbiddenException, ConflictException, ServerException {
        checkExist();
        checkName(newName);
        boolean isFile = isFile();
        if (isRoot()) {
            throw new ForbiddenException("We were unable to rename a root folder.");
        }
        final Path thePath = getPath();
        final boolean isFolder = isFolder();
        if (isFolder) {
            final List<VirtualFile> lockedFiles = new LockedFileFinder(this).findLockedFiles();
            if (!lockedFiles.isEmpty()) {
                throw new ForbiddenException(
                        String.format("Unable rename item '%s'. Child items '%s' are locked. ", getName(), lockedFiles));
            }
        } else {
            if (fileIsLockedAndLockTokenIsInvalid(lockToken)) {
                throw new ForbiddenException(String.format("We were unable to rename an item '%s'." +
                                                           " The item is currently locked by the system.", getPath()));
            }
        }

        if (parent.children.get(newName) != null) {
            throw new ConflictException(String.format("Item '%s' already exists. ", newName));
        }
        parent.children.remove(name);
        parent.children.put(newName, this);
        name = newName;
        path = null;

        lastModificationDate = System.currentTimeMillis();
        SearcherProvider searcherProvider = mountPoint.getSearcherProvider();
        if (searcherProvider != null) {
            try {
                searcherProvider.getSearcher(mountPoint, true).delete(thePath.toString(), isFile);
            } catch (ServerException e) {
                LOG.error(e.getMessage(), e);
            }
            try {
                searcherProvider.getSearcher(mountPoint, true).add(parent);
            } catch (ServerException e) {
                LOG.error(e.getMessage(), e);
            }
        }
        //mountPoint.getEventService().publish(new RenameEvent(mountPoint.getWorkspaceId(), getPath(), myPath, folder));
        return this;
    }

    @Override
    public VirtualFile rename(String newName) throws ForbiddenException, ConflictException, ServerException {
        return rename(newName, null);
    }

    @Override
    public void delete(String lockToken) throws ForbiddenException, ServerException {
        checkExist();
        boolean isFile = isFile();
        if (isRoot()) {
            throw new ForbiddenException("Unable delete root folder. ");
        }
        final Path myPath = getPath();
        final boolean folder = isFolder();
        if (folder) {
            final List<VirtualFile> lockedFiles = new LockedFileFinder(this).findLockedFiles();
            if (!lockedFiles.isEmpty()) {
                throw new ForbiddenException(
                        String.format("Unable delete item '%s'. Child items '%s' are locked. ", getName(), lockedFiles));
            }
            for (VirtualFile virtualFile : getTreeAsList(this)) {
                ((MemoryVirtualFile)virtualFile).exists = false;
            }
        } else {
            if (fileIsLockedAndLockTokenIsInvalid(lockToken)) {
                throw new ForbiddenException(String.format("Unable delete item '%s'. Item is locked. ", getPath()));
            }
        }
        parent.children.remove(name);
        exists = false;
        parent = null;
        path = null;
        SearcherProvider searcherProvider = mountPoint.getSearcherProvider();
        if (searcherProvider != null) {
            try {
                searcherProvider.getSearcher(mountPoint, true).delete(myPath.toString(), isFile);
            } catch (ServerException e) {
                LOG.error(e.getMessage(), e);
            }
        }
        //mountPoint.getEventService().publish(new DeleteEvent(mountPoint.getWorkspaceId(), myPath, folder));
    }

    List<VirtualFile> getTreeAsList(VirtualFile folder) throws ServerException {
        List<VirtualFile> list = newArrayList();
        folder.accept(new VirtualFileVisitor() {
            @Override
            public void visit(VirtualFile virtualFile) throws ServerException {
                if (virtualFile.isFolder()) {
                    for (VirtualFile child : virtualFile.getChildren()) {
                        child.accept(this);
                    }
                }
                list.add(virtualFile);
            }
        });
        return list;
    }

    @Override
    public void delete() throws ForbiddenException, ServerException {
        delete(null);
    }

    @Override
    public InputStream zip() throws ForbiddenException, ServerException {
        checkExist();
        if (!isFolder()) {
            throw new ForbiddenException(String.format("Unable export to zip. Item '%s' is not a folder. ", getPath()));
        }
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            final ZipOutputStream zipOut = new ZipOutputStream(out);
            final LinkedList<VirtualFile> q = new LinkedList<>();
            q.add(this);
            while (!q.isEmpty()) {
                for (VirtualFile current : q.pop().getChildren()) {
                    final String zipEntryName = current.getPath().subPath(getPath()).toString();
                    if (current.isFile()) {
                        final ZipEntry zipEntry = new ZipEntry(zipEntryName);
                        zipEntry.setTime(current.getLastModificationDate());
                        zipOut.putNextEntry(zipEntry);
                        zipOut.write(((MemoryVirtualFile)current).content);
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
            zipOut.close();
        } catch (IOException e) {
            throw new ServerException(e.getMessage(), e);
        }
        final byte[] zipContent = out.toByteArray();
        return new ByteArrayInputStream(zipContent);
    }

    @Override
    public void unzip(InputStream zipped, boolean overwrite, int stripNumber) throws ForbiddenException, ServerException,
                                                                                     ConflictException {
        checkExist();
        if (!isFolder()) {
            throw new ForbiddenException(String.format("Unable import zip. Item '%s' is not a folder. ", getPath()));
        }
        try (ZipInputStream zip = new ZipInputStream(ZipContent.newInstance(zipped).zippedData)) {
            InputStream noCloseZip = new NotClosableInputStream(zip);
            ZipEntry zipEntry;
            while ((zipEntry = zip.getNextEntry()) != null) {
                MemoryVirtualFile current = this;
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
                    for (int i = 0, stop = relPath.length() - 1; i < stop; i++) {
                        MemoryVirtualFile folder = newFolder(current, relPath.element(i));
                        if (current.addChild(folder)) {
                            current = folder;
                        } else {
                            current = current.children.get(relPath.element(i));
                        }
                    }
                }
                if (zipEntry.isDirectory()) {
                    if (current.children.get(name) == null) {
                        MemoryVirtualFile folder = newFolder(current, name);
                        current.addChild(folder);
                        //mountPoint.getEventService().publish(new CreateEvent(mountPoint.getWorkspaceId(), folder.getPath(), true));
                    }
                } else {
                    MemoryVirtualFile file = current.children.get(name);
                    if (file == null) {
                        file = newFile(current, name, noCloseZip);
                        current.addChild(file);
                        //mountPoint.getEventService().publish(new CreateEvent(mountPoint.getWorkspaceId(), file.getPath(), false));
                    } else {
                        if (file.isLocked()) {
                            throw new ForbiddenException(String.format("File '%s' already exists and locked. ", file.getPath()));
                        }
                        if (overwrite) {
                            file.updateContent(noCloseZip, null);
                            //mountPoint.getEventService().publish(new UpdateContentEvent(mountPoint.getWorkspaceId(), file.getPath()));
                        } else {
                            throw new ConflictException(String.format("File '%s' already exists. ", file.getPath()));
                        }
                    }
                }
                zip.closeEntry();
            }
            SearcherProvider searcherProvider = mountPoint.getSearcherProvider();
            if (searcherProvider != null) {
                try {
                    searcherProvider.getSearcher(mountPoint, true).add(this);
                } catch (ServerException e) {
                    LOG.error(e.getMessage(), e);
                }
            }

        } catch (IOException e) {
            throw new ServerException(e.getMessage(), e);
        }
    }

    @Override
    public String lock(long timeout) throws ForbiddenException, ConflictException {
        checkExist();
        if (isFile()) {
            if (this.lock != null) {
                throw new ConflictException("File already locked. ");
            }
            final String lockToken = NameGenerator.generate(null, 32);
            this.lock = new LockHolder(lockToken, timeout);
            lastModificationDate = System.currentTimeMillis();
            return lockToken;
        } else {
            throw new ForbiddenException(String.format("Unable lock '%s'. Locking allowed for files only. ", getPath()));
        }
    }

    @Override
    public VirtualFile unlock(String lockToken) throws ForbiddenException, ConflictException {
        checkExist();
        if (isFile()) {
            final LockHolder theLock = lock;
            if (theLock == null) {
                throw new ConflictException("File is not locked. ");
            } else if (isExpired(theLock)) {
                lock = null;
                throw new ConflictException("File is not locked. ");
            }
            if (theLock.lockToken.equals(lockToken)) {
                lock = null;
                lastModificationDate = System.currentTimeMillis();
            } else {
                throw new ForbiddenException("Unable remove lock from file. Lock token does not match. ");
            }
            lastModificationDate = System.currentTimeMillis();
            return this;
        } else {
            throw new ForbiddenException(String.format("Unable unlock '%s'. Locking allowed for files only. ", getPath()));
        }
    }

    @Override
    public boolean isLocked() {
        checkExist();
        final LockHolder myLock = lock;
        if (myLock != null) {
            if (isExpired(myLock)) {
                lock = null;
                return false;
            }
            return true;
        }
        return false;
    }

    private boolean isExpired(LockHolder lockHolder) {
        return lockHolder.expired < System.currentTimeMillis();
    }

    @Override
    public VirtualFile createFile(String name, InputStream content)
            throws ForbiddenException, ConflictException, ServerException {
        checkExist();
        checkName(name);
        if (isFolder()) {
            final MemoryVirtualFile newFile;
            try {
                newFile = newFile(this, name, content);
            } catch (IOException e) {
                throw new ServerException(String.format("Unable set content of '%s'. Error: %s", getPath(), e.getMessage()));
            }
            if (!addChild(newFile)) {
                throw new ConflictException(String.format("Item with the name '%s' already exists", name));
            }
            SearcherProvider searcherProvider = mountPoint.getSearcherProvider();
            if (searcherProvider != null) {
                try {
                    searcherProvider.getSearcher(mountPoint, true).add(newFile);
                } catch (ServerException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
            //mountPoint.getEventService().publish(new CreateEvent(mountPoint.getWorkspaceId(), newFile.getPath(), false));
            return newFile;
        } else {
            throw new ForbiddenException("Unable create new file. Item specified as parent is not a folder. ");
        }
    }

    @Override
    public VirtualFile createFile(String name, byte[] content) throws ForbiddenException, ConflictException, ServerException {
        return createFile(name, new ByteArrayInputStream(content));
    }

    @Override
    public VirtualFile createFile(String name, String content) throws ForbiddenException, ConflictException, ServerException {
        return createFile(name, content.getBytes());
    }

    @Override
    public VirtualFile createFolder(String name) throws ForbiddenException, ConflictException, ServerException {
        checkExist();
        checkName(name);
        if (isFolder()) {
            MemoryVirtualFile newFolder = null;
            MemoryVirtualFile current = this;
            if (name.indexOf('/') > 0) {
                final Path internPath = Path.fromString(name);
                for (String element : internPath.elements()) {
                    MemoryVirtualFile folder = newFolder(current, element);
                    if (current.addChild(folder)) {
                        newFolder = folder;
                        current = folder;
                    } else {
                        current = current.children.get(element);
                    }
                }
                if (newFolder == null) {
                    // Folder or folder hierarchy already exists.
                    throw new ConflictException(String.format("Item with the name '%s' already exists. ", name));
                }
            } else {
                newFolder = newFolder(this, name);
                if (!addChild(newFolder)) {
                    throw new ConflictException(String.format("Item with the name '%s' already exists. ", name));
                }
            }
            //mountPoint.getEventService().publish(new CreateEvent(mountPoint.getWorkspaceId(), newFolder.getPath(), true));
            return newFolder;
        } else {
            throw new ForbiddenException("Unable create new folder. Item specified as parent is not a folder. ");
        }
    }

    @Override
    public java.io.File toIoFile() {
        return null;
    }

    @Override
    public MountPoint getMountPoint() {
        return mountPoint;
    }

    @Override
    public int compareTo(VirtualFile o) {
        // To get nice order of items:
        // 1. Regular folders
        // 2. Files
        if (o == null) {
            throw new NullPointerException();
        }
        if (isFolder()) {
            return o.isFolder() ? getName().compareTo(o.getName()) : -1;
        } else if (o.isFolder()) {
            return 1;
        }
        return getName().compareTo(o.getName());
    }

    @Override
    public String toString() {
        return getPath().toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MemoryVirtualFile)) {
            return false;
        }
        MemoryVirtualFile other = (MemoryVirtualFile)o;
        return mountPoint.equals(other.mountPoint) && getPath().equals(other.getPath());
    }

    @Override
    public int hashCode() {
        int hashCode = 8;
        hashCode = 31 * hashCode + mountPoint.hashCode();
        hashCode = 31 * hashCode + getPath().hashCode();
        return hashCode;
    }

    private void checkExist() {
        if (!exists) {
            throw new RuntimeException(String.format("Item '%s' already removed. ", name));
        }
    }

    private void checkName(String name) throws ServerException {
        if (name == null || name.trim().isEmpty()) {
            throw new ServerException("Item's name is not set. ");
        }
    }

    private boolean fileIsLockedAndLockTokenIsInvalid(String lockToken) {
        if (isLocked()) {
            final LockHolder myLock = lock;
            return myLock != null && !myLock.lockToken.equals(lockToken);
        }
        return false;
    }

    private static class LockHolder {
        final String lockToken;
        final long   expired;

        LockHolder(String lockToken, long timeout) {
            this.lockToken = lockToken;
            this.expired = timeout > 0 ? (System.currentTimeMillis() + timeout) : Long.MAX_VALUE;
        }
    }
}
