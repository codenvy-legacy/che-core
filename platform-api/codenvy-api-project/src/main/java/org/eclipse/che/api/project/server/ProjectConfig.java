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
import org.eclipse.che.api.project.shared.Builders;
import org.eclipse.che.api.project.shared.Runners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author gazarenkov
 */
public class ProjectConfig {

    private String description;
    private String typeId;
    private Map<String, AttributeValue> attributes;
    private Runners runners;
    private Builders builders;
    private List <String> mixinTypes;

    public ProjectConfig(String description, String typeId, Map<String, AttributeValue> attributes, Runners runners,
                         Builders builders, List <String> mixinTypes) {

        this.description = description;
        this.typeId = typeId;
        this.attributes = (attributes == null)?new HashMap<String, AttributeValue>():attributes;
        this.builders = (builders == null)?new Builders():builders;
        this.runners = (runners == null)?new Runners():runners;
        this.mixinTypes = (mixinTypes == null)?new ArrayList<String>():mixinTypes;

    }

    public ProjectConfig(String description, String typeId) {

        this(description, typeId, new HashMap<String, AttributeValue>(), new Runners(), new Builders(), new ArrayList<String>());

    }

    public ProjectConfig() {
        this("", BaseProjectType.ID, new HashMap<String, AttributeValue>(), new Runners(), new Builders(), new ArrayList<String>());
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

    public Runners getRunners() {
        return runners;
    }

    public Builders getBuilders() {
        return builders;
    }

    public List<String> getMixinTypes() {
        return mixinTypes;
    }


}
