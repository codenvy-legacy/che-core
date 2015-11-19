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
package org.eclipse.che.api.project.server;

import org.eclipse.che.api.project.server.type.AttributeValue;
import org.eclipse.che.api.project.server.type.BaseProjectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author gazarenkov
 */
public class ProjectConfig {

    private String                      description;
    private String                      typeId;
    private Map<String, AttributeValue> attributes;
    private String                      recipe;
    private List<String>                mixins;

    public ProjectConfig(String description, String typeId, Map<String, AttributeValue> attributes, String recipe,
                         List<String> mixins) {
        this.description = description;
        this.typeId = typeId;
        this.attributes = (attributes == null) ? new HashMap<>() : attributes;
        this.recipe = recipe;
        this.mixins = (mixins == null) ? new ArrayList<>() : mixins;
    }

    public ProjectConfig(String description, String typeId) {
        this(description, typeId, new HashMap<>(), null, new ArrayList<>());
    }

    public ProjectConfig() {
        this("", BaseProjectType.ID, new HashMap<>(), null, new ArrayList<>());
    }

    public String getDescription() {
        return description;
    }

    public String getTypeId() {
        return typeId;
    }

    public Map<String, AttributeValue> getAttributes() {
        return attributes;
    }

    public String getRecipe() {
        return recipe;
    }

    public List<String> getMixins() {
        return mixins;
    }
}
