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

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;

import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.ui.smartTree.event.ExpandNodeEvent;
import org.eclipse.che.ide.ui.smartTree.event.ExpandNodeEvent.ExpandNodeHandler;
import org.eclipse.che.ide.ui.smartTree.event.GoIntoStateEvent;
import org.eclipse.che.ide.ui.smartTree.event.GoIntoStateEvent.GoIntoStateHandler;
import org.eclipse.che.ide.ui.smartTree.event.GoIntoStateEvent.HasGoIntoStateHandlers;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.che.ide.ui.smartTree.event.GoIntoStateEvent.State.ACTIVATED;
import static org.eclipse.che.ide.ui.smartTree.event.GoIntoStateEvent.State.DEACTIVATED;

/**
 * @author Vlad Zhukovskiy
 */
public class GoIntoMode implements HasGoIntoStateHandlers {
    private Tree tree;

    private boolean activated;

    private Element treeElement;

    private List<Node> selection = new ArrayList<>();

    private Node node;

    private boolean wasExpanded;

    private Element descendants;

    private Handler expandHandler;

    private HandlerRegistration expandHandlerRegistration;

    private   HandlerManager handlerManager;

    @Override
    public HandlerRegistration addGoIntoHandler(GoIntoStateHandler handler) {
        return ensureHandlers().addHandler(GoIntoStateEvent.getType(), handler);
    }

    private class Handler implements ExpandNodeHandler {
        @Override
        public void onExpand(ExpandNodeEvent event) {
            if (expandHandlerRegistration != null) {
                expandHandlerRegistration.removeHandler();
            }

            setNodes();
        }
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

    public GoIntoMode() {
        expandHandler = new Handler();
    }

    public boolean goInto(Node node) {
        if (!isNodeSupportGoMode(node)) {
            return false;
        }

        this.node = node;

        saveState();

        activated = true;

        if (!tree.findNode(node).isExpanded()) {
//            expandHandlerRegistration = tree.addExpandHandler(expandHandler);
            tree.setExpanded(node, true);
//            return false;
        }

        setNodes();

        return activated;
    }

    private void saveState() {
        treeElement = DOM.clone(tree.getContainer(null), true);
        wasExpanded = tree.findNode(node).isExpanded();
    }

    private void setNodes() {
        tree.getSelectionModel().deselectAll();
        descendants = tree.getView().getRootContainer(tree.findNode(node));

        Element root = tree.getContainer(null);
        root.setInnerHTML("");
        root.appendChild(descendants);



        tree.update();
        tree.ensureFocusElement();
        tree.getSelectionModel().select(node, false);

        fireEvent(new GoIntoStateEvent(ACTIVATED, node));
    }

    private boolean isNodeSupportGoMode(Node node) {
        return node.supportGoInto();
    }

    public boolean isActivated() {
        return activated;
    }

    public void reset() {
        tree.getSelectionModel().deselectAll();
        Element root = tree.getContainer(null);
        root.setInnerHTML("");

        NodeList<com.google.gwt.dom.client.Node> childNodes = treeElement.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            root.appendChild(childNodes.getItem(i));
        }

        Element foundNode = Document.get().getElementById(tree.findNode(node).getDomId());

        com.google.gwt.dom.client.Node oldDescendants = foundNode.getChild(1);
        foundNode.replaceChild(descendants, oldDescendants);

        if (!wasExpanded) {
            tree.setExpanded(node, false);
        }

        activated = false;

        tree.redraw(null, false);

        tree.scrollIntoView(node);

        selection.clear();
        treeElement = null;
        descendants = null;
        expandHandlerRegistration = null;

        tree.getSelectionModel().select(getLastNode(), false);

        fireEvent(new GoIntoStateEvent(DEACTIVATED, node));
    }

    public Node getLastNode() {
        return node;
    }
}
