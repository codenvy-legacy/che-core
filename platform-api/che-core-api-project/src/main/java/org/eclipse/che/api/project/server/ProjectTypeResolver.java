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
package org.eclipse.che.api.project.server;

import org.eclipse.che.api.core.ServerException;

/**
 * Provide possibility for resolving project type for newly projects(maven, ruby, python etc.)
 *
 * @author Evgen Vidolob
 */
public interface ProjectTypeResolver {

    /**
     * Resolve {@code project} type and fill {@code description}.
     *
     * @param project
     *         the project to resolve
     * @return {@code true} if this resolver resolve project type and fill description, {@code false} otherwise
     * @throws ServerException
     *         if an error occurs
     */
    boolean resolve(FolderEntry project) throws ServerException, ValueStorageException,
            InvalidValueException, ProjectTypeConstraintException;
}
