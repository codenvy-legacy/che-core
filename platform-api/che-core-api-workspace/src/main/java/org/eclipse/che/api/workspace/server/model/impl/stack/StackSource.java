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

/**
 * Defines the interface that describes the stack source.
 *
 * @author Alexander Andrienko
 */
public interface StackSource {

    /**
     * Returns type for the StackSource. There are three values for the StackSource: "recipe", "image", "location"
     */
    String getType();

    /**
     * Returns origin data for the Stack Source.
     * If the StackSource type is "recipe" then returns text plain recipe content.
     * If the StackSource type is "image" then returns image tag
     * If the StackSource type is "location" then returns link to the recipe
     */
    String getOrigin();
}
