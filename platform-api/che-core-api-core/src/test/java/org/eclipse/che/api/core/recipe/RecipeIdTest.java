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
package org.eclipse.che.api.core.recipe;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author andrew00x
 */
public class RecipeIdTest {
    @Test
    public void testRecipeIdNoCategoryToString() {
        RecipeId id = new RecipeId(RecipeId.Scope.project, "env");
        Assert.assertEquals(id.toString(), "project:/env");
    }

    @Test
    public void testRecipeIdToString() {
        RecipeId id = new RecipeId(RecipeId.Scope.system, "Java/Web", "Tomcat7");
        Assert.assertEquals(id.toString(), "system:/Java/Web/Tomcat7");
    }

    @Test
    public void testRecipeIdNoCategoryParse() {
        String fqn = "project://env";
        RecipeId id = RecipeId.parse(fqn);
        Assert.assertEquals(id.getScope(), RecipeId.Scope.project);
        Assert.assertEquals(id.getCategory(), "");
        Assert.assertEquals(id.getName(), "env");
    }

    @Test
    public void testRecipeIdParse() {
        String fqn = "system:/Java/Web/Tomcat7";
        RecipeId id = RecipeId.parse(fqn);
        Assert.assertEquals(id.getScope(), RecipeId.Scope.system);
        Assert.assertEquals(id.getCategory(), "Java/Web");
        Assert.assertEquals(id.getName(), "Tomcat7");
    }

    @Test(expectedExceptions = {IllegalArgumentException.class})
    public void testRecipeIdInvalidScope() {
        String fqn = "invalid:/env";
        RecipeId id = RecipeId.parse(fqn);
        Assert.assertEquals(id.getScope(), RecipeId.Scope.project);
        Assert.assertEquals(id.getCategory(), "");
        Assert.assertEquals(id.getName(), "env");
    }
}
