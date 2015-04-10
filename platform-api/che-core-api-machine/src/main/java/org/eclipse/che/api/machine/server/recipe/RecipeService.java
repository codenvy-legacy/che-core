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

import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.recipe.RecipeId;
import org.eclipse.che.api.machine.server.InvalidRecipeException;
import org.eclipse.che.api.machine.shared.Recipe;
import org.eclipse.che.api.machine.shared.dto.RecipeDescriptor;
import org.eclipse.che.dto.server.DtoFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * REST service for RecipeRepository
 *
 * @author gazarenkov
 */
@Singleton
public class RecipeService {

    private final RecipeRepository repository;

    @Inject
    public RecipeService(RecipeRepository repository) {
        this.repository = repository;
    }

    public RecipeDescriptor getRecipe(String path) throws ForbiddenException, InvalidRecipeException, NotFoundException {

        //RecipeId id = RecipeId.parse(fqn);

        Recipe recipe = repository.getRecipe(path);
                //storage(id.getScope()).getRecipe(id.getPath());

        RecipeDescriptor descriptor = DtoFactory.getInstance().createDto(RecipeDescriptor.class)
                .withScript(recipe.getScript())
                .withType(recipe.getType());

        return descriptor;

    }

    public List<RecipeDescriptor> findRecipe(String tag) {
        // TODO
        return new ArrayList<>();
    }

    public void addRecipe(RecipeDescriptor recipe) {
        // TODO
    }

    public void  deleteRecipe(String fqn) {
        //TODO
    }

//
//    private RecipeRepository storage(RecipeId.Scope scope) throws InvalidRecipeException {
//        if(scope.equals(RecipeId.Scope.system))
//            return systemScopeStorage;
//        else if(scope.equals(RecipeId.Scope.project))
//            return projectScopeStorage;
//        else if(scope.equals(RecipeId.Scope.user))
//            return userScopeStorage;
//        else
//            throw new InvalidRecipeException("Illegal scope "+scope);
//    }


}
