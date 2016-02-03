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

import com.google.common.base.Strings;
import com.google.common.io.Resources;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.machine.Command;
import org.eclipse.che.api.core.model.machine.Limits;
import org.eclipse.che.api.core.model.machine.MachineConfig;
import org.eclipse.che.api.core.model.machine.MachineSource;
import org.eclipse.che.api.core.model.machine.Recipe;
import org.eclipse.che.api.core.model.workspace.EnvironmentState;
import org.eclipse.che.api.core.model.workspace.ProjectConfig;
import org.eclipse.che.api.core.model.workspace.WorkspaceConfig;
import org.eclipse.che.api.machine.server.recipe.adapters.GroupSerializer;
import org.eclipse.che.api.machine.server.recipe.adapters.PermissionsSerializer;
import org.eclipse.che.api.machine.shared.Group;
import org.eclipse.che.api.machine.shared.Permissions;
import org.eclipse.che.api.workspace.server.dao.StackDao;
import org.eclipse.che.api.workspace.server.model.impl.stack.DecoratedStackImpl;
import org.eclipse.che.api.workspace.server.model.impl.stack.StackComponent;
import org.eclipse.che.api.workspace.server.model.impl.stack.StackSource;
import org.eclipse.che.api.workspace.server.stack.adapters.ByteArrayToBase64Serializer;
import org.eclipse.che.api.workspace.server.stack.adapters.CommandSerializer;
import org.eclipse.che.api.workspace.server.stack.adapters.EnvironmentStateSerializer;
import org.eclipse.che.api.workspace.server.stack.adapters.LimitsSerializer;
import org.eclipse.che.api.workspace.server.stack.adapters.MachineSourceSerializer;
import org.eclipse.che.api.workspace.server.stack.adapters.ProjectConfigSerializer;
import org.eclipse.che.api.workspace.server.stack.adapters.RecipeSerializer;
import org.eclipse.che.api.workspace.server.stack.adapters.StackComponentSerializer;
import org.eclipse.che.api.workspace.server.stack.adapters.StackSourceSerializer;
import org.eclipse.che.api.workspace.server.stack.adapters.WorkspaceConfigSerializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import static com.google.common.base.MoreObjects.firstNonNull;
import static java.util.Collections.emptyList;

/**
 * Loads list predefined stacks.
 *
 * @author Alexander Andrienko
 */
@Singleton
public class StackLoader {
    private static final Gson GSON = new GsonBuilder().registerTypeAdapter(StackComponent.class, new StackComponentSerializer())
                                                      .registerTypeAdapter(WorkspaceConfig.class, new WorkspaceConfigSerializer())
                                                      .registerTypeAdapter(ProjectConfig.class, new ProjectConfigSerializer())
                                                      .registerTypeAdapter(EnvironmentState.class, new EnvironmentStateSerializer())
                                                      .registerTypeAdapter(Command.class, new CommandSerializer())
                                                      .registerTypeAdapter(Recipe.class, new RecipeSerializer())
                                                      .registerTypeAdapter(Limits.class, new LimitsSerializer())
                                                      .registerTypeAdapter(MachineSource.class, new MachineSourceSerializer())
                                                      .registerTypeAdapter(MachineConfig.class, new MachineSourceSerializer())
                                                      .registerTypeAdapter(StackSource.class, new StackSourceSerializer())
                                                      .registerTypeHierarchyAdapter(byte[].class, new ByteArrayToBase64Serializer())
                                                      .registerTypeAdapter(Permissions.class, new PermissionsSerializer())
                                                      .registerTypeAdapter(Group.class, new GroupSerializer())
                                                      .create();

    private final Set<String> stackPaths;
    private final StackDao    stackDao;

    @Inject
    public StackLoader(@Named("predefined.stack.path") Set<String> stackPaths, StackDao stackDao) {
        this.stackPaths = firstNonNull(stackPaths, Collections.<String>emptySet());
        this.stackDao = stackDao;
    }

    @PostConstruct
    public void start() throws ServerException {
        for (String stackPath: stackPaths) {
            if (!Strings.isNullOrEmpty(stackPath)) {
                for (DecoratedStackImpl stack: loadStacks(stackPath)) {
                    try {
                        try {
                            stackDao.update(stack);
                        } catch (NotFoundException e) {
                            stackDao.create(stack);
                        }
                    } catch (ConflictException e) {
                        throw new ServerException("Failed to store stack " + stack, e);
                    }
                }
            }
        }
    }

    private List<DecoratedStackImpl> loadStacks(String stackPath) throws ServerException {
        try (InputStream is = getResource(stackPath)) {
            return firstNonNull(GSON.fromJson(new InputStreamReader(is), new TypeToken<List<DecoratedStackImpl>>() {}.getType()), emptyList());
        } catch (IOException | JsonIOException | JsonSyntaxException e) {
            throw new ServerException("Failed to get stacks from specified path " + stackPath, e);
        }
    }

    private InputStream getResource(String resource) throws IOException {
        File resourceFile = new File(resource);
        if (resourceFile.exists() && !resourceFile.isFile()) {
            throw new IOException(String.format("%s is not a file. ", resourceFile.getAbsolutePath()));
        }
        InputStream is = resourceFile.exists() ? new FileInputStream(resourceFile) : Resources.getResource(resource).openStream();
        if (is == null) {
            throw new IOException(String.format("Not found resource: %s", resource));
        }
        return is;
    }
}
