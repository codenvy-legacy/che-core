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
package org.eclipse.che.ide.api.tree;

import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.js.Promises;
import org.eclipse.che.ide.api.data.HasAttributes;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base implementation for all nodes that uses in the tree.
 *
 * @author Vlad Zhukovskiy
 */
public abstract class AbstractTreeNode implements Node, HasAttributes {

    private   Node                      parent;
    protected List<Node>                children;
    private   Map<String, List<String>> attributes;

    /** {@inheritDoc} */
    @Override
    public final Map<String, List<String>> getAttributes() {
        if (attributes == null) {
            attributes = new HashMap<>();
        }

        return attributes;
    }

    /** {@inheritDoc} */
    @Override
    public final void setAttributes(Map<String, List<String>> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return;
        }

        this.attributes = attributes;
    }

    /** {@inheritDoc} */
    @Override
    public final Node getParent() {
        return parent;
    }

    /** {@inheritDoc} */
    @Override
    public final void setParent(@NotNull Node parent) {
        this.parent = parent;
    }

    /** {@inheritDoc} */
    @Override
    public final Promise<List<Node>> getChildren(boolean forceUpdate) {
        if (children == null || forceUpdate) {
            return getChildrenImpl().then(new Function<List<Node>, List<Node>>() {
                @Override
                public List<Node> apply(List<Node> children) throws FunctionException {
                    if (children == null) {
                        setChildren(Collections.<Node>emptyList());
                        return Collections.emptyList();
                    }

                    for (Node node : children) {
                        node.setParent(AbstractTreeNode.this);
                    }

                    setChildren(children);

                    return children;
                }
            });
        }

        return Promises.resolve(children);
    }

    /**
     * Request descendants load for current node. This operation is asynchronous and may take some time to perform it.
     * Promise object should not be a {@code null} but it may have empty list if node doesn't have descendants, e.g. for leaf nodes.
     *
     * @return {@code Promise} with node descendants.
     */
    protected abstract Promise<List<Node>> getChildrenImpl();

    /** {@inheritDoc} */
    @Override
    public final void setChildren(List<Node> children) {
        this.children = children;
    }

    /** {@inheritDoc} */
    @Override
    public boolean supportGoInto() {
        return false;
    }
}
