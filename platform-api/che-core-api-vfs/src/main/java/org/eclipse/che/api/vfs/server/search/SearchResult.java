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

import java.util.List;
import java.util.Map;

/**
 *
 * @author I307348 - Ori Libhaber - ori.libhaber@sap.com
 */
public interface SearchResult<T> {
    
    public static final String TOTAL_HITS = "totalhits";
    public static final String TIME = "time";
    
    /**
     * get result of search as {@code List<T>}
     * @return {@code synchronized unmodifiable List<T>} containing the search result
     */
    public List<T> getResult();

    /**
     * get search meta data as {@code Map<String,String>}
     * @return {@code synchronized unmodifiable Map<String, String>} containing the search meta data
     */
    public Map<String, String> getMetadata();

}
