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
package org.eclipse.che.api.vfs.search;

import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.vfs.VirtualFileFilter;
import org.eclipse.che.api.vfs.VirtualFile;

public interface Searcher {
    /**
     * Return paths of matched items on virtual filesystem.
     *
     * @param query
     *         query expression
     * @return results of search
     * @throws ServerException
     *         if an error occurs
     */
    SearchResult search(QueryExpression query) throws ServerException;

    /**
     * Add VirtualFile to index.
     *
     * @param virtualFile
     *         VirtualFile to add
     * @throws ServerException
     *         if an error occurs
     */
    void add(VirtualFile virtualFile) throws ServerException;

    /**
     * Delete VirtualFile from index.
     *
     * @param path
     *         path of VirtualFile
     * @throws ServerException
     *         if an error occurs
     */
    void delete(String path, boolean isFile) throws ServerException;

    /**
     * Updated indexed VirtualFile.
     *
     * @param virtualFile
     *         VirtualFile to add
     * @throws ServerException
     *         if an error occurs
     */
    void update(VirtualFile virtualFile) throws ServerException;

    /** Close Searcher. */
    void close();

    boolean isClosed();

    boolean addIndexFilter(VirtualFileFilter indexFilter);

    boolean removeIndexFilter(VirtualFileFilter indexFilter);

    interface CloseCallback {
        void onClose();
    }
}