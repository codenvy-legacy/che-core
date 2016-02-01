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

import org.eclipse.che.ide.project.node.ItemReferenceBasedNode;

/**
 * Event that notifies of renaming provided
 *
 * @author Igor Vinokur
 */
public class RenameNodeEvent extends GwtEvent<RenameNodeEventHandler> {

    public static final Type<RenameNodeEventHandler> TYPE = new Type<>();

    private final ItemReferenceBasedNode node;
    private final String            newPathName;

    public RenameNodeEvent(ItemReferenceBasedNode node, String newPathName) {
        this.node = node;
        this.newPathName = newPathName;
    }

    /**
     * Returns the renamed node.
     */
    public ItemReferenceBasedNode getNode() {
        return node;
    }

    /**
     * Returns the new path name of the renamed node.
     *
     * @return new node name with its full path
     */
    public String getNewFilePathName() {
        return newPathName;
    }

    @Override
    public Type<RenameNodeEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(RenameNodeEventHandler handler) {
        handler.onNodeRenamed(this);
    }
}