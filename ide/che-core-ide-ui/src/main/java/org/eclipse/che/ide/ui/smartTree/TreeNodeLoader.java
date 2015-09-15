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

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.SimpleEventBus;

import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.ide.api.project.node.HasDataObject;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.api.project.node.interceptor.NodeInterceptor;
import org.eclipse.che.ide.ui.smartTree.event.BeforeLoadEvent;
import org.eclipse.che.ide.ui.smartTree.event.CancellableEvent;
import org.eclipse.che.ide.ui.smartTree.event.LoadEvent;
import org.eclipse.che.ide.ui.smartTree.event.LoadExceptionEvent;
import org.eclipse.che.ide.ui.smartTree.event.LoaderHandler;
import org.eclipse.che.ide.ui.smartTree.event.PostLoadEvent;
import org.eclipse.che.ide.util.loging.Log;

import javax.validation.constraints.NotNull;
import org.eclipse.che.commons.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class that perform loading node children. May transform nodes if ones passed set of node interceptors.
 *
 * @author Vlad Zhukovskiy
 * @see org.eclipse.che.ide.api.project.node.interceptor.NodeInterceptor
 */
public class TreeNodeLoader implements LoaderHandler.HasLoaderHandlers {
    /**
     * Temporary storage for current requested nodes. When children have been loaded requested node removes from temporary set.
     */
    Map<Node, Boolean> childRequested = new HashMap<>();

    /**
     * Last processed node. Maybe used in general purpose.
     */
    private Node lastRequest;

    /**
     * Set of node interceptors. They need to modify children nodes before they will be set into parent node.
     *
     * @see org.eclipse.che.ide.api.project.node.interceptor.NodeInterceptor
     */
    private List<NodeInterceptor> nodeInterceptors;

    /**
     * When caching is on nodes will be loaded from cache if they exist otherwise nodes will be loaded every time forcibly.
     */
    private boolean useCaching = false;

    private Tree tree;

    private GroupingHandlerRegistration handlerRegistration;

    private CTreeNodeLoaderHandler cTreeNodeLoaderHandler = new CTreeNodeLoaderHandler();

    /**
     * Event handler for the loading events.
     */
    private class CTreeNodeLoaderHandler implements LoadEvent.LoadHandler,
                                                    LoadExceptionEvent.LoadExceptionHandler,
                                                    BeforeLoadEvent.BeforeLoadHandler {
        @Override
        public void onLoad(final LoadEvent event) {
            Node parent = event.getRequestedNode();
            tree.getView().onLoadChange(tree.findNode(parent), false);

            //remove joint element if non-leaf node doesn't have any children
            if (!parent.isLeaf() && event.getReceivedNodes().isEmpty()) {
                tree.getView().onJointChange(tree.findNode(parent), Tree.Joint.NONE);
            }

            NodeDescriptor requested = tree.findNode(parent);

            if (requested == null) {
                //smth happened, that requested node isn't registered in storage
                Log.info(this.getClass(), "Requested node not found.");
            }

            //search node which has been removed from server to remove them from the tree
            List<NodeDescriptor> removedNodes = findRemovedNodes(requested, event.getReceivedNodes());

            //now search new nodes to add then into the tree
            List<Node> newNodes = findNewNodes(requested, event.getReceivedNodes());

            if (removedNodes.isEmpty() && newNodes.equals(event.getReceivedNodes())) {
                tree.getNodeStorage().replaceChildren(parent, newNodes);
            } else {
                for (NodeDescriptor removed : removedNodes) {
                    if (!tree.getNodeStorage().remove(removed.getNode())) {
                        Log.info(this.getClass(), "Failed to remove node: " + removed.getNode().getName());
                    }
                }

                for (Node newNode : newNodes) {
                    tree.getNodeStorage().add(parent, newNode);
                }
            }

            if (event.isReloadExpandedChild()) {
                Iterable<Node> filter = Iterables.filter(tree.getNodeStorage().getChildren(parent), new Predicate<Node>() {
                    @Override
                    public boolean apply(@Nullable Node input) {
                        return tree.hasChildren(input) && tree.isExpanded(input);
                    }
                });

                for (Node node : filter) {
                    loadChildren(node);
                }
            }

            fireEvent(new PostLoadEvent(event.getRequestedNode(), event.getReceivedNodes()));
        }

        @Override
        public void onLoadException(LoadExceptionEvent event) {
            //stub
        }

        @Override
        public void onBeforeLoad(BeforeLoadEvent event) {
            //stub
        }
    }

    private boolean isNodeHasSameDataObject(Node node, HasDataObject<?> dataNode) {
        return node != null
               && dataNode != null
               && node instanceof HasDataObject<?>
               && ((HasDataObject)node).getData().equals(dataNode.getData());
    }

