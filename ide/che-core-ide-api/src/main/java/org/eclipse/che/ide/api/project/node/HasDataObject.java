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
package org.eclipse.che.ide.api.project.node;

import javax.validation.constraints.NotNull;

/**
 * Indicates that specified node can contains data object, e.g. project descriptor or item reference.
 *
 * @author Vlad Zhukovskiy
 */
public interface HasDataObject<D> {
    /**
     * Retrieve stored data object.
     *
     * @return data object
     */
    @NotNull
    D getData();

    /**
     * Store data object.
     *
     * @param data
     *         data object
     */
    void setData(@NotNull D data);
}
