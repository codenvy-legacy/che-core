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
package org.eclipse.che.ide.api.event;

import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.user.client.rpc.AsyncCallback;
import org.eclipse.che.ide.api.project.tree.TreeNode;

/**
 * Event uses for updating treeNodes data after rename
 *
 * @author Alexander Andrienko
 */
public class RenameNodeEvent extends GwtEvent<RenameNodeEventHandler> {

    public static final Type<RenameNodeEventHandler> TYPE = new Type<>();

    private final TreeNode<?>         treeNode;
    private final AsyncCallback<Void> asyncCallback;
    private final String              newParenNodePath;

    public RenameNodeEvent(TreeNode<?> treeNode, String newParenNodePath, AsyncCallback<Void> asyncCallback) {
        this.treeNode = treeNode;
        this.newParenNodePath = newParenNodePath;
        this.asyncCallback = asyncCallback;
    }

    @Override
    public Type<RenameNodeEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(RenameNodeEventHandler handler) {
        handler.onNodeRenamed(treeNode, newParenNodePath, asyncCallback);
    }
}
