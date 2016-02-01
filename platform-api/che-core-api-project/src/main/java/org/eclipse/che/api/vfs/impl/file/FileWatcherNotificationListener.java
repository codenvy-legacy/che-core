/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.vfs.impl.file;

import java.io.File;

public interface FileWatcherNotificationListener {
    void pathCreated(File watchRoot, String subPath, boolean isDir);

    void pathDeleted(File watchRoot, String subPath, boolean isDir);

    void pathUpdated(File watchRoot, String subPath, boolean isDir);

    void started(File watchRoot);

    void errorOccurred(File watchRoot, Throwable cause);
}