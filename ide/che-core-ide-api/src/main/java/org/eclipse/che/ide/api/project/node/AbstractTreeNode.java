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

import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.js.Promises;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

    private   Node       parent;
    protected List<Node> children;
    private Map<String, List<String>> attributes = new HashMap<>();

    /** {@inheritDoc} */
    @Override
    public Map<String, List<String>> getAttributes() {
        return attributes;
    }

    /** {@inheritDoc} */
    @Override
    public void setAttributes(Map<String, List<String>> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return;
        }

        this.attributes = attributes;
    }

    /** {@inheritDoc} */
    @Nullable
    @Override
    public Node getParent() {
        return parent;
    }

    /** {@inheritDoc} */
    @Override
    public void setParent(@Nonnull Node parent) {
        this.parent = parent;
    }

    /** {@inheritDoc} */
    @Nonnull
    @Override
    public final Promise<List<Node>> getChildren(boolean forceUpdate) {
        if (children == null || children.isEmpty() || forceUpdate) {
            return getChildrenImpl().then(setParentAndSaveState());
        }

        return Promises.resolve(children);
    }

    /** {@inheritDoc} */
    @Nonnull
    protected abstract Promise<List<Node>> getChildrenImpl();

    /** {@inheritDoc} */
    @Override
    public void setChildren(List<Node> children) {
        this.children = children;
    }

    private Function<List<Node>, List<Node>> setParentAndSaveState() {
        return new Function<List<Node>, List<Node>>() {
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
        };
    }

    @Override
    public boolean supportGoInto() {
        return false;
    }
}
