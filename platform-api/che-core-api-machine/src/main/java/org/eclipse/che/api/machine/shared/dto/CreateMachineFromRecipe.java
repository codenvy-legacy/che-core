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
package org.eclipse.che.api.machine.shared.dto;

import org.eclipse.che.dto.shared.DTO;

/**
 * @author Alexander Garagatyi
 */
@DTO
public interface CreateMachineFromRecipe {
    String getOutputChannel();

    void setOutputChannel(String outputChannel);

    CreateMachineFromRecipe withOutputChannel(String outputChannel);

    String getType();

    void setType(String type);

    CreateMachineFromRecipe withType(String type);

    RecipeDescriptor getRecipeDescriptor();

    void setRecipeDescriptor(RecipeDescriptor recipeDescriptor);

    CreateMachineFromRecipe withRecipeDescriptor(RecipeDescriptor recipeDescriptor);

    String getWorkspaceId();

    void setWorkspaceId(String workspaceId);

    CreateMachineFromRecipe withWorkspaceId(String workspaceId);
}
