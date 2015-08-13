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

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.event.ItemEvent;
import org.eclipse.che.ide.api.event.ItemHandler;
import org.eclipse.che.ide.api.event.NodeChangedEvent;
import org.eclipse.che.ide.api.event.NodeChangedHandler;
import org.eclipse.che.ide.api.event.NodeExpandedEvent;
import org.eclipse.che.ide.api.event.PersistProjectTreeStateEvent;
import org.eclipse.che.ide.api.event.ProjectActionEvent;
import org.eclipse.che.ide.api.event.ProjectActionEvent.ProjectAction;
import org.eclipse.che.ide.api.event.ProjectActionHandler;
import org.eclipse.che.ide.api.event.RefreshProjectTreeEvent;
import org.eclipse.che.ide.api.event.RefreshProjectTreeHandler;
import org.eclipse.che.ide.api.event.RenameNodeEvent;
import org.eclipse.che.ide.api.event.RenameNodeEventHandler;
import org.eclipse.che.ide.api.event.RestoreProjectTreeStateEvent;
import org.eclipse.che.ide.api.mvp.View;
import org.eclipse.che.ide.api.parts.HasView;
import org.eclipse.che.ide.api.parts.ProjectExplorerPart;
import org.eclipse.che.ide.api.parts.base.BasePresenter;
import org.eclipse.che.ide.api.project.tree.TreeNode;
import org.eclipse.che.ide.api.project.tree.TreeStructure;
import org.eclipse.che.ide.api.project.tree.TreeStructureProviderRegistry;
import org.eclipse.che.ide.api.project.tree.VirtualFile;
import org.eclipse.che.ide.api.project.tree.generic.FileNode;
import org.eclipse.che.ide.api.project.tree.generic.Openable;
import org.eclipse.che.ide.api.project.tree.generic.StorableNode;
import org.eclipse.che.ide.api.project.tree.generic.UpdateTreeNodeDataIterable;
import org.eclipse.che.ide.api.selection.Selection;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.collections.Collections;
import org.eclipse.che.ide.collections.StringMap;
import org.eclipse.che.ide.collections.js.JsoArray;
import org.eclipse.che.ide.logger.AnalyticsEventLoggerExt;
import org.eclipse.che.ide.menu.ContextMenu;
import org.eclipse.che.ide.part.editor.CurrentProjectManager;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.ui.tree.SelectionModel;
import org.eclipse.che.ide.util.Config;
import org.eclipse.che.ide.util.loging.Log;
import org.vectomatic.dom.svg.ui.SVGResource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import static org.eclipse.che.ide.api.event.ItemEvent.ItemOperation.CREATED;
import static org.eclipse.che.ide.api.event.ItemEvent.ItemOperation.DELETED;
import static org.eclipse.che.ide.api.event.ProjectActionEvent.ProjectAction.CLOSED;
import static org.eclipse.che.ide.api.event.ProjectActionEvent.ProjectAction.OPENED;

/**
 * Project Explorer displays project's tree in a dedicated part (view).
 *
 * @author Nikolay Zamosenchuk
 * @author Artem Zatsarynnyy
 * @author Dmitry Shnurenko
 */
