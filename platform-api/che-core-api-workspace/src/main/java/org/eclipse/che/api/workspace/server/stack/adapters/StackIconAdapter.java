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
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.eclipse.che.api.workspace.server.stack.image.StackIcon;

import java.lang.reflect.Type;

/**
 * Type adapter for {@link StackIcon} objects.
 *
 * @author Alexander Andrienko
 */
public class StackIconAdapter implements JsonSerializer, JsonDeserializer {

    @Override
    public Object deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject stackIconObj = json.getAsJsonObject();
        return new StackIcon(stackIconObj.get("name") == null ? null : stackIconObj.get("name").getAsString(),
                             stackIconObj.get("mediaType") == null ? null : stackIconObj.get("mediaType").getAsString(),
                             null);
    }

    @Override
    public JsonElement serialize(Object src, Type typeOfSrc, JsonSerializationContext context) {
        return context.serialize(src, StackIcon.class);
    }
}
