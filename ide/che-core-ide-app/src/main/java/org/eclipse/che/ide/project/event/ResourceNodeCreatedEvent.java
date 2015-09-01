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

import org.eclipse.che.ide.project.node.ResourceBasedNode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Vlad Zhukovskiy
 */
public class ResourceNodeCreatedEvent<DataObject> extends GwtEvent<ResourceNodeCreatedEvent.ResourceNodeCreatedHandler> {

    public interface ResourceNodeCreatedHandler extends EventHandler {
        void onResourceCreatedEvent(ResourceNodeCreatedEvent event);
    }

    private static Type<ResourceNodeCreatedHandler> TYPE;

    public static Type<ResourceNodeCreatedHandler> getType() {
        if (TYPE == null) {
            TYPE = new Type<>();
        }
        return TYPE;
    }

    private final ResourceBasedNode parent;
    private final DataObject        newDataObject;

    public ResourceNodeCreatedEvent(@Nullable ResourceBasedNode parent, @Nonnull DataObject newDataObject) {
        this.parent = parent;
        this.newDataObject = newDataObject;
    }

    @Nonnull
    public ResourceBasedNode getParentNode() {
        return parent;
    }

    public DataObject getNewDataObject() {
        return newDataObject;
    }

    /** {@inheritDoc} */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public Type<ResourceNodeCreatedHandler> getAssociatedType() {
        return (Type)TYPE;
    }

    @Override
    protected void dispatch(ResourceNodeCreatedHandler handler) {
        handler.onResourceCreatedEvent(this);
    }
}
