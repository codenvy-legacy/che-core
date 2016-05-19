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
package org.eclipse.che.api.project.server.handlers;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.project.server.ProjectConfig;
import org.eclipse.che.api.project.server.FolderEntry;

import java.util.Map;

/**
 * @author gazarenkov
 */
public interface CreateModuleHandler extends ProjectHandler {

    void onCreateModule(FolderEntry parentFolder, String modulePath, ProjectConfig moduleConfig, Map<String, String> options)
            throws ForbiddenException, ConflictException, ServerException;
}
