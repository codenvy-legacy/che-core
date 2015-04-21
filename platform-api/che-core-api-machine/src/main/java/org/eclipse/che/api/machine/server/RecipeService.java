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
package org.eclipse.che.api.machine.server;

import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.rest.Service;
import org.eclipse.che.api.machine.server.dao.RecipeDao;
import org.eclipse.che.api.machine.shared.Group;
import org.eclipse.che.api.machine.shared.Permissions;
import org.eclipse.che.api.machine.shared.Recipe;
import org.eclipse.che.api.machine.shared.dto.GroupDescriptor;
import org.eclipse.che.api.machine.shared.dto.NewRecipe;
import org.eclipse.che.api.machine.shared.dto.PermissionsDescriptor;
import org.eclipse.che.api.machine.shared.dto.RecipeDescriptor;
import org.eclipse.che.api.machine.shared.dto.RecipeUpdate;
import org.eclipse.che.commons.lang.NameGenerator;
import org.eclipse.che.commons.user.User;
import org.eclipse.che.dto.server.DtoFactory;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.CREATED;

@Path("/recipe")
public class RecipeService extends Service {

    private final RecipeDao      recipeDao;
    private final Provider<User> user;

    @Inject
    private RecipeService(RecipeDao recipeDao, Provider<User> user) {
        this.recipeDao = recipeDao;
        this.user = user;
    }

    //TODO consider recipe creation for system/admin

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed({"user", "system/admin"})
    public Response createRecipe(NewRecipe newRecipe) throws ApiException {
        if (newRecipe == null) {
            throw new ForbiddenException("Recipe required");
        }
        if (isNullOrEmpty(newRecipe.getType())) {
            throw new ForbiddenException("Recipe type required");
        }
        if (isNullOrEmpty(newRecipe.getScript())) {
            throw new ForbiddenException("Recipe script required");
        }

        final String id = NameGenerator.generate("recipe", 16);
        final Recipe recipe = new RecipeImpl().withId(id)
                                              .withCreator(user.get().getId())
                                              .withType(newRecipe.getType())
                                              .withScript(newRecipe.getScript())
                                              .withTags(newRecipe.getTags());
        recipeDao.create(recipe);

        return Response.status(CREATED)
                       .entity(asRecipeDescriptor(recipe))
                       .build();
    }

    @GET
    @Path("/{id}")
    @Produces(TEXT_PLAIN)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    public String getRecipeScript(@PathParam("id") String id) {
        //TODO check permissions

        return recipeDao.getById(id).getScript();
    }

    @GET
    @Path("/{id}/json")
    @Produces(APPLICATION_JSON)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    public RecipeDescriptor getRecipe(@PathParam("id") String id) {
        //TODO check permissions

        return asRecipeDescriptor(recipeDao.getById(id));
    }

    @GET
    @Produces(APPLICATION_JSON)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    public List<RecipeDescriptor> getCreatedRecipes() {
        final List<Recipe> recipes = recipeDao.getByCreator(user.get().getId());
        return asRecipeDescriptors(recipes);
    }

    @GET
    @Path("/list")
    @Produces(APPLICATION_JSON)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    public List<RecipeDescriptor> searchRecipes(@QueryParam("tags") List<String> tags, @QueryParam("type") String type) {
        final List<Recipe> recipes = recipeDao.search(tags, type);
        return asRecipeDescriptors(recipes);
    }

    @PUT
    @Path("/{id}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed({"user", "system/admin"})
    public RecipeDescriptor updateRecipe(@PathParam("id") String id, RecipeUpdate update) {
        //TODO check permissions

        final Recipe recipe = recipeDao.getById(id);
        if (update.getType() != null) {
            recipe.setType(update.getType());
        }
        if (update.getScript() != null) {
            recipe.setScript(update.getScript());
        }
        if (update.getPermissions() != null) {
            recipe.setPermissions(PermissionsImpl.fromDescriptor(update.getPermissions()));
        }
        recipeDao.update(recipe);
        return asRecipeDescriptor(recipe);
    }

    @DELETE
    @Path("/{id}")
    public void removeRecipe(@PathParam("{id}") String id) {
        //TODO check permissions

        recipeDao.remove(id);
    }

    private List<RecipeDescriptor> asRecipeDescriptors(List<Recipe> recipes) {
        final List<RecipeDescriptor> descriptors = new ArrayList<>(recipes.size());
        for (Recipe recipe : recipes) {
            descriptors.add(asRecipeDescriptor(recipe));
        }
        return descriptors;
    }

    private RecipeDescriptor asRecipeDescriptor(Recipe recipe) {
        final Permissions permissions = recipe.getPermissions();
        final ArrayList<GroupDescriptor> groups = new ArrayList<>(permissions.getGroups().size());
        for (Group group : permissions.getGroups()) {
            groups.add(DtoFactory.getInstance()
                                 .createDto(GroupDescriptor.class)
                                 .withName(group.getName())
                                 .withUnit(group.getUnit())
                                 .withAcl(group.getAcl()));
        }

        final PermissionsDescriptor permissionsDescriptor = DtoFactory.getInstance()
                                                                      .createDto(PermissionsDescriptor.class)
                                                                      .withGroups(groups)
                                                                      .withUsers(permissions.getUsers());

        return DtoFactory.getInstance()
                         .createDto(RecipeDescriptor.class)
                         .withId(recipe.getId())
                         .withType(recipe.getType())
                         .withScript(recipe.getScript())
                         .withCreator(recipe.getCreator())
                         .withTags(recipe.getTags())
                         .withPermissions(permissionsDescriptor);
    }
}
