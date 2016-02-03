/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.workspace.server.stack.adapters;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.eclipse.che.api.core.model.machine.Recipe;
import org.eclipse.che.api.machine.server.recipe.RecipeImpl;

import java.lang.reflect.Type;

/**
 * Custom implementation of deserialize Recipe objects
 *
 * @author Alexander Andrienko
 */
public class RecipeAdapter implements JsonSerializer<Recipe>, JsonDeserializer<Recipe> {
    @Override
    public Recipe deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {
        return context.deserialize(jsonElement, RecipeImpl.class);
    }

    @Override
    public JsonElement serialize(Recipe recipe, Type type, JsonSerializationContext context) {
        return context.serialize(recipe, RecipeImpl.class);
    }
}
