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
package org.eclipse.che.ide.api.data;

/**
 * Indicates that specified class can contains data object.
 * For example {@code org.eclipse.che.ide.api.tree.Node} may contains data object.
 *
 * @author Vlad Zhukovskiy
 */
public interface HasDataObject<D> {
    /**
     * Retrieve stored data object.
     * May return {@code null} if no object specified.
     *
     * @return data object.
     */
    D getData();

    /**
     * Store data object.
     * May consume {@code null}.
     *
     * @param data
     *         data object
     */
    void setData(D data);
}
