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
import com.google.inject.name.Named;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.workspace.server.model.impl.stack.StackImpl;
import org.eclipse.che.api.workspace.server.model.impl.stack.Stack;
import org.eclipse.che.api.local.storage.LocalStorage;
import org.eclipse.che.api.local.storage.LocalStorageFactory;
import org.eclipse.che.api.workspace.server.dao.StackDao;
import org.eclipse.che.api.workspace.server.stack.StackGsonFactory;

import org.eclipse.che.api.workspace.server.stack.StackLoader;
import org.eclipse.che.api.workspace.server.stack.image.StackIcon;
import org.eclipse.che.commons.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.io.IOException;

import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.lang.String.format;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * Implementation local storage for {@link Stack}
 *
 * @author Alexander Andrienko
 */
@Singleton
public class LocalStackDaoImpl implements StackDao {

    private static final Logger LOG = LoggerFactory.getLogger(LocalStackDaoImpl.class);

    private static final String STORAGE_FILE   = "stacks.json";
    private static final String iconFolderName = "stack_img";

    private final Path                   iconFolderPath;
    private       LocalStorage           stackStorage;
    private       Map<String, StackImpl> stacks;
    private       ReadWriteLock          lock;

    @Inject
    public LocalStackDaoImpl(@Named("che.conf.storage") String localStorage,
                             StackGsonFactory stackGsonFactory,
                             LocalStorageFactory localStorageFactory) throws IOException {
        this.stackStorage = localStorageFactory.create(STORAGE_FILE, stackGsonFactory.getGson());
        this.stacks = new HashMap<>();
        this.lock = new ReentrantReadWriteLock();
        iconFolderPath = Paths.get(localStorage, iconFolderName);
        createIconFolder();
    }

    private void createIconFolder() {
        try {
            if (!Files.exists(iconFolderPath)) {
                Files.createDirectories(iconFolderPath);
            }
        } catch (IOException e) {
            LOG.error("Failed to create icon files storage folder by path " + iconFolderPath, e);
        }
    }

    @PostConstruct
    public void start() {
        Map<String, StackImpl> stackMap = stackStorage.loadMap(new TypeToken<Map<String, StackImpl>>() {});
        for (StackImpl stack : stackMap.values()) {
            StackLoader.setIconData(stack, iconFolderPath);
        }
        stacks.putAll(stackMap);
    }

    @PreDestroy
    public void stop() throws IOException {
        stackStorage.store(stacks);
        deleteFolderRecursive(iconFolderPath);//todo we can use FileUtils.deleteDirectory from apache lib
        createIconFolder();
        stacks.values().forEach(this::saveIcon);
    }

    @Override
    public void create(StackImpl stack) throws ConflictException, ServerException {
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
    public void remove(String id) throws ServerException {
        requireNonNull(id, "Stack id required");
        lock.writeLock().lock();
        try {
            stacks.remove(id);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void update(StackImpl update) throws NotFoundException, ServerException {
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

    private void saveIcon(StackImpl stack) {
        try {
            StackIcon stackIcon = stack.getStackIcon();
            if (stackIcon != null && stackIcon.getData() != null) {
                Path iconDirectory = iconFolderPath.resolve(stack.getId());
                if (Files.exists(iconDirectory)) {//todo do we need this?
                    deleteFolderRecursive(iconDirectory);
                }
                Files.createDirectories(iconDirectory);
                Path iconPath = iconDirectory.resolve(stackIcon.getName());
                Files.write(iconPath, stackIcon.getData(), CREATE, TRUNCATE_EXISTING);
            }
        } catch (IOException ex) {
            LOG.error(format("Failed to save icon for stack with id '%s'", stack.getId()), ex);
        }
    }

    private void deleteFolderRecursive(Path path) throws IOException {
        Files.walkFileTree(path, new FileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                return CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                LOG.error(format("Failed to delete file '%s' for clean up stack icon directory", path), exc);
                return CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return CONTINUE;
            }
        });
    }
}
