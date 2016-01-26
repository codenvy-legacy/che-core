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

/** @author andrew00x */
public class QueryExpression {
    private String name;
    private String path;
    private String mediaType;
    private String text;
    private int maxItems;

    public String getPath() {
        return path;
    }

    public QueryExpression setPath(String path) {
        this.path = path;
        return this;
    }

    public String getName() {
        return name;
    }

    public QueryExpression setName(String name) {
        this.name = name;
        return this;
    }

    public String getMediaType() {
        return mediaType;
    }

    public QueryExpression setMediaType(String mediaType) {
        this.mediaType = mediaType;
        return this;
    }

    public String getText() {
        return text;
    }

    public QueryExpression setText(String text) {
        this.text = text;
        return this;
    }

    @Override
    public String toString() {
        return String.format("QueryExpression{name='%s', path='%s', mediaType='%s', text='%s',maxItems='%d'}", 
                name, 
                path,
                mediaType,
                text,
                maxItems
        );
    }

    public int getMaxItems() {
        return maxItems;
}

    public QueryExpression setMaxItems(int maxItems) {
        this.maxItems = maxItems;
        return this;
    }
}
