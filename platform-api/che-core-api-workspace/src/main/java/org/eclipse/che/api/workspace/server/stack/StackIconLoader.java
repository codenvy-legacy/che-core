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

package org.eclipse.che.api.workspace.server.stack;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.workspace.server.dao.StackIconDao;
import org.eclipse.che.api.workspace.server.stack.image.StackIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

/**
 * Class for loading icons(@link StackIcon) for predefined stack list to the storage {@link StackIconDao}
 * @see org.eclipse.che.api.workspace.server.model.impl.stack.Stack
 *
 * @author Alexander Andrienko
 */
@Singleton
public class StackIconLoader {

    private static final Logger LOG = LoggerFactory.getLogger(StackIconLoader.class);

    private final Map<String, byte[]> loadedIconData;
    private final Path                stackIconJsonPath;
    private final Path                stackIconFolderPath;
    private final Gson                gson;
    private final StackIconDao        stackIconDao;

    @Inject
    public StackIconLoader(@Named("stack.predefined.icons.json") String stackJson,
                           @Named("stack.predefined.icons.folder") String stackIconFolder,
                           StackIconDao stackIconDao,
                           StackIconGsonFactory stackIconGsonFactory) {
        this.loadedIconData = new HashMap<>();
        this.stackIconJsonPath = Paths.get(stackJson);
        this.stackIconFolderPath = Paths.get(stackIconFolder);
        this.stackIconDao = stackIconDao;
        this.gson = stackIconGsonFactory.getGson();
    }

    /**
     * Load predefined stack icons to the {@link StackIconDao}
     */
    @PostConstruct
    public void start() {
        loadIconData(stackIconFolderPath);
        if (Files.exists(stackIconJsonPath) && Files.isRegularFile(stackIconJsonPath)) {
            try(BufferedReader reader = Files.newBufferedReader((stackIconJsonPath))) {
                List<StackIcon> stackIcons = gson.fromJson(reader, new TypeToken<List<StackIcon>>() {}.getType());
                stackIcons.forEach(this::loadStackIcon);
            } catch (IOException e) {
                LOG.error("Failed to store stack icons ", e);
            }
        }
    }

    private void loadStackIcon(StackIcon stackIcon) {
        try {
            byte[] data = loadedIconData.get(stackIcon.getName());
            stackIcon.setData(data);
            stackIconDao.save(stackIcon);
        } catch (ServerException e) {
            LOG.error(format("Failed to save data for icon '%s'", stackIcon.getName()), e);
        }
    }

    public void loadIconData(Path folderPath) {
        try {
            DirectoryStream<Path> directoryStream = Files.newDirectoryStream(folderPath);
            for (Path innerFile : directoryStream) {
                if (Files.isRegularFile(innerFile) && Files.isReadable(innerFile)) {
                    byte[] data = Files.readAllBytes(innerFile);
                    loadedIconData.put(innerFile.getFileName().toString(), data);
                }
            }
        } catch (IOException e) {
            LOG.error("Failed to load stack icons data ", e);
        }
    }
}
