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

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.user.server.dao.PreferenceDao;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

/**
 * @author Eugene Voevodin
 */
@Singleton
public class LocalPreferenceDaoImpl implements PreferenceDao {
    private static final Logger LOG = LoggerFactory.getLogger(LocalPreferenceDaoImpl.class);

    private final Gson                             gson;
    private final File                             storageFile;
    private final Map<String, Map<String, String>> storage;
    private final ReadWriteLock                    lock;

    @Inject
    public LocalPreferenceDaoImpl(@Nullable @Named("preferences.store_location") String dirPath) {
        if (dirPath == null || dirPath.isEmpty()) {
            storageFile = new File(System.getProperty("java.io.tmpdir"), "PreferencesStore.json");
        } else {
            storageFile = new File(dirPath, "PreferencesStore.json");
        }
        storage = new HashMap<>();
        lock = new ReentrantReadWriteLock();
        gson = new Gson();
    }

    @PostConstruct
    private void start() {
        // use write lock since we are init storage at this stage
        lock.writeLock().lock();
        try {
            if (storageFile.exists()) {
                Reader reader = null;
                try {
                    reader = Files.newReader(storageFile, Charset.forName("UTF-8"));
                    Map<String, Map<String, String>> m = gson.fromJson(reader, new TypeToken<Map<String, Map<String, String>>>() {
                    }.getType());
                    if (m != null) {
                        storage.putAll(m);
                    }
                } catch (Exception e) {
                    LOG.error(String.format("Failed load user profiles form %s", storageFile), e);
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException ignored) {
                        }
                    }
                }
            }
            // Add default entry if file doesn't exist or invalid or empty.
            if (storage.isEmpty()) {
                final Map<String, String> newPreferences = new HashMap<>(4);
                newPreferences.put("preference1", "value");
                newPreferences.put("preference2", "value");
                storage.put("codenvy", newPreferences);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @PreDestroy
    private void stop() {
        lock.writeLock().lock();
        try {
            Writer writer = null;
            try {
                writer = Files.newWriter(storageFile, Charset.forName("UTF-8"));
                gson.toJson(storage, new TypeToken<Map<String, Map<String, String>>>() {
                }.getType(), writer);
            } catch (Exception e) {
                LOG.error(String.format("Failed setPreferences user profiles form %s", storageFile), e);
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException e) {
                        LOG.error(String.format("Failed setPreferences user profiles form %s", storageFile), e);
                    }
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void setPreferences(String userId, Map<String, String> preferences) throws ServerException, NotFoundException {
        lock.writeLock().lock();
        try {
            storage.put(userId, new HashMap<>(preferences));
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Map<String, String> getPreferences(String userId) throws ServerException {
        lock.readLock().lock();
        try {
            final Map<String, String> preferences = new HashMap<>();
            if (storage.containsKey(userId)) {
                preferences.putAll(this.storage.get(userId));
            }
            return preferences;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Map<String, String> getPreferences(String userId, String filter) throws ServerException {
        lock.readLock().lock();
        try {
            return filter(getPreferences(userId), filter);
        } finally {
            lock.readLock().unlock();
        }
    }

    private Map<String, String> filter(Map<String, String> preferences, String filter) {
        final Map<String, String> filtered = new HashMap<>();
        final Pattern pattern = Pattern.compile(filter);
        for (Map.Entry<String, String> entry : preferences.entrySet()) {
            if (pattern.matcher(entry.getKey()).matches()) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }
        return filtered;
    }

    @Override
    public void remove(String userId) throws ServerException {
        lock.writeLock().lock();
        try {
            storage.remove(userId);
        } finally {
            lock.writeLock().unlock();
        }
    }
}
