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

import java.io.File;

public class FSLuceneSearcherProvider extends AbstractLuceneSearcherProvider {
    private final File              indexDirectory;
    private final VirtualFileFilter indexFilter;

    /**
     * @param indexDirectory
     *         directory for creation index
     * @param indexFilter
     *         common filter for files that should not be indexed. If complex excluding rules needed then few filters might be combined
     *         with {@link VirtualFileFilters#createAndFilter} or {@link VirtualFileFilters#createOrFilter} methods
     * @see LuceneSearcher
     */
    public FSLuceneSearcherProvider(File indexDirectory, VirtualFileFilter indexFilter) {
        this.indexDirectory = indexDirectory;
        this.indexFilter = indexFilter;
    }

    @Override
    protected LuceneSearcher createLuceneSearcher() {
        return new FSLuceneSearcher(indexDirectory, indexFilter, () -> searcherReference.set(null));
    }
}
