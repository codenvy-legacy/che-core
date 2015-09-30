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

import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.commons.annotation.Nullable;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Base interface for operating with nodes in the tree.
 *
 * @author Vladyslav Zhukovskyi
 */
public interface Node {
    /**
     * Represent node name. For files it should be file name with extension, for directories - directory name, etc.
     *
     * @return node name
     */
    @NotNull
    String getName();

    /**
     * Return parent node descriptor.
     *
     * @return parent node descriptor
     */
    @Nullable
    Node getParent();

    /**
     * Set parent node descriptor.
     *
     * @param parent
     *         parent node descriptor
     */
    void setParent(@NotNull Node parent);


    /**
     * Initialize loading of node descendants. If parameter <code>forceUpdate</code> sets with true, than loading will be
     * performed always, otherwise children will be loaded from cache if they exist or empty list. Method returns
     * <code>Promise<List<NodeDescriptor>></code> promise. To obtain loaded descendants, one of the following methods should
     * be called:
     * <p>
     * <code>{@link org.eclipse.che.api.promises.client.Promise#then(org.eclipse.che.api.promises.client.Function)}</code>
     * <code>{@link org.eclipse.che.api.promises.client.Promise#then(org.eclipse.che.api.promises.client.Function,
     * org.eclipse.che.api.promises.client.Function)}</code>
     * <code>{@link org.eclipse.che.api.promises.client.Promise#then(org.eclipse.che.api.promises.client.Operation)}</code>
     * <code>{@link org.eclipse.che.api.promises.client.Promise#then(org.eclipse.che.api.promises.client.Operation,
     * org.eclipse.che.api.promises.client.Function)}</code>
     * <code>{@link org.eclipse.che.api.promises.client.Promise#then(org.eclipse.che.api.promises.client.Operation,
     * org.eclipse.che.api.promises.client.Operation)}</code>
     * <code>{@link org.eclipse.che.api.promises.client.Promise#then(org.eclipse.che.api.promises.client.Thenable)}</code>
     * </p>
     * <p/>
     * In case if loading was failed, one of the following methods should be called to obtain error message:
     * <p>
     * <code>{@link org.eclipse.che.api.promises.client.Promise#catchError(org.eclipse.che.api.promises.client.Function)}</code>
     * <code>{@link org.eclipse.che.api.promises.client.Promise#catchError(org.eclipse.che.api.promises.client.Operation)}</code>
     * </p>
     *
     * @param forceUpdate
     *         true if descendants should be loaded immediately, otherwise descendants would be loaded from cache if
     *         they exist or empty list
     * @return descendants promise
     */
    @NotNull
    Promise<List<Node>> getChildren(boolean forceUpdate);

    /**
     * Set children for the current node.
     *
     * @param children
     *         children
     */
    void setChildren(List<Node> children);

    /**
     * Indicated whether node is a leaf or not.
     *
     * @return true if node is a leaf node, false if otherwise
     */
    boolean isLeaf();

    boolean supportGoInto();
}