    private List<Node> findNewNodes(NodeDescriptor parent, final List<Node> loadedChildren) {
        final List<NodeDescriptor> existed = parent.getChildren();

        if (existed == null || existed.isEmpty()) {
            return loadedChildren;
        }

        Iterable<Node> newItems = Iterables.filter(loadedChildren, new Predicate<Node>() {
            @Override
            public boolean apply(Node loadedChild) {
                for (NodeDescriptor nodeDescriptor : existed) {
                    if (nodeDescriptor.getNode().getName().equals(loadedChild.getName())
                        && nodeDescriptor.getNode().getClass().equals(loadedChild.getClass())) {
                        return false;
                    }
                }
                return true;
            }
        });

        return Lists.newArrayList(newItems);
    }

    private List<NodeDescriptor> findRemovedNodes(NodeDescriptor parent, final List<Node> loadedChildren) {
        List<NodeDescriptor> existed = parent.getChildren();

        if (existed == null || existed.isEmpty()) {
            return Collections.emptyList();
        }

        Iterable<NodeDescriptor> removedItems = Iterables.filter(existed, new Predicate<NodeDescriptor>() {
            @Override
            public boolean apply(NodeDescriptor existedChild) {
                boolean found = false;
                for (Node loadedChild : loadedChildren) {
                    if (loadedChild.getName().equals(existedChild.getNode().getName())
                        && loadedChild.getClass().equals(existedChild.getNode().getClass())) {
                        found = true;
                    }
                }
                return !found;
            }
        });

        return Lists.newArrayList(removedItems);
    }

    private SimpleEventBus eventBus;

    /**
     * Creates  a new tree node value provider instance.
     *
     * @param nodeInterceptors
     *         set of {@link org.eclipse.che.ide.api.project.node.interceptor.NodeInterceptor}
     */
    public TreeNodeLoader(@Nullable Set<NodeInterceptor> nodeInterceptors) {
        this.nodeInterceptors = new ArrayList<>();
        if (nodeInterceptors != null) {
            this.nodeInterceptors.addAll(nodeInterceptors);
            Collections.sort(this.nodeInterceptors, new Comparator<NodeInterceptor>() {
                @Override
                public int compare(NodeInterceptor o1, NodeInterceptor o2) {
                    return o1.weightOrder().compareTo(o2.weightOrder());
                }
            });
        }
    }

    /**
     * Checks whether node has children or not. This method may allow tree to determine
     * whether to show expand control near non-leaf node.
     *
     * @param parent
     *         node
     * @return true if node has children, otherwise false
     */
    public boolean mayHaveChildren(@NotNull Node parent) {
        return !parent.isLeaf();
    }

    /**
     * Initiates a load request for the parent's children.
     *
     * @param parent
     *         parent node
     * @return true if the load was requested, otherwise false
     */
    public boolean loadChildren(@NotNull Node parent) {
        return loadChildren(parent, false);
    }

    public boolean loadChildren(Node parent, boolean reloadExpandedChild) {
        if (childRequested.containsKey(parent)) {
            return false;
        }

        childRequested.put(parent, reloadExpandedChild);
        return _load(parent);
    }

