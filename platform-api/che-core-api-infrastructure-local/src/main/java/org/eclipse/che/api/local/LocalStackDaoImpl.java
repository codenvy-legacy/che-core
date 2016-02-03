/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.local;

import com.google.common.reflect.TypeToken;
import com.google.inject.Singleton;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.machine.Command;
import org.eclipse.che.api.core.model.machine.Limits;
import org.eclipse.che.api.core.model.machine.MachineConfig;
import org.eclipse.che.api.core.model.machine.MachineSource;
import org.eclipse.che.api.core.model.machine.Recipe;
import org.eclipse.che.api.machine.server.recipe.adapters.GroupSerializer;
import org.eclipse.che.api.machine.server.recipe.adapters.PermissionsSerializer;
import org.eclipse.che.api.machine.shared.Group;
import org.eclipse.che.api.machine.shared.Permissions;
import org.eclipse.che.api.workspace.server.model.impl.stack.StackImpl;
import org.eclipse.che.api.workspace.server.model.impl.stack.Stack;
import org.eclipse.che.api.core.model.workspace.EnvironmentState;
import org.eclipse.che.api.core.model.workspace.ProjectConfig;
import org.eclipse.che.api.core.model.workspace.WorkspaceConfig;
import org.eclipse.che.api.local.storage.LocalStorage;
import org.eclipse.che.api.local.storage.LocalStorageFactory;
import org.eclipse.che.api.workspace.server.model.impl.stack.StackComponent;
import org.eclipse.che.api.workspace.server.dao.StackDao;
import org.eclipse.che.api.workspace.server.model.impl.stack.StackSource;
import org.eclipse.che.api.workspace.server.stack.adapters.CommandAdapter;
import org.eclipse.che.api.workspace.server.stack.adapters.EnvironmentStateAdapter;
import org.eclipse.che.api.workspace.server.stack.adapters.LimitsAdapter;
import org.eclipse.che.api.workspace.server.stack.adapters.MachineConfigAdapter;
import org.eclipse.che.api.workspace.server.stack.adapters.MachineSourceAdapter;
import org.eclipse.che.api.workspace.server.stack.adapters.ProjectConfigAdapter;
import org.eclipse.che.api.workspace.server.stack.adapters.RecipeAdapter;
import org.eclipse.che.api.workspace.server.stack.adapters.StackComponentAdapter;
import org.eclipse.che.api.workspace.server.stack.adapters.StackSourceAdapter;
import org.eclipse.che.api.workspace.server.stack.adapters.WorkspaceConfigAdapter;
import org.eclipse.che.commons.annotation.Nullable;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.io.IOException;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * Implementation local storage for {@link Stack}
 *
 * @author Alexander Andrienko
 */
@Singleton
public class LocalStackDaoImpl implements StackDao {

    private static final String STORAGE_FILE = "stacks.json";

    private LocalStorage           stackStorage;
    private Map<String, StackImpl> stacks;
    private ReadWriteLock          lock;

    @Inject
    public LocalStackDaoImpl(LocalStorageFactory localStorageFactory) throws IOException, ServerException {
        HashMap<Class<?>, Object> adapters = new HashMap<>();
        adapters.put(StackComponent.class, new StackComponentAdapter());
        adapters.put(WorkspaceConfig.class, new WorkspaceConfigAdapter());
        adapters.put(ProjectConfig.class, new ProjectConfigAdapter());
        adapters.put(EnvironmentState.class, new EnvironmentStateAdapter());
        adapters.put(Command.class, new CommandAdapter());
        adapters.put(Recipe.class, new RecipeAdapter());
        adapters.put(Limits.class, new LimitsAdapter());
        adapters.put(MachineSource.class, new MachineSourceAdapter());
        adapters.put(MachineConfig.class, new MachineConfigAdapter());
        adapters.put(StackSource.class, new StackSourceAdapter());
        adapters.put(Permissions.class, new PermissionsSerializer());
        adapters.put(Group.class, new GroupSerializer());

        this.stackStorage = localStorageFactory.create(STORAGE_FILE,
                                                       Collections.unmodifiableMap(adapters));
        this.stacks = new HashMap<>();
        this.lock = new ReentrantReadWriteLock();
    }

    @PostConstruct
    public void start() {
        stacks.putAll(stackStorage.loadMap(new TypeToken<Map<String, StackImpl>>() {}));
    }

    @PreDestroy
    public void stop() throws IOException {
        stackStorage.store(stacks);
    }

    @Override
    public void create(StackImpl stack) throws ConflictException {
        requireNonNull(stack, "Stack required");
        lock.writeLock().lock();
        try {
            if (stacks.containsKey(stack.getId())) {
                throw new ConflictException(format("Stack with id %s is already exist", stack.getId()));
            }
            stacks.put(stack.getId(), stack);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public StackImpl getById(String id) throws NotFoundException {
        requireNonNull(id, "Stack id required");
        lock.readLock().lock();
        try {
            final StackImpl stack = stacks.get(id);
            if (stack == null) {
                throw new NotFoundException(format("Stack with id %s was not found", id));
            }
            return stack;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void remove(String id) {
        requireNonNull(id, "Stack id required");
        lock.writeLock().lock();
        try {
            stacks.remove(id);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void update(StackImpl update) throws NotFoundException {
        requireNonNull(update, "Stack required");
        requireNonNull(update.getId(), "Stack id required");
        lock.writeLock().lock();
        try {
            String updateId = update.getId();
            if (!stacks.containsKey(updateId)) {
                throw new NotFoundException(format("Stack with id %s was not found", updateId));
            }
            stacks.replace(updateId, update);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public List<StackImpl> getByCreator(String creator, int skipCount, int maxItems) {
        requireNonNull(creator, "Stack creator required");
        lock.readLock().lock();
        try {
            return stacks.values().stream()
                         .skip(skipCount)
                         .filter(stack -> creator.equals(stack.getCreator()))
                         .limit(maxItems)
                         .collect(toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<StackImpl> searchStacks(@Nullable List<String> tags, int skipCount, int maxItems) {
        lock.readLock().lock();
        try {
            return stacks.values().stream()
                         .skip(skipCount)
                         .filter(decoratedStack -> tags == null || decoratedStack.getTags().containsAll(tags))
                         .limit(maxItems)
                         .collect(toList());
        } finally {
            lock.readLock().unlock();
        }
    }
}
