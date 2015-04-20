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

import org.eclipse.che.api.account.server.dao.Account;
import org.eclipse.che.api.account.server.dao.AccountDao;
import org.eclipse.che.api.account.server.dao.Member;
import org.eclipse.che.api.account.server.dao.Subscription;
import org.eclipse.che.api.account.server.dao.SubscriptionQueryBuilder;
import org.eclipse.che.api.account.shared.dto.AccountSearchCriteria;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.workspace.server.dao.WorkspaceDao;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.eclipse.che.api.account.server.Constants.RESOURCES_LOCKED_PROPERTY;
import static org.eclipse.che.api.account.shared.dto.SubscriptionState.ACTIVE;

/**
 * @author Eugene Voevodin
 */
@Singleton
public class LocalAccountDaoImpl implements AccountDao {

    private final List<Account>      accounts;
    private final List<Member>       members;
    private final List<Subscription> subscriptions;
    private final ReadWriteLock      lock;

    private final WorkspaceDao workspaceDao;

    @Inject
    public LocalAccountDaoImpl(@Named("codenvy.local.infrastructure.accounts") Set<Account> accounts,
                               @Named("codenvy.local.infrastructure.account.members") Set<Member> members,
                               @Named("codenvy.local.infrastructure.account.subscriptions") Set<Subscription> subscriptions,
                               WorkspaceDao workspaceDao) {
        this.workspaceDao = workspaceDao;
        this.accounts = new LinkedList<>();
        this.members = new LinkedList<>();
        this.subscriptions = new LinkedList<>();
        lock = new ReentrantReadWriteLock();
        try {
            for (Account account : accounts) {
                create(account);
            }
            for (Member member : members) {
                addMember(member);
            }
            for (Subscription subscription : subscriptions) {
                addSubscription(subscription);
            }
        } catch (Exception e) {
            // fail if can't validate this instance properly
            throw new RuntimeException(e);
        }
    }

