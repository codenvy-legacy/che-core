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

import com.jayway.restassured.response.Response;

import org.eclipse.che.api.core.rest.ApiExceptionMapper;
import org.eclipse.che.api.core.rest.shared.dto.ServiceError;
import org.eclipse.che.api.machine.server.dao.RecipeDao;
import org.eclipse.che.api.machine.shared.Recipe;
import org.eclipse.che.api.machine.shared.dto.GroupDescriptor;
import org.eclipse.che.api.machine.shared.dto.NewRecipe;
import org.eclipse.che.api.machine.shared.dto.PermissionsDescriptor;
import org.eclipse.che.api.machine.shared.dto.RecipeDescriptor;
import org.eclipse.che.api.workspace.server.dao.MemberDao;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.user.UserImpl;
import org.eclipse.che.dto.server.DtoFactory;
import org.everrest.assured.EverrestJetty;
import org.everrest.core.Filter;
import org.everrest.core.GenericContainerRequest;
import org.everrest.core.RequestFilter;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.LinkedList;

import static com.jayway.restassured.RestAssured.given;
import static java.util.Arrays.asList;
import static org.everrest.assured.JettyHttpServer.ADMIN_USER_NAME;
import static org.everrest.assured.JettyHttpServer.ADMIN_USER_PASSWORD;
import static org.everrest.assured.JettyHttpServer.SECURE_PATH;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author Eugene Voevodin
 */
@Listeners(value = {EverrestJetty.class, MockitoTestNGListener.class})
public class RecipeServiceTest {

    static final String             USER_ID = "user123";
    static final EnvironmentFilter  FILTER  = new EnvironmentFilter();
    static final ApiExceptionMapper MAPPER  = new ApiExceptionMapper();
    static final LinkedList<String> ROLES   = new LinkedList<>(asList("user"));

    @Mock
    RecipeDao     recipeDao;
    @Mock
    MemberDao     memberDao;
    @InjectMocks
    RecipeService service;

    @Filter
    public static class EnvironmentFilter implements RequestFilter {

        public void doFilter(GenericContainerRequest request) {
            EnvironmentContext.getCurrent().setUser(new UserImpl("user", USER_ID, "token", ROLES, false));
        }
    }

    @Test
    public void shouldThrowForbiddenExceptionOnCreateRecipeWithNullBody() {
        final Response response = given().auth()
                                         .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
                                         .contentType("application/json")
                                         .when()
                                         .post(SECURE_PATH + "/recipe");

        assertEquals(response.getStatusCode(), 403);
        assertEquals(unwrapDto(response, ServiceError.class).getMessage(), "Recipe required");
    }

    @Test
    public void shouldThrowForbiddenExceptionOnCreateRecipeWithNewRecipeWhichDoesNotHaveType() {
        final NewRecipe newRecipe = newDto(NewRecipe.class).withScript("FROM ubuntu\n");

        final Response response = given().auth()
                                         .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
                                         .contentType("application/json")
                                         .body(newRecipe)
                                         .when()
                                         .post(SECURE_PATH + "/recipe");

        assertEquals(response.getStatusCode(), 403);
        assertEquals(unwrapDto(response, ServiceError.class).getMessage(), "Recipe type required");
    }

    @Test
    public void shouldThrowForbiddenExceptionOnCreateRecipeWithNewRecipeWhichDoesNotHaveScript() {
        final NewRecipe newRecipe = newDto(NewRecipe.class).withType("docker");

        final Response response = given().auth()
                                         .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
                                         .contentType("application/json")
                                         .body(newRecipe)
                                         .when()
                                         .post(SECURE_PATH + "/recipe");

        assertEquals(response.getStatusCode(), 403);
        assertEquals(unwrapDto(response, ServiceError.class).getMessage(), "Recipe script required");
    }

    @Test
    public void shouldCreateNewRecipe() {
        final GroupDescriptor group = newDto(GroupDescriptor.class).withName("public").withAcl(asList("read"));
        final NewRecipe newRecipe = newDto(NewRecipe.class).withType("docker")
                                                           .withScript("FROM ubuntu\n")
                                                           .withTags(asList("java", "mongo"))
                                                           .withPermissions(newDto(PermissionsDescriptor.class).withGroups(asList(group)));

        final Response response = given().auth()
                                         .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
                                         .contentType("application/json")
                                         .body(newRecipe)
                                         .when()
                                         .post(SECURE_PATH + "/recipe");

        assertEquals(response.getStatusCode(), 201);
        verify(recipeDao).create(any(Recipe.class));
        final RecipeDescriptor descriptor = unwrapDto(response, RecipeDescriptor.class);
        assertNotNull(descriptor.getId());
        assertEquals(descriptor.getCreator(), USER_ID);
        assertEquals(descriptor.getScript(), newRecipe.getScript());
        assertEquals(descriptor.getTags(), newRecipe.getTags());
        assertEquals(descriptor.getPermissions(), newRecipe.getPermissions());
    }

