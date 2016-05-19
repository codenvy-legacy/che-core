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

public interface HasReadOnlyProperty {

    /**
     * Sets the editable state.
     *
     * @param isReadOnly
     *         the read only state
     */
    void setReadOnly(final boolean isReadOnly);

    /**
     * Returns whether the shown text can be manipulated.
     *
     * @return the viewer's readOnly state
     */
    boolean isReadOnly();
}
