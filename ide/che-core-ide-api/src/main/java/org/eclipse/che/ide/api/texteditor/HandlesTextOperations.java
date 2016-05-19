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
package org.eclipse.che.ide.api.texteditor;

/**
 * Interface for text editor view that handles text operations.
 *
 * @author "Mickaël Leduque"
 */
public interface HandlesTextOperations {

    /**
     * Returns whether the operation specified by the given operation code can be performed.
     *
     * @param operation
     *         the operation code
     * @return <code>true</code> if the specified operation can be performed
     */
    boolean canDoOperation(int operation);

    /**
     * Performs the operation specified by the operation code on the target. <code>doOperation</code> must only be called if
     * <code>canDoOperation</code> returns <code>true</code>.
     *
     * @param operation
     *         the operation code
     */
    void doOperation(int operation);
}
