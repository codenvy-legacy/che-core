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
package org.eclipse.che.ide.project.event;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

import org.eclipse.che.ide.project.event.ResourceNodeEvent.ResourceNodeHandler;
import org.eclipse.che.ide.project.node.ResourceBasedNode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Vlad Zhukovskiy
 */
public class ResourceNodeEvent extends GwtEvent<ResourceNodeHandler> {

    public interface ResourceNodeHandler extends EventHandler {
        void onResourceEvent(ResourceNodeEvent event);
    }

    private static Type<ResourceNodeHandler> TYPE;

    public static Type<ResourceNodeHandler> getType() {
        if (TYPE == null) {
            TYPE = new Type<>();
        }
        return TYPE;
    }

    private final ResourceBasedNode parent;
    private final ResourceBasedNode node;
    private final Event             event;

    public ResourceNodeEvent(@Nullable ResourceBasedNode parent, @Nonnull ResourceBasedNode node, @Nonnull Event event) {
        this.parent = parent;
        this.node = node;
        this.event = event;
    }

    public ResourceNodeEvent(@Nonnull ResourceBasedNode node, @Nonnull Event event) {
        this(null, node, event);
    }

    @Nonnull
    public Event getEvent() {
        return event;
    }

    @Nonnull
    public ResourceBasedNode getNode() {
        return node;
    }

    @Nullable
    public ResourceBasedNode getParent() {
        return parent;
    }

    /** {@inheritDoc} */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public Type<ResourceNodeHandler> getAssociatedType() {
        return (Type)TYPE;
    }

    @Override
    protected void dispatch(ResourceNodeHandler handler) {
        handler.onResourceEvent(this);
    }

    public enum Event {
        CREATED, DELETED, RENAMED
    }
}
