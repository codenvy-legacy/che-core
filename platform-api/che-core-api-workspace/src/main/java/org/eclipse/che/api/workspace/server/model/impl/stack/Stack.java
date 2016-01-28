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
package org.eclipse.che.api.workspace.server.model.impl.stack;

import org.eclipse.che.api.core.model.workspace.WorkspaceConfig;
import org.eclipse.che.api.machine.shared.Permissible;
import org.eclipse.che.commons.annotation.Nullable;

import java.util.List;

/**
 * Defines the interface for managing stack of technologies.
 *
 * <p>Stack is the recipe/image/snapshot which declares workspace
 * environment with certain components (technologies) and provides additional
 * meta information for it.</p>
 *
 * @author Alexander Andrienko
 */
public interface Stack extends Permissible {
    
    /**
     * Returns the unique stack identifier. (e.g. "stack123").
     */
    String getId();
    
    /**
     * Returns the unique stack name. (e.g. "Ruby on Rails").
     */
    String getName();

    /**
     * Returns identifier of user who is the stack creator.
     */
    String getCreator();
    
    /**
     * Returns the stack description, short information about the stack.
     */
    @Nullable
    String getDescription();
    
    /**
     * Returns the scope of the stack. Scope contains two state: general and advanced. If stack defined common technology then method
     * returns "general" scope. If stack is defined detailed concrete technology implementation then method returns "advanced"
     */
    String getScope();
    
    /**
     * Returns list technology tags. Tag links the stack with list Project Templates.
     */
    List<String> getTags();

    /**
     * Returns the workspaceConfig for creation workspace.
     * This workspaceConfig can be used for store machine source, list predefined commands, projects etc.
     */
    WorkspaceConfig getWorkspaceConfig();

    /**
     *  Returns the source for the stack.
     *  (e.g. "type:recipe, origin: recipeLink", "type:script, origin:recipeScript")
     */
    StackSource getSource();
    
    /**
     * Returns the list of the components that stack consist of.
     */
    List<? extends  StackComponent> getComponents();
}
