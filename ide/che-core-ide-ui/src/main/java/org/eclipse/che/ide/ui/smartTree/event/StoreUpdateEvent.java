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
import org.eclipse.che.ide.ui.smartTree.event.StoreUpdateEvent.StoreUpdateHandler;

import java.util.Collections;
import java.util.List;

/**
 * Event fires when nodes have been changed.
 *
 * @author Vlad Zhukovskiy
 */
public final class StoreUpdateEvent extends GwtEvent<StoreUpdateHandler> {

    public interface HasStoreUpdateHandlers extends HasHandlers {
        HandlerRegistration addStoreUpdateHandler(StoreUpdateHandler handler);
    }

    public interface StoreUpdateHandler extends EventHandler {
        void onUpdate(StoreUpdateEvent event);
    }

    private static Type<StoreUpdateHandler> TYPE;

    public static Type<StoreUpdateHandler> getType() {
        if (TYPE == null) {
            TYPE = new Type<>();
        }
        return TYPE;
    }

    private List<Node> nodes;

    public StoreUpdateEvent(List<Node> nodes) {
        this.nodes = Collections.unmodifiableList(nodes);
    }

    /** {@inheritDoc} */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public Type<StoreUpdateHandler> getAssociatedType() {
        return (Type)getType();
    }

    public List<Node> getNodes() {
        return nodes;
    }

    /** {@inheritDoc} */
    @Override
    protected void dispatch(StoreUpdateHandler handler) {
        handler.onUpdate(this);
    }
}