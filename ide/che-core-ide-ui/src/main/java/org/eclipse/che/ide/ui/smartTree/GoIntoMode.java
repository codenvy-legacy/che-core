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
package org.eclipse.che.ide.ui.smartTree;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;

import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.ui.smartTree.event.GoIntoStateEvent;
import org.eclipse.che.ide.ui.smartTree.event.GoIntoStateEvent.GoIntoStateHandler;
import org.eclipse.che.ide.ui.smartTree.event.GoIntoStateEvent.HasGoIntoStateHandlers;

import java.util.List;

import static org.eclipse.che.ide.ui.smartTree.event.GoIntoStateEvent.State.ACTIVATED;
import static org.eclipse.che.ide.ui.smartTree.event.GoIntoStateEvent.State.DEACTIVATED;

/**
 * @author Vlad Zhukovskiy
 */
public class GoIntoMode implements HasGoIntoStateHandlers {
    private Tree tree;

    private boolean activated;

    private Node goIntoNode;

    private HandlerManager handlerManager;

    private List<Node> rootNodes;

    @Override
    public HandlerRegistration addGoIntoHandler(GoIntoStateHandler handler) {
        return ensureHandlers().addHandler(GoIntoStateEvent.getType(), handler);
    }

    public void bindTree(Tree tree) {
        this.tree = tree;
    }

    protected HandlerManager ensureHandlers() {
        if (handlerManager == null) {
            handlerManager = new HandlerManager(this);
        }
        return handlerManager;
    }

    /** {@inheritDoc} */
    @Override
    public void fireEvent(GwtEvent<?> event) {
        if (handlerManager != null) {
            handlerManager.fireEvent(event);
        }
    }

    public boolean goInto(Node node) {
        if (!node.supportGoInto()) {
            return false;
        }

        //save node
        goIntoNode = node;

        //save root nodes
        rootNodes = tree.getRootNodes();

        //reset selection
        tree.getSelectionModel().deselectAll();

        Element rootContainer = tree.getContainer(null);
        rootContainer.setInnerHTML("");
        rootContainer.appendChild(tree.findNode(node).getRootContainer());

        //if go into node is collapsed - then we need to expand it
        if (!tree.findNode(node).isExpanded()) {
            tree.setExpanded(node, true);
        }

        //then select go into node
        tree.getSelectionModel().select(node, false);

        tree.update();

        fireEvent(new GoIntoStateEvent(ACTIVATED, node));

        return activated = true;
    }

    public boolean isActivated() {
        return activated;
    }

    public void reset() {
        //reset selection
        tree.getSelectionModel().deselectAll();

        Element rootContainer = tree.getContainer(null);
        rootContainer.setInnerHTML("");

        //restore root nodes
        for (Node rootNode : rootNodes) {
            NodeDescriptor descriptor = tree.findNode(rootNode);
            rootContainer.appendChild(descriptor.getRootContainer());
        }

        //then re-add our go into node
        tree.getNodeStorage().add(goIntoNode.getParent(), goIntoNode);
        tree.scrollIntoView(goIntoNode);
        tree.getSelectionModel().select(goIntoNode, false);

        tree.update();

        activated = false;

        fireEvent(new GoIntoStateEvent(DEACTIVATED, goIntoNode));
    }

    public Node getLastNode() {
        return goIntoNode;
    }
}
