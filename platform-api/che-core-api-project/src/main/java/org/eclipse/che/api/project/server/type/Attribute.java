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
package org.eclipse.che.api.project.server.type;

import org.eclipse.che.api.project.server.ValueStorageException;

/**
 * @author gazarenkov
 */
public abstract class Attribute {

    protected String projectType;
    protected String name;
    protected String description;
    protected boolean required;
    protected boolean variable;

    protected Attribute(String projectType, String name, String description, boolean required, boolean variable) {
        this.projectType = projectType;
        this.name = name;
        this.description = description;
        this.required = required;
        this.variable = variable;
    }


    public String getId() {
        return projectType+":"+name;
    }


    public String getProjectType() {
        return projectType;
    }


    public String getDescription() {
        return description;
    }


    public boolean isRequired() {
        return required;
    }


    public boolean isVariable() {
        return variable;
    }


    public String getName() {
        return name;
    }

    public abstract AttributeValue getValue() throws ValueStorageException;
}
