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

import com.google.common.base.Strings;
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
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.commons.annotation.Nullable;
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
import org.eclipse.che.ide.ui.smartTree.DelayedTask;
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
import org.eclipse.che.ide.ui.smartTree.event.CollapseNodeEvent;
import org.eclipse.che.ide.ui.smartTree.event.ExpandNodeEvent;
import org.eclipse.che.ide.ui.smartTree.event.GoIntoStateEvent;
import org.eclipse.che.ide.ui.smartTree.event.GoIntoStateEvent.GoIntoStateHandler;
import org.eclipse.che.ide.ui.smartTree.event.SelectionChangedEvent;
import org.eclipse.che.ide.ui.smartTree.event.SelectionChangedEvent.SelectionChangedHandler;
import org.eclipse.che.ide.ui.smartTree.event.StoreRemoveEvent;
import org.eclipse.che.ide.ui.smartTree.presentation.DefaultPresentationRenderer;
import org.eclipse.che.ide.ui.smartTree.sorting.AlphabeticalFilter;
import org.eclipse.che.ide.util.loging.Log;
import org.vectomatic.dom.svg.ui.SVGImage;
import org.vectomatic.dom.svg.ui.SVGResource;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.Set;

import static org.eclipse.che.ide.api.event.FileEvent.FileOperation.CLOSE;
import static org.eclipse.che.ide.ui.menu.PositionController.HorizontalAlign.MIDDLE;
import static org.eclipse.che.ide.ui.menu.PositionController.VerticalAlign.BOTTOM;
import static org.eclipse.che.ide.ui.smartTree.event.GoIntoStateEvent.State.ACTIVATED;
import static org.eclipse.che.ide.ui.smartTree.event.GoIntoStateEvent.State.DEACTIVATED;

