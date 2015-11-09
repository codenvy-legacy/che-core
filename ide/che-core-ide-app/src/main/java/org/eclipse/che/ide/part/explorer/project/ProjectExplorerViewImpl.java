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

import com.google.common.annotations.Beta;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.ContextMenuEvent;
import com.google.gwt.event.dom.client.ContextMenuHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.Resources;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.event.ActivePartChangedEvent;
import org.eclipse.che.ide.api.event.ActivePartChangedHandler;
import org.eclipse.che.ide.api.event.FileEvent;
import org.eclipse.che.ide.api.parts.base.BaseView;
import org.eclipse.che.ide.api.parts.base.ToolButton;
import org.eclipse.che.ide.api.project.node.HasAction;
import org.eclipse.che.ide.api.project.node.HasProjectDescriptor;
import org.eclipse.che.ide.api.project.node.HasStorablePath;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.api.project.node.interceptor.NodeInterceptor;
import org.eclipse.che.ide.api.project.node.settings.HasSettings;
import org.eclipse.che.ide.api.project.tree.VirtualFile;
import org.eclipse.che.ide.menu.ContextMenu;
import org.eclipse.che.ide.project.node.NodeManager;
import org.eclipse.che.ide.project.node.ProjectDescriptorNode;
import org.eclipse.che.ide.project.node.SyntheticBasedNode;
import org.eclipse.che.ide.ui.Tooltip;
import org.eclipse.che.ide.ui.smartTree.KeyboardNavigationHandler;
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
import org.eclipse.che.ide.ui.smartTree.UniqueKeyProvider;
import org.eclipse.che.ide.ui.smartTree.event.CollapseNodeEvent;
import org.eclipse.che.ide.ui.smartTree.event.ExpandNodeEvent;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.eclipse.che.ide.api.event.FileEvent.FileOperation.CLOSE;
import static org.eclipse.che.ide.ui.menu.PositionController.HorizontalAlign.MIDDLE;
import static org.eclipse.che.ide.ui.menu.PositionController.VerticalAlign.BOTTOM;
import static org.eclipse.che.ide.ui.smartTree.event.GoIntoStateEvent.State.ACTIVATED;
import static org.eclipse.che.ide.ui.smartTree.event.GoIntoStateEvent.State.DEACTIVATED;

/**
 * Implementation of the {@link ProjectExplorerView}.
 *
 * @author Vlad Zhukovskiy
 */
