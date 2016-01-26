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

/**
 *
 * @author I307348 - Ori Libhaber - ori.libhaber@sap.com
 */
public interface SearcherWithMetadata<T> extends Searcher{
    
    public default SearchResult<T> searchWithMetadata(QueryExpression query) throws ServerException{
        String[] search = search(query);
        SearchResult<String> result = new DefaultSearchResult<>(search);
        return (SearchResult<T>) result;
    }
    
}
