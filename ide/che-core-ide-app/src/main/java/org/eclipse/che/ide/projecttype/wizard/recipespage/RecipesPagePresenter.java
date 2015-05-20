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

import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.machine.gwt.client.RecipeServiceClient;
import org.eclipse.che.api.machine.shared.dto.RecipeDescriptor;
import org.eclipse.che.api.project.shared.dto.ImportProject;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.ide.api.wizard.AbstractWizardPage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Project wizard page for selecting recipe.
 *
 * @author Artem Zatsarynnyy
 */
@Singleton
public class RecipesPagePresenter extends AbstractWizardPage<ImportProject> implements RecipesPageView.ActionDelegate {

    private final RecipesPageView     view;
    private final RecipeServiceClient recipeServiceClient;

    @Inject
    protected RecipesPagePresenter(RecipesPageView view, RecipeServiceClient recipeServiceClient) {
        this.view = view;
        this.recipeServiceClient = recipeServiceClient;
        this.view.setDelegate(this);
    }

    /** {@inheritDoc} */
    @Override
    public void go(AcceptsOneWidget container) {
        container.setWidget(view);
        view.setRecipes(Collections.<String>emptyList());
        requestRecipes();
    }

    private void requestRecipes() {
        recipeServiceClient.getAllRecipes(0, 10).then(new Operation<List<RecipeDescriptor>>() {
            @Override
            public void apply(List<RecipeDescriptor> arg) throws OperationException {
                final List<String> recipes = new ArrayList<>();
                for (RecipeDescriptor descriptor : arg) {
                    recipes.add(descriptor.getId());
                }

                view.setRecipes(recipes);
                updateView();
            }
        });
    }

    /** Updates view from data-object. */
    private void updateView() {
        final String recipe = dataObject.getProject().getRecipe();
        if (recipe != null) {
            view.selectRecipe(recipe);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void recipeSelected(String recipe) {
        dataObject.getProject().setRecipe(recipe);
    }
}
