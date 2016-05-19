/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
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
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.project.shared.dto.ProjectReference;
import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.ide.api.action.Action;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.action.Presentation;
import org.eclipse.che.ide.api.action.PromisableAction;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.event.ProjectActionEvent;
import org.eclipse.che.ide.api.event.ProjectActionHandler;
import org.eclipse.che.ide.api.mvp.View;
import org.eclipse.che.ide.api.parts.HasView;
import org.eclipse.che.ide.api.parts.base.BasePresenter;
import org.eclipse.che.ide.api.project.node.HasProjectDescriptor;
import org.eclipse.che.ide.api.project.node.HasStorablePath;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.api.project.node.event.ProjectPartLoadEvent;
import org.eclipse.che.ide.api.project.node.event.ProjectPartLoadEvent.ProjectPartLoadHandler;
import org.eclipse.che.ide.api.selection.Selection;
import org.eclipse.che.ide.part.explorer.project.ProjectExplorerView.ActionDelegate;
import org.eclipse.che.ide.project.event.ResourceNodeRenamedEvent;
import org.eclipse.che.ide.project.event.SynchronizeProjectViewEvent;
import org.eclipse.che.ide.project.node.ModuleDescriptorNode;
import org.eclipse.che.ide.project.node.NodeManager;
import org.eclipse.che.ide.project.node.ProjectDescriptorNode;
import org.eclipse.che.ide.ui.smartTree.event.CollapseNodeEvent;
import org.eclipse.che.ide.ui.smartTree.event.ExpandNodeEvent;
import org.eclipse.che.ide.ui.smartTree.event.GoIntoStateEvent;
import org.eclipse.che.ide.ui.toolbar.PresentationFactory;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author Vlad Zhukovskiy
 */
