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
package org.eclipse.che.api.project.gwt.client;

import org.eclipse.che.api.project.shared.dto.ProjectTypeDefinition;
import org.eclipse.che.ide.rest.AsyncRequestCallback;

import java.util.List;

/**
 * Client for Project Type service.
 *
 * @author Artem Zatsarynnyy
 */
public interface ProjectTypeServiceClient {
    /**
     * Get information about all registered project types.
     *
     * @param callback
     *         the callback to use for the response
     */
    void getProjectTypes(AsyncRequestCallback<List<ProjectTypeDefinition>> callback);
}
