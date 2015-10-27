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
import com.google.gwtmockito.GwtMockitoTestRunner;

import org.eclipse.che.api.machine.gwt.client.RecipeServiceClient;
import org.eclipse.che.api.machine.shared.dto.recipe.RecipeDescriptor;
import org.eclipse.che.api.project.gwt.client.ProjectTypeServiceClient;
import org.eclipse.che.api.project.shared.dto.ProjectTypeDefinition;
import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.Promise;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** @author Artem Zatsarynnyy */
@RunWith(GwtMockitoTestRunner.class)
public class RecipesPagePresenterTest {

//    private static final String PROJECT_TYPE_ID = "project type ID";
//    private static final String RECIPE_URL      = "recipe URL";
//
//    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
//    private ImportProject dataObject;
//
    @Mock
    private RecipesPageView          view;
//    @Mock
//    private RecipeServiceClient      recipeServiceClient;
//    @Mock
//    private ProjectTypeServiceClient projectTypeServiceClient;
//
//    @Mock
//    private Promise<ProjectTypeDefinition>  projectTypePromise;
//    @Mock
//    private Promise<List<RecipeDescriptor>> recipesPromise;
//
//    @Captor
//    private ArgumentCaptor<Function<ProjectTypeDefinition, Promise<List<RecipeDescriptor>>>> functionCaptor;
//    @Captor
//    private ArgumentCaptor<Operation<List<RecipeDescriptor>>>                                recipesCaptor;
//
    @InjectMocks
    private RecipesPagePresenter page;
//
//    @Before
//    public void setUp() {
//        page.init(dataObject);
//    }
//
    @Test
    public void shouldSetDelegate() {
        verify(view).setDelegate(eq(page));
    }
//
//    @Test
//    public void shouldChangeDataObjectOnRecipeSelected() {
//        page.onRecipeSelected(RECIPE_URL);
//
//        verify(dataObject.getProject()).setRecipe(eq(RECIPE_URL));
//    }
//
//    @Test
//    public void testGo() throws Exception {
//        AcceptsOneWidget container = mock(AcceptsOneWidget.class);
//        ProjectTypeDefinition projectType = mock(ProjectTypeDefinition.class);
//        List<RecipeDescriptor> recipesList = new ArrayList<>();
//
//        when(dataObject.getProject().getType()).thenReturn(PROJECT_TYPE_ID);
//        when(projectTypeServiceClient.getProjectType(anyString())).thenReturn(projectTypePromise);
//        when(recipeServiceClient.getAllRecipes()).thenReturn(recipesPromise);
//        when(projectTypePromise.thenPromise(any(Function.class))).thenReturn(recipesPromise);
//
//        page.go(container);
//
//        verify(container).setWidget(eq(view));
//        // recipes list should be clear immediately after showing the view
//        verify(view).clearRecipes();
//        verify(projectTypeServiceClient).getProjectType(eq(PROJECT_TYPE_ID));
//        verify(recipeServiceClient).getAllRecipes();
//
//        verify(projectTypePromise).thenPromise(functionCaptor.capture());
//        functionCaptor.getValue().apply(projectType);
//
//        verify(recipesPromise).then(recipesCaptor.capture());
//        recipesCaptor.getValue().apply(recipesList);
//
//        verify(view).setRecipes(anyListOf(String.class));
//    }
}
