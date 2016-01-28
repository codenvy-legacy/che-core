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
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.apache.commons.codec.binary.Base64;

import java.lang.reflect.Type;

/**
 * Custom implementation of deserialize byte[]
 *
 * @author Alexander Andrienko
 */
public class ByteArrayToBase64Serializer implements JsonSerializer<byte[]>, JsonDeserializer<byte[]> {

    public byte[] deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        return Base64.decodeBase64(json.getAsString());
    }

    public JsonElement serialize(byte[] source, Type type, JsonSerializationContext context) {
        return new JsonPrimitive(Base64.encodeBase64String(source));
    }
}
