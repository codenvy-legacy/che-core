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
package org.eclipse.che.api.account;

import org.eclipse.che.api.account.server.AccountWorkspaceHooks;
import org.eclipse.che.api.account.server.dao.Account;
import org.eclipse.che.api.account.server.dao.AccountDao;
import org.eclipse.che.api.account.server.dao.Member;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.model.workspace.UsersWorkspace;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.user.User;
import org.eclipse.che.commons.user.UserImpl;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.ArrayList;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertTrue;

/**
 * Tests for {@link AccountWorkspaceHooks}.
 *
 * @author Eugene Voevodin
 */
@Listeners(MockitoTestNGListener.class)
public class AccountWorkspaceHooksTest {

    @Mock
    AccountDao            accountDao;
    @InjectMocks
    AccountWorkspaceHooks workspaceHooks;

    User currentUser;

    @BeforeMethod
    public void setUpEnvironment() {
        currentUser = new UserImpl("name", "id", "token", singletonList("user"), false);
        EnvironmentContext environmentContext = new EnvironmentContext();
        environmentContext.setUser(currentUser);
        EnvironmentContext.setCurrent(environmentContext);
    }

    @Test
    public void allowWorkspaceStartWhenWorkspaceIsRegisteredAndAccountIdIsNull() throws Exception {
        workspaceHooks.beforeCreate(mock(UsersWorkspace.class), null);

        verify(accountDao, never()).update(any());
    }

    @Test(expectedExceptions = ForbiddenException.class,
          expectedExceptionsMessageRegExp = "Workspace start rejected. " +
                                            "Impossible to determine account for workspace '.+', user '.+' is owner of zero or several accounts. " +
                                            "Specify account identifier!")
    public void rejectWorkspaceStartWithNotRegisteredWorkspaceAndNullAccountIdAndMultipleUserAccounts() throws Exception {
        UsersWorkspace workspace = mock(UsersWorkspace.class);
        when(accountDao.getByWorkspace(workspace.getId())).thenThrow(new NotFoundException(""));
        when(accountDao.getByOwner(currentUser.getId())).thenReturn(asList(mock(Account.class), mock(Account.class)));

        workspaceHooks.beforeStart(workspace, null);
    }

    @Test
    public void allowWorkspaceStartWithNotRegisteredWorkspaceAndNullAccountIdAndSingleUserAccount() throws Exception {
        UsersWorkspace workspace = mock(UsersWorkspace.class);
        when(accountDao.getByWorkspace(workspace.getId())).thenThrow(new NotFoundException(""));
        when(accountDao.getByOwner(currentUser.getId())).thenReturn(singletonList(mock(Account.class)));

        workspaceHooks.beforeStart(workspace, null);

        verify(accountDao).update(any());
    }

    @Test
    public void allowWorkspaceStartWithAccountIdWhichIsEqualToWorkspacesAccount() throws Exception {
        UsersWorkspace workspace = mock(UsersWorkspace.class);
        Account account = new Account().withId("account123");
        when(accountDao.getByWorkspace(workspace.getId())).thenReturn(account);

        workspaceHooks.beforeStart(workspace, "account123");

        verify(accountDao, never()).update(any());
    }

    @Test(expectedExceptions = ForbiddenException.class,
          expectedExceptionsMessageRegExp = "Workspace start rejected. " +
                                            "Workspace is already added to account '.+' " +
                                            "which is different from specified one '.+'")
    public void rejectWorkspaceStartWithAccountIdWhichIsNotEqualToWorkspacesAccount() throws Exception {
        UsersWorkspace workspace = mock(UsersWorkspace.class);
        Account account = new Account().withId("321account");
        when(accountDao.getByWorkspace(workspace.getId())).thenReturn(account);

        workspaceHooks.beforeStart(workspace, "account123");
    }

    @Test(expectedExceptions = ForbiddenException.class,
          expectedExceptionsMessageRegExp = "Workspace start rejected. User '.+' doesn't own account '.+'")
    public void rejectWorkspaceStartWithIdOfAccountWhichIsNotOwnedByCurrentUser() throws Exception {
        UsersWorkspace workspace = mock(UsersWorkspace.class);
        when(accountDao.getByWorkspace(workspace.getId())).thenThrow(new NotFoundException(""));
        Account account = new Account().withId("account123");
        when(accountDao.getById(account.getId())).thenReturn(account);
        Member member = new Member().withUserId(currentUser.getId()).withRoles(singletonList("account/member"));
        when(accountDao.getMembers(anyString())).thenReturn(singletonList(member));

        workspaceHooks.beforeStart(workspace, "account123");
    }

    @Test
    public void allowWorkspaceStartWithIdOfAccountWhichOwnedByCurrentUser() throws Exception {
        UsersWorkspace workspace = mock(UsersWorkspace.class);
        when(accountDao.getByWorkspace(workspace.getId())).thenThrow(new NotFoundException(""));
        Account account = new Account().withId("account123");
        when(accountDao.getById(account.getId())).thenReturn(account);
        Member member = new Member().withUserId(currentUser.getId()).withRoles(singletonList("account/owner"));
        when(accountDao.getMembers(anyString())).thenReturn(singletonList(member));

        workspaceHooks.beforeStart(workspace, "account123");

        verify(accountDao).update(any());
    }

    @Test
    public void workspaceShouldBeRemovedFromAccountWhenWorkspaceIsRemoved() throws Exception {
        UsersWorkspace workspace = mock(UsersWorkspace.class);
        when(workspace.getId()).thenReturn("workspace123");
        Account account = new Account();
        account.setWorkspaces(new ArrayList<>(singletonList(workspace)));
        when(accountDao.getByWorkspace(workspace.getId())).thenReturn(account);

        workspaceHooks.afterRemove("workspace123");

        assertTrue(account.getWorkspaces().isEmpty());
        verify(accountDao).update(account);
    }

    @Test
    public void nothingShouldBeDoneIfRemovalWorkspaceIsNotRelatedToAnyAccount() throws Exception {
        UsersWorkspace workspace = mock(UsersWorkspace.class);
        when(accountDao.getByWorkspace(workspace.getId())).thenThrow(new NotFoundException(""));

        workspaceHooks.afterRemove(workspace.getId());

        verify(accountDao, never()).update(any());
    }
}
