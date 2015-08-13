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
package org.eclipse.che.api.local.storage;


import com.google.common.io.Files;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Implementation of file system storage for model objects.
 *
 * @author Anton Korneta
 */
public class LocalStorage {

    private static final Logger LOG  = LoggerFactory.getLogger(LocalStorage.class);
    private static final Gson   GSON = new Gson();

    /** json file to store and load */
    private final File storedFile;

    public LocalStorage(String rootDirPath, String fileName)
            throws IOException {
        File rootDir = new File(rootDirPath);
        if (!rootDir.exists() && !rootDir.mkdirs()) {
            throw new IOException("Impossible to create root folder for local storage");
        }
        storedFile = new File(rootDir, fileName);
    }

    public void store(Object storedObj) throws IOException {
        try (Writer writer = Files.newWriter(storedFile, Charset.forName("UTF-8"))) {
            GSON.toJson(storedObj, writer);
        }
    }

    /**
     * @param <T>
     *         the type of the desired object.
     * @param token
     *         type holder.
     * @return an object of type T from the json file or null if json invalid or file not found.
     */
    public <T> T load(TypeToken<T> token) {
        T result = null;
        try (Reader reader = Files.newReader(storedFile, Charset.forName("UTF-8"))) {
            result = GSON.fromJson(reader, token.getType());
        } catch (JsonSyntaxException e) {
            LOG.warn(storedFile.getName() + " contains invalid JSON content");
        } catch (IOException ioEx) {
            LOG.warn("Impossible to read from " + storedFile.getName());
        }
        return result;
    }

    /**
     * @param <T>
     *         the type of the desired object.
     * @param listToken
     *         list type holder.
     * @return list objects of type T from json file. If json invalid or file not found return emptyList.
     */
    public <T> List<T> loadList(TypeToken<List<T>> listToken) {
        List<T> result = load(listToken);
        if (result == null) {
            return Collections.emptyList();
        }
        return result;
    }

    /**
     * @param <K>
     *         the type of keys maintained by this map
     * @param <V>
     *         the type of mapped values
     * @param mapToken
     *         map type holder.
     * @return map objects from json file. If json invalid or file not found return emptyMap.
     */
    public <K, V> Map<K, V> loadMap(TypeToken<Map<K, V>> mapToken) {
        Map<K, V> result = load(mapToken);
        if (result == null) {
            return Collections.emptyMap();
        }
        return result;
    }
}