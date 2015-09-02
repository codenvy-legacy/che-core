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
package org.eclipse.che.ide.ui.smartTree;

import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.ui.smartTree.UniqueKeyProvider;

import javax.annotation.Nonnull;

/**
 * Current ID provider is responsible for providing a unique identification for a specified node.
 *
 * @author Vlad Zhukovskiy
 */
public interface NodeUniqueKeyProvider extends UniqueKeyProvider<Node> {
    /** {@inheritDoc} */
    @Nonnull
    public String getKey(@Nonnull Node item);
}
