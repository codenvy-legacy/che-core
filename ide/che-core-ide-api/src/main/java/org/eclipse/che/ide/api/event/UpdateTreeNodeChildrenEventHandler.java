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

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import org.eclipse.che.ide.api.project.tree.TreeNode;

/**
 *
 * Handler for {@link UpdateTreeNodeChildrenEvent}
 *
 * @author Alexander Andrienko
 */
public interface UpdateTreeNodeChildrenEventHandler extends EventHandler {

    /**
     * Updates children data for some updated parent
     * @param parentNode parent Node which was updated
     * @param callback launches update next children
     */
    void updateChildrenData(TreeNode<?> parentNode, AsyncCallback<Void> callback);
}
