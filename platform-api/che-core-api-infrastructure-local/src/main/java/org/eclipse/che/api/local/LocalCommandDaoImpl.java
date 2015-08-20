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

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.reflect.TypeToken;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.local.storage.LocalStorage;
import org.eclipse.che.api.local.storage.LocalStorageFactory;
import org.eclipse.che.api.machine.server.command.CommandImpl;
import org.eclipse.che.api.machine.server.dao.CommandDao;
import org.eclipse.che.api.machine.shared.ManagedCommand;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.lang.String.format;
import static org.eclipse.che.commons.lang.Strings.isNullOrEmpty;

/**
 * In memory implementation of {@link CommandDao}
 *
 * @author Eugene Voevodin
 * @author Anton Korneta
 */
@Singleton
public class LocalCommandDaoImpl implements CommandDao {

    private final Map<String, ManagedCommand> commands;
    private final ReadWriteLock               lock;
    private final LocalStorage                commandStorage;

    @Inject
    public LocalCommandDaoImpl(LocalStorageFactory storageFactory) throws IOException {
        this.commands = new HashMap<>();
        lock = new ReentrantReadWriteLock();
        commandStorage = storageFactory.create("commands.json");
    }

    @Inject
    @PostConstruct
    public void start(@Named("codenvy.local.infrastructure.commands") Set<ManagedCommand> defaultCommands) {
        commands.putAll(commandStorage.loadMap(new TypeToken<Map<String, CommandImpl>>() {}));
        if (commands.isEmpty()) {
            try {
                for (ManagedCommand command : defaultCommands) {
                    create(command);
                }
            } catch (Exception ex) {
                // fail if can't validate this instance properly
                throw new RuntimeException(ex);
            }
        }
    }

    @PreDestroy
    public void stop() throws IOException {
        commandStorage.store(commands);
    }

    @Override
    public void create(ManagedCommand command) throws ConflictException, ServerException {
        lock.writeLock().lock();
        try {
            if (commands.containsKey(command.getId())) {
                throw new ConflictException("Command with id " + command.getId() + " already exists");

            }
            commands.put(command.getId(), command);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void update(ManagedCommand update) throws NotFoundException, ServerException, ConflictException {
        lock.writeLock().lock();
        try {
            final CommandImpl target = (CommandImpl)commands.get(update.getId());
            if (target == null) {
                throw new NotFoundException("Command " + update.getId() + " was not found");
            }
            for (ManagedCommand command : commands.values()) {
                if (!command.equals(target) &&
                    command.getName().equals(update.getName()) &&
                    command.getWorkspaceId().equals(update.getWorkspaceId()) &&
                    command.getCreator().equals(update.getCreator())) {
                    throw new ConflictException(format("Command with name '%s' in workspace '%s' for user '%s' already exists",
                                                       update.getName(),
                                                       update.getWorkspaceId(),
                                                       update.getCreator()));
                }
            }
            if (!isNullOrEmpty(update.getName())) {
                target.setName(update.getName());
            }
            if (!isNullOrEmpty(update.getCommandLine())) {
                target.setCommandLine(update.getCommandLine());
            }
            if (!isNullOrEmpty(update.getVisibility())) {
                target.setVisibility(update.getVisibility());
            }
            if (update.getWorkingDir() != null) {
                target.setWorkingDir(update.getWorkingDir());
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void remove(String id) throws ServerException {
        lock.writeLock().lock();
        try {
            commands.remove(id);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public ManagedCommand getCommand(String id) throws NotFoundException, ServerException {
        lock.readLock().lock();
        try {
            final ManagedCommand command = commands.get(id);
            if (command == null) {
                throw new NotFoundException("Command " + id + " was not found");
            }
            return command;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<ManagedCommand> getCommands(final String workspaceId,
                                            final String creator,
                                            int skipCount,
                                            int maxItems) throws ServerException {
        lock.readLock().lock();
        try {
            return FluentIterable.from(commands.values())
                                 .filter(new Predicate<ManagedCommand>() {
                                     @Override
                                     public boolean apply(ManagedCommand command) {
                                         return command.getCreator().equals(creator) || command.getWorkspaceId().equals(workspaceId);
                                     }
                                 })
                                 .toList();
        } finally {
            lock.readLock().unlock();
        }
    }
}
