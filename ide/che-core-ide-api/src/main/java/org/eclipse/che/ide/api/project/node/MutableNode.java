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
package org.eclipse.che.ide.api.project.node;

/**
 * Indicates that specified node can be transformed into leaf node.
 *
 * @author Vlad Zhukovskiy
 */
public interface MutableNode {
    /**
     * Set current node status into leaf.
     *
     * @param leaf
     *         true if node should be transformed into leaf
     */
    void setLeaf(boolean leaf);
}
