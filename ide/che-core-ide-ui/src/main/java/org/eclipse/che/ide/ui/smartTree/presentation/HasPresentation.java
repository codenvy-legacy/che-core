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
package org.eclipse.che.ide.ui.smartTree.presentation;

import javax.annotation.Nonnull;

/**
 * Indicates that specified node can has presentation to allow customize various
 * parameters, e.g. node icon, presentable text, info text, etc.
 *
 * @author Vlad Zhukovskiy
 */
public interface HasPresentation {
    /**
     * Method called during node rendering.
     *
     * @param presentation
     *         node presentation
     */
    void updatePresentation(@Nonnull NodePresentation presentation);

    NodePresentation getPresentation();
}
