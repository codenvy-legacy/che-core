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
package org.eclipse.che.api.machine.gwt.client;

import org.eclipse.che.api.machine.shared.dto.RecipeDescriptor;
import org.eclipse.che.api.promises.client.Promise;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Client for Recipe API.
 *
 * @author Artem Zatsarynnyy
 */
public interface RecipeServiceClient {

    /**
     * Create recipe.
     *
     * @param type
     *         type of recipe (e.g., Dockerfile)
     * @param script
     *         recipe script
     */
    Promise<RecipeDescriptor> createRecipe(@Nonnull String type, @Nonnull String script);

    /** Get recipe script by recipe's ID. */
    Promise<String> getRecipeScript(@Nonnull String id);

    /** Get recipe by ID. */
    Promise<RecipeDescriptor> getRecipe(@Nonnull String id);

    /**
     * Get all recipes.
     *
     * @param skipCount
     *         count of items which should be skipped
     * @param maxItems
     *         max count of items to fetch
     */
    Promise<List<RecipeDescriptor>> getAllRecipes(int skipCount, int maxItems);

    /**
     * Search for recipes which type is equal to the specified {@code type}
     * and tags contain all of the specified {@code tags}.
     *
     * @param tags
     *         recipe tags
     * @param type
     *         recipe type
     * @param skipCount
     *         count of items which should be skipped
     * @param maxItems
     *         max count of items to fetch
     */
    Promise<List<RecipeDescriptor>> searchRecipes(@Nullable List<String> tags, @Nullable String type, int skipCount, int maxItems);

    /**
     * Update recipe.
     *
     * @param id
     *         ID of the recipe that should be updated
     * @param type
     *         new type for recipe
     * @param script
     *         new script for recipe
     */
    Promise<RecipeDescriptor> updateRecipe(@Nonnull String id, @Nullable String type, @Nullable String script);

    /** Remove recipe with the given ID. */
    Promise<Void> removeRecipe(@Nonnull String id);
}
