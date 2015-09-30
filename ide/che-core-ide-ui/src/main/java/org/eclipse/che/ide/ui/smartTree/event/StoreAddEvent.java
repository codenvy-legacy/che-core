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
import org.eclipse.che.ide.ui.smartTree.event.StoreAddEvent.StoreAddHandler;

import java.util.Collections;
import java.util.List;

/**
 * Indicates that an element has been added to the Store.
 *
 * @author Vlad Zhukovskiy
 */
public class StoreAddEvent extends GwtEvent<StoreAddHandler> {

    public interface HasStoreAddHandlers extends HasHandlers {
        HandlerRegistration addStoreAddHandler(StoreAddHandler handler);
    }

    public interface StoreAddHandler extends EventHandler {
        void onAdd(StoreAddEvent event);
    }

    private static Type<StoreAddHandler> TYPE;

    public static Type<StoreAddHandler> getType() {
        if (TYPE == null) {
            TYPE = new Type<>();
        }
        return TYPE;
    }

    private final List<Node> nodes;
    private final int        index;

    public StoreAddEvent(int index, List<Node> nodes) {
        this.index = index;
        this.nodes = Collections.unmodifiableList(nodes);
    }

    public StoreAddEvent(int index, Node node) {
        this.index = index;
        nodes = Collections.singletonList(node);
    }

    @Override
    public Type<StoreAddHandler> getAssociatedType() {
        return getType();
    }

    public int getIndex() {
        return index;
    }

    public List<Node> getNodes() {
        return nodes;
    }

    /** {@inheritDoc} */
    @Override
    protected void dispatch(StoreAddHandler handler) {
        handler.onAdd(this);
    }
}