    @Test
    public void shouldNotBeAbleToCreateNewRecipeWithPublicSearchPermissionForUser() {
        final GroupDescriptor group = newDto(GroupDescriptor.class).withName("public").withAcl(asList("read", "search"));
        final NewRecipe newRecipe = newDto(NewRecipe.class).withType("docker")
                                                           .withScript("FROM ubuntu\n")
                                                           .withPermissions(newDto(PermissionsDescriptor.class).withGroups(asList(group)));

        final Response response = given().auth()
                                         .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
                                         .contentType("application/json")
                                         .body(newRecipe)
                                         .when()
                                         .post(SECURE_PATH + "/recipe");

        assertEquals(response.getStatusCode(), 403);
        final ServiceError error = unwrapDto(response, ServiceError.class);
        assertEquals(error.getMessage(), "User " + USER_ID + " doesn't have access to use 'public: search' permission");
    }

    @Test
    public void shouldBeAbleToCreateNewRecipeWithPublicSearchPermissionForSystemAdmin() {
        ROLES.add("system/admin");

        final GroupDescriptor group = newDto(GroupDescriptor.class).withName("public").withAcl(asList("read", "search"));
        final NewRecipe newRecipe = newDto(NewRecipe.class).withType("docker")
                                                           .withScript("FROM ubuntu\n")
                                                           .withPermissions(newDto(PermissionsDescriptor.class).withGroups(asList(group)));

        final Response response = given().auth()
                                         .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
                                         .contentType("application/json")
                                         .body(newRecipe)
                                         .when()
                                         .post(SECURE_PATH + "/recipe");

        assertEquals(response.getStatusCode(), 201);
        final RecipeDescriptor descriptor = unwrapDto(response, RecipeDescriptor.class);
        assertEquals(descriptor.getPermissions(), newRecipe.getPermissions());

        ROLES.remove("system/admin");
    }

//    @Test
//    public void shouldBeAbleToGetRecipeScript() {
//        final Recipe recipe = new RecipeImpl().withCreator(USER_ID)
//                                              .withId("recipe123")
//                                              .withScript("FROM ubuntu\n");
//        when(recipeDao.getById(recipe.getId())).thenReturn(recipe);
//
//        final Response response = given().auth()
//                                         .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
//                                         .when()
//                                         .get(SECURE_PATH + "/recipe/" + recipe.getId());
//
//        assertEquals(response.getStatusCode(), 200);
//        assertEquals(response.getBody().print(), recipe.getScript());
//    }
//
//    @Test
//    public void shouldBeAbleToGetRecipeById() {
//        final Recipe recipe = new RecipeImpl().withCreator(USER_ID)
//                                              .withId("recipe123")
//                                              .withType("docker")
//                                              .withScript("FROM ubuntu\n");
//        when(recipeDao.getById(recipe.getId())).thenReturn(recipe);
//
//        final Response response = given().auth()
//                                         .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
//                                         .when()
//                                         .get(SECURE_PATH + "/recipe/" + recipe.getId() + "/json");
//
//        assertEquals(response.getStatusCode(), 200);
//        final RecipeDescriptor descriptor = unwrapDto(response, RecipeDescriptor.class);
//        assertEquals(descriptor.getType(), recipe.getType());
//        assertEquals(descriptor.getScript(), recipe.getScript());
//        assertEquals(descriptor.getId(), recipe.getId());
//        assertEquals(descriptor.getTags(), recipe.getTags());
//        assertEquals(descriptor.getPermissions(), recipe.getPermissions());
//    }

    private static <T> T newDto(Class<T> clazz) {
        return DtoFactory.getInstance().createDto(clazz);
    }

    private static <T> T unwrapDto(Response response, Class<T> dtoClass) {
        return DtoFactory.getInstance().createDtoFromJson(response.getBody().print(), dtoClass);
    }
}
