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
package org.eclipse.che.ide.ui.smartTree.event;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;

import org.eclipse.che.ide.api.project.node.Node;

import java.util.Collections;
import java.util.List;

/**
 * Indicates that an element has been added to the Store.
 *
 * @author Vlad Zhukovskiy
 */
public class NodeAddedEvent extends GwtEvent<NodeAddedEvent.NodeAddedEventHandler> {

    public interface HasNodeAddedEventHandlers extends HasHandlers {
        HandlerRegistration addNodeAddedHandler(NodeAddedEventHandler handler);
    }

    public interface NodeAddedEventHandler extends EventHandler {
        void onNodeAdded(NodeAddedEvent event);
    }

    private static Type<NodeAddedEventHandler> TYPE;

    public static Type<NodeAddedEventHandler> getType() {
        if (TYPE == null) {
            TYPE = new Type<>();
        }
        return TYPE;
    }

    private final List<Node> nodes;

    public NodeAddedEvent(List<Node> nodes) {
        this.nodes = Collections.unmodifiableList(nodes);
    }

    public NodeAddedEvent(Node node) {
        nodes = Collections.singletonList(node);
    }

    /** {@inheritDoc} */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Type<NodeAddedEventHandler> getAssociatedType() {
        return (Type)getType();
    }

    public List<Node> getNodes() {
        return nodes;
    }

    /** {@inheritDoc} */
    @Override
    protected void dispatch(NodeAddedEventHandler handler) {
        handler.onNodeAdded(this);
    }
}