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
package org.eclipse.che.ide.event;

import com.google.gwt.event.shared.GwtEvent;

import org.eclipse.che.ide.project.node.ResourceBasedNode;

import java.util.List;

/**
 * Event that notifies of nodes deleted
 *
 * @author Igor Vinokur
 */
public class NodesDeletedEvent extends GwtEvent<NodesDeletedEventHandler> {

    public static final Type<NodesDeletedEventHandler> TYPE = new Type<>();

    private final List<ResourceBasedNode<?>> nodes;

    public NodesDeletedEvent(List<ResourceBasedNode<?>> nodes) {
        this.nodes = nodes;
    }

    @Override
    public Type<NodesDeletedEventHandler> getAssociatedType() {
        return TYPE;
    }

    /**
     * Returns the list of deleted nodes
     */
    public List<ResourceBasedNode<?>> getDeletedNodes() {
        return nodes;
    }

    @Override
    protected void dispatch(NodesDeletedEventHandler handler) {
        handler.onNodesDeleted(this);
    }
}
