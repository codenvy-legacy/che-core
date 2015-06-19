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
 * Update children of some parent TreeNode
 *
 * @author Alexander Andrienko
 */
public class UpdateTreeNodeChildrenEvent extends GwtEvent<UpdateTreeNodeChildrenEventHandler> {

    public static final Type<UpdateTreeNodeChildrenEventHandler> TYPE = new Type<>();

    private final TreeNode<?>         treeNode;
    private final AsyncCallback<Void> asyncCallback;

    public UpdateTreeNodeChildrenEvent(TreeNode<?> treeNode, AsyncCallback<Void> asyncCallback) {
        this.treeNode = treeNode;
        this.asyncCallback = asyncCallback;
    }

    @Override
    public Type<UpdateTreeNodeChildrenEventHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(UpdateTreeNodeChildrenEventHandler handler) {
        handler.updateChildrenData(treeNode, asyncCallback);
    }
}
