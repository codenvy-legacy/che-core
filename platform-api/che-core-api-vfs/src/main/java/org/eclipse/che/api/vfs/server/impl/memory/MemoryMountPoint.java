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

import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.vfs.server.MountPoint;
import org.eclipse.che.api.vfs.server.VirtualFile;
import org.eclipse.che.api.vfs.server.search.SearcherProvider;

/**
 * In-memory implementation of MountPoint.
 * <p/>
 * NOTE: This implementation is not thread safe.
 *
 * @author andrew00x
 */
public class MemoryMountPoint implements MountPoint {
    private final EventService     eventService;
    private final SearcherProvider searcherProvider;

    private VirtualFile root;

    public MemoryMountPoint(EventService eventService, SearcherProvider searcherProvider) {
        this.eventService = eventService;
        this.searcherProvider = searcherProvider;
        root = new MemoryVirtualFile(this);
    }

    @Override
    public VirtualFile getRoot() {
        return root;
    }

    @Override
    public void reset() {
        root = null;
    }

    @Override
    public SearcherProvider getSearcherProvider() {
        return searcherProvider;
    }

    @Override
    public EventService getEventService() {
        return eventService;
    }
}
