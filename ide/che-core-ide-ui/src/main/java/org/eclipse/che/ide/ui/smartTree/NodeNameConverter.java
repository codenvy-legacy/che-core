package org.eclipse.che.ide.ui.smartTree;

import org.eclipse.che.ide.api.project.node.Node;

/**
 * @author Vlad Zhukovskiy
 */
public class NodeNameConverter implements NodeConverter<Node, String> {
    @Override
    public String convert(Node node) {
        return node.getName();
    }
}
