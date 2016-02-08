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
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.local.storage.LocalStorage;
import org.eclipse.che.api.local.storage.LocalStorageFactory;
import org.eclipse.che.api.workspace.server.dao.StackIconDao;
import org.eclipse.che.api.workspace.server.stack.StackIconGsonFactory;
import org.eclipse.che.api.workspace.server.stack.image.StackIcon;
import org.eclipse.che.commons.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

/**
 * In-memory implementation {@link StackIconDao}
 *
 * @author Alexander Andrienko
 */
@Singleton
public class LocalStackIconDaoImpl implements StackIconDao {

    private static final Logger LOG          = LoggerFactory.getLogger(LocalStackIconDaoImpl.class);
    private static final String STORAGE_FILE = "stacks-icons.json";

    private final Map<String, StackIcon> icons;
    private final Path                   iconFolderPath;
    private final LocalStorage           stackStorage;
    private final ReadWriteLock          lock;

    @Inject
    public LocalStackIconDaoImpl(@javax.inject.Named("che.conf.storage") String pathToLocalStorage,
                                 StackIconGsonFactory gsonFactory,
                                 LocalStorageFactory localStorageFactory) throws IOException {
        this.stackStorage = localStorageFactory.create(STORAGE_FILE, gsonFactory.getGson());
        icons = new HashMap<>();
        lock = new ReentrantReadWriteLock();
        iconFolderPath = Paths.get(pathToLocalStorage, "stack-img");
        initFileStorage();
    }

    private void initFileStorage() {
        try {
            if (!Files.exists(iconFolderPath)) {
                Files.createDirectories(iconFolderPath);
            }
        } catch (IOException e) {
            LOG.error("Failed create icon files storage ", e);
        }
    }

    @PostConstruct
    public void start() {
        try {
            Map<String, StackIcon> stackIconMap = stackStorage.loadMap(new TypeToken<Map<String, StackIcon>>() {
            });
            Map<String, Path> paths = Files.walk(iconFolderPath, 1).filter(Files::isRegularFile)
                                           .filter(Files::isReadable)
                                           .collect(toMap(path -> path.getFileName().toString(), (path) -> path));

            for (StackIcon stackIcon : stackIconMap.values()) {
                String iconName = stackIcon.getName();
                if (paths.keySet().contains(iconName)) {
                    Path path = paths.get(iconName);
                    stackIcon.setData(Files.readAllBytes(path));
                    icons.put(stackIcon.getStackId(), stackIcon);
                }
            }
        } catch (IOException e) {
            LOG.error("Failed to load stack icons data ", e);
        }
    }

    @PreDestroy
    public void stop() throws IOException {
        stackStorage.store(icons);
    }

    @Nullable
    @Override
    public StackIcon getIcon(String stackId) {
        requireNonNull(stackId, "Stack id required");
        lock.readLock().lock();
        try {
            return icons.get(stackId);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void save(StackIcon stackIcon) throws ServerException {
        requireNonNull(stackIcon, "Stack icon required");
        lock.writeLock().lock();
        try {
            Path iconPath = Paths.get(iconFolderPath.toString(), stackIcon.getName());
            Files.write(iconPath, stackIcon.getData(), CREATE, TRUNCATE_EXISTING);
            icons.put(stackIcon.getStackId(), stackIcon);
        } catch (IOException e) {
            throw new ServerException(format("Failed to store stack icon for stack with id %s", stackIcon.getStackId()), e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void remove(String stackId) throws NotFoundException, ServerException {
        requireNonNull(stackId, "Stack id required");
        lock.writeLock().lock();
        try {
            if (!icons.containsKey(stackId)) {
                throw new NotFoundException(format("Icon for stack id '%s' was not found", stackId));
            }
            StackIcon icon = icons.get(stackId);
            Path filePath = Paths.get(iconFolderPath.toString(), icon.getName());
            Files.deleteIfExists(filePath);
            icons.remove(stackId);
        } catch (IOException e) {
            throw new ServerException(format("Failed to delete icon for stack with id '%s' ", stackId), e);
        } finally {
            lock.writeLock().unlock();
        }
    }
}
