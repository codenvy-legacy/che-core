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
package org.eclipse.che.api.core.rest.permission;

import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.ServerException;

import javax.ws.rs.core.SecurityContext;
import java.util.List;
import java.util.Map;

/**
 * @author gazarenkov
 */
public interface PermissionChecker {

    String getMethod();

    void checkPermissions(String method, Map<String, String> params, SecurityContext context) throws ForbiddenException, ServerException;

}
