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

import elemental.events.KeyboardEvent;
import elemental.events.MouseEvent;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.Resources;
import org.eclipse.che.ide.api.parts.base.BaseView;
import org.eclipse.che.ide.api.project.tree.AbstractTreeNode;
import org.eclipse.che.ide.api.project.tree.TreeNode;
import org.eclipse.che.ide.ui.tree.Tree;
import org.eclipse.che.ide.ui.tree.TreeNodeElement;
import org.eclipse.che.ide.util.input.SignalEvent;
import org.vectomatic.dom.svg.ui.SVGImage;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Project Explorer view.
 *
 * @author Andrey Plotnikov
 * @author Artem Zatsarynnyy
 * @author Dmitry Shnurenko
 */
@Singleton
public class ProjectExplorerViewImpl extends BaseView<ProjectExplorerView.ActionDelegate> implements ProjectExplorerView {

    private final Resources           resources;
    private final FlowPanel           projectHeader;
    private final AbstractTreeNode<?> rootNode;
    private final List<TreeNode<?>>   openedNodes;
    private final List<String>        openedNodeNames;

    protected final Tree<TreeNode<?>> tree;

    @Inject
    public ProjectExplorerViewImpl(Resources resources, ProjectTreeNodeRenderer projectTreeNodeRenderer) {
        super(resources);

        this.resources = resources;

        ProjectTreeNodeDataAdapter projectTreeNodeDataAdapter = new ProjectTreeNodeDataAdapter();
        tree = Tree.create(resources, projectTreeNodeDataAdapter, projectTreeNodeRenderer, true);
        setContentWidget(tree.asWidget());

        this.openedNodes = new ArrayList<>();
        this.openedNodeNames = new ArrayList<>();

        projectHeader = new FlowPanel();
        projectHeader.setStyleName(resources.partStackCss().idePartStackToolbarBottom());

        tree.asWidget().ensureDebugId("projectExplorerTree-panel");
        minimizeButton.ensureDebugId("projectExplorer-minimizeBut");

        // create special 'invisible' root node that will contain 'visible' root nodes
        rootNode = new AbstractTreeNode<Void>(null, null, null, null) {
            @Nonnull
            @Override
            public String getId() {
                return "ROOT";
            }

            @Nonnull
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

                } else if (event.getKeyCode() == KeyboardEvent.KeyCode.DELETE) {
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
    public void showEmptyTree() {
        openedNodes.clear();

        rootNode.setChildren(openedNodes);

        updateTree();

        toolBar.remove(projectHeader);
        setToolbarHeight(22);
    }

    private void updateTree() {
        tree.getSelectionModel().clearSelections();
        tree.getModel().setRoot(rootNode);
        tree.renderTree(0);
    }

    /** {@inheritDoc} */
    @Override
    public void addNodeToTree(@Nonnull TreeNode<?> openedNode) {
        String nodeName = openedNode.getDisplayName();

        if (!openedNodeNames.contains(nodeName)) {
            openedNodeNames.add(nodeName);

            openedNodes.add(openedNode);
        }

        openedNode.setParent(rootNode);

        rootNode.setChildren(openedNodes);

        updateTree();
    }

    /** {@inheritDoc} */
    @Override
    public void updateNode(@Nonnull TreeNode<?> oldNode, @Nonnull TreeNode<?> newNode) {
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
    public void selectNode(@Nonnull TreeNode<?> node) {
        tree.getSelectionModel().selectSingleNode(node);
        delegate.onNodeSelected(node, tree.getSelectionModel());
    }

    /** {@inheritDoc} */
    @Override
    public void expandAndSelectNode(@Nonnull TreeNode<?> node) {
        tree.autoExpandAndSelectNode(node, true);
        delegate.onNodeSelected(node, tree.getSelectionModel());
    }

    /** {@inheritDoc} */
    @Override
    public void setProjectHeader(@Nonnull ProjectDescriptor project) {
        if (toolBar.getWidgetIndex(projectHeader) < 0) {
            toolBar.addSouth(projectHeader, 28);
            setToolbarHeight(50);
        }

        projectHeader.clear();

        FlowPanel delimiter = new FlowPanel();
        delimiter.setStyleName(resources.partStackCss().idePartStackToolbarSeparator());
        projectHeader.add(delimiter);

        SVGImage icon = new SVGImage("private".equals(project.getVisibility()) ?
                                     resources.privateProject() : resources.publicProject());
        icon.getElement().setAttribute("class", resources.partStackCss().idePartStackToolbarBottomIcon());
        projectHeader.add(icon);

        InlineLabel projectTitle = new InlineLabel(project.getName());
        projectHeader.add(projectTitle);

        FlowPanel refreshButton = new FlowPanel();
        refreshButton.add(new SVGImage(resources.refresh()));
        refreshButton.setStyleName(resources.partStackCss().idePartStackToolbarBottomButton());
        refreshButton.addStyleName(resources.partStackCss().idePartStackToolbarBottomButtonRight());
        refreshButton.ensureDebugId("projectExplorer-button-refreshTree");
        projectHeader.add(refreshButton);

        refreshButton.addDomHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if (delegate != null) {
                    delegate.onRefreshTree();
                }
            }
        }, ClickEvent.getType());
    }

    @Nonnull
    @Override
    public TreeNode<?> getSelectedNode() {
        // Tree always must to have one selected node at least.
        // Return the first one until we don't support multi-selection.
        return tree.getSelectionModel().getSelectedNodes().get(0);
    }

    @Nonnull
    public List<TreeNode<?>> getSelectedNodes() {
        return tree.getSelectionModel().getSelectedNodes();
    }

    @Override
    protected void focusView() {
        tree.asWidget().getElement().getFirstChildElement().focus();
    }

}
