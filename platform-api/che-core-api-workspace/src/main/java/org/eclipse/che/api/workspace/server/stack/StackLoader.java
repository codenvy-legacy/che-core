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

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.workspace.server.dao.StackDao;
import org.eclipse.che.api.workspace.server.model.impl.stack.StackImpl;
import org.eclipse.che.api.workspace.server.stack.image.StackIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import static java.lang.String.format;

/**
 * Class for loading list predefined {@link org.eclipse.che.api.workspace.server.model.impl.stack.Stack} to the {@link StackDao}
 * and set {@link StackIcon} to the predefined stack.
 *
 * @author Alexander Andrienko
 */
@Singleton
public class StackLoader {
    private static final Logger LOG = LoggerFactory.getLogger(StackLoader.class);

    private final Map<String, byte[]> loadedIconData;
    private final Path                stackJsonPath;
    private final Path                stackIconFolderPath;
    private final StackDao            stackDao;
    private final Gson                gson;

    @Inject
    public StackLoader(StackGsonFactory stackGsonFactory,
                       @Named("stack.predefined.list.json") String stacksPath,
                       @Named("stack.predefined.icons.folder") String stackIconFolder,
                       StackDao stackDao) {
        this.stackJsonPath = Paths.get(stacksPath);
        this.stackIconFolderPath = Paths.get(stackIconFolder);
        this.stackDao = stackDao;
        this.gson = stackGsonFactory.getGson();
        this.loadedIconData = new HashMap<>();
    }

    /**
     * Load predefined stacks with their icons to the {@link StackDao}
     */
    @PostConstruct
    public void start() {
        loadIconData(stackIconFolderPath);

        if (Files.exists(stackJsonPath) && Files.isRegularFile(stackJsonPath)) {
            try (BufferedReader reader = Files.newBufferedReader(stackJsonPath)) {
                List<StackImpl> stacks = gson.fromJson(reader, new TypeToken<List<StackImpl>>() {}.getType());
                stacks.forEach(this::loadStack);
            } catch (IOException e) {
                LOG.error("Failed to store stacks ", e);
            }
        }
    }

    private void loadIconData(Path folderPath) {
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

    private void loadStack(StackImpl stack) {
        try {
            setIcon(stack);
            stackDao.update(stack);
        } catch (NotFoundException | ServerException e) {
            try {
                stackDao.create(stack);
            } catch (Exception ex) {
                LOG.error(format("Failed to load stack with id '%s' ", stack.getId()), ex.getMessage());
            }
        }
    }

    private void setIcon(StackImpl stack) {
        StackIcon stackIcon = stack.getStackIcon();
        if (stackIcon == null) {
            return;
        }
        byte[] data = loadedIconData.get(stack.getName());
        if (data != null) {
            stackIcon = new StackIcon(stack.getId(), stackIcon.getName(), stackIcon.getMediaType(), data);
            stack.setStackIcon(stackIcon);
        } else {
            stack.setStackIcon(null);
        }
    }
}
