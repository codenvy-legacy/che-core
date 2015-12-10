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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author I307348 - Ori Libhaber - ori.libhaber@sap.com
 */
public class DefaultSearchResult<T> implements SearchResult<T>{

    private final List<T> result;
    private final Map<String, String> metadata;

    public DefaultSearchResult(List<T> result, Map<String, String> metadata) {
        this.result = result;
        this.metadata = metadata;
    }
    
    public DefaultSearchResult(T[] result, Map<String, String> metadata){
        this(Arrays.asList(result),metadata);
    }

    public DefaultSearchResult(T[] result) {
        this(result, new LinkedHashMap<>());
    }

    @Override
    public List<T> getResult() {
        return Collections.synchronizedList(Collections.unmodifiableList(result));
    }

    @Override
    public Map<String, String> getMetadata() {
        return Collections.synchronizedMap(Collections.unmodifiableMap(metadata));
    }

}
