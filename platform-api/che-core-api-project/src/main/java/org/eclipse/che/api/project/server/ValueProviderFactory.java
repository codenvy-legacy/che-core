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
package org.eclipse.che.api.project.server;

/**
 * Factory for {@link ValueProvider}.
 *
 * @author andrew00x
 */
public interface ValueProviderFactory {

    /** Create new instance of ValueProvider2. Project is used for access to low-level information about project.
     * @param projectFolder*/
    ValueProvider newInstance(FolderEntry projectFolder);
}
