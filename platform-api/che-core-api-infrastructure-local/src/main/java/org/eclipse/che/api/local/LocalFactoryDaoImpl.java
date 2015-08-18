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


import com.google.common.reflect.TypeToken;

import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.factory.FactoryImage;
import org.eclipse.che.api.factory.FactoryStore;
import org.eclipse.che.api.factory.dto.Factory;
import org.eclipse.che.api.local.storage.LocalStorage;
import org.eclipse.che.api.local.storage.LocalStorageFactory;
import org.eclipse.che.commons.lang.NameGenerator;
import org.eclipse.che.commons.lang.Pair;
import org.eclipse.che.dto.server.DtoFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Vladyslav Zhukovskii
 * @author Anton Korneta
 */
@Singleton
public class LocalFactoryDaoImpl implements FactoryStore {

    private final Map<String, Set<FactoryImage>> images;
    private final Map<String, Factory>           factories;
    private final ReentrantReadWriteLock         lock;
    private final LocalStorage                   imageStorage;
    private final LocalStorage                   factoryStorage;

    @Inject
    public LocalFactoryDaoImpl(LocalStorageFactory localStorageFactory) throws IOException {
        images = new HashMap<>();
        factories = new HashMap<>();
        lock = new ReentrantReadWriteLock();
        imageStorage = localStorageFactory.create("images.json");
        factoryStorage = localStorageFactory.create("factories.json");
    }

    @PostConstruct
    public void start() {
        images.putAll(imageStorage.loadMap(new TypeToken<Map<String, Set<FactoryImage>>>() {}));
        factories.putAll(factoryStorage.loadMap(new TypeToken<Map<String, Factory>>() {}));
    }

    @PreDestroy
    public void stop() throws IOException {
        imageStorage.store(images);
        factoryStorage.store(factories);
    }

    @Override
    public String saveFactory(Factory factoryUrl, Set<FactoryImage> images) throws ApiException {
        lock.writeLock().lock();
        try {
            final Factory newFactoryUrl = DtoFactory.getInstance().clone(factoryUrl);
            newFactoryUrl.setId(NameGenerator.generate("", 16));
            final Set<FactoryImage> newImages = new LinkedHashSet<>(images.size());
            for (FactoryImage image : images) {
                newImages.add(new FactoryImage(Arrays.copyOf(image.getImageData(), image.getImageData().length), image.getMediaType(),
                                               image.getName()));
            }

            factories.put(newFactoryUrl.getId(), newFactoryUrl);
            this.images.put(newFactoryUrl.getId(), newImages);

            return newFactoryUrl.getId();
        } catch (IOException e) {
            throw new ConflictException(e.getLocalizedMessage());
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void removeFactory(String id) throws ApiException {
        lock.writeLock().lock();
        try {
            factories.remove(id);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Factory getFactory(String id) {
        lock.readLock().lock();
        try {
            return factories.get(id);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<Factory> findByAttribute(Pair<String, String>... attributes) throws ApiException {
        final List<Factory> result = new LinkedList<>();
        lock.readLock().lock();
        try {
            for (Pair<String, String> attribute : attributes) {
                final String name = attribute.first;
                final String value = attribute.second;
                if (name == null || value == null) {
                    continue;
                }
            }
        } finally {
            lock.readLock().unlock();
        }
        return result;
    }

    @Override
    public Set<FactoryImage> getFactoryImages(String factoryId, String imageId) throws ApiException {
        lock.readLock().lock();
        try {
            if (imageId == null) {
                return images.get(factoryId);
            }
            for (FactoryImage image : images.get(factoryId)) {
                if (image.getName().equals(imageId)) {
                    FactoryImage imageCopy;
                    try {
                        imageCopy = new FactoryImage(Arrays.copyOf(image.getImageData(), image.getImageData().length), image.getMediaType(),
                                                     image.getName());
                    } catch (IOException e) {
                        // Seems that error may happen just if media type isn't supported but since we just create copy of existed FactoryImage
                        // such error isn't excepted here.
                        throw new ConflictException(e.getMessage());
                    }
                    return Collections.singleton(imageCopy);
                }
            }
            return Collections.emptySet();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Update factory at storage.
     *
     * @param factoryId
     *         - factory information
     * @param factory
     *         - factory information
     * @return - if of stored factory
     * @throws org.eclipse.che.api.core.ApiException
     */
    @Override
    public String updateFactory(String factoryId, Factory factory) throws ApiException {
        lock.writeLock().lock();
        try {
            final Factory clonedFactory = DtoFactory.getInstance().clone(factory);
            factories.put(factoryId, clonedFactory);
            return clonedFactory.getId();
        } finally {
            lock.writeLock().unlock();
        }
    }
}
