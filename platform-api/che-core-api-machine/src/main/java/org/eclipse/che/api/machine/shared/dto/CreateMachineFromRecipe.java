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
 * Describes information needed for machine creation from recipe
 *
 * @author Alexander Garagatyi
 */
@DTO
public interface CreateMachineFromRecipe {
    /**
     * Channel of websocket where machine logs should be put
     */
    String getOutputChannel();

    void setOutputChannel(String outputChannel);

    CreateMachineFromRecipe withOutputChannel(String outputChannel);

    /**
     * Type of machine implementation
     */
    String getType();

    void setType(String type);

    CreateMachineFromRecipe withType(String type);

    /**
     * Description of recipe for machine instance
     */
    RecipeDescriptor getRecipeDescriptor();

    void setRecipeDescriptor(RecipeDescriptor recipeDescriptor);

    CreateMachineFromRecipe withRecipeDescriptor(RecipeDescriptor recipeDescriptor);

    /**
     * Id of a workspace machine should be bound to
     */
    String getWorkspaceId();

    void setWorkspaceId(String workspaceId);

    CreateMachineFromRecipe withWorkspaceId(String workspaceId);
}