@Singleton
public class ProjectExplorerPresenter extends BasePresenter implements ActionDelegate,
                                                                       ProjectExplorerPart,
                                                                       HasView {

    private final ProjectExplorerView view;
    private final EventBus            eventBus;
    private final NodeManager         nodeManager;
    private final AppContext          appContext;
    private final ActionManager       actionManager;

    @Inject
    public ProjectExplorerPresenter(final ProjectExplorerView view,
                                    final EventBus eventBus,
                                    final NodeManager nodeManager,
                                    final AppContext appContext,
                                    final ActionManager actionManager) {
        this.view = view;
        this.eventBus = eventBus;
        this.nodeManager = nodeManager;
        this.appContext = appContext;
        this.actionManager = actionManager;

        view.setDelegate(this);

        initEventHandlers();
    }

    @Override
    public void onOpen() {
        super.onOpen();
        if (appContext.getCurrentProject() == null) {
            nodeManager.getProjects()
                       .then(_showProjectsList());
        } else {
            ProjectDescriptor rootProject = appContext.getCurrentProject().getRootProject();
            ProjectDescriptorNode projectDescriptorNode = nodeManager.wrap(rootProject);
            view.setRootNode(projectDescriptorNode);
        }
    }

    private void initEventHandlers() {
        eventBus.addHandler(ProjectPartLoadEvent.getType(), new ProjectPartLoadHandler() {
            /** {@inheritDoc} */
            @Override
            public void onProjectPartLoad(ProjectPartLoadEvent event) {
                if (appContext.getCurrentProject() == null) {
                    nodeManager.getProjects()
                               .then(_showProjectsList());
                } else {
                    ProjectDescriptor rootProject = appContext.getCurrentProject().getRootProject();
                    ProjectDescriptorNode projectDescriptorNode = nodeManager.wrap(rootProject);
                    view.setRootNode(projectDescriptorNode);
                }
            }
        });


        eventBus.addHandler(ProjectActionEvent.TYPE, new ProjectActionHandler() {
            private HandlerRegistration handlerRegistration;

            /** {@inheritDoc} */
            @Override
            public void onProjectReady(ProjectActionEvent event) {

            }

            /** {@inheritDoc} */
            @Override
            public void onProjectClosing(ProjectActionEvent event) {
                //nothing to do
            }

            /** {@inheritDoc} */
            @Override
            public void onProjectClosed(ProjectActionEvent event) {
                view.resetGoIntoMode();
                if (!event.isCloseBeforeOpening()) {
                    nodeManager.getProjects()
                               .then(_showProjectsList());
                }

            }

            @Override
            public void onProjectOpened(ProjectActionEvent event) {
                ProjectDescriptor projectDescriptor = event.getProject();

                if (!Strings.isNullOrEmpty(projectDescriptor.getContentRoot())) {
                    handlerRegistration = view.addGoIntoStateHandler(new GoIntoStateEvent.GoIntoStateHandler() {
                        @Override
                        public void onGoIntoStateChanged(GoIntoStateEvent event) {
                            if (event.getState() == GoIntoStateEvent.State.ACTIVATED) {
                                eventBus.fireEvent(ProjectActionEvent.createProjectOpenedEvent(
                                        ((HasProjectDescriptor)event.getNode()).getProjectDescriptor()));
                                handlerRegistration.removeHandler();
                            }
                        }
                    });
                } else {
                    eventBus.fireEvent(ProjectActionEvent.createProjectOpenedEvent(projectDescriptor));
                }

                ProjectDescriptorNode projectDescriptorNode = nodeManager.wrap(projectDescriptor);
                view.setRootNode(projectDescriptorNode);
            }
        });

        eventBus.addHandler(ResourceNodeRenamedEvent.getType(), new ResourceNodeRenamedEvent.ResourceNodeRenamedHandler() {
            @Override
            public void onResourceRenamedEvent(final ResourceNodeRenamedEvent event) {
                Object newDataObject = event.getNewDataObject();
                String path = null;

                //Here we have old node with old data object and new renamed data object
                //so we need fetch storable path from it and call tree to find node by this
                //storable path. When tree will find our node by path, the old node will be
                //deleted automatically. Otherwise if we can't determine storable path from
                //node, then we just take parent node and try navigate on it in the tree.

                if (newDataObject instanceof ProjectReference) {
                    reloadChildren(); //we should simple reload project list
                } else if (newDataObject instanceof ItemReference) {
                    path = ((ItemReference)newDataObject).getPath();
                } else if (event.getNode() instanceof ModuleDescriptorNode) {
                    path = ((ProjectDescriptor)newDataObject).getPath();
                } else if (event.getNode().getParent() != null && event.getNode().getParent() instanceof HasStorablePath) {
                    path = ((HasStorablePath)event.getNode().getParent()).getStorablePath();
                }

                if (!Strings.isNullOrEmpty(path)) {
                    getNodeByPath(new HasStorablePath.StorablePath(path), true).then(selectNode());
                }
            }
        });

        eventBus.addHandler(SynchronizeProjectViewEvent.getType(), new SynchronizeProjectViewEvent.SynchronizeProjectViewHandler() {
            @Override
            public void onProjectViewSynchronizeEvent(SynchronizeProjectViewEvent event) {
                if (event.getNode() != null) {
                    reloadChildren(event.getNode());
                } else if (event.getByClass() != null) {
                    reloadChildrenByType(event.getByClass());
                } else {
                    reloadChildren();
                }
            }
        });
    }

    private Function<Node, Node> selectNode() {
        return new Function<Node, Node>() {
            @Override
            public Node apply(Node node) throws FunctionException {
                select(node, false);

                return node;
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public View getView() {
        return view;
    }

    /** {@inheritDoc} */
    @NotNull
    @Override
    public String getTitle() {
        return "Project";
    }

    /** {@inheritDoc} */
    @Nullable
    @Override
    public ImageResource getTitleImage() {
        return null;
    }

    /** {@inheritDoc} */
    @Nullable
    @Override
    public String getTitleToolTip() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public int getSize() {
        return 250;
    }

    /** {@inheritDoc} */
    @Override
    public void go(AcceptsOneWidget container) {
        container.setWidget(view);
    }

    /** {@inheritDoc} */
    @Override
    public void onSelectionChanged(List<Node> selection) {
        setSelection(new Selection<>(selection));
        updateAppContext(selection);
    }

    @Override
    public void onDeleteKeyPressed() {
        final Action deleteItemAction = actionManager.getAction("deleteItem");

        final Presentation presentation = new PresentationFactory().getPresentation(deleteItemAction);
        final ActionEvent event = new ActionEvent("", presentation, actionManager, 0, null);


        if (deleteItemAction instanceof PromisableAction) {
            ((PromisableAction)deleteItemAction).promise(event);
        } else {
            deleteItemAction.actionPerformed(event);
        }
    }

    private void updateAppContext(List<Node> nodes) {
        for (Node node : nodes) {
            if (node instanceof HasProjectDescriptor) {
                ProjectDescriptor descriptor = ((HasProjectDescriptor)node).getProjectDescriptor();
                if (appContext.getCurrentProject() != null) {
                    appContext.getCurrentProject().setProjectDescription(descriptor);
                    return;
                }
            }
        }
    }

    public void setExpanded(Node node, boolean expand) {
        view.setExpanded(node, expand);
    }

    public void goInto(Node node) {
        view.setGoIntoModeOn(node);
    }

    private Operation<List<Node>> _showProjectsList() {
        return new Operation<List<Node>>() {
            @Override
            public void apply(List<Node> nodes) throws OperationException {
                view.setRootNodes(nodes);
            }
        };
    }

    public void reloadChildren() {
        view.reloadChildren(null, true);
    }

    public void reloadChildren(Node node) {
        view.reloadChildren(node);
    }

    public void reloadChildrenByType(Class<?> type) {
        view.reloadChildrenByType(type);
    }

    public void resetGoIntoMode() {
        view.resetGoIntoMode();
    }

    public boolean isGoIntoActivated() {
        return view.isGoIntoActivated();
    }

    public void expandAll() {
        view.expandAll();
    }

    public void collapseAll() {
        view.collapseAll();
    }

    public List<Node> getVisibleNodes() {
        return view.getVisibleNodes();
    }

    public void showHiddenFiles(boolean show) {
        view.showHiddenFiles(show);
    }

    public boolean isShowHiddenFiles() {
        return view.isShowHiddenFiles();
    }

    public Promise<Node> getNodeByPath(HasStorablePath path) {
        return view.getNodeByPath(path, false);
    }

    public Promise<Node> getNodeByPath(HasStorablePath path, boolean forceUpdate) {
        return view.getNodeByPath(path, forceUpdate);
    }

    public void select(Node item, boolean keepExisting) {
        view.select(item, keepExisting);
    }

    public void select(List<Node> items, boolean keepExisting) {
        view.select(items, keepExisting);
    }

    public boolean isExpanded(Node node) {
        return view.isExpanded(node);
    }

    public void scrollFromSource(HasStorablePath path) {
        view.scrollFromSource(path);
    }

    public HandlerRegistration addExpandHandler(ExpandNodeEvent.ExpandNodeHandler handler) {
        return view.addExpandHandler(handler);
    }

    public HandlerRegistration addCollapseHandler(CollapseNodeEvent.CollapseNodeHandler handler) {
        return view.addCollapseHandler(handler);
    }
}
