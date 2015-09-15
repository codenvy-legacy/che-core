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

import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.ide.api.mvp.View;
import org.eclipse.che.ide.api.parts.base.BaseActionDelegate;
import org.eclipse.che.ide.api.project.node.HasStorablePath;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.ui.smartTree.TreeNodeStorage.StoreSortInfo;
import org.eclipse.che.ide.ui.smartTree.event.CollapseNodeEvent;
import org.eclipse.che.ide.ui.smartTree.event.ExpandNodeEvent;
import org.eclipse.che.ide.ui.smartTree.event.GoIntoStateEvent;

import java.util.List;

/**
 * @author Vlad Zhukovskiy
 */
public interface NewProjectExplorerView extends View<NewProjectExplorerView.ActionDelegate> {

    void setRootNodes(List<Node> nodes);

    void setRootNode(Node node);

    List<StoreSortInfo> getSortInfo();

    void onApplySort();

    void scrollFromSource(HasStorablePath path);

    boolean setGoIntoModeOn(Node node);

    HandlerRegistration addGoIntoStateHandler(GoIntoStateEvent.GoIntoStateHandler handler);

    void resetGoIntoMode();

    boolean isGoIntoActivated();

    boolean isFoldersAlwaysOnTop();

    void setFoldersAlwaysOnTop(boolean foldersAlwaysOnTop);

    void reloadChildren(Node parent);

    void reloadChildren(Node parent, boolean deep);

    void reloadChildrenByType(Class<?> type);

    void expandAll();

    void collapseAll();

    List<Node> getVisibleNodes();

    void showHiddenFiles(boolean show);

    boolean isShowHiddenFiles();

    Promise<Node> getNodeByPath(HasStorablePath path);

    void select(Node item, boolean keepExisting);

    void select(List<Node> items, boolean keepExisting);

    boolean isExpanded(Node node);

    void setExpanded(Node node, boolean expand);

    HandlerRegistration addExpandHandler(ExpandNodeEvent.ExpandNodeHandler handler);

    HandlerRegistration addCollapseHandler(CollapseNodeEvent.CollapseNodeHandler handler);

    public interface ActionDelegate extends BaseActionDelegate {
        void onSelectionChanged(List<Node> selection);

        void reloadSelectedNodes();
    }
}
