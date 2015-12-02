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

import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.vfs.server.MountPoint;
import org.eclipse.che.api.vfs.server.search.LuceneSearcherProvider;
import org.eclipse.che.api.vfs.server.search.Searcher;

public class MemoryLuceneSearcherProvider extends LuceneSearcherProvider {
    private MemoryLuceneSearcher searcher;

    @Override
    public Searcher getSearcher(MountPoint mountPoint, boolean create) throws ServerException {
        if (searcher == null) {
            searcher = new MemoryLuceneSearcher(getIndexedMediaTypes());
            searcher.init(mountPoint);
        }
        return searcher;
    }
}
