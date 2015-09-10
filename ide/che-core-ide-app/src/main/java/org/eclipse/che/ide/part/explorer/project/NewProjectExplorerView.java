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
import org.eclipse.che.ide.api.project.node.HasDataObject;
import org.eclipse.che.ide.api.project.node.HasStorablePath;
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

    List<StoreSortInfo> getSortInfo();

    void onApplySort();

    void scrollFromSource(Object object);

    boolean setGoIntoModeOn(Node node);

    void resetGoIntoMode();

    boolean isGoIntoActivated();

    boolean isFoldersAlwaysOnTop();

    void setFoldersAlwaysOnTop(boolean foldersAlwaysOnTop);

    void reloadChildren(Node parent, HasDataObject<?> selectAfter, boolean actionPerformed, boolean goInto);

    void reloadChildrenByType(Class<?> type);

    void navigate(HasStorablePath node, boolean select, boolean callAction);

    Promise<Node> navigate(HasStorablePath node, boolean select);

    void expandAll();

    void collapseAll();

    List<Node> getVisibleNodes();

    void showHiddenFiles(boolean show);

    boolean isShowHiddenFiles();

    public interface ActionDelegate extends BaseActionDelegate {
        void onSelectionChanged(List<Node> selection);

        void reloadSelectedNodes();
    }
}
