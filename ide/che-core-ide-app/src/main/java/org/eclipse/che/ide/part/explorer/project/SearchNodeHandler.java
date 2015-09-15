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
package org.eclipse.che.ide.part.explorer.project;

import com.google.common.base.Strings;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.rpc.AsyncCallback;

import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.callback.AsyncPromiseHelper;
import org.eclipse.che.ide.api.project.node.HasStorablePath;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.ui.smartTree.Tree;
import org.eclipse.che.ide.ui.smartTree.event.BeforeExpandNodeEvent;
import org.eclipse.che.ide.ui.smartTree.event.BeforeExpandNodeEvent.BeforeExpandNodeHandler;
import org.eclipse.che.ide.ui.smartTree.event.ExpandNodeEvent;
import org.eclipse.che.ide.ui.smartTree.event.ExpandNodeEvent.ExpandNodeHandler;
import org.eclipse.che.ide.ui.smartTree.event.NodeAddedEvent;
import org.eclipse.che.ide.ui.smartTree.event.NodeAddedEvent.NodeAddedEventHandler;
import org.eclipse.che.ide.ui.smartTree.event.PostLoadEvent;
import org.eclipse.che.ide.ui.smartTree.event.PostLoadEvent.PostLoadHandler;

import java.util.List;

/**
 * Search node handler, perform searching specified node in the tree by storable value.
 * For example if user passes "/project/path/to/file" then this node handler will check
 * opened root nodes and if it contains project node with path "/project" then it will
 * search children by path "path/to/file".
 * @author Vlad Zhukovskiy
 */
public class SearchNodeHandler implements ExpandNodeHandler, BeforeExpandNodeHandler, NodeAddedEventHandler, PostLoadHandler {

    private Tree tree;

    private boolean inSearchMode = false;

    private HasStorablePath path;

    private AsyncCallback<Node> callback;

    HandlerRegistration expandHandlerRegistration;
    HandlerRegistration beforeHandlerRegistration;
    HandlerRegistration addNodeAddedHandlerRegistration;
    HandlerRegistration addPostLoadHandlerRegistration;

    public SearchNodeHandler(Tree tree) {
        this.tree = tree;

        expandHandlerRegistration = tree.addExpandHandler(this);
        beforeHandlerRegistration = tree.addBeforeExpandHandler(this);
        addNodeAddedHandlerRegistration = tree.addNodeAddedHandler(this);
        addPostLoadHandlerRegistration = tree.getNodeLoader().addPostLoadHandler(this);
    }

    public Promise<Node> getNodeByPath(final HasStorablePath path) {
        return AsyncPromiseHelper.createFromAsyncRequest(new AsyncPromiseHelper.RequestCall<Node>() {
            @Override
            public void makeCall(AsyncCallback<Node> callback) {
                getNodeByPath(path, callback);
            }
        });
    }

    protected void getNodeByPath(HasStorablePath path, AsyncCallback<Node> callback) {
        if (path == null || Strings.isNullOrEmpty(path.getStorablePath())) {
            callback.onFailure(new IllegalArgumentException("Invalid search path"));
        }

        if (inSearchMode) {
            callback.onFailure(new IllegalStateException("Project explorer has been already activated in search mode"));
        }

        this.callback = callback;
        this.path = path;
        this.inSearchMode = true;

        resetGoIntoMode();

        Node rootNode = getRootNode(path);

        if (rootNode == null) {
            inSearchMode = false;
            return;
        }

        tree.setExpanded(rootNode, true);
    }

    @Override
    public void onBeforeExpand(BeforeExpandNodeEvent event) {
        if (!isInSearchMode()) {
            return;
        }

        Node node = event.getNode();

        if (tree.isExpanded(node)) {
            List<Node> children = tree.getNodeStorage().getChildren(node);

            for (Node child : children) {
                if (!(child instanceof HasStorablePath)) {
                    continue;
                }

                String childPath = ((HasStorablePath)child).getStorablePath();

                if (path.getStorablePath().equals(childPath)) {
                    callback.onSuccess(child);
                    inSearchMode = false;
                    return;
                } else if (path.getStorablePath().startsWith(childPath)) {
                    event.setCancelled(true); //disallow to continue expanding current node
                    tree.setExpanded(child, true);
                    return;
                }
            }

            //node wasn't found, try to make request to load the same children, may be there is a new nodes on server were created
            tree.getNodeLoader().loadChildren(node);
        }
    }

    @Override
    public void onExpand(ExpandNodeEvent event) {
        if (!isInSearchMode()) {
            return;
        }

        List<Node> children = tree.getNodeStorage().getChildren(event.getNode());

        for (Node child : children) {
            if (!(child instanceof HasStorablePath)) {
                continue;
            }

            String childPath = ((HasStorablePath)child).getStorablePath();

            if (path.getStorablePath().equals(childPath)) {
                callback.onSuccess(child);
                inSearchMode = false;
                return;
            } else if (path.getStorablePath().startsWith(childPath)) {
                tree.setExpanded(child, true);
                return;
            }
        }

        //node wasn't found, try to make request to load the same children, may be there is a new nodes on server were created
        tree.getNodeLoader().loadChildren(event.getNode());
    }

    @Override
    public void onNodeAdded(NodeAddedEvent event) {
        if (!isInSearchMode()) {
            return;
        }

        List<Node> addedNodes = event.getNodes();

        for (Node node : addedNodes) {
            if (!(node instanceof HasStorablePath)) {
                continue;
            }

            String childPath = ((HasStorablePath)node).getStorablePath();

            if (path.getStorablePath().equals(childPath)) {
                callback.onSuccess(node);
                inSearchMode = false;
                break;
            } else if (path.getStorablePath().startsWith(childPath)) {
                tree.setExpanded(node, true);
                return;
            }
        }
    }

    @Override
    public void onPostLoad(PostLoadEvent event) {
        if (!isInSearchMode()) {
            return;
        }

        List<Node> receivedNodes = event.getReceivedNodes();

        boolean exist = false;

        for (Node receivedNode : receivedNodes) {
            if (!(receivedNode instanceof HasStorablePath)) {
                continue;
            }

            String childPath = ((HasStorablePath)receivedNode).getStorablePath();

            if (path.getStorablePath().equals(childPath)) {
                exist = true;
                break;
            } else if (path.getStorablePath().startsWith(childPath)) {
                exist = true;
                break;
            }
        }

        if (!exist) {
            callback.onFailure(new IllegalStateException("Node '" + path.getStorablePath() + "' not found"));
            inSearchMode = false;
        }
    }

    public boolean isInSearchMode() {
        return inSearchMode;
    }

    private void resetGoIntoMode() {
        if (tree.getGoIntoMode().isActivated()) {
            tree.getGoIntoMode().reset();
        }
    }

    private Node getRootNode(HasStorablePath path) {
        for (Node root : tree.getRootNodes()) {
            if (!(root instanceof HasStorablePath)) {
                continue;
            }

            String rootPath = ((HasStorablePath)root).getStorablePath();

            if (Strings.isNullOrEmpty(rootPath)) {
                continue;
            }

            if (!path.getStorablePath().startsWith(rootPath)) {
                continue;
            }

            return root;
        }

        return null;
    }
}
