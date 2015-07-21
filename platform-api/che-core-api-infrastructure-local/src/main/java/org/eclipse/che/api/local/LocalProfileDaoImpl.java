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
import org.eclipse.che.api.user.server.dao.Profile;
import org.eclipse.che.api.user.server.dao.UserProfileDao;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Singleton
public class LocalProfileDaoImpl implements UserProfileDao {
    private static final Logger LOG = LoggerFactory.getLogger(LocalProfileDaoImpl.class);

    private final File                 storageFile;
    private final Gson                 gson;
    private final Map<String, Profile> profiles;
    private final ReadWriteLock        lock;

    @Inject
    public LocalProfileDaoImpl(@Nullable @Named("user.local.db") String dirPath) {
        if (dirPath == null || dirPath.isEmpty()) {
            storageFile = new File(System.getProperty("java.io.tmpdir"), "profile.json");
        } else {
            storageFile = new File(dirPath, "profile.json");
        }
        gson = new Gson();
        profiles = new HashMap<>();
        lock = new ReentrantReadWriteLock();
    }

    @PostConstruct
    private void start() {
        // use write lock since we are validate storage at this stage
        lock.writeLock().lock();
        try {
            if (storageFile.exists()) {
                Reader reader = null;
                try {
                    reader = Files.newReader(storageFile, Charset.forName("UTF-8"));
                    Map<String, Profile> m = gson.fromJson(reader, new TypeToken<Map<String, Profile>>() {
                    }.getType());
                    if (m != null) {
                        profiles.putAll(m);
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
            if (profiles.isEmpty()) {
                final Map<String, String> attributes = new HashMap<>(2);
                attributes.put("firstName", "Che");
                attributes.put("lastName", "Codenvy");
                Profile profile = new Profile().withId("codenvy")
                                               .withUserId("codenvy")
                                               .withAttributes(attributes);
                profiles.put(profile.getId(), profile);
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
                gson.toJson(profiles, new TypeToken<Map<String, Profile>>() {
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
    public void create(Profile profile) {
        lock.writeLock().lock();
        try {
            // just replace existed profile
            final Profile copy = new Profile().withId(profile.getId()).withUserId(profile.getUserId())
                                              .withAttributes(new LinkedHashMap<>(profile.getAttributes()));
            profiles.put(copy.getId(), copy);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void update(Profile profile) throws NotFoundException {
        lock.writeLock().lock();
        try {
            final Profile myProfile = profiles.get(profile.getId());
            if (myProfile == null) {
                throw new NotFoundException(String.format("Profile not found %s", profile.getId()));
            }
            myProfile.getAttributes().clear();
            myProfile.getAttributes().putAll(profile.getAttributes());
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void remove(String id) throws NotFoundException {
        lock.writeLock().lock();
        try {
            final Profile profile = profiles.remove(id);
            if (profile == null) {
                throw new NotFoundException(String.format("Profile not found %s", id));
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Profile getById(String id) throws NotFoundException {
        lock.readLock().lock();
        try {
            final Profile profile = profiles.get(id);
            if (profile == null) {
                throw new NotFoundException(String.format("Profile not found %s", id));
            }
            return new Profile().withId(profile.getId()).withUserId(profile.getUserId())
                                .withAttributes(new LinkedHashMap<>(profile.getAttributes()));
        } finally {
            lock.readLock().unlock();
        }
    }
}
