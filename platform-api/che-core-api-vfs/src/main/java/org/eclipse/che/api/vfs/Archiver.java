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
package org.eclipse.che.api.vfs;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.ServerException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface Archiver {
    void compress(OutputStream zipOutput) throws IOException, ServerException;

    void compress(OutputStream zipOutput, VirtualFileFilter filter) throws IOException, ServerException;

    void extract(InputStream zipInput, boolean overwrite, int stripNumber)
            throws IOException, ForbiddenException, ConflictException, ServerException;
}
