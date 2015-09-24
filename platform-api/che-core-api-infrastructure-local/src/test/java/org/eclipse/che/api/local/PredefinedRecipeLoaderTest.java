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

import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.local.storage.PredefinedRecipeLoader;
import org.eclipse.che.api.machine.server.dao.RecipeDao;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

/**
 * @author Anton Korneta
 */
@Listeners(MockitoTestNGListener.class)
public class PredefinedRecipeLoaderTest {

    private PredefinedRecipeLoader recipeLoader;

    @Mock
    private RecipeDao recipeDao;

    @Test
    public void shouldLoadPredictableRecipesFromValidJson() throws Exception {
        recipeLoader = new PredefinedRecipeLoader("recipes.json", recipeDao);

        recipeLoader.start();

        verify(recipeDao, times(2)).create(any());
    }

    @Test(expectedExceptions = ServerException.class, expectedExceptionsMessageRegExp = "Impossible to put recipes into local dao")
    public void shouldThrowExceptionWhenLoadPredictableRecipesFromInvalidJson() throws Exception {
        recipeLoader = new PredefinedRecipeLoader("invalid-recipes.json", recipeDao);

        recipeLoader.start();
    }

    @Test(expectedExceptions = ServerException.class, expectedExceptionsMessageRegExp = "File with predefined recipes any.json doesn't exist")
    public void shouldThrowExceptionWhenCannotLoadPredictableRecipes() throws Exception {
        recipeLoader = new PredefinedRecipeLoader("any.json", recipeDao);

        recipeLoader.start();
    }
}