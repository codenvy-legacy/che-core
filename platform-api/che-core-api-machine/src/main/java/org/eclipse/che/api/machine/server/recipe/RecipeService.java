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

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;

import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.rest.Service;
import org.eclipse.che.api.machine.server.PermissionsChecker;
import org.eclipse.che.api.machine.server.PermissionsImpl;
import org.eclipse.che.api.machine.server.RecipeImpl;
import org.eclipse.che.api.machine.server.dao.RecipeDao;
import org.eclipse.che.api.machine.shared.Group;
import org.eclipse.che.api.machine.shared.Permissions;
import org.eclipse.che.api.machine.shared.Recipe;
import org.eclipse.che.api.machine.shared.dto.GroupDescriptor;
import org.eclipse.che.api.machine.shared.dto.NewRecipe;
import org.eclipse.che.api.machine.shared.dto.PermissionsDescriptor;
import org.eclipse.che.api.machine.shared.dto.RecipeDescriptor;
import org.eclipse.che.api.machine.shared.dto.RecipeUpdate;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.lang.NameGenerator;
import org.eclipse.che.commons.user.User;
import org.eclipse.che.dto.server.DtoFactory;

import javax.annotation.Nullable;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
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
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.CREATED;

@Path("/recipe")
public class RecipeService extends Service {

    private static final Function<Recipe, RecipeDescriptor> RECIPE_TO_DESCRIPTOR_FUNCTION = new RecipeToDescriptorFunction();

    private final RecipeDao          recipeDao;
    private final PermissionsChecker permissionsChecker;

    @Inject
    public RecipeService(RecipeDao recipeDao, PermissionsChecker permissionsChecker) {
        this.recipeDao = recipeDao;
        this.permissionsChecker = permissionsChecker;
    }

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed({"user", "system/admin", "system/manager"})
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
        Permissions permissions = null;
        if (newRecipe.getPermissions() != null) {
            checkPublicPermission(newRecipe.getPermissions());
            permissions = PermissionsImpl.fromDescriptor(newRecipe.getPermissions());
        }

        final Recipe recipe = new RecipeImpl().withId(NameGenerator.generate("recipe", 16))
                                              .withCreator(currentUser().getId())
                                              .withType(newRecipe.getType())
                                              .withScript(newRecipe.getScript())
                                              .withTags(newRecipe.getTags())
                                              .withPermissions(permissions);
        recipeDao.create(recipe);

