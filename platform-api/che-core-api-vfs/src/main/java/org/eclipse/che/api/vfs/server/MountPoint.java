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
package org.eclipse.che.api.vfs.server;

import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.vfs.server.search.SearcherProvider;

/**
 * Attaches any point on backend filesystem some VirtualFile (root folder). Only children of root folder may be accessible through this
 * API.
 *
 * @author andrew00x
 */
public interface MountPoint {
    /**
     * Get root folder of virtual file system. Any files in higher level than root are not accessible through virtual file system API.
     *
     * @return root folder of virtual file system
     */
    VirtualFile getRoot();

    /** Get searcher provider associated with this MountPoint. Method may return {@code null} if implementation doesn't support searching. */
    SearcherProvider getSearcherProvider();

    /** Get EventService. EventService may be used for propagation events about updates of any items associated with this MountPoint. */
    EventService getEventService();

    /** Call after unmount this MountPoint to release used resources, e.g. clear caches */
    void reset();
}
