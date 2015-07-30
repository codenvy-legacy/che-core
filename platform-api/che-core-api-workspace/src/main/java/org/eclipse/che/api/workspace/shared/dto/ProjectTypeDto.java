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

import org.eclipse.che.api.core.model.project.type.ProjectType;
import org.eclipse.che.dto.shared.DTO;

import java.util.List;

/**
 * @author Alexander Garagatyi
 */
@DTO
public interface ProjectTypeDto extends ProjectType {
//    boolean isTypeOf(String typeId);

    @Override
    String getId();

    void setId(String id);

    ProjectTypeDto withId(String id);

    @Override
    String getDisplayName();

    void setDisplayName(String displayName);

    ProjectTypeDto withDisplayName(String displayName);

    @Override
    List<AttributeDto> getAttributes();

    @Override
    AttributeDto getAttribute(String name);

    void setAttributes(List<AttributeDto> attributes);

    ProjectTypeDto withAttributes(List<AttributeDto> attributes);

    @Override
    List<ProjectTypeDto> getParents();

    void setParents(List<ProjectTypeDto> parents);

    ProjectTypeDto withParents(List<ProjectTypeDto> parents);

    @Override
    String getDefaultRecipe();

    void setDefaultRecipe(String defaultRecipe);

    ProjectTypeDto withDefaultRecipe(String defaultRecipe);

    @Override
    boolean getCanBeMixin();

    void setCanBeMixin(Boolean canBeMixin);

    ProjectTypeDto withCanBeMixin(Boolean canBeMixin);

    @Override
    boolean getCanBePrimary();

    void setCanBePrimary(boolean canBePrimary);

    ProjectTypeDto withCanBePrimary(boolean canBePrimary);

    @Override
    boolean isPersisted();

    void setPersisted(boolean persisted);

    ProjectTypeDto withPersisted(boolean persisted);
}
