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

import com.google.inject.Inject;
import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.ServerException;

import javax.inject.Singleton;
import javax.ws.rs.core.SecurityContext;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author gazarenkov
 */
@Singleton
public class PermissionManager {

    private final Map<String, PermissionChecker> pcheckers;

    public PermissionManager() {
        pcheckers = new HashMap<>();
    }

    @Inject(optional = true)
    public void init(Set<PermissionChecker> checkers) {
        for(PermissionChecker checker:checkers) {
            pcheckers.put(checker.getMethod(), checker);
        }
    }

    public void checkPermission(String method, Map<String, String> params, SecurityContext context)
            throws BadRequestException, ForbiddenException, ServerException {

        // no PermissionChecker configured means we need no checks
        if(pcheckers.isEmpty())
            return;

        if(!pcheckers.containsKey(method)) {
            throw new BadRequestException("Permission checker not found for method '" + method + "'");
        }
        pcheckers.get(method).checkPermissions(method, params, context);
    }
}