@Singleton
public class ProjectExplorerPartPresenter extends BasePresenter implements ProjectExplorerView.ActionDelegate,
                                                                           RefreshProjectTreeHandler,
                                                                           ProjectExplorerPart,
                                                                           CurrentProjectManager.ProjectChangedListener,
                                                                           HasView {
    private final ProjectExplorerView            view;
    private final EventBus                       eventBus;
    private final ContextMenu                    contextMenu;
    private final ProjectServiceClient           projectServiceClient;
    private final CoreLocalizationConstant       coreLocalizationConstant;
    private final AppContext                     appContext;
    private final TreeStructureProviderRegistry  treeStructureProviderRegistry;
    private final DeleteNodeHandler              deleteNodeHandler;
    private final Provider<ProjectListStructure> projectListStructureProvider;
    private final AnalyticsEventLoggerExt        eventLogger;
    private final Provider<EditorAgent>          editorAgentProvider;
    private final CurrentProjectManager          projectManager;


    private TreeStructure      currentTreeStructure;
    private Array<TreeNode<?>> nodesToRefresh;

    /** Instantiates the Project Explorer presenter. */
    @Inject
    public ProjectExplorerPartPresenter(ProjectExplorerView view,
                                        EventBus eventBus,
                                        ProjectServiceClient projectServiceClient,
                                        ContextMenu contextMenu,
                                        CoreLocalizationConstant coreLocalizationConstant,
                                        AppContext appContext,
                                        TreeStructureProviderRegistry treeStructureProviderRegistry,
                                        DeleteNodeHandler deleteNodeHandler,
                                        AnalyticsEventLoggerExt eventLogger,
                                        Provider<ProjectListStructure> projectListStructureProvider,
                                        Provider<EditorAgent> editorAgentProvider,
                                        CurrentProjectManager projectManager) {
        this.view = view;
        this.eventBus = eventBus;
        this.contextMenu = contextMenu;
        this.projectServiceClient = projectServiceClient;
        this.coreLocalizationConstant = coreLocalizationConstant;
        this.appContext = appContext;
        this.treeStructureProviderRegistry = treeStructureProviderRegistry;
        this.deleteNodeHandler = deleteNodeHandler;
        this.eventLogger = eventLogger;
        this.projectListStructureProvider = projectListStructureProvider;
        this.view.setTitle(coreLocalizationConstant.projectExplorerTitleBarText());
        this.editorAgentProvider = editorAgentProvider;

        this.projectManager = projectManager;
        this.projectManager.addListener(this);

        bind();
    }

    /** {@inheritDoc} */
    @Override
    public void go(AcceptsOneWidget container) {
        container.setWidget(view);
    }

    @Override
    public View getView() {
        return view;
    }

    /** {@inheritDoc} */
    @Override
    public void onOpen() {
        if (Config.getProjectName() == null) {
            setTree(projectListStructureProvider.get(), OPENED);
        } else {
            projectServiceClient.getProject(Config.getProjectName(), new AsyncRequestCallback<ProjectDescriptor>() {
                @Override
                protected void onSuccess(ProjectDescriptor result) {
                }

                @Override
                protected void onFailure(Throwable exception) {
                    setTree(projectListStructureProvider.get(), OPENED);
                }
            });
        }
    }

    /** {@inheritDoc} */
    @Nonnull
    @Override
    public String getTitle() {
        return coreLocalizationConstant.projectExplorerButtonTitle();
    }

    /** {@inheritDoc} */
    @Override
    public void setVisible(boolean visible) {
        view.setVisible(visible);
    }

    /** {@inheritDoc} */
    @Override
    public ImageResource getTitleImage() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public SVGResource getTitleSVGImage() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getTitleToolTip() {
        return "This View helps you to do basic operation with your projects.";
    }

    /** {@inheritDoc} */
    @Override
    public int getSize() {
        return 230;
    }

    /** Adds behavior to view's components. */
    protected void bind() {
        view.setDelegate(this);

        eventBus.addHandler(ProjectActionEvent.TYPE, new ProjectActionHandler() {
            @Override
            public void onProjectOpened(ProjectActionEvent event) {
                final ProjectDescriptor project = event.getProject();
                setTree(treeStructureProviderRegistry.getTreeStructureProvider(project.getType()).get(), OPENED);
                view.setProjectHeader(event.getProject());
                eventLogger.logEvent("project-opened", new HashMap<String, String>());
            }

            @Override
            public void onProjectClosing(ProjectActionEvent event) {
            }

            @Override
            public void onProjectClosed(ProjectActionEvent event) {
                // this isn't case when some project going to open while previously opened project is closing
                if (!event.isCloseBeforeOpening()) {
                    setTree(projectListStructureProvider.get(), CLOSED);
                    view.hideProjectHeader();
                }
            }
        });

        eventBus.addHandler(RefreshProjectTreeEvent.TYPE, this);

        eventBus.addHandler(NodeChangedEvent.TYPE, new NodeChangedHandler() {
            @Override
            public void onNodeRenamed(NodeChangedEvent event) {
                if (appContext.getCurrentProject() == null) {
                    // any opened project - all projects list is shown
                    setTree(currentTreeStructure, null);
                } else {
                    updateNode(event.getNode().getParent());
                    view.selectNode(event.getNode());
                }
            }
        });

        eventBus.addHandler(ItemEvent.TYPE, new ItemHandler() {
            @Override
            public void onItem(final ItemEvent event) {
                if (DELETED == event.getOperation()) {
                    refreshAndSelectNode(event.getItem().getParent());
                } else if (CREATED == event.getOperation()) {
                    final TreeNode<?> selectedNode = view.getSelectedNode();
                    updateNode(selectedNode.getParent());
                    updateNode(selectedNode);
                    view.expandAndSelectNode(event.getItem());
                }
            }
        });

        eventBus.addHandler(RenameNodeEvent.TYPE, new RenameNodeEventHandler() {
            @Override
            public void onNodeRenamed(TreeNode<?> parentNode, String newParenNodePath) {
                if (parentNode instanceof UpdateTreeNodeDataIterable) {
                    final StorableNode parent = (StorableNode)parentNode;
                    Array<TreeNode<?>> children = JsoArray.create();
                    children.add(parent);
                    getAllChildren(parentNode, children);
                    getOpenedUnCashedFiles(parent, children);
                    final Iterator<TreeNode<?>> treeNodeIterator = children.asIterable().iterator();

                    updateDataTreeNode(parent, newParenNodePath, treeNodeIterator);
                }
            }
        });
    }

    private void updateDataTreeNode(final StorableNode parent, final String renamedNodeNewPath, final Iterator<TreeNode<?>> children) {
        if (!children.hasNext()) {
            eventBus.fireEvent(NodeChangedEvent.createNodeRenamedEvent(parent.getParent()));
            return;
        }

        TreeNode treeNode = children.next();

        if (treeNode instanceof UpdateTreeNodeDataIterable && treeNode instanceof StorableNode) {
            String newPath = solveNewPath(renamedNodeNewPath, ((StorableNode)treeNode).getPath());

            if (treeNode.equals(parent)) {
                newPath = renamedNodeNewPath;
            }

            ((UpdateTreeNodeDataIterable)treeNode).updateData(new AsyncCallback<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    updateDataTreeNode(parent, renamedNodeNewPath, children);
                }

                @Override
                public void onFailure(Throwable throwable) {
                    Log.error(getClass(), "Error update treeNode data " + throwable);
                    eventBus.fireEvent(new RefreshProjectTreeEvent());
                }
            }, newPath);
            return;
        }

        updateDataTreeNode(parent, renamedNodeNewPath, children);
    }

    private String solveNewPath(String nodeRenamedPath, String currentPath) {
        String prefixPath = nodeRenamedPath.substring(0, nodeRenamedPath.lastIndexOf("/") + 1);
        currentPath = currentPath.replace(prefixPath, "");
        currentPath = currentPath.substring(currentPath.indexOf("/"), currentPath.length());
        nodeRenamedPath = nodeRenamedPath.substring(nodeRenamedPath.lastIndexOf("/") + 1, nodeRenamedPath.length());

        return prefixPath + nodeRenamedPath + currentPath;
    }

    private void getAllChildren(TreeNode<?> parent, Array<TreeNode<?>> children) {
        Array<TreeNode<?>> childrenForCurrentDeep = parent.getChildren();

        if (childrenForCurrentDeep.isEmpty()) {
            return;
        }

        children.addAll(childrenForCurrentDeep);

        for (TreeNode<?> node : childrenForCurrentDeep.asIterable()) {
            getAllChildren(node, children);
        }
    }

    private void getOpenedUnCashedFiles(StorableNode parentNode, Array<TreeNode<?>> children) {
        StringMap<EditorPartPresenter> editorParts = editorAgentProvider.get().getOpenedEditors();
        for (EditorPartPresenter editorPart : editorParts.getValues().asIterable()) {
            VirtualFile file = editorPart.getEditorInput().getFile();

            if (!(file instanceof FileNode)) {
                continue;
            }

            FileNode fileNode = (FileNode)file;
            String path = fileNode.getPath();

            if (path.startsWith(parentNode.getPath() + "/") && !children.contains(fileNode)) {
                children.add(fileNode);
            }
        }
    }

    @Override
    public void onRefreshTree() {
        onRefreshProjectTree(new RefreshProjectTreeEvent(null, true));
    }

    /**
     * Refreshes project tree.
     *
     * @param event
     */
    public void onRefreshProjectTree(RefreshProjectTreeEvent event) {
        eventBus.fireEvent(new PersistProjectTreeStateEvent());

        if (appContext.getCurrentProject() == null) {
            setTree(projectListStructureProvider.get(), null);
            return;
        }

        if (event.refreshSubtree()) {
            nodesToRefresh = view.getOpenedTreeNodes();
            refreshTreeNodes();
            return;
        }

        if (event.getNode() != null) {
            refreshAndSelectNode(event.getNode());
            return;
        }

        currentTreeStructure.getRootNodes(new AsyncCallback<Array<TreeNode<?>>>() {
            @Override
            public void onSuccess(Array<TreeNode<?>> result) {
                for (TreeNode<?> childNode : result.asIterable()) {
                    // clear children in order to force to refresh
                    childNode.setChildren(Collections.<TreeNode<?>>createArray());
                    refreshAndSelectNode(childNode);

                    ProjectDescriptor projectDescriptor = appContext.getCurrentProject().getRootProject();
                    String workspaceName = projectDescriptor.getWorkspaceName();
                    String fullProjectPath = "/" + workspaceName + projectDescriptor.getPath();
                    eventBus.fireEvent(new RestoreProjectTreeStateEvent(fullProjectPath));
                }
            }

            @Override
            public void onFailure(Throwable caught) {
                Log.error(ProjectExplorerPartPresenter.class, caught);
            }
        });
    }

    /**
     * Asynchronously refreshes all nodes from the nodesToRefresh list.
     */
    private void refreshTreeNodes() {
        if (nodesToRefresh != null && !nodesToRefresh.isEmpty()) {
            TreeNode<?> treeNode = nodesToRefresh.get(0);
            nodesToRefresh.remove(0);

            refreshNode(treeNode, new AsyncCallback<TreeNode<?>>() {
                @Override
                public void onSuccess(TreeNode<?> result) {
                    refreshTreeNodes();
                }

                @Override
                public void onFailure(Throwable caught) {
                    Log.error(ProjectExplorerPartPresenter.class, caught);

                    // Skip current node and continue refreshing.
                    refreshTreeNodes();
                }
            });
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onNodeSelected(final TreeNode<?> node, final SelectionModel<?> model) {
        final Array<?> allSelected = model.getSelectedNodes();
        final List<Object> newSelection = new ArrayList<>();
        for (final Object item : allSelected.asIterable()) {
            newSelection.add(item);
        }
        if (newSelection.contains(node)) {
            setSelection(new Selection<>(newSelection, node));
        } else {
            setSelection(new Selection<>(newSelection));
        }

        if (node != null && node instanceof StorableNode && appContext.getCurrentProject() != null) {
            String pathToFile = ((StorableNode)node).getPath();
            ProjectDescriptor descriptor = node.getProject().getData();

            projectManager.matchFileToProject(pathToFile, descriptor);

            appContext.getCurrentProject().setProjectDescription(descriptor);

            view.setProjectHeader(descriptor);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onProjectChanged(@Nonnull ProjectDescriptor descriptor) {
        view.setProjectHeader(descriptor);
    }

    @Override
    public void expandNode(TreeNode<?> node) {
        view.expandAndSelectNode(node);
        view.updateNode(node, node);

        eventBus.fireEvent(new NodeExpandedEvent());
    }

    /** {@inheritDoc} */
    @Override
    public void onNodeExpanded(@Nonnull final TreeNode<?> node) {
        if (node.getChildren().isEmpty()) {
            // If children is empty then node may be not refreshed yet?
            node.refreshChildren(new AsyncCallback<TreeNode<?>>() {
                @Override
                public void onSuccess(TreeNode<?> result) {
                    if (node instanceof Openable) {
                        ((Openable)node).open();
                    }
                    if (!result.getChildren().isEmpty()) {
                        updateNode(result);
                    }
                }

                @Override
                public void onFailure(Throwable caught) {
                    Log.error(ProjectExplorerPartPresenter.class, caught);
                }
            });
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onNodeAction(@Nonnull TreeNode<?> node) {
        node.processNodeAction();
    }

    /** {@inheritDoc} */
    @Override
    public void onContextMenu(int mouseX, int mouseY) {
        contextMenu.show(mouseX, mouseY);
    }

    /** {@inheritDoc} */
    @Override
    public void onDeleteKey() {
        List<StorableNode> selectedNodes = new ArrayList<>();
        for (Object node : view.getSelectedNodes().asIterable()) {
            if (node instanceof StorableNode) {
                selectedNodes.add((StorableNode)node);
            }
        }
        deleteNodeHandler.deleteNodes(selectedNodes);
    }

    /** {@inheritDoc} */
    @Override
    public void onEnterKey() {
        view.getSelectedNode().processNodeAction();
    }

    private void setTree(@Nonnull final TreeStructure treeStructure, @Nullable final ProjectAction actionType) {
        currentTreeStructure = treeStructure;
        if (appContext.getCurrentProject() != null) {
            appContext.getCurrentProject().setCurrentTree(currentTreeStructure);
        }
        treeStructure.getRootNodes(new AsyncCallback<Array<TreeNode<?>>>() {
            @Override
            public void onSuccess(Array<TreeNode<?>> result) {
                view.setRootNodes(result, actionType);
            }

            @Override
            public void onFailure(Throwable caught) {
                Log.error(ProjectExplorerPartPresenter.class, caught.getMessage());
            }
        });
    }

    public void refreshNode(TreeNode<?> node, final AsyncCallback<TreeNode<?>> callback) {
        node.refreshChildren(new AsyncCallback<TreeNode<?>>() {
            @Override
            public void onSuccess(TreeNode<?> result) {
                updateNode(result);
                callback.onSuccess(result);
            }

            @Override
            public void onFailure(Throwable caught) {
                callback.onFailure(caught);
            }
        });
    }

    private void refreshAndSelectNode(TreeNode<?> node) {
        refreshNode(node, new AsyncCallback<TreeNode<?>>() {
            @Override
            public void onSuccess(TreeNode<?> result) {
                view.selectNode(result);
            }

            @Override
            public void onFailure(Throwable caught) {
                Log.error(ProjectExplorerPartPresenter.class, caught);
            }
        });
    }

    // TODO remove this method in the nearest feature
    // TODO just use updateNode of view
    private void updateNode(TreeNode<?> node) {
        view.updateNode(node, node);
    }
}
