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

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.SimpleEventBus;

import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.api.promises.client.js.Promises;
import org.eclipse.che.ide.ui.smartTree.event.BeforeLoadEvent;
import org.eclipse.che.ide.ui.smartTree.event.CancellableEvent;
import org.eclipse.che.ide.ui.smartTree.event.LoadEvent;
import org.eclipse.che.ide.ui.smartTree.event.LoadExceptionEvent;
import org.eclipse.che.ide.api.project.node.interceptor.NodeInterceptor;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.ui.smartTree.event.LoaderHandler;
import org.eclipse.che.ide.util.loging.Log;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
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
    Set<Node> childRequested = new HashSet<>();

    /**
     * Last processed node. Maybe used in general purpose.
     */
    private Node lastRequest;

    /**
     * Set of node interceptors. They need to modify children nodes before they will be set into parent node.
     *
     * @see org.eclipse.che.ide.api.project.node.interceptor.NodeInterceptor
     */
    private Set<NodeInterceptor> nodeInterceptors;

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
        public void onLoad(LoadEvent event) {
            Node parent = event.getRequestedNode();
            tree.getView().onLoadChange(tree.findNode(parent), false);
            tree.getNodeStorage().replaceChildren(parent, event.getReceivedNodes());
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

    private SimpleEventBus eventBus;

    /**
     * Creates  a new tree node value provider instance.
     *
     * @param nodeInterceptors
     *         set of {@link org.eclipse.che.ide.api.project.node.interceptor.NodeInterceptor}
     */
    public TreeNodeLoader(@Nullable Set<NodeInterceptor> nodeInterceptors) {
        this.nodeInterceptors = nodeInterceptors;
    }

    /**
     * Checks whether node has children or not. This method may allow tree to determine
     * whether to show expand control near non-leaf node.
     *
     * @param parent
     *         node
     * @return true if node has children, otherwise false
     */
    public boolean mayHaveChildren(@Nonnull Node parent) {
        return !parent.isLeaf();
    }

    /**
     * Initiates a load request for the parent's children.
     *
     * @param parent
     *         parent node
     * @return true if the load was requested, otherwise false
     */
    public boolean loadChildren(@Nonnull Node parent) {
        if (childRequested.contains(parent)) {
            return false;
        }

        childRequested.add(parent);
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
    @Nonnull
    private Operation<PromiseError> onLoadFailure(@Nonnull final Node parent) {
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
     * @return instance of {@link org.eclipse.che.api.promises.client.Operation} which contains promise with loaded children
     */
    @Nonnull
    private Operation<List<Node>> onLoadSuccess(@Nonnull final Node parent) {
        return new Operation<List<Node>>() {
            @Override
            public void apply(List<Node> children) throws OperationException {
                childRequested.remove(parent);
                fireEvent(new LoadEvent(parent, children));
            }
        };
    }

    /**
     * Initiates a load request for the parent's children.
     * Also fire {@link org.eclipse.che.ide.ui.smartTree.event.BeforeLoadEvent} event.
     *
     * @param parent
     *         parent node
     * @return true if load was requested, otherwise false
     */
    private boolean _load(@Nonnull Node parent) {
        if (fireEvent(new BeforeLoadEvent(parent))) {
            lastRequest = parent;

            parent.getChildren(!useCaching)
                  .thenPromise(interceptChildren(parent))
                  .then(onLoadSuccess(parent))
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
    private boolean fireEvent(@Nonnull GwtEvent<?> event) {
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
    @Nonnull
    private Function<List<Node>, Promise<List<Node>>> interceptChildren(@Nonnull final Node parent) {
        return new Function<List<Node>, Promise<List<Node>>>() {
            @Override
            public Promise<List<Node>> apply(List<Node> children) throws FunctionException {
                if (nodeInterceptors.isEmpty()) {
                    return Promises.resolve(children);
                }

                Promise<List<Node>> internalPromise = null;

                for (final NodeInterceptor interceptor : nodeInterceptors) {
                    if (internalPromise == null) {
                        internalPromise = interceptor.intercept(parent, children);
                    } else {
                        internalPromise.thenPromise(new Function<List<Node>, Promise<List<Node>>>() {
                            @Override
                            public Promise<List<Node>> apply(List<Node> arg) throws FunctionException {
                                return interceptor.intercept(parent, arg);
                            }
                        });
                    }
                }

                return internalPromise;
            }
        };
    }

    /** {@inheritDoc} */
    @Nonnull
    @Override
    public HandlerRegistration addBeforeLoadHandler(@Nonnull BeforeLoadEvent.BeforeLoadHandler handler) {
        return addHandler(BeforeLoadEvent.getType(), handler);
    }

    /** {@inheritDoc} */
    @Nonnull
    @Override
    public HandlerRegistration addLoadExceptionHandler(@Nonnull LoadExceptionEvent.LoadExceptionHandler handler) {
        return addHandler(LoadExceptionEvent.getType(), handler);
    }

    /** {@inheritDoc} */
    @Override
    public HandlerRegistration addLoaderHandler(LoaderHandler handler) {
        GroupingHandlerRegistration group = new GroupingHandlerRegistration();
        group.add(addHandler(BeforeLoadEvent.getType(), handler));
        group.add(addHandler(LoadEvent.getType(), handler));
        group.add(addHandler(LoadExceptionEvent.getType(), handler));
        return group;
    }

    /** {@inheritDoc} */
    @Override
    public HandlerRegistration addLoadHandler(LoadEvent.LoadHandler handler) {
        return addHandler(LoadEvent.getType(), handler);
    }

    @Nonnull
    protected <H extends EventHandler> HandlerRegistration addHandler(@Nonnull GwtEvent.Type<H> type, @Nonnull H handler) {
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
