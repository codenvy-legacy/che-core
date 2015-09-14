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

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.callback.AsyncPromiseHelper;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.Resources;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.parts.base.BaseView;
import org.eclipse.che.ide.api.parts.base.ToolButton;
import org.eclipse.che.ide.api.project.node.HasAction;
import org.eclipse.che.ide.api.project.node.HasDataObject;
import org.eclipse.che.ide.api.project.node.HasProjectDescriptor;
import org.eclipse.che.ide.api.project.node.HasStorablePath;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.api.project.node.interceptor.NodeInterceptor;
import org.eclipse.che.ide.api.project.node.settings.HasSettings;
import org.eclipse.che.ide.menu.ContextMenu;
import org.eclipse.che.ide.project.node.NodeManager;
import org.eclipse.che.ide.project.node.ProjectDescriptorNode;
import org.eclipse.che.ide.project.node.SyntheticBasedNode;
import org.eclipse.che.ide.ui.Tooltip;
import org.eclipse.che.ide.ui.smartTree.LoadParams;
import org.eclipse.che.ide.ui.smartTree.NodeDescriptor;
import org.eclipse.che.ide.ui.smartTree.NodeNameConverter;
import org.eclipse.che.ide.ui.smartTree.NodeUniqueKeyProvider;
import org.eclipse.che.ide.ui.smartTree.SortDir;
import org.eclipse.che.ide.ui.smartTree.SpeedSearch;
import org.eclipse.che.ide.ui.smartTree.Tree;
import org.eclipse.che.ide.ui.smartTree.TreeNodeLoader;
import org.eclipse.che.ide.ui.smartTree.TreeNodeStorage;
import org.eclipse.che.ide.ui.smartTree.TreeNodeStorage.StoreSortInfo;
import org.eclipse.che.ide.ui.smartTree.TreeStyles;
import org.eclipse.che.ide.ui.smartTree.event.BeforeExpandNodeEvent;
import org.eclipse.che.ide.ui.smartTree.event.BeforeExpandNodeEvent.BeforeExpandNodeHandler;
import org.eclipse.che.ide.ui.smartTree.event.ExpandNodeEvent;
import org.eclipse.che.ide.ui.smartTree.event.ExpandNodeEvent.ExpandNodeHandler;
import org.eclipse.che.ide.ui.smartTree.event.GoIntoStateEvent;
import org.eclipse.che.ide.ui.smartTree.event.GoIntoStateEvent.GoIntoStateHandler;
import org.eclipse.che.ide.ui.smartTree.event.SelectionChangedEvent;
import org.eclipse.che.ide.ui.smartTree.event.SelectionChangedEvent.SelectionChangedHandler;
import org.eclipse.che.ide.ui.smartTree.presentation.DefaultPresentationRenderer;
import org.eclipse.che.ide.ui.smartTree.sorting.AlphabeticalFilter;
import org.eclipse.che.ide.util.loging.Log;
import org.vectomatic.dom.svg.ui.SVGImage;
import org.vectomatic.dom.svg.ui.SVGResource;

import javax.validation.constraints.NotNull;
import org.eclipse.che.commons.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import static org.eclipse.che.ide.ui.menu.PositionController.HorizontalAlign.MIDDLE;
import static org.eclipse.che.ide.ui.menu.PositionController.VerticalAlign.BOTTOM;
import static org.eclipse.che.ide.ui.smartTree.event.GoIntoStateEvent.State.ACTIVATED;
import static org.eclipse.che.ide.ui.smartTree.event.GoIntoStateEvent.State.DEACTIVATED;

/**
 * @author Vlad Zhukovskiy
 */
