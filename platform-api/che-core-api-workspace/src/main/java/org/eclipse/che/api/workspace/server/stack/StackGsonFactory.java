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
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.core.model.machine.Command;
import org.eclipse.che.api.core.model.machine.Limits;
import org.eclipse.che.api.core.model.machine.MachineConfig;
import org.eclipse.che.api.core.model.machine.MachineSource;
import org.eclipse.che.api.core.model.machine.Recipe;
import org.eclipse.che.api.core.model.workspace.EnvironmentState;
import org.eclipse.che.api.core.model.workspace.ProjectConfig;
import org.eclipse.che.api.core.model.workspace.WorkspaceConfig;
import org.eclipse.che.api.machine.server.recipe.adapters.GroupAdapter;
import org.eclipse.che.api.machine.server.recipe.adapters.PermissionsAdapter;
import org.eclipse.che.api.machine.server.recipe.adapters.RecipeTypeAdapter;
import org.eclipse.che.api.machine.shared.Group;
import org.eclipse.che.api.machine.shared.Permissions;
import org.eclipse.che.api.workspace.server.model.impl.stack.StackComponent;
import org.eclipse.che.api.workspace.server.model.impl.stack.StackSource;
import org.eclipse.che.api.workspace.server.stack.adapters.CommandAdapter;
import org.eclipse.che.api.workspace.server.stack.adapters.EnvironmentStateAdapter;
import org.eclipse.che.api.workspace.server.stack.adapters.LimitsAdapter;
import org.eclipse.che.api.workspace.server.stack.adapters.MachineSourceAdapter;
import org.eclipse.che.api.workspace.server.stack.adapters.ProjectConfigAdapter;
import org.eclipse.che.api.workspace.server.stack.adapters.StackComponentAdapter;
import org.eclipse.che.api.workspace.server.stack.adapters.StackSourceAdapter;
import org.eclipse.che.api.workspace.server.stack.adapters.WorkspaceConfigAdapter;

/**
 * Gson factory for local {@link org.eclipse.che.api.workspace.server.model.impl.stack.Stack} storage.
 * This class generate {@link Gson} for serialization and deserialization Stack
 * @see org.eclipse.che.api.workspace.server.dao.StackDao - local stack storage
 *
 * @author Alexander Andrienko
 */
@Singleton
public class StackGsonFactory {

    private final Gson gson;

    @Inject
    public StackGsonFactory() {
        gson = new GsonBuilder().registerTypeAdapter(StackComponent.class, new StackComponentAdapter())
                                .registerTypeAdapter(WorkspaceConfig.class, new WorkspaceConfigAdapter())
                                .registerTypeAdapter(ProjectConfig.class, new ProjectConfigAdapter())
                                .registerTypeAdapter(EnvironmentState.class, new EnvironmentStateAdapter())
                                .registerTypeAdapter(Command.class, new CommandAdapter())
                                .registerTypeAdapter(Recipe.class, new RecipeTypeAdapter())
                                .registerTypeAdapter(Limits.class, new LimitsAdapter())
                                .registerTypeAdapter(MachineSource.class, new MachineSourceAdapter())
                                .registerTypeAdapter(MachineConfig.class, new MachineSourceAdapter())
                                .registerTypeAdapter(StackSource.class, new StackSourceAdapter())
                                .registerTypeAdapter(Permissions.class, new PermissionsAdapter())
                                .registerTypeAdapter(Group.class, new GroupAdapter())
                                .setPrettyPrinting()
                                .create();
    }

    /**
     * Returns {@link Gson} for serialization and deserialization {@link org.eclipse.che.api.workspace.server.model.impl.stack.Stack}
     */
    public Gson getGson() {
        return gson;
    }
}
