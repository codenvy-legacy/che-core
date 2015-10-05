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
package org.eclipse.che.api.account.server;

import com.google.inject.Singleton;

import org.eclipse.che.api.account.server.dao.Account;
import org.eclipse.che.api.account.server.dao.AccountDao;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.workspace.UsersWorkspace;
import org.eclipse.che.api.workspace.server.WorkspaceHooks;
import org.eclipse.che.api.workspace.server.WorkspaceManager;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.user.User;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.util.List;

import static java.lang.String.format;

/**
 * Rejects/Allows {@link WorkspaceManager} operations.
 *
 * @author Eugene Voevodin
 */
@Singleton
public class AccountWorkspaceHooks implements WorkspaceHooks {

    private final AccountDao accountDao;

    @Inject
    public AccountWorkspaceHooks(AccountDao accountDao) {
        this.accountDao = accountDao;
    }

    /**
     * Rejects workspace start if it is impossible to determine account
     * which <i>is/should be</i> related with running workspace.
     *
     * <p>When workspace is not added to account yet, tries to detect an account
     * and add workspace to it.
     *
     * <p>Reject/Allow scheme:
     * <pre>
     *  Is accountId specified ?
     *      NO: Is workspace already added to any account?
     *          YES: workspace start is <b>allowed</b>
     *          NO: Does user have a single account?
     *              YES: add workspace to account, start is <b>allowed</b>
     *              NO: workspace start is <b>rejected</b>
     *      YES: Is workspace already added to any account?
     *          YES: Does this account id equal to specified accountId?
     *              YES: workspace start is <b>allowed</b>
     *              NO: workspace start is <b>rejected</b>
     *          NO: Is the current user an account owner of the specified account?
     *              YES: add workspace to account, start is <b>allowed</b>
     *              NO: workspace start is <b>rejected</b>
     * </pre>
     */
    @Override
    public void beforeStart(@NotNull UsersWorkspace workspace, @Nullable String accountId) throws NotFoundException,
                                                                                                  ForbiddenException,
                                                                                                  ServerException {
        User currentUser = EnvironmentContext.getCurrent().getUser();
        if (accountId == null) {
            // check if account is already specified for given workspace
            if (!accountDao.isWorkspaceRegistered(workspace.getId())) {
                // when workspace is not added to any of accounts
                // try to detect whether current user is owner of a single account
                List<Account> ownedAccounts = accountDao.getByOwner(currentUser.getId());
                if (ownedAccounts.size() != 1) {
                    throw new ForbiddenException(format("Workspace start rejected. Impossible to determine account for workspace '%s', "
                                                        + "user '%s' is owner of zero or several accounts. Specify account identifier!",
                                                        workspace.getId(),
                                                        workspace.getOwner()));
                }
                // account detection is completed
                // if user is owner of a single account add workspace to it
                Account account = ownedAccounts.get(0);
                account.getWorkspaces().add(workspace);
                accountDao.update(account);
            }
        } else {
            // check if workspace is already added to any account
            try {
                Account account = accountDao.getByWorkspace(workspace.getId());
                if (!account.getId().equals(accountId)) {
                    throw new ForbiddenException(format("Workspace start rejected. Workspace is already added to account '%s' "
                                                        + "which is different from specified one '%s'",
                                                        account.getId(),
                                                        accountId));
                }
            } catch (NotFoundException ignored) {
                // workspace is not added to any account
                // check if user is owner of account which identifier was specified
                if (accountDao.getMembers(accountId)
                              .stream()
                              .noneMatch(member -> currentUser.getId().equals(member.getUserId())
                                                   && member.getRoles().contains("account/owner"))) {
                    throw new ForbiddenException(format("Workspace start rejected. User '%s' doesn't own account '%s'",
                                                        currentUser.getId(),
                                                        accountId));
                }
                // otherwise when user is owner of specified account add workspace to it
                Account account = accountDao.getById(accountId);
                account.getWorkspaces().add(workspace);
                accountDao.update(account);
            }
        }
    }

    @Override
    public void beforeCreate(@NotNull UsersWorkspace workspace, @Nullable String accountId) throws NotFoundException, ServerException {}

    @Override
    public void afterCreate(@NotNull UsersWorkspace workspace, @Nullable String accountId) throws ServerException {}

    /**
     * Removes workspace from account if necessary(workspace is related to any account).
     */
    @Override
    public void afterRemove(@NotNull String workspaceId) throws ServerException {
        try {
            Account account = accountDao.getByWorkspace(workspaceId);
            account.getWorkspaces().removeIf(workspace -> workspace.getId().equals(workspaceId));
            accountDao.update(account);
        } catch (NotFoundException ignored) {
        }
    }
}
