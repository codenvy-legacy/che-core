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
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.workspace.server.dao.WorkspaceDao;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Eugene Voevodin
 */
@Singleton
public class LocalAccountDaoImpl implements AccountDao {

    private final List<Account> accounts;
    private final List<Member>  members;
    private final ReadWriteLock lock;

    private final WorkspaceDao workspaceDao;

    @Inject
    public LocalAccountDaoImpl(@Named("codenvy.local.infrastructure.accounts") Set<Account> accounts,
                               @Named("codenvy.local.infrastructure.account.members") Set<Member> members,
                               WorkspaceDao workspaceDao) {
        this.workspaceDao = workspaceDao;
        this.accounts = new LinkedList<>();
        this.members = new LinkedList<>();
        lock = new ReentrantReadWriteLock();
        try {
            for (Account account : accounts) {
                create(account);
            }
            for (Member member : members) {
                addMember(member);
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
}