    @Override
    public void create(Account account) throws ConflictException {
        lock.writeLock().lock();
        try {
            for (Account a : accounts) {
                if (a.getId().equals(account.getId())) {
                    throw new ConflictException(String.format("Account with id %s already exists.", account.getId()));
                }
                if (a.getName().equals(account.getName())) {
                    throw new ConflictException(String.format("Account with name %s already exists.", account.getName()));
                }
            }
            accounts.add(new Account().withId(account.getId()).withName(account.getName())
                                      .withAttributes(new LinkedHashMap<>(account.getAttributes())));
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Account getById(String id) throws NotFoundException {
        lock.readLock().lock();
        try {
            for (Account account : accounts) {
                if (account.getId().equals(id)) {
                    return new Account().withId(account.getId()).withName(account.getName())
                                        .withAttributes(new LinkedHashMap<>(account.getAttributes()));
                }
            }
        } finally {
            lock.readLock().unlock();
        }
        throw new NotFoundException(String.format("Not found account %s", id));
    }

    @Override
    public Account getByName(String name) throws NotFoundException {
        lock.readLock().lock();
        try {
            for (Account account : accounts) {
                if (account.getName().equals(name)) {
                    return new Account().withId(account.getId()).withName(account.getName())
                                        .withAttributes(new LinkedHashMap<>(account.getAttributes()));
                }
            }
        } finally {
            lock.readLock().unlock();
        }
        throw new NotFoundException(String.format("Not found account %s", name));
    }

    @Override
    public List<Account> getByOwner(String owner) {
        final List<Account> result = new LinkedList<>();
        lock.readLock().lock();
        try {
            for (Member member : members) {
                if (member.getUserId().equals(owner)) {
                    for (Account account : accounts) {
                        if (account.getId().equals(member.getAccountId()) && member.getRoles().contains("account/owner")) {
                            result.add(new Account().withId(account.getId()).withName(account.getName())
                                                    .withAttributes(new LinkedHashMap<>(account.getAttributes())));
                        }
                    }
                }
            }
        } finally {
            lock.readLock().unlock();
        }
        return result;
    }

    @Override
    public List<Member> getByMember(String userId) {
        final List<Member> result = new LinkedList<>();
        lock.readLock().lock();
        try {
            for (Member member : members) {
                if (userId.equals(member.getUserId())) {
                    result.add(new Member().withUserId(member.getUserId()).withAccountId(member.getAccountId())
                                           .withRoles(new ArrayList<>(member.getRoles())));
                }
            }
        } finally {
            lock.readLock().unlock();
        }
        return result;
    }

    @Override
    public void update(Account account) throws NotFoundException {
        lock.writeLock().lock();
        try {
            Account myAccount = null;
            for (int i = 0, size = accounts.size(); i < size && myAccount == null; i++) {
                if (accounts.get(i).getId().equals(account.getId())) {
                    myAccount = accounts.get(i);
                }
            }
            if (myAccount == null) {
                throw new NotFoundException(String.format("Not found account %s", account.getId()));
            }
            myAccount.setName(account.getName());
            myAccount.getAttributes().clear();
            myAccount.getAttributes().putAll(account.getAttributes());
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void remove(String id) throws NotFoundException, ServerException, ConflictException {
        lock.writeLock().lock();
        try {
            Account myAccount = null;
            for (int i = 0, size = accounts.size(); i < size && myAccount == null; i++) {
                if (accounts.get(i).getId().equals(id)) {
                    myAccount = accounts.get(i);
                }
            }
            if (myAccount == null) {
                throw new NotFoundException(String.format("Not found account %s", id));
            }
            if (!workspaceDao.getByAccount(id).isEmpty()) {
                throw new ConflictException("It is not possible to remove account that has associated workspaces");
            }
            for (Iterator<Member> itr = members.iterator(); itr.hasNext(); ) {
                final Member member = itr.next();
                if (member.getAccountId().equals(id)) {
                    itr.remove();
                }
            }
            accounts.remove(myAccount);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void addMember(Member member) throws NotFoundException, ConflictException {
        lock.writeLock().lock();
        try {
            Account myAccount = null;
            for (int i = 0, size = accounts.size(); i < size && myAccount == null; i++) {
                if (accounts.get(i).getId().equals(member.getAccountId())) {
                    myAccount = accounts.get(i);
                }
            }
            if (myAccount == null) {
                throw new NotFoundException(String.format("Not found account %s", member.getAccountId()));
            }

            for (Member m : members) {
                if (m.getUserId().equals(member.getUserId()) && m.getAccountId().equals(member.getAccountId())) {
                    throw new ConflictException(String.format("Membership of user %s in account %s already exists.",
                                                              member.getUserId(), member.getAccountId())
                    );
                }
            }

            members.add(new Member().withUserId(member.getUserId()).withAccountId(member.getAccountId())
                                    .withRoles(new ArrayList<>(member.getRoles())));
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public List<Member> getMembers(String accountId) {
        final List<Member> result = new LinkedList<>();
        lock.readLock().lock();
        try {
            for (Member member : members) {
                if (accountId.equals(member.getAccountId())) {
                    result.add(new Member().withUserId(member.getUserId()).withAccountId(member.getAccountId())
                                           .withRoles(new ArrayList<>(member.getRoles())));
                }
            }
        } finally {
            lock.readLock().unlock();
        }
        return result;
    }

    @Override
    public void removeMember(Member member) throws NotFoundException {
        lock.writeLock().lock();
        try {
            Member myMember = null;
            for (int i = 0, size = members.size(); i < size && myMember == null; i++) {
                if (members.get(i).getUserId().equals(member.getUserId())) {
                    myMember = members.get(i);
                }
            }
            if (myMember == null) {
                throw new NotFoundException(String.format("User with id %s hasn't any account membership", member.getUserId()));
            }

            Account myAccount = null;
            for (int i = 0, size = accounts.size(); i < size && myAccount == null; i++) {
                if (accounts.get(i).getId().equals(member.getAccountId())) {
                    myAccount = accounts.get(i);
                }
            }
            if (myAccount == null) {
                throw new NotFoundException(String.format("Not found account %s", member.getAccountId()));
            }

            members.remove(myMember);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void addSubscription(Subscription subscription) throws NotFoundException {
        lock.writeLock().lock();
        try {
            Account myAccount = null;
            for (int i = 0, size = accounts.size(); i < size && myAccount == null; i++) {
                if (accounts.get(i).getId().equals(subscription.getAccountId())) {
                    myAccount = accounts.get(i);
                }
            }
            if (myAccount == null) {
                throw new NotFoundException(String.format("Not found account %s", subscription.getAccountId()));
            }
            subscriptions.add(new Subscription(subscription));
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void removeSubscription(String subscriptionId) throws NotFoundException {
        lock.writeLock().lock();
        try {
            Subscription subscription = null;
            for (int i = 0, size = subscriptions.size(); i < size && subscription == null; i++) {
                if (subscriptions.get(i).getId().equals(subscriptionId)) {
                    subscription = subscriptions.get(i);
                }
            }
            if (subscription == null) {
                throw new NotFoundException(String.format("Not found subscription %s", subscriptionId));
            }
            subscriptions.remove(subscription);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Subscription getSubscriptionById(String subscriptionId) throws NotFoundException {
        lock.readLock().lock();
        try {
            Subscription subscription = null;
            for (int i = 0, size = subscriptions.size(); i < size && subscription == null; i++) {
                if (subscriptions.get(i).getId().equals(subscriptionId)) {
                    subscription = subscriptions.get(i);
                }
            }
            if (subscription == null) {
                throw new NotFoundException(String.format("Not found subscription %s", subscriptionId));
            }
            return new Subscription(subscription);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<Subscription> getActiveSubscriptions(String accountId) {
        final List<Subscription> result = new LinkedList<>();
        lock.readLock().lock();
        try {
            for (Subscription subscription : subscriptions) {
                if (accountId.equals(subscription.getAccountId()) && ACTIVE.equals(subscription.getState())) {
                    result.add(new Subscription(subscription));
                }
            }
        } finally {
            lock.readLock().unlock();
        }
        return result;
    }

    @Override
    public Subscription getActiveSubscription(String accountId, String serviceId) {
        lock.readLock().lock();
        try {
            for (Subscription subscription : subscriptions) {
                if (accountId.equals(subscription.getAccountId()) && serviceId.equals(subscription.getServiceId())
                    && ACTIVE.equals(subscription.getState())) {
                    return new Subscription(subscription);
                }
            }
        } finally {
            lock.readLock().unlock();
        }
        return null;
    }

    @Override
    public void updateSubscription(Subscription subscription) throws NotFoundException, ServerException {
        lock.writeLock().lock();
        try {
            int i = 0;
            Subscription mySubscription = null;
            for (int size = subscriptions.size(); i < size && mySubscription == null; i++) {
                if (subscriptions.get(i).getId().equals(subscription.getId())) {
                    mySubscription = subscriptions.get(i);
                }
            }
            if (mySubscription == null) {
                throw new NotFoundException(String.format("Not found subscription %s", subscription.getId()));
            }
            subscriptions.remove(i);
            subscriptions.add(i, new Subscription(subscription));
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public SubscriptionQueryBuilder getSubscriptionQueryBuilder() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Account> getAccountsWithLockedResources() throws ServerException, ForbiddenException {
        List<Account> result = new LinkedList<>();
        lock.readLock().lock();
        try {
            for (Account account : accounts) {
                final String lockedAttribute = account.getAttributes().get(RESOURCES_LOCKED_PROPERTY);
                if (Boolean.parseBoolean(lockedAttribute)) {
                    result.add(account);
                }
            }
        } finally {
            lock.readLock().unlock();
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public List<Account> find(AccountSearchCriteria searchCriteria, int skipLimit, int maxItems) {
        List<Account> result = new ArrayList<>(accounts);

        if (!isNullOrEmpty(searchCriteria.getOwnerIds())) {
            List<Account> byOwnerIds = new LinkedList<>();
            for (String ownerId : searchCriteria.getOwnerIds()) {
                byOwnerIds.addAll(getByOwner(ownerId));
            }
            result.retainAll(byOwnerIds);
        }

        if (!isNullOrEmpty(searchCriteria.getAccountIds())) {
            List<Account> byIds = new LinkedList<>();
            for (String id : searchCriteria.getAccountIds()) {
                try {
                    byIds.add(getById(id));
                } catch (NotFoundException e) {
                    // ignore
                }
            }
            result.retainAll(byIds);
        }

        if (searchCriteria.getServiceId() != null) {
            List<Account> byServiceId = new LinkedList<>();
            for (Subscription subscription : subscriptions) {
                if (subscription.getServiceId().equals(searchCriteria.getServiceId())) {
                    try {
                        byServiceId.add(getById(subscription.getAccountId()));
                    } catch (NotFoundException e) {
                        // ignore
                    }
                }
            }
            result.retainAll(byServiceId);
        }

        if (skipLimit >= result.size()) {
            return Collections.emptyList();
        }
        int toIndex = Math.min(skipLimit + maxItems, result.size());

        return result.subList(skipLimit, toIndex);
    }

    private boolean isNullOrEmpty(List<String> list) {
        return list == null || list.isEmpty();
    }
}
