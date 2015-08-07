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
package org.eclipse.che.api.account.server.event;

import org.eclipse.che.api.account.server.dao.AccountWorkspacesDao;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.rest.permission.PermissionChecker;
import org.eclipse.che.api.user.server.dao.MembershipDao;
import org.eclipse.che.api.user.shared.model.Membership;

import javax.inject.Singleton;
import javax.ws.rs.core.SecurityContext;
import java.util.Map;

/**
 * @author gazarenkov
 * @TODO move to hosted
 */
@Singleton
public class GetWorkspacePermissionChecker implements PermissionChecker {

    private final MembershipDao membershipDao;
    private final AccountWorkspacesDao accountWorkspacesDao;

    public GetWorkspacePermissionChecker(MembershipDao membershipDao, AccountWorkspacesDao accountWorkspacesDao) {
        this.membershipDao = membershipDao;
        this.accountWorkspacesDao = accountWorkspacesDao;
    }

    @Override
    public String getMethod() {
        return "get workspace";
    }

    @Override
    public void checkPermissions(String method, Map<String, String> params, SecurityContext context) throws ForbiddenException, ServerException {
//
//        String id = params.get("id");
//        Membership m = membershipDao.getMembership(context.getUserPrincipal().getName(), "workspace", id);
//        if(m.getRoles().contains("workspace/developer") || m.getRoles().contains("workspace/admin"))
//            return;
//        else {
//            String accountId = this.accountWorkspacesDao.getWorkspaces(id);
//            if(m.getRoles().contains("account/admin"))
//                return;
//        }
//
//        throw new ForbiddenException("Permission denied for "+context.getUserPrincipal().getName());

    }
}
