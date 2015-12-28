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

import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.vfs.server.VirtualFileSystem;

import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractLuceneSearcherProvider implements SearcherProvider {
    protected final AtomicReference<LuceneSearcher> searcherReference = new AtomicReference<>();

    @Override
    public Searcher getSearcher(VirtualFileSystem virtualFileSystem, boolean create) throws ServerException {
        LuceneSearcher cachedSearcher = searcherReference.get();
        if (cachedSearcher == null && create) {
            LuceneSearcher searcher = createLuceneSearcher();
            if (searcherReference.compareAndSet(null, searcher)) {
                searcher.init(virtualFileSystem);
            }
            cachedSearcher = searcherReference.get();
        }
        return cachedSearcher;
    }

    protected abstract LuceneSearcher createLuceneSearcher();
}
