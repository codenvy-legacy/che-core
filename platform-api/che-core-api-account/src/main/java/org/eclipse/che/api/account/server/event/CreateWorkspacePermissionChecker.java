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

import com.google.inject.Inject;
import org.eclipse.che.api.account.server.dao.Account;
import org.eclipse.che.api.account.server.dao.AccountDao;
import org.eclipse.che.api.core.*;
import org.eclipse.che.api.core.rest.permission.PermissionChecker;
import org.eclipse.che.commons.env.EnvironmentContext;

import javax.inject.Singleton;
import javax.ws.rs.core.SecurityContext;
import java.util.List;
import java.util.Map;

/**
 * @author gazarenkov
 * @TODO move to hosted
 */
@Singleton
public class CreateWorkspacePermissionChecker implements PermissionChecker {

    private final AccountDao accountDao;

    @Inject
    public CreateWorkspacePermissionChecker(AccountDao accountDao) {
        this.accountDao = accountDao;
    }

    @Override
    public String getMethod() {
        return "new workspace";
    }

    @Override
    public void checkPermissions(String method, Map<String, String> params, SecurityContext context) throws ForbiddenException, ServerException {
//
//        if(context.isUserInRole("system/admin"))
//            return;
//        else if(!context.isUserInRole("user"))
//            throw new ForbiddenException("Permission denied for not authorized user");
//
//        String accountId = params.get("accountId");
//
//        Account account = accountDao.getById(accountId);
//
//
//        //check user has access to add new workspace
////        if (!context.isUserInRole("system/admin")) {
//         ensureCurrentUserOwnerOf(account);
////        }
    }

    private void ensureCurrentUserOwnerOf(Account target) throws ServerException, NotFoundException, ConflictException {
        final List<Account> accounts = accountDao.getByOwner(currentUser().getId());
        for (Account account : accounts) {
            if (account.getId().equals(target.getId())) {
                return;
            }
        }
        throw new ConflictException("You can create workspace associated only with your own account");
    }

    private org.eclipse.che.commons.user.User currentUser() {
        return EnvironmentContext.getCurrent().getUser();
    }
}
