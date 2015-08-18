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

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.user.server.dao.UserDao;
import org.eclipse.che.api.user.server.dao.User;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Singleton
public class LocalUserDaoImpl implements UserDao {
    private final List<User>    users;
    private final ReadWriteLock lock;

    @Inject
    public LocalUserDaoImpl(@Named("codenvy.local.infrastructure.users") Set<User> users) {
        this.users = new LinkedList<>();
        lock = new ReentrantReadWriteLock();
        try {
            for (User user : users) {
                create(user);
            }
        } catch (Exception e) {
            // fail if can't validate this instance properly
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean authenticate(String alias, String password) throws NotFoundException {
        lock.readLock().lock();
        try {
            User myUser = null;
            for (int i = 0, size = users.size(); i < size && myUser == null; i++) {
                if (users.get(i).getAliases().contains(alias)) {
                    myUser = users.get(i);
                }
            }
            if (myUser == null) {
                throw new NotFoundException(String.format("User not found %s", alias));
            }
            return password.equals(myUser.getPassword());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void create(User user) throws ConflictException {
        lock.writeLock().lock();
        try {
            final String userId = user.getId();
            final Set<String> aliases = new HashSet<>(user.getAliases());
            for (User u : users) {
                if (u.getId().equals(userId)) {
                    throw new ConflictException(
                            String.format("Unable create new user '%s'. User id %s is already in use.", user.getEmail(), userId));
                }
                for (String alias : u.getAliases()) {
                    if (aliases.contains(alias)) {
                        throw new ConflictException(
                                String.format("Unable create new user '%s'. User alias %s is already in use.", user.getEmail(), alias));
                    }
                }
            }
            users.add(doClone(user));
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void update(User user) throws NotFoundException {
        lock.writeLock().lock();
        try {
            User myUser = null;
            for (int i = 0, size = users.size(); i < size && myUser == null; i++) {
                if (users.get(i).getId().equals(user.getId())) {
                    myUser = users.get(i);
                }
            }
            if (myUser == null) {
                throw new NotFoundException(String.format("User not found %s", user.getId()));
            }
            myUser.getAliases().clear();
            myUser.getAliases().addAll(user.getAliases());
            myUser.setEmail(user.getEmail());
            myUser.setPassword(user.getPassword());
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void remove(String id) throws NotFoundException {
        lock.writeLock().lock();
        try {
            User myUser = null;
            for (int i = 0, size = users.size(); i < size && myUser == null; i++) {
                if (users.get(i).getId().equals(id)) {
                    myUser = users.get(i);
                }
            }
            if (myUser == null) {
                throw new NotFoundException(String.format("User not found %s", id));
            }
            users.remove(myUser);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public User getByAlias(String alias) throws NotFoundException {
        lock.readLock().lock();
        try {
            User user = null;
            for (int i = 0, size = users.size(); i < size && user == null; i++) {
                if (users.get(i).getAliases().contains(alias)) {
                    user = users.get(i);
                }
            }
            if (user == null) {
                throw new NotFoundException(String.format("User not found %s", alias));
            }
            return doClone(user);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public User getById(String id) throws NotFoundException {
        lock.readLock().lock();
        try {
            User user = null;
            for (int i = 0, size = users.size(); i < size && user == null; i++) {
                if (users.get(i).getId().equals(id)) {
                    user = users.get(i);
                }
            }
            if (user == null) {
                throw new NotFoundException(String.format("User not found %s", id));
            }
            return doClone(user);
        } finally {
            lock.readLock().unlock();
        }
    }

    private User doClone(User user) {
        return new User().withId(user.getId())
                         .withName(user.getName())
                         .withEmail(user.getEmail())
                         .withPassword(user.getPassword())
                         .withAliases(new ArrayList<>(user.getAliases()));
    }
}
