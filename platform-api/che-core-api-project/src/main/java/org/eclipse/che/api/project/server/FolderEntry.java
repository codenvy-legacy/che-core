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
package org.eclipse.che.api.project.server;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.vfs.server.LazyIterator;
import org.eclipse.che.api.vfs.server.MountPoint;
import org.eclipse.che.api.vfs.server.VirtualFile;
import org.eclipse.che.api.vfs.server.VirtualFileFilter;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Folder entry.
 *
 * @author andrew00x
 */
public class FolderEntry extends VirtualFileEntry {
    private static final VirtualFileFilter FOLDER_FILTER = new VirtualFileFilter() {
        @Override
        public boolean accept(VirtualFile file) {
            return file.isFolder();
        }
    };

    private static final VirtualFileFilter FILES_FILTER = new VirtualFileFilter() {
        @Override
        public boolean accept(VirtualFile file) {
            return file.isFile();
        }
    };

    public FolderEntry(String workspace, VirtualFile virtualFile) {
        super(workspace, virtualFile);
    }

    public FolderEntry copyTo(String newParent) throws NotFoundException, ForbiddenException, ConflictException, ServerException {
        final VirtualFile vf = getVirtualFile();
        final MountPoint mp = vf.getMountPoint();
        return new FolderEntry(getWorkspace(), vf.copyTo(mp.getVirtualFile(newParent)));
    }

    /**
     * Get child by relative path.
     *
     * @param path
     *         relative path
     * @return child
     * @throws ForbiddenException
     *         if access to child item is forbidden
     * @throws ServerException
     *         if other error occurs
     */
    public VirtualFileEntry getChild(String path) throws ForbiddenException, ServerException {
        final VirtualFile child = getVirtualFile().getChild(path);
        if (child == null) {
            return null;
        }
        if (child.isFile()) {
            return new FileEntry(getWorkspace(), child);
        }
        return new FolderEntry(getWorkspace(), child);
    }

    /**
     * Get children of this folder. If current user doesn't have read access to some child they aren't added in result list.
     *
     * @throws ServerException
     *         if an error occurs
     */
    public List<VirtualFileEntry> getChildren() throws ServerException {
        return getChildren(VirtualFileFilter.ALL);
    }

    /**
     * Get child files of this folder. If current user doesn't have read access to some child they aren't added in result list.
     *
     * @throws ServerException
     *         if an error occurs
     */
    public List<FileEntry> getChildFiles() throws ServerException {
        final LazyIterator<VirtualFile> vfChildren = getVirtualFile().getChildren(FILES_FILTER);
        final List<FileEntry> children = new ArrayList<>();
        while (vfChildren.hasNext()) {
            children.add(new FileEntry(getWorkspace(), vfChildren.next()));
        }
        return children;
    }

    /**
     * Gets child folders of this folder. If current user doesn't have read access to some child they aren't added in result list.
     *
     * @throws ServerException
     *         if an error occurs
     */
    public List<FolderEntry> getChildFolders() throws ServerException {
        final LazyIterator<VirtualFile> vfChildren = getVirtualFile().getChildren(FOLDER_FILTER);
        final List<FolderEntry> children = new ArrayList<>();
        while (vfChildren.hasNext()) {
            children.add(new FolderEntry(getWorkspace(), vfChildren.next()));
        }
        return children;
    }

    List<VirtualFileEntry> getChildren(VirtualFileFilter filter) throws ServerException {
        final LazyIterator<VirtualFile> vfChildren = getVirtualFile().getChildren(filter);
        final List<VirtualFileEntry> children = new ArrayList<>();
        while (vfChildren.hasNext()) {
            final VirtualFile vf = vfChildren.next();
            if (vf.isFile()) {
                children.add(new FileEntry(getWorkspace(), vf));
            } else {
                children.add(new FolderEntry(getWorkspace(), vf));
            }
        }
        return children;
    }

