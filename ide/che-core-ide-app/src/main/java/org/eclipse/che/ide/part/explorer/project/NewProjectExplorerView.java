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
package org.eclipse.che.ide.part.explorer.project;

import com.google.gwt.event.shared.HandlerRegistration;

import org.eclipse.che.ide.api.mvp.View;
import org.eclipse.che.ide.api.parts.base.BaseActionDelegate;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.ui.smartTree.TreeNodeStorage.StoreSortInfo;
import org.eclipse.che.ide.ui.smartTree.event.BeforeExpandNodeEvent.BeforeExpandNodeHandler;

import java.util.List;

/**
 * @author Vlad Zhukovskiy
 */
public interface NewProjectExplorerView extends View<NewProjectExplorerView.ActionDelegate> {

    void setRootNodes(List<Node> nodes);

    void setRootNode(Node node);

    void onChildrenCreated(Node parent, Node child);

    void onChildrenRemoved(Node node);

    List<StoreSortInfo> getSortInfo();

    void onApplySort();

    void scrollFromSource(Object object);

    boolean goInto(Node node);

    void resetGoIntoMode();

    boolean isFoldersAlwaysOnTop();

    void setFoldersAlwaysOnTop(boolean foldersAlwaysOnTop);

    void synchronizeTree();

    HandlerRegistration addBeforeExpandNodeHandler(BeforeExpandNodeHandler handler);

    void reloadChildren(List<Node> nodes, Object selectAfter, boolean callAction);

    void reloadChildrenByType(Class<?> type);

    public interface ActionDelegate extends BaseActionDelegate {
        void onSelectionChanged(List<Node> selection);

        void reloadSelectedNodes();
    }
}