        return Response.status(CREATED)
                       .entity(RECIPE_TO_DESCRIPTOR_FUNCTION.apply(recipe))
                       .build();
    }

    @GET
    @Path("/{id}")
    @Produces(TEXT_PLAIN)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    public String getRecipeScript(@PathParam("id") String id) throws ApiException {
        final Recipe recipe = recipeDao.getById(id);

        final String userId = currentUser().getId();
        if (!permissionsChecker.hasAccess(recipe, userId, "read")) {
            throw new ForbiddenException(format("User %s doesn't have access to recipe %s", userId, id));
        }

        return recipe.getScript();
    }

    @GET
    @Path("/{id}/json")
    @Produces(APPLICATION_JSON)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    public RecipeDescriptor getRecipe(@PathParam("id") String id) throws ApiException {
        final Recipe recipe = recipeDao.getById(id);

        final String userId = currentUser().getId();
        if (!permissionsChecker.hasAccess(recipe, userId, "read")) {
            throw new ForbiddenException(format("User %s doesn't have access to recipe %s", userId, id));
        }

        return RECIPE_TO_DESCRIPTOR_FUNCTION.apply(recipe);
    }

    @GET
    @Produces(APPLICATION_JSON)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    public List<RecipeDescriptor> getCreatedRecipes() {
        final List<Recipe> recipes = recipeDao.getByCreator(currentUser().getId());
        return FluentIterable.from(recipes)
                             .transform(RECIPE_TO_DESCRIPTOR_FUNCTION)
                             .toList();
    }

    @GET
    @Path("/list")
    @Produces(APPLICATION_JSON)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    public List<RecipeDescriptor> searchRecipes(@QueryParam("tags") List<String> tags, @QueryParam("type") String type) {
        final List<Recipe> recipes = recipeDao.search(tags, type);
        return FluentIterable.from(recipes)
                             .transform(RECIPE_TO_DESCRIPTOR_FUNCTION)
                             .toList();
    }

    //TODO consider update_acl permission

    @PUT
    @Path("/{id}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    public RecipeDescriptor updateRecipe(@PathParam("id") String id, RecipeUpdate update) throws ApiException {
        final Recipe recipe = recipeDao.getById(id);

        final String userId = currentUser().getId();
        if (!permissionsChecker.hasAccess(recipe, userId, "write")) {
            throw new ForbiddenException(format("User %s doesn't have access to update recipe %s", userId, id));
        }
        boolean updateRequired = false;
        if (update.getType() != null) {
            recipe.setType(update.getType());
            updateRequired = true;
        }
        if (update.getScript() != null) {
            recipe.setScript(update.getScript());
            updateRequired = true;
        }
        if (!update.getTags().isEmpty()) {
            recipe.setTags(update.getTags());
            updateRequired = true;
        }
        if (update.getPermissions() != null) {
            //ensure that user has access to update recipe permissions
            if (!permissionsChecker.hasAccess(recipe, userId, "update_acl")) {
                throw new ForbiddenException(format("User %s doesn't have access to update recipe %s permissions", userId, id));
            }
            checkPublicPermission(update.getPermissions());
            recipe.setPermissions(PermissionsImpl.fromDescriptor(update.getPermissions()));
            updateRequired = true;
        }
        if (updateRequired) {
            recipeDao.update(recipe);
        }
        return RECIPE_TO_DESCRIPTOR_FUNCTION.apply(recipe);
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"user", "system/admin", "system/manager"})
    public void removeRecipe(@PathParam("id") String id) throws ApiException {
        final Recipe recipe = recipeDao.getById(id);

        final String userId = currentUser().getId();
        if (!permissionsChecker.hasAccess(recipe, userId, "write")) {
            throw new ForbiddenException(format("User %s doesn't have access to recipe %s", userId, id));
        }

        recipeDao.remove(id);
    }

    /**
     * User who is neither 'workspace/admin' no 'workspace/developer' can't create or
     * update recipe permissions to 'public: search', this operation allowed only
     * for 'system/admin' or 'system/manager'
     */
    private void checkPublicPermission(PermissionsDescriptor permissions) throws ForbiddenException {
        final User user = currentUser();
        if (!user.isMemberOf("system/admin") && !user.isMemberOf("system/manager")) {
            for (GroupDescriptor group : permissions.getGroups()) {
                if ("public".equalsIgnoreCase(group.getName()) && group.getAcl().contains("search")) {
                    throw new ForbiddenException("User " + user.getId() + " doesn't have access to use 'public: search' permission");
                }
            }
        }
    }

    private User currentUser() {
        return EnvironmentContext.getCurrent().getUser();
    }

    /**
     * Transforms {@link Recipe} to {@link RecipeDescriptor}.
     * It is stateless so thread safe.
     */
    private static class RecipeToDescriptorFunction implements Function<Recipe, RecipeDescriptor> {

        @Nullable
        @Override
        public RecipeDescriptor apply(Recipe recipe) {
            final RecipeDescriptor descriptor = DtoFactory.getInstance()
                                                          .createDto(RecipeDescriptor.class)
                                                          .withId(recipe.getId())
                                                          .withType(recipe.getType())
                                                          .withScript(recipe.getScript())
                                                          .withCreator(recipe.getCreator())
                                                          .withTags(recipe.getTags());
            final Permissions permissions = recipe.getPermissions();
            if (permissions != null) {
                final List<GroupDescriptor> groups = new ArrayList<>(permissions.getGroups().size());
                for (Group group : permissions.getGroups()) {
                    groups.add(DtoFactory.getInstance()
                                         .createDto(GroupDescriptor.class)
                                         .withName(group.getName())
                                         .withUnit(group.getUnit())
                                         .withAcl(group.getAcl()));
                }
                descriptor.setPermissions(DtoFactory.getInstance()
                                                    .createDto(PermissionsDescriptor.class)
                                                    .withGroups(groups)
                                                    .withUsers(permissions.getUsers()));
            }
            return descriptor;
        }
    }
}
