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
package org.eclipse.che.api.vfs.server.util;

import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.vfs.server.VirtualFile;
import org.eclipse.che.api.vfs.server.VirtualFileFilter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Filter based on media type of file.
 *
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 */
public class MediaTypeFilter implements VirtualFileFilter {
    private final Set<String> mediaTypes;

    public MediaTypeFilter(Collection<String> mediaTypes) {
        this.mediaTypes = new HashSet<>(mediaTypes);
    }

    @Override
    public boolean accept(VirtualFile file) {
        return mediaTypes.contains(getMediaType(file));
    }

    /** Get virtual file media type. Any additional parameters (e.g. 'charset') are removed. */
    private String getMediaType(VirtualFile virtualFile) {
        String mediaType;
        try {
            mediaType = virtualFile.getMediaType();
        } catch (ServerException e) {
            throw new RuntimeException(e.getMessage());
        }
        final int paramStartIndex = mediaType.indexOf(';');
        if (paramStartIndex != -1) {
            mediaType = mediaType.substring(0, paramStartIndex).trim();
        }
        return mediaType;
    }
}
