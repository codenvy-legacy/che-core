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

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.local.storage.deserialize.GroupSerializer;
import org.eclipse.che.api.local.storage.deserialize.PermissionsSerializer;
import org.eclipse.che.api.machine.server.dao.RecipeDao;
import org.eclipse.che.api.machine.server.recipe.RecipeImpl;
import org.eclipse.che.api.machine.shared.Group;
import org.eclipse.che.api.machine.shared.Permissions;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;

/**
 * Loads predefined recipes into local dao.
 *
 * @author Anton Korneta
 */
@Singleton
public class PredefinedRecipeLoader {
    private static final Gson GSON = new GsonBuilder().registerTypeAdapter(Permissions.class, new PermissionsSerializer())
                                                      .registerTypeAdapter(Group.class, new GroupSerializer())
                                                      .create();

    private final String    recipesFileName;
    private final RecipeDao recipeDao;

    @Inject
    public PredefinedRecipeLoader(@Named("che.local.infrastructure.recipes.filename") String recipesName, RecipeDao recipeDao) {
        this.recipesFileName = recipesName;
        this.recipeDao = recipeDao;
    }

    @PostConstruct
    public void start() throws ServerException {
        URL recipeUrl = Thread.currentThread().getContextClassLoader().getResource(recipesFileName);
        if (recipeUrl == null) {
            throw new ServerException("File with predefined recipes " + recipesFileName + " doesn't exist");
        }
        try (InputStream is = recipeUrl.openStream()) {
            List<RecipeImpl> recipes = GSON.fromJson(new InputStreamReader(is), new TypeToken<List<RecipeImpl>>() {}.getType());
            for (RecipeImpl recipe : recipes) {
                recipeDao.create(recipe);
            }
        } catch (IOException | ConflictException | JsonIOException | JsonSyntaxException e) {
            throw new ServerException("Impossible to put recipes into local dao", e);
        }
    }
}
