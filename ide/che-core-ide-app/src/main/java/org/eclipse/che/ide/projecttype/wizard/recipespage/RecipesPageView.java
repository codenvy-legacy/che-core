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
package org.eclipse.che.ide.projecttype.wizard.recipespage;

import com.google.inject.ImplementedBy;

import org.eclipse.che.ide.api.mvp.View;

import java.util.List;

/**
 * The view of {@link RecipesPagePresenter}.
 *
 * @author Artem Zatsarynnyy
 */
@ImplementedBy(RecipesPageViewImpl.class)
public interface RecipesPageView extends View<RecipesPageView.ActionDelegate> {

    /**
     * Set recipes list.
     *
     * @param recipes
     *         recipes list to set
     */
    void setRecipes(List<String> recipes);

    /**
     * Select the specified recipe.
     *
     * @param recipe
     *         recipe to select
     */
    void selectRecipe(String recipe);

    interface ActionDelegate {

        /**
         * Called when recipe selected.
         *
         * @param recipe
         *         selected recipe
         */
        void recipeSelected(String recipe);
    }
}
