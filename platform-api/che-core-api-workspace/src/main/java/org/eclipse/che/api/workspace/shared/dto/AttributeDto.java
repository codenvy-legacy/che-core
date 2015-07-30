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
package org.eclipse.che.api.workspace.shared.dto;

import org.eclipse.che.api.core.model.project.type.Attribute;
import org.eclipse.che.dto.shared.DTO;

/**
 * @author Alexander Garagatyi
 */
@DTO
public interface AttributeDto extends Attribute {
    @Override
    String getId();

    void setId(String id);

    AttributeDto withId(String id);

    @Override
    String getProjectType();

    void setProjectType(String projectType);

    AttributeDto withProjectType(String projectType);

    @Override
    String getDescription();

    void setDescription(String description);

    AttributeDto withDescription(String description);

    @Override
    boolean isRequired();

    void setRequired(boolean required);

    AttributeDto withRequired(boolean required);

    @Override
    boolean isVariable();

    void setVariable(boolean variable);

    AttributeDto withVariable(boolean variable);

    @Override
    String getName();

    void setName(String name);

    AttributeDto withName(String name);

    @Override
    ValueDto getValue();

    void setValue(ValueDto value);

    AttributeDto withValue(ValueDto value);
}
