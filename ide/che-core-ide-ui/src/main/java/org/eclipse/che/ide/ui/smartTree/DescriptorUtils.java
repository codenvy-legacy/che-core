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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.commons.lang.Pair;
import org.eclipse.che.ide.api.project.node.Node;

import java.util.Collections;
import java.util.List;

/**
 * @author Vlad Zhukovskiy
 */
public class DescriptorUtils {

    public static List<Node> getNewNodes(final List<Node> existed, final List<Node> toCheck) {
        Iterables.removeIf(toCheck, new Predicate<Node>() {
            @Override
            public boolean apply(@Nullable Node newNode) {
                for (Node exist : existed) {
                    if (exist.getName().equals(newNode.getName())) {
                        return true;
                    }
                }

                return false;
            }
        });

        return toCheck;
    }

    public static List<Node> getRemovedNodes(final List<Node> existed, final List<Node> toCheck) {
        Iterables.removeIf(existed, new Predicate<Node>() {
            @Override
            public boolean apply(@Nullable Node exist) {
                for (Node check : toCheck) {
                    if (check.getName().equals(exist.getName())) {
                        return true;
                    }
                }

                return false;
            }
        });

        return existed;
    }

    public static List<Pair<Node, Node>> getMutatedNode(final List<Node> existed, final List<Node> toCheck) {
        List<Pair<Node, Node>> mutatedPair = Lists.newArrayList();

        if (existed == null || toCheck == null) {
            return mutatedPair;
        }

        for (Node exist : existed) {
            for (Node checked : toCheck) {
                if (exist.getName().equals(checked.getName()) && !exist.getClass().equals(checked.getClass())) {
                    mutatedPair.add(Pair.of(exist, checked));
                    break;
                }
            }
        }

        return mutatedPair;
    }

    public static List<Node> getNodesFromDescriptor(NodeDescriptor descriptor) {
        if (descriptor == null) {
            return null;
        }

        if (descriptor.getChildren().isEmpty()) {
            return Collections.emptyList();
        }

        return Lists.newArrayList(Iterables.transform(descriptor.getChildren(), extractNode()));
    }

    public static Function<NodeDescriptor, Node> extractNode() {
        return new Function<NodeDescriptor, Node>() {
            @Nullable
            @Override
            public Node apply(@Nullable NodeDescriptor descriptor) {
                if (descriptor == null) {
                    throw new NullPointerException();
                }

                return descriptor.getNode();
            }
        };
    }
}