    /**
     * Creates new file in this folder.
     *
     * @param name
     *         name
     * @param content
     *         content. In case of {@code null} empty file is created
     * @param mediaType
     *         media type of content, may be {@code null}
     * @return newly create VirtualFile
     * @throws ForbiddenException
     *         if copy operation is forbidden
     * @throws ConflictException
     *         if operation causes conflict, e.g. name conflict
     * @throws ServerException
     *         if other error occurs
     * @see org.eclipse.che.api.vfs.server.VirtualFile#createFile(String, String, java.io.InputStream)
     */
    public FileEntry createFile(String name, byte[] content, String mediaType)
            throws ForbiddenException, ConflictException, ServerException {
        if (isRoot(getVirtualFile())) {
            throw new ForbiddenException("Can't create file in root folder.");
        }
        return createFile(name, content == null ? null : new ByteArrayInputStream(content), mediaType);
    }

    /**
     * Creates new file in this folder.
     *
     * @param name
     *         name
     * @param content
     *         content. In case of {@code null} empty file is created
     * @param mediaType
     *         media type of content, may be {@code null}
     * @return newly create VirtualFile
     * @throws ForbiddenException
     *         if copy operation is forbidden
     * @throws ConflictException
     *         if operation causes conflict, e.g. name conflict
     * @throws ServerException
     *         if other error occurs
     * @see org.eclipse.che.api.vfs.server.VirtualFile#createFile(String, String, java.io.InputStream)
     */
    public FileEntry createFile(String name, InputStream content, String mediaType)
            throws ForbiddenException, ConflictException, ServerException {
        if (isRoot(getVirtualFile())) {
            throw new ForbiddenException("Can't create file in root folder.");
        }
        return new FileEntry(getWorkspace(), getVirtualFile().createFile(name, mediaType, content));
    }

    /**
     * Creates new VirtualFile which denotes folder and use this one as parent folder.
     *
     * @param name
     *         name. If name is string separated by '/' all nonexistent parent folders must be created.
     * @return newly create VirtualFile that denotes folder
     * @throws ForbiddenException
     *         if copy operation is forbidden
     * @throws ConflictException
     *         if item with specified {@code name} already exists
     * @throws ServerException
     *         if other error occurs
     */
    public FolderEntry createFolder(String name) throws ConflictException, ServerException, ForbiddenException {
        return new FolderEntry(getWorkspace(), getVirtualFile().createFolder(name));
    }

    /**
     * Tests whether this FolderEntry contains project structure.
     *
     * @throws ServerException
     *         if an error occurs
     */
    public boolean isProjectFolder() throws ServerException {
    	fixProjectFolderName();
        final VirtualFileEntry projectFile;
        try {
            projectFile = getChild(Constants.CODENVY_PROJECT_FILE_RELATIVE_PATH);
        } catch (ForbiddenException e) {
            // If have access to the project then must have access to its meta-information. If don't have access then treat that as server error.
            throw new ServerException(e.getServiceError());
        }
        return projectFile != null && projectFile.isFile();
    }

    /**
     * Tests whether this FolderEntry is a root folder.
     */
    public boolean isRoot() {
        return isRoot(getVirtualFile());
    }

    private boolean isRoot(VirtualFile virtualFile) {
        return virtualFile.isRoot();
    }
    
    /**
     * If this is a project folder that has the old ".codenvy" projects directory but not
     * the new ".che" folder (Constants.CODEVNY_DIR) then fix it, otherwise don't do anything.
     * @throws ServerException
     */
    private void fixProjectFolderName() throws ServerException {
		try {
			VirtualFileEntry folderEntry = getChild(Constants.CODENVY_DIR);
			if (folderEntry == null) {
				folderEntry = getChild(".codenvy");
				if (folderEntry != null && folderEntry.isFolder()) {
					folderEntry.rename(Constants.CODENVY_DIR);
				}
			}
		} catch (ForbiddenException | ConflictException e) {
			throw new ServerException(e.getMessage(), e);
		}
	}    
}