    /**
     * Called when children haven't been successfully loaded.
     * Also fire {@link org.eclipse.che.ide.ui.smartTree.event.LoadExceptionEvent} event.
     *
     * @param parent
     *         parent node, children which haven't been loaded
     * @return instance of {@link org.eclipse.che.api.promises.client.Operation} which contains promise with error
     */
    @NotNull
    private Operation<PromiseError> onLoadFailure(@NotNull final Node parent) {
        return new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError t) throws OperationException {
                childRequested.remove(parent);
                fireEvent(new LoadExceptionEvent(parent, t.getCause()));
            }
        };
    }

    /**
     * Called when children have been successfully received.
     * Also fire {@link org.eclipse.che.ide.ui.smartTree.event.LoadEvent} event.
     *
     * @param parent
     *         parent node, children which have been loaded
     */
    private void onLoadSuccess(@NotNull final Node parent, List<Node> children) {
        boolean reloadExpandedChild = childRequested.remove(parent);
        fireEvent(new LoadEvent(parent, children, reloadExpandedChild));
    }

    /**
     * Initiates a load request for the parent's children.
     * Also fire {@link org.eclipse.che.ide.ui.smartTree.event.BeforeLoadEvent} event.
     *
     * @param parent
     *         parent node
     * @return true if load was requested, otherwise false
     */
    private boolean _load(@NotNull final Node parent) {
        if (fireEvent(new BeforeLoadEvent(parent))) {
            lastRequest = parent;

            parent.getChildren(!useCaching)
                  .then(interceptChildren(parent))
                  .catchError(onLoadFailure(parent));
            return true;
        }

        return false;
    }

    /**
     * Fires the given event.
     *
     * @param event
     *         event to fire
     * @return true if the specified event wasn't cancelled, otherwise false
     */
    private boolean fireEvent(@NotNull GwtEvent<?> event) {
        if (eventBus != null) {
            eventBus.fireEvent(event);
        }
        if (event instanceof CancellableEvent) {
            return !((CancellableEvent)event).isCancelled();
        }
        return true;
    }

    /**
     * Returns the last processed node.
     *
     * @return last processed node
     */
    @Nullable
    public Node getLastRequest() {
        return lastRequest;
    }

    /**
     * Perform iteration on every node interceptor, passing to ones the list of children
     * to filter them before inserting into parent node.
     *
     * @param parent
     *         parent node
     * @return instance of {@link org.eclipse.che.api.promises.client.Function} with promise that contains list of intercepted children
     */
    @NotNull
    private Operation<List<Node>> interceptChildren(@NotNull final Node parent) {
        return new Operation<List<Node>>() {
            @Override
            public void apply(List<Node> children) throws OperationException {
                if (nodeInterceptors.isEmpty()) {
                    onLoadSuccess(parent, children);
                }

                iterate(new LinkedList<>(nodeInterceptors), parent, children);
            }
        };
    }

    private void iterate(final LinkedList<NodeInterceptor> deque, final Node parent, final List<Node> children) {
        if (deque.isEmpty()) {
            onLoadSuccess(parent, children);
            return;
        }


        NodeInterceptor interceptor = deque.poll();

        interceptor.intercept(parent, children).then(new Operation<List<Node>>() {
            @Override
            public void apply(List<Node> arg) throws OperationException {
                iterate(deque, parent, arg);
            }
        });
    }

    /** {@inheritDoc} */
    @NotNull
    @Override
    public HandlerRegistration addBeforeLoadHandler(@NotNull BeforeLoadEvent.BeforeLoadHandler handler) {
        return addHandler(BeforeLoadEvent.getType(), handler);
    }

    /** {@inheritDoc} */
    @NotNull
    @Override
    public HandlerRegistration addLoadExceptionHandler(@NotNull LoadExceptionEvent.LoadExceptionHandler handler) {
        return addHandler(LoadExceptionEvent.getType(), handler);
    }

    @Override
    public HandlerRegistration addPostLoadHandler(PostLoadEvent.PostLoadHandler handler) {
        return addHandler(PostLoadEvent.getType(), handler);
    }

    /** {@inheritDoc} */
    @Override
    public HandlerRegistration addLoaderHandler(LoaderHandler handler) {
        GroupingHandlerRegistration group = new GroupingHandlerRegistration();
        group.add(addHandler(BeforeLoadEvent.getType(), handler));
        group.add(addHandler(LoadEvent.getType(), handler));
        group.add(addHandler(LoadExceptionEvent.getType(), handler));
        group.add(addHandler(PostLoadEvent.getType(), handler));
        return group;
    }

    /** {@inheritDoc} */
    @Override
    public HandlerRegistration addLoadHandler(LoadEvent.LoadHandler handler) {
        return addHandler(LoadEvent.getType(), handler);
    }

    @NotNull
    protected <H extends EventHandler> HandlerRegistration addHandler(@NotNull GwtEvent.Type<H> type, @NotNull H handler) {
        if (eventBus == null) {
            eventBus = new SimpleEventBus();
        }
        return eventBus.addHandler(type, handler);
    }

    /**
     * Indicates that node value provider uses caching. It means that if node already has
     * children they will be returned to the tree, otherwise children nodes will be forcibly
     * loaded from the server.
     *
     * @return true if value provider uses caching, otherwise false
     */
    public boolean isUseCaching() {
        return useCaching;
    }

    /**
     * Set cache using.
     *
     * @param useCaching
     *         true if value provider should use caching, otherwise false
     */
    public void setUseCaching(boolean useCaching) {
        this.useCaching = useCaching;
    }

    /**
     * Binds tree to current node loader.
     *
     * @param tree
     *         tree instance
     */
    public void bindTree(Tree tree) {
        if (this.tree != null) {
            handlerRegistration.removeHandler();
        }

        this.tree = tree;

        if (tree != null) {
            if (handlerRegistration == null) {
                handlerRegistration = new GroupingHandlerRegistration();
            }

            handlerRegistration.add(addBeforeLoadHandler(cTreeNodeLoaderHandler));
            handlerRegistration.add(addLoadHandler(cTreeNodeLoaderHandler));
            handlerRegistration.add(addLoadExceptionHandler(cTreeNodeLoaderHandler));
        }
    }
}
