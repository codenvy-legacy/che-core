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

import org.eclipse.che.api.core.model.machine.Recipe;
import org.eclipse.che.dto.shared.DTO;

/**
 * @author Alexander Garagatyi
 */
@DTO
public interface RecipeDto extends Recipe {

    @Override
    String getType();

    void setType(String type);

    RecipeDto withType(String type);

    @Override
    String getScript();

    void setScript(String script);

    RecipeDto withScript(String script);
}
