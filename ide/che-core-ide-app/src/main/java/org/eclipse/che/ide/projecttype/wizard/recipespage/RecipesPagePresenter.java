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
import org.eclipse.che.api.machine.shared.dto.recipe.RecipeDescriptor;
import org.eclipse.che.api.project.gwt.client.ProjectTypeServiceClient;
import org.eclipse.che.api.project.shared.dto.ProjectTypeDefinition;
import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
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
public class RecipesPagePresenter extends AbstractWizardPage<ProjectConfigDto> implements RecipesPageView.ActionDelegate {

    private final RecipesPageView          view;
    private final RecipeServiceClient      recipeServiceClient;
    private final ProjectTypeServiceClient projectTypeServiceClient;

    @Inject
    protected RecipesPagePresenter(RecipesPageView view,
                                   RecipeServiceClient recipeServiceClient,
                                   ProjectTypeServiceClient projectTypeServiceClient) {
        this.view = view;
        this.recipeServiceClient = recipeServiceClient;
        this.projectTypeServiceClient = projectTypeServiceClient;

        view.setDelegate(this);
    }

    @Override
    public void go(AcceptsOneWidget container) {
        container.setWidget(view);
        view.clearRecipes();
        requestRecipesAndUpdateView();
    }

    @Override
    public void onRecipeSelected(String recipe) {

        //dataObject.getProject().setRecipe(recipe);
    }

    private void requestRecipesAndUpdateView() {
        final String projectTypeID = dataObject.getType();
        final Promise<ProjectTypeDefinition> projectTypePromise = projectTypeServiceClient.getProjectType(projectTypeID);
        final Promise<List<RecipeDescriptor>> recipesPromise = recipeServiceClient.getAllRecipes();

        final List<String> recipesList = new ArrayList<>();

        projectTypePromise.thenPromise(new Function<ProjectTypeDefinition, Promise<List<RecipeDescriptor>>>() {
            @Override
            public Promise<List<RecipeDescriptor>> apply(ProjectTypeDefinition arg) throws FunctionException {
                final String defaultRecipeURL = arg.getDefaultRecipe();
                if (defaultRecipeURL != null) {
                    recipesList.add(defaultRecipeURL);
                }

                return recipesPromise;
            }
        }).then(new Operation<List<RecipeDescriptor>>() {
            @Override
            public void apply(List<RecipeDescriptor> arg) throws OperationException {
                for (RecipeDescriptor descriptor : arg) {
                    recipesList.add(descriptor.getLink("get recipe script").getHref());
                }

                view.setRecipes(recipesList);
                updateView();
            }
        });
    }

    /** Updates view from data-object. */
    private void updateView() {
        final String recipe = null; //dataObject.getRecipe();
        if (recipe != null) {
            view.selectRecipe(recipe);
        }
    }
}
