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
package org.eclipse.che.ide.api.project.tree.generic;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 *
 * This interface uses for recursive updating children data for some parent which was updated, f.e rename some node
 *
 * @author Alexander Andrienko
 */
public interface UpdateTreeNodeDataIterable {
    /**
     * Takes away new node data from server, update node and launch action in callBack to continue recursion
     * @param callback callback
     * @param updatedParentNodePath path of the parent node which was updated
     */
    void updateData(AsyncCallback<Void> callback, String updatedParentNodePath);
}
