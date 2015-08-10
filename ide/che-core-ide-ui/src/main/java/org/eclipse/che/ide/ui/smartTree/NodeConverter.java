package org.eclipse.che.ide.ui.smartTree;

import org.eclipse.che.ide.api.project.node.Node;

/**
 * @author Vlad Zhukovskiy
 */
public interface NodeConverter<N extends Node, D> {
    D convert(N node);
}