@Singleton
public class NewProjectExplorerViewImpl extends BaseView<NewProjectExplorerView.ActionDelegate> implements NewProjectExplorerView,
                                                                                                           GoIntoStateHandler {
    private Resources                resources;
    private ProjectExplorerResources explorerResources;
    private NodeManager              nodeManager;
    private AppContext               appContext;
    private Tree                     tree;
    private FlowPanel                projectHeader;
    private ToolButton               goIntoBackButton;

    private StoreSortInfo foldersOnTopSort = new StoreSortInfo(new FoldersOnTopFilter(), SortDir.ASC);

    private HandlerRegistration navigateBeforeExpandHandler = null;
    private HandlerRegistration navigateExpandHandler       = null;

    private GoIntoHandler goIntoHandler;
    private boolean goIntoOnceExecuted = false;
    private String  contentRoot        = null;

    @Inject
    public NewProjectExplorerViewImpl(Resources resources,
                                      ProjectExplorerResources explorerResources,
                                      final ContextMenu contextMenu,
                                      CoreLocalizationConstant coreLocalizationConstant,
                                      Set<NodeInterceptor> nodeInterceptorSet,
                                      NodeManager nodeManager,
                                      AppContext appContext) {
        super(resources);
        this.resources = resources;
        this.explorerResources = explorerResources;
        this.nodeManager = nodeManager;
        this.appContext = appContext;

        setTitle(coreLocalizationConstant.projectExplorerTitleBarText());

        projectHeader = new FlowPanel();
        projectHeader.setStyleName(resources.partStackCss().idePartStackToolbarBottom());

        TreeNodeStorage nodeStorage = new TreeNodeStorage(new NodeUniqueKeyProvider() {
            @NotNull
            @Override
            public String getKey(@NotNull Node item) {
                if (item instanceof HasStorablePath) {
                    return ((HasStorablePath)item).getStorablePath();
                } else {
                    return String.valueOf(item.hashCode());
                }
            }
        });

        TreeNodeLoader nodeLoader = new TreeNodeLoader(nodeInterceptorSet);

        tree = new Tree(nodeStorage, nodeLoader);
        tree.setContextMenuInvocationHandler(new Tree.ContextMenuInvocationHandler() {
            @Override
            public void invokeContextMenuOn(int x, int y) {
                contextMenu.show(x, y);
            }
        });
        tree.getNodeStorage().add(Collections.<Node>emptyList());


        StoreSortInfo alphabetical = new StoreSortInfo(new AlphabeticalFilter(), SortDir.ASC);
        tree.getNodeStorage().addSortInfo(foldersOnTopSort);
        tree.getNodeStorage().addSortInfo(alphabetical);

        tree.getSelectionModel().addSelectionChangedHandler(new SelectionChangedHandler() {
            @Override
            public void onSelectionChanged(SelectionChangedEvent event) {
                delegate.onSelectionChanged(event.getSelection());
            }
        });

        tree.getGoIntoMode().addGoIntoHandler(this);

        tree.setNodePresentationRenderer(new ProjectExplorerRenderer(tree.getTreeStyles()));
        tree.ensureDebugId("projectTree");
        tree.setAutoSelect(true);

        new SpeedSearch(tree, new NodeNameConverter());

        ScrollPanel panel = new ScrollPanel(tree);
        panel.ensureDebugId("projectExplorer");

        setContentWidget(panel);

        tree.addDomHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if (navigateBeforeExpandHandler != null) {
                    navigateBeforeExpandHandler.removeHandler();
                }
                if (navigateExpandHandler != null) {
                    navigateExpandHandler.removeHandler();
                }
            }
        }, ClickEvent.getType());

        goIntoHandler = new GoIntoHandler();
        tree.addExpandHandler(goIntoHandler);
    }

    @Override
    public void setRootNodes(List<Node> nodes) {
        hideProjectInfo();

        if (nodes == null || nodes.isEmpty()) {
            tree.getNodeStorage().clear();
            return;
        }

        tree.getNodeStorage().replaceChildren(null, nodes);

        if (nodes.size() == 1) {
            contentRoot = getContentRootOrNull(nodes.get(0));
            tree.setExpanded(nodes.get(0), true);
        }

        showProjectInfo();
    }

    @Nullable
    private String getContentRootOrNull(@NotNull Node node) {
        if (node instanceof ProjectDescriptorNode && isValidContentRoot((HasProjectDescriptor)node)) {
            ProjectDescriptor descriptor = ((HasProjectDescriptor)node).getProjectDescriptor();
            String rawContentRoot = descriptor.getContentRoot();

            return descriptor.getPath() + (rawContentRoot.startsWith("/") ? rawContentRoot : "/" + rawContentRoot);
        }

        return null;
    }

    private boolean isValidContentRoot(@NotNull HasProjectDescriptor node) {
        return !Strings.isNullOrEmpty(node.getProjectDescriptor().getContentRoot()); //TODO maybe add more checks
    }

    @Override
    public List<StoreSortInfo> getSortInfo() {
        return tree.getNodeStorage().getSortInfo();
    }

    @Override
    public void onApplySort() {
        //TODO need to improve this block of code to allow automatically save expand state before applying sorting
        tree.getExpandStateHandler().saveState();
        tree.getNodeStorage().applySort(false);
        tree.getExpandStateHandler().loadState();
    }

    @Override
    public void scrollFromSource(Object object) {
        for (NodeDescriptor nodeDescriptor : tree.getNodeStorage().getStoredNodes()) {
            Node storedNode = nodeDescriptor.getNode();
            if (storedNode.equals(object) ||
                (storedNode instanceof HasDataObject<?> && ((HasDataObject)storedNode).getData().equals(object))) {
                tree.setExpanded(storedNode, true);
                tree.getSelectionModel().select(storedNode, false);
                return;
            }
        }
    }

    public void clear() {
        tree.clear();
    }

    @Override
    public void setRootNode(Node node) {
        setRootNodes(Collections.singletonList(node));
    }

    private void showProjectInfo() {
        if (toolBar.getWidgetIndex(projectHeader) < 0) {
            toolBar.addSouth(projectHeader, 28);
            setToolbarHeight(50);
        }
        projectHeader.clear();

        List<Node> rootItems = tree.getNodeStorage().getRootItems();

        for (Node rootNode : rootItems) {
            if (rootNode instanceof ProjectDescriptorNode) {
                ProjectDescriptor descriptor = ((ProjectDescriptorNode)rootNode).getProjectDescriptor();

                if (descriptor == null) {
                    continue;
                }

                FlowPanel delimiter = new FlowPanel();
                delimiter.setStyleName(resources.partStackCss().idePartStackToolbarSeparator());
                projectHeader.add(delimiter);

                SVGImage projectVisibilityImage = new SVGImage("private".equals(descriptor.getVisibility()) ? resources.privateProject()
                                                                                                            : resources.publicProject());
                projectVisibilityImage.getElement().setAttribute("class", resources.partStackCss().idePartStackToolbarBottomIcon());

                projectHeader.add(projectVisibilityImage);

                InlineLabel projectTitle = new InlineLabel(descriptor.getName());
                projectHeader.add(projectTitle);

                SVGImage collapseAllIcon = new SVGImage(explorerResources.collapse());
                ToolButton collapseAll = new ToolButton(collapseAllIcon);
                collapseAll.addClickHandler(new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        if (tree.getGoIntoMode().isActivated()) {
                            Node lastNode = tree.getGoIntoMode().getLastNode();
                            tree.setExpanded(lastNode, false, true);
                            return;
                        }

                        tree.collapseAll();
                    }
                });
                Tooltip.create((elemental.dom.Element)collapseAll.getElement(),
                               BOTTOM,
                               MIDDLE,
                               "Collapse All");
                collapseAll.ensureDebugId("collapseAllButton");
                addMenuButton(collapseAll);

                FlowPanel refreshButton = new FlowPanel();
                refreshButton.add(new SVGImage(resources.refresh()));
                refreshButton.setStyleName(resources.partStackCss().idePartStackToolbarBottomButton());
                refreshButton.addStyleName(resources.partStackCss().idePartStackToolbarBottomButtonRight());
                refreshButton.ensureDebugId("refreshSelectedFolder");
                projectHeader.add(refreshButton);

                refreshButton.addDomHandler(new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        delegate.reloadSelectedNodes();
                    }
                }, ClickEvent.getType());

                Tooltip.create((elemental.dom.Element)refreshButton.getElement(),
                               BOTTOM,
                               MIDDLE,
                               "Refresh selected folder");

                return;
            }
        }

        hideProjectInfo();
    }

    private void hideProjectInfo() {
        goIntoBackButton = null;
        menuPanel.clear();
        toolBar.remove(projectHeader);
        setToolbarHeight(22);
        contentRoot = null;
        goIntoHandler.resetHandler();
    }

    private boolean isNodeHasSameDataObject(Node node, HasDataObject<?> dataNode) {
        return node != null
               && dataNode != null
               && node instanceof HasDataObject<?>
               && ((HasDataObject)node).getData().equals(dataNode.getData());
    }

    public void reloadChildren(Node parent, final HasDataObject<?> selectAfter, final boolean actionPerformed, final boolean goInto) {
        if (appContext.getCurrentProject() == null) {
            //project doesn't opened, so reload project list
            if (parent != null) {
                Log.info(this.getClass(), "Project isn't opened, so node to reload will be ignored.");
            }

            if (goInto) {
                throw new IllegalArgumentException("Go Into doesn't support for Project Reference node.");
            }

            nodeManager.getProjects().then(new Operation<List<Node>>() {
                @Override
                public void apply(List<Node> projects) throws OperationException {
                    setRootNodes(projects);

                    for (Node projectNode : projects) {
                        if (isNodeHasSameDataObject(projectNode, selectAfter)) {
                            tree.getSelectionModel().select(projectNode, false);

                            if (actionPerformed && projectNode instanceof HasAction) {
                                ((HasAction)projectNode).actionPerformed();
                            }

                            return;
                        }
                    }
                }
            });

            return;
        }

        TreeNodeLoader loader = tree.getNodeLoader();

        if (parent != null) {
            LoadParams params = new LoadParams(selectAfter, actionPerformed, goInto);

            if (!loader.loadChildren(parent, params)) {
                Log.info(this.getClass(), "Node has been already requested for loading children");
            }
        } else {

            if (selectAfter != null) {
                Log.info(this.getClass(), "Data object selection will be ignored during reload root nodes.");
            }

            if (actionPerformed) {
                Log.info(this.getClass(), "Action performing will be ignored during reload root nodes.");
            }

            if (goInto) {
                Log.info(this.getClass(), "Go into performing will be ignored during reload root nodes.");
            }

            List<Node> rootNodes = tree.getRootNodes();
            for (Node root : rootNodes) {
                if (!loader.loadChildren(root)) {
                    Log.info(this.getClass(), "Node has been already requested for loading children");
                }
            }
        }
    }

    @Override
    public void reloadChildrenByType(Class<?> type) {
        List<Node> rootNodes = tree.getRootNodes();
        for (Node rootNode : rootNodes) {
            List<Node> allChildren = tree.getNodeStorage().getAllChildren(rootNode);
            for (Node child : allChildren) {
                if (child.getClass().equals(type)) {
                    NodeDescriptor nodeDescriptor = tree.findNode(child);
                    if (nodeDescriptor.isLoaded()) {
                        tree.getNodeLoader().loadChildren(child);
                    }
                }
            }
        }
    }

    @Override
    public Promise<Node> navigate(final HasStorablePath node, final boolean select) {
        return AsyncPromiseHelper.createFromAsyncRequest(new AsyncPromiseHelper.RequestCall<Node>() {
            @Override
            public void makeCall(AsyncCallback<Node> callback) {
                navigate(node, select, false, callback);
            }
        });
    }

    protected void navigate(final HasStorablePath node, final boolean select, final boolean callAction,
                            final AsyncCallback<Node> callback) {
        if (navigateBeforeExpandHandler != null) {
            navigateBeforeExpandHandler.removeHandler();
        }
        if (navigateExpandHandler != null) {
            navigateExpandHandler.removeHandler();
        }

        List<Node> rootNodes = tree.getRootNodes();

        //first get all rendered node, maybe we already have this node in the list
        for (Node rootNode : rootNodes) {
            List<Node> allChildren = tree.getNodeStorage().getAllChildren(rootNode);

            Node seekNode = null;

            try {
                seekNode = Iterables.find(allChildren, new Predicate<Node>() {
                    @Override
                    public boolean apply(@Nullable Node possible) {
                        return possible instanceof HasStorablePath
                               && ((HasStorablePath)possible).getStorablePath().equals(node.getStorablePath());
                    }
                });
            } catch (NoSuchElementException e) {
                Log.info(this.getClass(), "Node doesn't found, continue searching");
            }

            if (seekNode != null) {
                Node parent = seekNode.getParent();
                if (!tree.isExpanded(parent)) {
                    tree.setExpanded(parent, true);
                }

                if (select) {
                    tree.getSelectionModel().select(seekNode, false);
                    tree.scrollIntoView(seekNode);
                }

                if (seekNode instanceof HasAction && callAction) {
                    ((HasAction)seekNode).actionPerformed();
                }

                if (callback != null) {
                    callback.onSuccess(seekNode);
                }
                return;
            }
        }

        //so, we don't have rendered node yet, iterate through root nodes and search parent root node
        Node preProcessedNode = null;
        for (Node rootNode : rootNodes) {
            if (rootNode instanceof HasStorablePath && node.getStorablePath().startsWith(((HasStorablePath)rootNode).getStorablePath())) {
                preProcessedNode = rootNode;
                break;
            }
        }

        if (preProcessedNode == null) {
            //we have nothing to do, just exit
            return;
        }

        BeforeExpandNodeHandler beforeExpandNodeHandler = new BeforeExpandNodeHandler() {
            @Override
            public void onBeforeExpand(BeforeExpandNodeEvent event) {
                Node eventNode = event.getNode();

                if (!(eventNode instanceof HasStorablePath)) {
                    event.setCancelled(true);
                    return;
                }

                HasStorablePath path = (HasStorablePath)eventNode;

                if (!node.getStorablePath().startsWith(path.getStorablePath())) {
                    event.setCancelled(true);
                }
            }
        };

        ExpandNodeHandler expandNodeHandler = new ExpandNodeHandler() {
            @Override
            public void onExpand(ExpandNodeEvent event) {
                final Node eventNode = event.getNode();
                List<Node> children = tree.getNodeStorage().getChildren(eventNode);
                for (Node child : children) {
                    if (child instanceof HasStorablePath &&
                        ((HasStorablePath)child).getStorablePath().equals(node.getStorablePath())) {
                        if (select) {
                            tree.getSelectionModel().select(child, false);
                            tree.scrollIntoView(child);
                        }

                        if (child instanceof HasAction) {
                            ((HasAction)child).actionPerformed();
                        }

                        if (callback != null) {
                            callback.onSuccess(child);
                        }

                        return;
                    }
                }
            }
        };

        navigateBeforeExpandHandler = tree.addBeforeExpandHandler(beforeExpandNodeHandler);
        navigateExpandHandler = tree.addExpandHandler(expandNodeHandler);

        tree.setExpanded(preProcessedNode, true, true);
    }

    @Override
    public void navigate(final HasStorablePath node, final boolean select, final boolean callAction) {
        navigate(node, select, callAction, null);
    }


    public List<Node> getVisibleNodes() {
        return tree.getAllChildNodes(tree.getRootNodes(), true);
    }

    @Override
    public void showHiddenFiles(boolean show) {
        for (Node node : tree.getRootNodes()) {
            if (node instanceof HasSettings) {
                ((HasSettings)node).getSettings().setShowHiddenFiles(show);
            }
        }

        reloadChildren(null, null, false, false);
    }

    @Override
    public boolean isShowHiddenFiles() {
        for (Node node : tree.getRootNodes()) {
            if (node instanceof HasSettings) {
                return ((HasSettings)node).getSettings().isShowHiddenFiles();
            }
        }
        return false;
    }

    @Override
    public boolean setGoIntoModeOn(Node node) {
        return tree.getGoIntoMode().goInto(node);
    }

    @Override
    public void addGoIntoStateHandler(GoIntoStateHandler handler) {
        tree.getGoIntoMode().addGoIntoHandler(handler);
    }

    @Override
    public void resetGoIntoMode() {
        if (tree.getGoIntoMode().isActivated()) {
            tree.getGoIntoMode().reset();
        }
    }

    @Override
    public boolean isGoIntoActivated() {
        return tree.getGoIntoMode().isActivated();
    }

    @Override
    public void onGoIntoStateChanged(GoIntoStateEvent event) {
        if (event.getState() == ACTIVATED) {
            //lazy button initializing
            if (goIntoBackButton == null) {
                initGoIntoBackButton();
                return;
            }

            goIntoBackButton.setVisible(true);

        } else if (event.getState() == DEACTIVATED) {
            goIntoBackButton.setVisible(false);
        }
    }

    private void initGoIntoBackButton() {
        SVGImage upwardArrowIcon = new SVGImage(explorerResources.up());
        goIntoBackButton = new ToolButton(upwardArrowIcon);
        goIntoBackButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                tree.getGoIntoMode().reset();
            }
        });
        Tooltip.create((elemental.dom.Element)goIntoBackButton.getElement(),
                       BOTTOM,
                       MIDDLE,
                       "Go Back");
        addMenuButton(goIntoBackButton);
    }

    @Override
    public boolean isFoldersAlwaysOnTop() {
        return tree.getNodeStorage().getSortInfo().contains(foldersOnTopSort);
    }

    @Override
    public void setFoldersAlwaysOnTop(boolean foldersAlwaysOnTop) {
        if (isFoldersAlwaysOnTop() != foldersAlwaysOnTop) {
            if (foldersAlwaysOnTop) {
                tree.getNodeStorage().addSortInfo(foldersOnTopSort);
            } else {
                tree.getNodeStorage().getSortInfo().remove(foldersOnTopSort);
            }
        }
    }

    @Override
    public void expandAll() {
        tree.expandAll();
    }

    @Override
    public void collapseAll() {
        tree.collapseAll();
    }

    public class GoIntoHandler implements ExpandNodeHandler {
        @Override
        public void onExpand(ExpandNodeEvent event) {
            if (Strings.isNullOrEmpty(contentRoot) || tree.getGoIntoMode().isActivated() || goIntoOnceExecuted) {
                return;
            }

            Node expandedNode = event.getNode();

            if (!(expandedNode instanceof HasStorablePath)) {
                return;
            }

            if (contentRoot.equals(((HasStorablePath)expandedNode).getStorablePath())) {
                goIntoOnceExecuted = true;
                tree.getGoIntoMode().goInto(event.getNode());
                return;
            }

            List<Node> children = tree.getNodeStorage().getChildren(expandedNode);

            for (Node child : children) {
                if (!(child.supportGoInto() || child instanceof HasStorablePath)) {
                    continue;
                }

                final String path = ((HasStorablePath)child).getStorablePath();
                if (contentRoot.startsWith(path)) {
                    tree.setExpanded(child, true);
                    break;
                }
            }
        }

        public void resetHandler() {
            goIntoOnceExecuted = false;
        }
    }

    public interface ProjectExplorerResources extends ClientBundle {
        @Source("gear.svg")
        SVGResource gear();

        @Source("source.svg")
        SVGResource source();

        @Source("upwardArrow.svg")
        SVGResource up();

        @Source("collapse.svg")
        SVGResource collapse();
    }

    private class ProjectExplorerRenderer extends DefaultPresentationRenderer<Node> {

        public ProjectExplorerRenderer(TreeStyles treeStyles) {
            super(treeStyles);
        }

        @Override
        public Element render(Node node, String domID, Tree.Joint joint, int depth) {
            Element element = super.render(node, domID, joint, depth);

            element.setAttribute("name", node.getName());

            if (node instanceof HasStorablePath) {
                element.setAttribute("path", ((HasStorablePath)node).getStorablePath());
            }

            if (node instanceof HasAction) {
                element.setAttribute("actionable", "true");
            }

            if (node instanceof HasProjectDescriptor) {
                element.setAttribute("project", ((HasProjectDescriptor)node).getProjectDescriptor().getPath());
            }

            if (node instanceof SyntheticBasedNode<?>) {
                element.setAttribute("synthetic", "true");
            }

            return element;
        }
    }
}
