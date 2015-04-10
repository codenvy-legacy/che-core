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
package org.eclipse.che.api.machine.server.recipe;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.machine.server.InvalidRecipeException;
import org.eclipse.che.api.machine.shared.Recipe;

import java.util.List;

/**
 *
 * Repository for storing recipes
 *
 * @author gazarenkov
 *
 */
public interface RecipeRepository {

    /**
     * Gets recipes by path
     * @param path
     * @return recipes
     * @throws NotFoundException
     */
    Recipe getRecipe(String path) throws NotFoundException;

    /**
     * Finds recipes by tags
     * @param tag
     * @return list of recipes
     */
    List<Recipe> findRecipes(String tag);

    /**
     * Adds recipe to repository
     * @param recipe
     * @throws InvalidRecipeException
     */
    void addRecipe(Recipe recipe) throws InvalidRecipeException;

    /**
     * Deletes recipe from repository
     * @param path
     */
    void deleteRecipe(String path);

}
