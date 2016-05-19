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
package org.eclipse.che.ide.jseditor.client.filetype;

import org.eclipse.che.ide.api.project.tree.VirtualFile;

import java.util.List;

/**
 * An interface for a file identification service.
 *
 * @author "Mickaël Leduque"
 */
public interface FileTypeIdentifier {

    /**
     * Returns a list of possible content types for the file.
     *
     * @param file
     *         the file to identify
     * @return a list of content type or null if identification failed
     */
    List<String> identifyType(VirtualFile file);
}
