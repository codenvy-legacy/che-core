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
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.machine.gwt.client.events.ExtServerStateEvent;
import org.eclipse.che.api.machine.gwt.client.events.ExtServerStateHandler;
import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.project.shared.dto.ProjectReference;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.event.ItemEvent;
import org.eclipse.che.ide.api.event.ItemHandler;
import org.eclipse.che.ide.api.event.NodeChangedEvent;
import org.eclipse.che.ide.api.event.NodeChangedHandler;
import org.eclipse.che.ide.api.event.NodeExpandedEvent;
import org.eclipse.che.ide.api.event.PersistProjectTreeStateEvent;
import org.eclipse.che.ide.api.event.ProjectActionEvent;
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
import org.eclipse.che.ide.menu.ContextMenu;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.Unmarshallable;
import org.eclipse.che.ide.ui.tree.SelectionModel;
import org.eclipse.che.ide.util.loging.Log;
import org.vectomatic.dom.svg.ui.SVGResource;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.eclipse.che.ide.api.event.ItemEvent.ItemOperation.CREATED;
import static org.eclipse.che.ide.api.event.ItemEvent.ItemOperation.DELETED;

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
                                                                           ExtServerStateHandler,
                                                                           ProjectActionHandler,
                                                                           ProjectExplorerPart,
                                                                           RenameNodeEventHandler,
                                                                           ItemHandler,
                                                                           NodeChangedHandler,
                                                                           HasView {
    private static final int ONE_SEC = 1_000;

    private final ProjectExplorerView           view;
    private final EventBus                      eventBus;
    private final ContextMenu                   contextMenu;
    private final ProjectServiceClient          projectServiceClient;
    private final CoreLocalizationConstant      coreLocalizationConstant;
    private final AppContext                    appContext;
    private final TreeStructureProviderRegistry treeStructureProviderRegistry;
    private final DeleteNodeHandler             deleteNodeHandler;
    private final Provider<EditorAgent>         editorAgentProvider;
    private final DtoUnmarshallerFactory        dtoUnmarshallerFactory;

    private TreeStructure     currentTreeStructure;
    private List<TreeNode<?>> nodesToRefresh;

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
                                        DtoUnmarshallerFactory dtoUnmarshallerFactory,
                                        Provider<EditorAgent> editorAgentProvider) {
        this.view = view;
        this.view.setDelegate(this);
        this.view.setTitle(coreLocalizationConstant.projectExplorerTitleBarText());

        this.eventBus = eventBus;
        this.contextMenu = contextMenu;
        this.projectServiceClient = projectServiceClient;
        this.coreLocalizationConstant = coreLocalizationConstant;
        this.appContext = appContext;
        this.treeStructureProviderRegistry = treeStructureProviderRegistry;
        this.deleteNodeHandler = deleteNodeHandler;
        this.editorAgentProvider = editorAgentProvider;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;

        eventBus.addHandler(NodeChangedEvent.TYPE, this);
        eventBus.addHandler(ItemEvent.TYPE, this);
        eventBus.addHandler(RenameNodeEvent.TYPE, this);
        eventBus.addHandler(RefreshProjectTreeEvent.TYPE, this);
        eventBus.addHandler(ProjectActionEvent.TYPE, this);
        eventBus.addHandler(ExtServerStateEvent.TYPE, this);
    }

    /** {@inheritDoc} */
    @Override
    public void onExtServerStopped(ExtServerStateEvent event) {
        view.showEmptyTree();
    }


    /** {@inheritDoc} */
    @Override
    public void onExtServerStarted(ExtServerStateEvent event) {
        showAllProjects();
    }

    private void showAllProjects() {
        Unmarshallable<List<ProjectReference>> unmarshaller = dtoUnmarshallerFactory.newListUnmarshaller(ProjectReference.class);
        projectServiceClient.getProjects(new AsyncRequestCallback<List<ProjectReference>>(unmarshaller) {
            @Override
            protected void onSuccess(List<ProjectReference> result) {
                if (result.isEmpty()) {
                    view.showEmptyTree();
                }

                for (ProjectReference reference : result) {
                    openProject(reference);
                }
            }

            @Override
            protected void onFailure(Throwable exception) {
                Log.error(ProjectExplorerPartPresenter.class, exception);
            }
        });
    }

    private void openProject(@Nonnull ProjectReference reference) {
        Unmarshallable<ProjectDescriptor> unmarshaller = dtoUnmarshallerFactory.newUnmarshaller(ProjectDescriptor.class);
        projectServiceClient.getProject(reference.getName(), new AsyncRequestCallback<ProjectDescriptor>(unmarshaller) {
            @Override
            protected void onSuccess(final ProjectDescriptor project) {
                //this timer need to wait some time when previous project opening and refreshing
                Timer timer = new Timer() {
                    @Override
                    public void run() {
                        addProject(project);

                        eventBus.fireEvent(new RestoreProjectTreeStateEvent(project.getPath()));
                    }
                };

                timer.schedule(ONE_SEC);

            }

            @Override
            protected void onFailure(Throwable throwable) {
                Log.error(ProjectExplorerPartPresenter.class, throwable);
            }
        });
    }

    private void addProject(@Nonnull ProjectDescriptor descriptor) {
        appContext.addOpenedProject(descriptor);

        TreeStructure treeStructure = treeStructureProviderRegistry.getTreeStructureProvider(descriptor.getType()).get();

        CurrentProject currentProject = new CurrentProject(descriptor);

        currentProject.setCurrentTree(treeStructure);
        appContext.setCurrentProject(currentProject);

        addProjectToTree(treeStructure);
    }

    private void addProjectToTree(@Nonnull final TreeStructure treeStructure) {
        currentTreeStructure = treeStructure;

        treeStructure.getRootNodes(new AsyncCallback<List<TreeNode<?>>>() {
            @Override
            public void onSuccess(List<TreeNode<?>> result) {
                for (final TreeNode<?> node : result) {
                    view.addNodeToTree(node);

                    view.expandAndSelectNode(node);

                    updateNode(node);
                }
            }

            @Override
            public void onFailure(Throwable caught) {
                Log.error(ProjectExplorerPartPresenter.class, "Can't get root nodes" + caught);
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void onProjectCreated(ProjectActionEvent event) {
        addProject(event.getProject());
    }

    /** {@inheritDoc} */
    @Override
    public void onProjectDeleted(ProjectActionEvent event) {
        appContext.removeOpenedProject(event.getProjectName());

        showAllProjects();
    }

    /** {@inheritDoc} */
    @Override
    public void onNodeRenamed(NodeChangedEvent event) {
        if (appContext.getCurrentProject() != null) {
            updateNode(event.getNode().getParent());
            view.selectNode(event.getNode());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onItem(ItemEvent event) {
        TreeNode<?> currentNode = event.getItem();
        TreeNode<?> parentNode = currentNode.getParent();

        if (DELETED == event.getOperation()) {
            eventBus.fireEvent(new PersistProjectTreeStateEvent());

            refreshAndSelectNode(parentNode);
        } else if (CREATED == event.getOperation()) {
            nodesToRefresh = view.getOpenedTreeNodes();
            refreshTreeNodes();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onNodeRenamed(TreeNode<?> parentNode, String newParentNodePath) {
        if (parentNode instanceof UpdateTreeNodeDataIterable) {
            final StorableNode parent = (StorableNode)parentNode;
            List<TreeNode<?>> children = new ArrayList<>();
            children.add(parent);
            getAllChildren(parentNode, children);
            getOpenedUnCashedFiles(parent, children);
            final Iterator<TreeNode<?>> treeNodeIterator = children.iterator();

            updateDataTreeNode(parent, newParentNodePath, treeNodeIterator);
        }
    }

    private void getAllChildren(TreeNode<?> parent, List<TreeNode<?>> children) {
        List<TreeNode<?>> childrenForCurrentDeep = parent.getChildren();

        if (childrenForCurrentDeep.isEmpty()) {
            return;
        }

        children.addAll(childrenForCurrentDeep);

        for (TreeNode<?> node : childrenForCurrentDeep) {
            getAllChildren(node, children);
        }
    }

    private void getOpenedUnCashedFiles(StorableNode parentNode, List<TreeNode<?>> children) {
        Map<String, EditorPartPresenter> editorParts = editorAgentProvider.get().getOpenedEditors();
        for (EditorPartPresenter editorPart : editorParts.values()) {
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

        if (event.refreshSubtree()) {
            nodesToRefresh = view.getOpenedTreeNodes();
            refreshTreeNodes();
            return;
        }

        if (event.getNode() != null) {
            refreshAndSelectNode(event.getNode());
            return;
        }

        currentTreeStructure.getRootNodes(new AsyncCallback<List<TreeNode<?>>>() {
            @Override
            public void onSuccess(List<TreeNode<?>> result) {
                for (TreeNode<?> childNode : result) {
                    // clear children in order to force to refresh
                    childNode.setChildren(new ArrayList<TreeNode<?>>());
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
        final List<?> allSelected = model.getSelectedNodes();
        final List<Object> newSelection = new ArrayList<>();
        for (final Object item : allSelected) {
            newSelection.add(item);
        }
        if (newSelection.contains(node)) {
            setSelection(new Selection<>(newSelection, node));
        } else {
            setSelection(new Selection<>(newSelection));
        }

        CurrentProject currentProject = appContext.getCurrentProject();

        if (node != null && node instanceof StorableNode && currentProject != null) {
            ProjectDescriptor descriptor = node.getProject().getData();

            currentProject.setProjectDescription(descriptor);

            currentTreeStructure = currentProject.getCurrentTree();

            view.setProjectHeader(descriptor);
        }
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
        for (Object node : view.getSelectedNodes()) {
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
