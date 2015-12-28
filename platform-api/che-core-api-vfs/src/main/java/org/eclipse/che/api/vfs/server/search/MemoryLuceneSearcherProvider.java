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

import org.eclipse.che.api.vfs.server.VirtualFileFilter;
import org.eclipse.che.api.vfs.server.VirtualFileFilters;

public class MemoryLuceneSearcherProvider extends AbstractLuceneSearcherProvider {
    private final VirtualFileFilter indexFilter;

    /**
     * @param indexFilter
     *         common filter for files that should not be indexed. If complex excluding rules needed then few filters might be combined
     *         with {@link VirtualFileFilters#createAndFilter} or {@link VirtualFileFilters#createOrFilter} methods
     * @see LuceneSearcher
     */
    public MemoryLuceneSearcherProvider(VirtualFileFilter indexFilter) {
        this.indexFilter = indexFilter;
    }

    @Override
    protected LuceneSearcher createLuceneSearcher() {
        return new MemoryLuceneSearcher(indexFilter, () -> searcherReference.set(null));
    }
}