/**
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

    private ToolButton          goIntoBackButton;
    private ToolButton          scrollFromSourceButton;
    private SearchNodeHandler   searchNodeHandler;
    private HandlerRegistration closeEditorOnNodeRemovedHandler;

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

        ScrollPanel treePanel = new ScrollPanel(tree);
        treePanel.ensureDebugId("projectExplorer");
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
                        initScrollFromSourceButton();
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

    @Override
    public void setRootNodes(List<Node> nodes) {
        hideProjectInfo();
        removeCloseEditorHandler();

        if (nodes == null || nodes.isEmpty()) {
            tree.getNodeStorage().clear();
            return;
        }

        tree.getNodeStorage().replaceChildren(null, nodes);

        if (nodes.size() == 1) {
            final String contentRoot = getContentRootOrNull(nodes.get(0));
            if (!Strings.isNullOrEmpty(contentRoot)) {
                getNodeByPath(new HasStorablePath.StorablePath(contentRoot), false).then(waitRenderAndPerformGoInto());
            } else {
                tree.setExpanded(nodes.get(0), true);
            }
        }

        registerCloseEditorHandler();
        showProjectInfo();
    }

    @Override
    public void addNode(Node node) {
        tree.getNodeStorage().add(node);

        if (tree.getRootNodes().size() == 1) {
            showProjectInfo();
        }
    }

    @Override
    public void removeNode(Node node) {
        tree.getNodeStorage().remove(node);

        if (tree.getRootNodes().isEmpty()) {
            hideProjectInfo();
        }
    }

    private void registerCloseEditorHandler() {
        closeEditorOnNodeRemovedHandler = tree.getNodeStorage().addStoreRemoveHandler(new StoreRemoveEvent.StoreRemoveHandler() {
            @Override
            public void onRemove(StoreRemoveEvent event) {
                Node removedNode = event.getNode();

                if (!(removedNode instanceof HasStorablePath)) {
                    return;
                }

                NavigableMap<String, EditorPartPresenter> openedEditors = editorAgentProvider.get().getOpenedEditors();
                if (openedEditors == null || openedEditors.isEmpty()) {
                    return;
                }

                for (EditorPartPresenter editorPartPresenter : openedEditors.values()) {
                    VirtualFile openedFile = editorPartPresenter.getEditorInput().getFile();
                    if (openedFile.getPath().equals(((HasStorablePath)removedNode).getStorablePath())) {
                        eventBus.fireEvent(new FileEvent(openedFile, CLOSE));
                    }
                }
            }
        });
    }

    private void removeCloseEditorHandler() {
        if (closeEditorOnNodeRemovedHandler != null) {
            closeEditorOnNodeRemovedHandler.removeHandler();
        }
    }

    private Operation<Node> waitRenderAndPerformGoInto() {
        return new Operation<Node>() {
            @Override
            public void apply(final Node node) throws OperationException {
                new DelayedTask() {
                    @Override
                    public void onExecute() {
                        setGoIntoModeOn(node);
                    }
                }.delay(250);
            }
        };
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
    public void scrollFromSource(HasStorablePath path) {
        getNodeByPath(path, false).then(new Operation<Node>() {
            @Override
            public void apply(Node node) throws OperationException {
                tree.scrollIntoView(node);
                tree.getSelectionModel().select(node, false);
            }
        });
    }

    public void clear() {
        tree.clear();
    }

    @Override
    public void setRootNode(Node node) {
        setRootNodes(Collections.singletonList(node));
    }

    @Override
    public void replaceParentNode(ProjectDescriptorNode descriptorNode) {
        tree.getNodeStorage().remove(descriptorNode);

        tree.getNodeStorage().add(descriptorNode);
    }

    private void showProjectInfo() {
        List<Node> rootItems = tree.getNodeStorage().getRootItems();

        for (Node rootNode : rootItems) {
            if (rootNode instanceof ProjectDescriptorNode) {
                ProjectDescriptor descriptor = ((ProjectDescriptorNode)rootNode).getProjectDescriptor();

                if (descriptor == null) {
                    continue;
                }

                SVGImage refreshIcon = new SVGImage(resources.refresh());
                ToolButton refreshButton = new ToolButton(refreshIcon);
                refreshButton.addClickHandler(new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        reloadChildren(null, true);
                    }
                });
                refreshButton.ensureDebugId("refreshSelectedFolder");

                Tooltip.create((elemental.dom.Element) refreshButton.getElement(),
                        BOTTOM,
                        MIDDLE,
                        "Refresh selected folder");
                addToolButton(refreshButton);

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
                addToolButton(collapseAll);
                return;
            }
        }

        hideProjectInfo();
    }

    private void hideProjectInfo() {
        goIntoBackButton = null;

        if (goIntoBackButton != null) {
            removeToolButton(goIntoBackButton);
        }

        if (scrollFromSourceButton != null) {
            removeToolButton(scrollFromSourceButton);
        }
    }

    @Override
    public void reloadChildren(Node parent) {
        reloadChildren(parent, false);
    }

    public void reloadChildren(Node parent, boolean deep) {
        if (appContext.getCurrentProject() == null) {
            //project doesn't opened, so reload project list
            if (parent != null) {
                Log.warn(this.getClass(), "Project isn't opened, so node to reload will be ignored.");
            }

            nodeManager.getProjects().then(new Operation<List<Node>>() {
                @Override
                public void apply(List<Node> projects) throws OperationException {
                    setRootNodes(projects);
                }
            });

            return;
        }

        TreeNodeLoader loader = tree.getNodeLoader();

        if (parent != null) {
            if (!loader.loadChildren(parent, deep)) {
                Log.warn(this.getClass(), "Node has been already requested for loading children");
            }
        } else {
            List<Node> rootNodes = tree.getRootNodes();
            for (Node root : rootNodes) {
                if (!loader.loadChildren(root, deep)) {
                    Log.warn(this.getClass(), "Node has been already requested for loading children");
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
    public void select(Node node, boolean keepExisting) {
        tree.getSelectionModel().select(node, keepExisting);
    }

    @Override
    public void select(List<Node> nodes, boolean keepExisting) {
        tree.getSelectionModel().select(nodes, keepExisting);
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

        ProjectDescriptor openedProjectDescriptor = appContext.getCurrentProject().getProjectDescription();

        for (Node node : tree.getRootNodes()) {
            if (node instanceof ProjectDescriptorNode && openedProjectDescriptor.equals(((ProjectDescriptorNode)node).getData())) {
                reloadChildren(node);
            }
        }
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
    public HandlerRegistration addGoIntoStateHandler(GoIntoStateHandler handler) {
        return tree.getGoIntoMode().addGoIntoHandler(handler);
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
        goIntoBackButton = new ToolButton(new SVGImage(explorerResources.up()));
        goIntoBackButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                tree.getGoIntoMode().reset();
            }
        });
        goIntoBackButton.ensureDebugId("goBackButton");
        Tooltip.create((elemental.dom.Element) goIntoBackButton.getElement(),
                BOTTOM,
                MIDDLE,
                "Go Back");
        addToolButton(goIntoBackButton);
    }

    private void initScrollFromSourceButton() {
        scrollFromSourceButton = new ToolButton(new SVGImage(explorerResources.source()));
        scrollFromSourceButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                final String sourcePath = editorAgentProvider.get().getActiveEditor().getEditorInput().getFile().getPath();
                scrollFromSource(new HasStorablePath.StorablePath(sourcePath));
            }
        });
        scrollFromSourceButton.ensureDebugId("scrollFromSourceButton");
        Tooltip.create((elemental.dom.Element)scrollFromSourceButton.getElement(),
                       BOTTOM,
                       MIDDLE,
                       "Scroll From Source");
        addToolButton(scrollFromSourceButton);
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

    @Override
    public Promise<Node> getNodeByPath(HasStorablePath path, boolean forceUpdate) {
        return searchNodeHandler.getNodeByPath(path, forceUpdate);
    }

    @Override
    public boolean isExpanded(Node node) {
        return tree.isExpanded(node);
    }

    @Override
    public void setExpanded(Node node, boolean expand) {
        tree.setExpanded(node, expand);
    }

    @Override
    public HandlerRegistration addExpandHandler(ExpandNodeEvent.ExpandNodeHandler handler) {
        return tree.addExpandHandler(handler);
    }

    @Override
    public HandlerRegistration addCollapseHandler(CollapseNodeEvent.CollapseNodeHandler handler) {
        return tree.addCollapseHandler(handler);
    }
}
