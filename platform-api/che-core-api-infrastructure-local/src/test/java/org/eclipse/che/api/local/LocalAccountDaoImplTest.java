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
package org.eclipse.che.api.local;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.eclipse.che.api.account.server.dao.Account;
import org.eclipse.che.api.account.server.dao.AccountDao;
import org.eclipse.che.api.account.server.dao.Member;
import org.eclipse.che.api.account.server.dao.Subscription;
import org.eclipse.che.api.account.shared.dto.AccountSearchCriteria;
import org.eclipse.che.api.user.server.dao.UserDao;
import org.eclipse.che.api.workspace.server.dao.WorkspaceDao;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.doReturn;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author Anatoliy Bazko
 */
@Listeners(value = {MockitoTestNGListener.class})
public class LocalAccountDaoImplTest {

    private static final Account ACCOUNT_1 = new Account().withId("id1").withName("name1");
    private static final Account ACCOUNT_2 = new Account().withId("id2").withName("name2");
    private static final Account ACCOUNT_3 = new Account().withId("id3").withName("name3");
    private static final Account ACCOUNT_4 = new Account().withId("id4").withName("name4");
    private static final Account ACCOUNT_5 = new Account().withId("id5").withName("name5");
    private static final Account ACCOUNT_6 = new Account().withId("id6").withName("name6");

    private static final Subscription SUBSCRIPTION_1 = new Subscription().withAccountId("id1").withServiceId("OnPremises");
    private static final Subscription SUBSCRIPTION_2 = new Subscription().withAccountId("id2").withServiceId("OnPremises");
    private static final Subscription SUBSCRIPTION_3 = new Subscription().withAccountId("id3").withServiceId("Saas");

    private static final Member MEMBER_1 = new Member().withAccountId("id1").withUserId("userId1").withRoles(ImmutableList.of("account/owner"));

    @Mock
    private AccountSearchCriteria searchCriteria;
    @Mock
    private UserDao               userDao;
    @Mock
    private WorkspaceDao          workspaceDao;

    private AccountDao accountDao;

    @BeforeMethod
    public void setUp() throws Exception {
        Set<Account> accounts = ImmutableSet.of(ACCOUNT_1, ACCOUNT_2, ACCOUNT_3, ACCOUNT_4, ACCOUNT_5, ACCOUNT_6);
        Set<Subscription> subscriptions = ImmutableSet.of(SUBSCRIPTION_1, SUBSCRIPTION_2, SUBSCRIPTION_3);
        Set<Member> members = ImmutableSet.of(MEMBER_1);

        accountDao = new LocalAccountDaoImpl(accounts, members, subscriptions, workspaceDao);
    }

    @Test
    public void findShouldReturnAllAccountsWithoutSearchCriteria() throws Exception {
        List<Account> result = accountDao.find(searchCriteria, 0, 20);
        assertResult(result, ACCOUNT_1, ACCOUNT_2, ACCOUNT_3, ACCOUNT_4, ACCOUNT_5, ACCOUNT_6);
    }

    @Test
    public void findShouldReturnPage1WithoutSearchCriteria() throws Exception {
        List<Account> result = accountDao.find(searchCriteria, 0, 2);
        assertResult(result, ACCOUNT_1, ACCOUNT_2);
    }

    @Test
    public void findShouldReturnPage2WithoutSearchCriteria() throws Exception {
        List<Account> result = accountDao.find(searchCriteria, 2, 2);
        assertResult(result, ACCOUNT_3, ACCOUNT_4);
    }

    @Test
    public void findShouldReturnLastPageWithoutSearchCriteria() throws Exception {
        List<Account> result = accountDao.find(searchCriteria, 4, 2);
        assertResult(result, ACCOUNT_5, ACCOUNT_6);
    }

    @Test
    public void findShouldReturnNothingIfBeginIndexOutOfRangeWithoutSearchCriteria() throws Exception {
        List<Account> result = accountDao.find(searchCriteria, 7, 1);
        assertTrue(result.isEmpty());
    }

    @Test
    public void findShouldReturnAccountIfSearchByAccountId() throws Exception {
        doReturn(ImmutableList.of("id1")).when(searchCriteria).getIds();

        List<Account> result = accountDao.find(searchCriteria, 0, 20);
        assertResult(result, ACCOUNT_1);
    }

    @Test
    public void findShouldReturnAccountIfSearchByEmailOwner() throws Exception {
        doReturn(ImmutableList.of("id1")).when(searchCriteria).getIds();
        doReturn(ImmutableList.of("userId1")).when(searchCriteria).getOwnerIds();

        List<Account> result = accountDao.find(searchCriteria, 0, 20);
        assertResult(result, ACCOUNT_1);
    }

    @Test
    public void findShouldReturnAccountsListIfSearchBySubscription() throws Exception {
        doReturn("OnPremises").when(searchCriteria).getServiceId();

        List<Account> result = accountDao.find(searchCriteria, 0, 20);
        assertResult(result, ACCOUNT_1, ACCOUNT_2);
    }

    @Test
    public void findShouldReturnAccountIfSearchBySubscriptionAndAccountId() throws Exception {
        doReturn("OnPremises").when(searchCriteria).getServiceId();
        doReturn(ImmutableList.of("id2")).when(searchCriteria).getIds();

        List<Account> result = accountDao.find(searchCriteria, 0, 20);
        assertResult(result, ACCOUNT_2);
    }

    private void assertResult(List<Account> result, Account... accounts) {
        assertEquals(result.size(), accounts.length);
        for (Account account : accounts) {
            assertTrue(result.contains(account));
        }
    }
}
