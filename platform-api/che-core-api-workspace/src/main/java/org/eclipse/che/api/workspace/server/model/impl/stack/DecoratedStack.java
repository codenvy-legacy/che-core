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

import org.eclipse.che.api.workspace.server.stack.image.StackIcon;

/**
 * Defines the stack decorated by stack icon.
 * This base server stack model, which used for storing stack data.
 *
 * @author Alexander Andrienko
 */
public interface DecoratedStack extends Stack {
    /**
     * Returns icon for the Stack
     */
    StackIcon getIcon();

    /**
     * Set icon for the Stack
     */
    void setIcon(StackIcon stackIcon);
}
