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

import org.eclipse.che.api.vfs.server.Path;

/**
 * Some default behavior for virtual file systems.
 * 
 * @author Tareq Sharafy (tareq.sha@sap.com)
 */
public class VirtualFileDefaults {

    /**
     * Checks whether the given path should be ignored by the VFS implementations in filters and permission checks.
     * 
     * @param path
     *            The file to check
     * @return Whether should be ignored.
     */
    public static boolean isPathIgnored(Path path) {
        int length = path.length();
        // TODO use common constants for these
        return length >= 2 && ".codenvy".equals(path.element(length - 2))
                && "misc.xml".equals(path.element(length - 1));
    }

}