@Singleton
public class ProjectExplorerViewImpl extends BaseView<ProjectExplorerView.ActionDelegate> implements ProjectExplorerView,
                                                                                                     GoIntoStateHandler {
    private final Resources                resources;
    private final ProjectExplorerResources explorerResources;
    private final NodeManager              nodeManager;
    private final AppContext               appContext;
    private final Provider<EditorAgent>    editorAgentProvider;
    private final EventBus                 eventBus;
    private final Tree                     tree;
    private StoreSortInfo foldersOnTopSort = new StoreSortInfo(new FoldersOnTopFilter(), SortDir.ASC);

    private ToolButton              goBackButton;
    private ToolButton              scrollFromSourceButton;
    private ToolButton              refreshButton;
    private ToolButton              collapseAllButton;
    private SearchNodeHandler       searchNodeHandler;
    private UniqueKeyProvider<Node> nodeIdProvider;

    public static final String GO_BACK_BUTTON_ID            = "goBackButton";
    public static final String SCROLL_FROM_SOURCE_BUTTON_ID = "scrollFromSourceButton";
    public static final String REFRESH_BUTTON_ID            = "refreshSelectedFolder";
    public static final String COLLAPSE_ALL_BUTTON_ID       = "collapseAllButton";
    public static final String PROJECT_TREE_WIDGET_ID       = "projectTree";
    public static final String PROJECT_EXPLORER_ID          = "projectExplorer";

    @Inject
    public ProjectExplorerViewImpl(final Resources resources,
                                   final ProjectExplorerResources explorerResources,
                                   final ContextMenu contextMenu,
                                   final CoreLocalizationConstant coreLocalizationConstant,
                                   final Set<NodeInterceptor> nodeInterceptorSet,
                                   final NodeManager nodeManager,
                                   final AppContext appContext,
                                   final Provider<EditorAgent> editorAgentProvider,
                                   final EventBus eventBus) {
        super(resources);
        this.resources = resources;
        this.explorerResources = explorerResources;
        this.nodeManager = nodeManager;
        this.appContext = appContext;
        this.editorAgentProvider = editorAgentProvider;
        this.eventBus = eventBus;

        setTitle(coreLocalizationConstant.projectExplorerTitleBarText());

        nodeIdProvider = new NodeUniqueKeyProvider() {
            @NotNull
            @Override
            public String getKey(@NotNull Node item) {
                if (item instanceof HasStorablePath) {
                    return ((HasStorablePath)item).getStorablePath();
                } else {
                    return String.valueOf(item.hashCode());
                }
            }
        };

        TreeNodeStorage nodeStorage = new TreeNodeStorage(nodeIdProvider);

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
        tree.ensureDebugId(PROJECT_TREE_WIDGET_ID);
        tree.setAutoSelect(true);

        ScrollPanel treePanel = new ScrollPanel(tree);
        treePanel.ensureDebugId(PROJECT_EXPLORER_ID);
        setContentWidget(treePanel);

        //hook that allow pass right click outer the widget
        treePanel.addDomHandler(new ContextMenuHandler() {
            @Override
            public void onContextMenu(ContextMenuEvent event) {
                event.preventDefault();
                tree.onBrowserEvent((Event)event.getNativeEvent());
            }
        }, ContextMenuEvent.getType());

        bindExternalNavigationHandler();
        bindScrollFromSourceButtonHandlers();

        searchNodeHandler = new SearchNodeHandler(tree);
    }

    private void bindExternalNavigationHandler() {
        KeyboardNavigationHandler extHandler = new KeyboardNavigationHandler() {
            @Override
            public void onDelete(NativeEvent evt) {
                delegate.onDeleteKeyPressed();
            }
        };

        extHandler.bind(tree);
    }

    private void bindScrollFromSourceButtonHandlers() {
        eventBus.addHandler(ActivePartChangedEvent.TYPE, new ActivePartChangedHandler() {
            @Override
            public void onActivePartChanged(ActivePartChangedEvent event) {
                if (event.getActivePart() instanceof EditorPartPresenter) {
                    if (scrollFromSourceButton == null) {
                        scrollFromSourceButton = new ToolButton(new SVGImage(explorerResources.source()));
                        scrollFromSourceButton.addClickHandler(new ClickHandler() {
                            @Override
                            public void onClick(ClickEvent event) {
                                final String sourcePath = editorAgentProvider.get().getActiveEditor().getEditorInput().getFile().getPath();
                                scrollFromSource(new HasStorablePath.StorablePath(sourcePath));
                            }
                        });
                        scrollFromSourceButton.ensureDebugId(SCROLL_FROM_SOURCE_BUTTON_ID);
                        Tooltip.create((elemental.dom.Element)scrollFromSourceButton.getElement(),
                                       BOTTOM,
                                       MIDDLE,
                                       "Scroll From Source");
                        addToolButton(scrollFromSourceButton);
                    }

                    scrollFromSourceButton.setVisible(true);

                    EditorPartPresenter activeEditor = editorAgentProvider.get().getActiveEditor();

                    if (activeEditor == null) {
                        return;
                    }

                    activeEditor.addCloseHandler(new EditorPartPresenter.EditorPartCloseHandler() {
                        @Override
                        public void onClose(EditorPartPresenter editor) {
                            scrollFromSourceButton.setVisible(false);
                        }
                    });
                }
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void removeAllNodes() {
        hideToolbar();
        tree.getNodeStorage().clear();
    }

    /** {@inheritDoc} */
    @Override
    public List<Node> getRootNodes() {
        return tree.getRootNodes();
    }

    /** {@inheritDoc} */
    @Override
    public void addNode(Node parent, Node child) {
        if (child == null) {
            throw new IllegalArgumentException("Children shouldn't be null");
        }

        addNodes(parent, Collections.singletonList(child));
    }

    /** {@inheritDoc} */
    @Override
    public void addNodes(Node parent, List<Node> children) {
        if (children == null) {
            hideToolbar();
            throw new IllegalArgumentException("Children shouldn't be null");
        }

        showToolbar();

        if (tree.getRootNodes().isEmpty()) {
            tree.getNodeStorage().replaceChildren(null, children);
            return;
        }

        if (parent == null) {
            tree.getNodeStorage().add(children);
        } else {
            tree.getNodeStorage().add(parent, children);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void removeNode(Node node, boolean closeMissingFiles) {
        tree.getNodeStorage().remove(node);

        if (tree.getRootNodes().isEmpty()) {
            hideToolbar();
        }

        if (!(node instanceof HasStorablePath) || !closeMissingFiles) {
            return;
        }

        Map<String, EditorPartPresenter> openedEditors = editorAgentProvider.get().getOpenedEditors();
        if (openedEditors == null || openedEditors.isEmpty()) {
            return;
        }

        closeEditor((HasStorablePath)node, openedEditors);
    }

    private void closeEditor(HasStorablePath node, Map<String, EditorPartPresenter> openedEditors) {
        for (EditorPartPresenter editorPartPresenter : openedEditors.values()) {
            VirtualFile openedFile = editorPartPresenter.getEditorInput().getFile();
            if (openedFile.getPath().equals(node.getStorablePath())) {
                eventBus.fireEvent(new FileEvent(openedFile, CLOSE));
            }
        }
    }

    /** {@inheritDoc} */
    @Beta
    @Override
    public List<StoreSortInfo> getSortInfo() {
        return tree.getNodeStorage().getSortInfo();
    }

    /** {@inheritDoc} */
    @Beta
    @Override
    public void onApplySort() {
        //TODO need to improve this block of code to allow automatically save expand state before applying sorting
        tree.getNodeStorage().applySort(false);
    }

    /** {@inheritDoc} */
    @Override
    public Promise<Node> scrollFromSource(HasStorablePath path) {
        return getNodeByPath(path, false, true).then(new Function<Node, Node>() {
            @Override
            public Node apply(Node node) throws FunctionException {
                tree.scrollIntoView(node);
                tree.getSelectionModel().select(node, false);

                return node;
            }
        });
    }

    private void showToolbar() {
        if (refreshButton == null) {
            refreshButton = new ToolButton(new SVGImage(resources.refresh()));
            refreshButton.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    List<Node> selectedNodes = tree.getSelectionModel().getSelectedNodes();

                    for (Node node : selectedNodes) {
                        if (node.isLeaf()) {
                            continue;
                        }

                        if (tree.isExpanded(node)) {
                            tree.getNodeLoader().loadChildren(node, true);
                        }
                    }
                }
            });
            refreshButton.ensureDebugId(REFRESH_BUTTON_ID);

            Tooltip.create((elemental.dom.Element)refreshButton.getElement(),
                           BOTTOM,
                           MIDDLE,
                           "Refresh selected folder");
            addToolButton(refreshButton);
        }

        if (collapseAllButton == null) {
            collapseAllButton = new ToolButton(new SVGImage(explorerResources.collapse()));
            collapseAllButton.addClickHandler(new ClickHandler() {
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
            Tooltip.create((elemental.dom.Element)collapseAllButton.getElement(),
                           BOTTOM,
                           MIDDLE,
                           "Collapse All");
            collapseAllButton.ensureDebugId(COLLAPSE_ALL_BUTTON_ID);
            addToolButton(collapseAllButton);
        }

        refreshButton.setVisible(true);
        collapseAllButton.setVisible(true);
    }

    private void hideToolbar() {
        if (refreshButton != null) {
            refreshButton.setVisible(false);
        }

        if (collapseAllButton != null) {
            collapseAllButton.setVisible(false);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void reloadChildren(Node parent) {
        reloadChildren(parent, false);
    }

    /** {@inheritDoc} */
    @Override
    public void reloadChildren(Node parent, boolean deep) {
        TreeNodeLoader loader = tree.getNodeLoader();

        if (parent != null) {
            if (tree.isExpanded(parent) && !loader.loadChildren(parent, deep)) {
                Log.warn(getClass(), "Node has been already requested for loading children");
            }
        } else {
            List<Node> rootNodes = tree.getRootNodes();
            for (Node root : rootNodes) {
                if (tree.isExpanded(root) && !loader.loadChildren(root, deep)) {
                    Log.warn(getClass(), "Node has been already requested for loading children");
                }
            }
        }
    }

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
    @Override
    public void select(Node node, boolean keepExisting) {
        tree.getSelectionModel().select(node, keepExisting);
    }

    /** {@inheritDoc} */
    @Override
    public void select(List<Node> nodes, boolean keepExisting) {
        tree.getSelectionModel().select(nodes, keepExisting);
    }

    /** {@inheritDoc} */
    @Override
    public List<Node> getVisibleNodes() {
        return tree.getAllChildNodes(tree.getRootNodes(), true);
    }

    /** {@inheritDoc} */
    @Override
    public void showHiddenFiles(boolean show) {
        for (Node node : tree.getRootNodes()) {
            if (node instanceof HasSettings) {
                ((HasSettings)node).getSettings().setShowHiddenFiles(show);
            }
        }

        ProjectDescriptor openedProjectDescriptor = appContext.getCurrentProject().getProjectDescription();

        for (Node node : tree.getRootNodes()) {
            if (node instanceof ProjectDescriptorNode && openedProjectDescriptor.equals(((ProjectDescriptorNode)node).getData())) {
                reloadChildren(node);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isShowHiddenFiles() {
        for (Node node : tree.getRootNodes()) {
            if (node instanceof HasSettings) {
                return ((HasSettings)node).getSettings().isShowHiddenFiles();
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public List<Node> getAllNodes() {
        return tree.getNodeStorage().getAll();
    }

    /** {@inheritDoc} */
    @Override
    public List<Node> getAllNodes(Node parent) {
        return tree.getNodeStorage().getAllChildren(parent);
    }

    /** {@inheritDoc} */
    @Override
    public UniqueKeyProvider<Node> getNodeIdProvider() {
        return nodeIdProvider;
    }

    /** {@inheritDoc} */
    @Override
    public boolean reIndex(String oldId, Node node) {
        return tree.getNodeStorage().reIndexNode(oldId, node);
    }

    /** {@inheritDoc} */
    @Override
    public void refresh(Node node) {
        tree.refresh(node);
    }

    /** {@inheritDoc} */
    @Override
    public boolean setGoIntoModeOn(Node node) {
        return tree.getGoIntoMode().goInto(node);
    }

    /** {@inheritDoc} */
    @Override
    public HandlerRegistration addGoIntoStateHandler(GoIntoStateHandler handler) {
        return tree.getGoIntoMode().addGoIntoHandler(handler);
    }

    /** {@inheritDoc} */
    @Override
    public void resetGoIntoMode() {
        if (tree.getGoIntoMode().isActivated()) {
            tree.getGoIntoMode().reset();
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isGoIntoActivated() {
        return tree.getGoIntoMode().isActivated();
    }

    /** {@inheritDoc} */
    @Override
    public void onGoIntoStateChanged(GoIntoStateEvent event) {
        if (event.getState() == ACTIVATED) {
            //lazy button initializing
            if (goBackButton == null) {
                initGoIntoBackButton();
                return;
            }

            goBackButton.setVisible(true);

        } else if (event.getState() == DEACTIVATED) {
            goBackButton.setVisible(false);
        }
    }

    private void initGoIntoBackButton() {
        goBackButton = new ToolButton(new SVGImage(explorerResources.up()));
        goBackButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                tree.getGoIntoMode().reset();
            }
        });
        goBackButton.ensureDebugId(GO_BACK_BUTTON_ID);
        Tooltip.create((elemental.dom.Element)goBackButton.getElement(), BOTTOM, MIDDLE, "Go Back");
        addToolButton(goBackButton);
    }

    /** {@inheritDoc} */
    @Beta
    @Override
    public boolean isFoldersAlwaysOnTop() {
        return tree.getNodeStorage().getSortInfo().contains(foldersOnTopSort);
    }

    /** {@inheritDoc} */
    @Beta
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

    /** {@inheritDoc} */
    @Override
    public void expandAll() {
        tree.expandAll();
    }

    /** {@inheritDoc} */
    @Override
    public void collapseAll() {
        tree.collapseAll();
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

    /** {@inheritDoc} */
    @Override
    public Promise<Node> getNodeByPath(HasStorablePath path, boolean forceUpdate, boolean closeMissingFiles) {
        return searchNodeHandler.getNodeByPath(path, forceUpdate, closeMissingFiles);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isExpanded(Node node) {
        return tree.isExpanded(node);
    }

    /** {@inheritDoc} */
    @Override
    public void setExpanded(Node node, boolean expand) {
        tree.setExpanded(node, expand);
    }

    /** {@inheritDoc} */
    @Override
    public HandlerRegistration addExpandHandler(ExpandNodeEvent.ExpandNodeHandler handler) {
        return tree.addExpandHandler(handler);
    }

    /** {@inheritDoc} */
    @Override
    public HandlerRegistration addCollapseHandler(CollapseNodeEvent.CollapseNodeHandler handler) {
        return tree.addCollapseHandler(handler);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isLoaded(Node node) {
        return tree.findNode(node) != null && tree.findNode(node).isLoaded();
    }
}
