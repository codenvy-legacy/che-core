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
package org.eclipse.che.ide.part.projectexplorer;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import elemental.events.KeyboardEvent;
import elemental.events.MouseEvent;

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.Resources;
import org.eclipse.che.ide.api.parts.base.BaseView;
import org.eclipse.che.ide.api.project.tree.AbstractTreeNode;
import org.eclipse.che.ide.api.project.tree.TreeNode;
import org.eclipse.che.ide.ui.tree.Tree;
import org.eclipse.che.ide.ui.tree.TreeNodeElement;
import org.eclipse.che.ide.util.input.SignalEvent;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.vectomatic.dom.svg.ui.SVGImage;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * Project Explorer view.
 *
 * @author Andrey Plotnikov
 * @author Artem Zatsarynnyy
 */
@Singleton
public class ProjectExplorerViewImpl extends BaseView<ProjectExplorerView.ActionDelegate> implements ProjectExplorerView {

    protected Tree<TreeNode<?>>   tree;
    private   FlowPanel           projectHeader;
    private   AbstractTreeNode<?> rootNode;

    private ProjectTreeNodeDataAdapter projectTreeNodeDataAdapter;

    /** Create view. */
    @Inject
    public ProjectExplorerViewImpl(Resources resources,
                                   ProjectTreeNodeRenderer projectTreeNodeRenderer) {
        super(resources);

        projectTreeNodeDataAdapter = new ProjectTreeNodeDataAdapter();
        tree = Tree.create(resources, projectTreeNodeDataAdapter, projectTreeNodeRenderer, true);
        setContentWidget(tree.asWidget());

        projectHeader = new FlowPanel();
        projectHeader.setStyleName(resources.partStackCss().idePartStackToolbarBottom());

        tree.asWidget().ensureDebugId("projectExplorerTree-panel");
        minimizeButton.ensureDebugId("projectExplorer-minimizeBut");

        // create special 'invisible' root node that will contain 'visible' root nodes
        rootNode = new AbstractTreeNode<Void>(null, null, null, null) {
            @NotNull
            @Override
            public String getId() {
                return "ROOT";
            }

            @NotNull
            @Override
            public String getDisplayName() {
                return "ROOT";
            }

            @Override
            public boolean isLeaf() {
                return false;
            }

            @Override
            public void refreshChildren(AsyncCallback<TreeNode<?>> callback) {
            }
        };

        tree.setTreeEventHandler(new Tree.Listener<TreeNode<?>>() {
            @Override
            public void onNodeAction(TreeNodeElement<TreeNode<?>> node) {
                delegate.onNodeAction(node.getData());
            }

            @Override
            public void onNodeClosed(TreeNodeElement<TreeNode<?>> node) {
            }

            @Override
            public void onNodeContextMenu(final int mouseX, final int mouseY, TreeNodeElement<TreeNode<?>> node) {
                delegate.onNodeSelected(node.getData(), tree.getSelectionModel());
                Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        delegate.onContextMenu(mouseX, mouseY);
                    }
                });
            }

            @Override
            public void onNodeDragStart(TreeNodeElement<TreeNode<?>> node, MouseEvent event) {
            }

            @Override
            public void onNodeDragDrop(TreeNodeElement<TreeNode<?>> node, MouseEvent event) {
            }

            @Override
            public void onNodeExpanded(TreeNodeElement<TreeNode<?>> node) {
                delegate.onNodeExpanded(node.getData());
            }

            @Override
            public void onNodeSelected(TreeNodeElement<TreeNode<?>> node, SignalEvent event) {
                delegate.onNodeSelected(node.getData(), tree.getSelectionModel());
            }

            @Override
            public void onRootContextMenu(final int mouseX, final int mouseY) {
                Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                    @Override
                    public void execute() {
                        delegate.onContextMenu(mouseX, mouseY);
                    }
                });
            }

            @Override
            public void onRootDragDrop(MouseEvent event) {
            }

            @Override
            public void onKeyboard(KeyboardEvent event) {
                if (event.getKeyCode() == KeyboardEvent.KeyCode.ENTER) {
                    delegate.onEnterKey();

                } else
                if (event.getKeyCode() == KeyboardEvent.KeyCode.DELETE) {
                    delegate.onDeleteKey();
                }
            }
        });
    }

    @Override
    public List<TreeNode<?>> getOpenedTreeNodes() {
        List<TreeNodeElement<TreeNode<?>>> treeNodes = tree.getVisibleTreeNodes();
        List<TreeNode<?>> openedNodes = new ArrayList<>();
        for (TreeNodeElement<TreeNode<?>> treeNodeElement : treeNodes) {
            if (treeNodeElement.isOpen()) {
                openedNodes.add(treeNodeElement.getData());
            }
        }
        return openedNodes;
    }

    /** {@inheritDoc} */
    @Override
    public void setRootNodes(@NotNull final List<TreeNode<?>> rootNodes) {
        // provided rootNodes should be set as child nodes for rootNode
        rootNode.setChildren(rootNodes);
        for (TreeNode<?> treeNode : rootNodes) {
            treeNode.setParent(rootNode);
        }

        tree.getSelectionModel().clearSelections();
        tree.getModel().setRoot(rootNode);
        tree.renderTree(0);

        if (rootNodes.isEmpty()) {
            delegate.onNodeSelected(null, tree.getSelectionModel());
        } else {
            final TreeNode<?> firstNode = rootNodes.get(0);
            if (!firstNode.isLeaf()) {
                // expand first node that usually represents project itself
                tree.autoExpandAndSelectNode(firstNode, false);
                delegate.onNodeExpanded(firstNode);
            }
            // auto-select first node
            tree.getSelectionModel().selectSingleNode(firstNode);
            delegate.onNodeSelected(firstNode, tree.getSelectionModel());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void updateNode(@NotNull TreeNode<?> oldNode, @NotNull TreeNode<?> newNode) {
        // get currently selected node
        final List<TreeNode<?>> selectedNodes = tree.getSelectionModel().getSelectedNodes();
        TreeNode<?> selectedNode = null;
        if (!selectedNodes.isEmpty()) {
            selectedNode = selectedNodes.get(0);
        }

        List<List<String>> pathsToExpand = tree.replaceSubtree(oldNode, newNode, false);
        tree.expandPaths(pathsToExpand, false);

        // restore selected node
        if (selectedNode != null) {
            tree.getSelectionModel().selectSingleNode(selectedNode);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void selectNode(@NotNull TreeNode<?> node) {
        tree.getSelectionModel().selectSingleNode(node);
        delegate.onNodeSelected(node, tree.getSelectionModel());
    }

    /** {@inheritDoc} */
    @Override
    public void expandAndSelectNode(@NotNull TreeNode<?> node) {
        tree.autoExpandAndSelectNode(node, true);
        delegate.onNodeSelected(node, tree.getSelectionModel());
    }

    @NotNull
    @Override
    public TreeNode<?> getSelectedNode() {
        // Tree always must to have one selected node at least.
        // Return the first one until we don't support multi-selection.
        return tree.getSelectionModel().getSelectedNodes().get(0);
    }

    @NotNull
    public List<TreeNode<?>> getSelectedNodes() {
        return tree.getSelectionModel().getSelectedNodes();
    }

    @Override
    protected void focusView() {
        tree.asWidget().getElement().getFirstChildElement().focus();
    }

}
