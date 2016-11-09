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
package org.eclipse.che.vfs.impl.fs;

import java.io.File;

import javax.inject.Singleton;

import org.eclipse.che.api.vfs.server.VirtualFile;

/**
 * Resolves location of virtual filesystem item on local filesystem.
 *
 * @author Vitaly Parfonov
 */
@Singleton
public class LocalPathResolver {
    public String resolve(VirtualFile virtualFile) {
        File file = virtualFile.getIoFile();
        return file != null ? file.getAbsolutePath() : null;
    }
}
