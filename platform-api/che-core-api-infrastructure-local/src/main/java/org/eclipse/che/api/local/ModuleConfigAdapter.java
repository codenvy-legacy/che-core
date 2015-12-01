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
package org.eclipse.che.api.local;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.eclipse.che.api.core.model.workspace.ModuleConfig;
import org.eclipse.che.api.workspace.server.model.impl.ModuleConfigImpl;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Dmitry Shnurenko
 */
public class ModuleConfigAdapter implements JsonDeserializer<ModuleConfig>, JsonSerializer<ModuleConfig> {

    @Override
    public ModuleConfig deserialize(JsonElement element, Type type, JsonDeserializationContext context) throws JsonParseException {
        return deserializeModule(element);
    }

    private ModuleConfig deserializeModule(JsonElement element) {
        ModuleConfigImpl config = new ModuleConfigImpl();
        JsonObject configJson = element.getAsJsonObject();

        config.setName(configJson.get("name") == null ? null : configJson.get("name").getAsString());
        config.setPath(configJson.get("path") == null ? null : configJson.get("path").getAsString());
        config.setType(configJson.get("type") == null ? null : configJson.get("type").getAsString());
        config.setDescription(configJson.get("description") == null ? null : configJson.get("description").getAsString());
        config.setMixins(deserializeMixins(configJson));
        config.setModules(deserializeModules(configJson));
        config.setAttributes(deserializeAttributes(configJson));

        return config;
    }

    private List<ModuleConfig> deserializeModules(JsonObject configJson) {
        JsonArray modulesFromJson = configJson.get("modules") == null ? null : configJson.get("modules").getAsJsonArray();

        if (modulesFromJson == null) {
            return Collections.emptyList();
        }

        List<ModuleConfig> modules = new ArrayList<>(modulesFromJson.size());
        for (JsonElement jsonElement : modulesFromJson) {
            modules.add(deserializeModule(jsonElement));
        }

        return modules;
    }

    private List<String> deserializeMixins(JsonObject configJson) {
        JsonArray mixinsFromJson = configJson.get("mixins") == null ? null : configJson.get("mixins").getAsJsonArray();

        if (mixinsFromJson == null) {
            return Collections.emptyList();
        }

        List<String> mixins = new ArrayList<>(mixinsFromJson.size());
        for (JsonElement jsonElement : mixinsFromJson) {
            mixins.add(jsonElement.getAsString());
        }

        return mixins;
    }

    private Map<String, List<String>> deserializeAttributes(JsonObject configJson) {
        JsonObject attributesFromJson = configJson.get("attributes") == null ? null : configJson.get("attributes").getAsJsonObject();

        if (attributesFromJson == null) {
            return Collections.emptyMap();
        }
        Map<String, List<String>> result = new HashMap<>();

        //traversing over map
        for (Map.Entry<String, JsonElement> entry : attributesFromJson.entrySet()) {
            JsonArray jsonValue = entry.getValue().getAsJsonArray();
            if (jsonValue.isJsonNull()) {
                continue;
            }
            List<String> stringValues = new ArrayList<>();
            for (JsonElement jsonElement : jsonValue) {
                if (!jsonElement.isJsonNull()) {
                    stringValues.add(jsonElement.getAsString());
                }
            }
            result.put(entry.getKey(), stringValues);
        }
        return result;
    }

    @Override
    public JsonElement serialize(ModuleConfig moduleConfig, Type type, JsonSerializationContext context) {
        return serializeModule(moduleConfig, context);
    }

    private JsonElement serializeModule(ModuleConfig moduleConfig, JsonSerializationContext context) {
        JsonObject object = new JsonObject();

        object.addProperty("name", moduleConfig.getName());
        object.addProperty("path", moduleConfig.getPath());
        object.addProperty("type", moduleConfig.getType());
        object.addProperty("description", moduleConfig.getDescription());
        object.add("mixins", context.serialize(moduleConfig.getMixins()));
        object.add("attributes", context.serialize(moduleConfig.getAttributes()));
        object.add("modules", serializeModules(moduleConfig, context));

        return object;
    }

    private JsonElement serializeModules(ModuleConfig moduleConfig, JsonSerializationContext context) {
        JsonArray modules = new JsonArray();

        for (ModuleConfig config : moduleConfig.getModules()) {
            modules.add(serializeModule(config, context));
        }

        return modules;
    }
}
