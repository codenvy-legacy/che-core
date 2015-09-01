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

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
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
import org.eclipse.che.ide.part.explorer.project.NewProjectExplorerView.ActionDelegate;
import org.eclipse.che.ide.project.event.ResourceNodeDeletedEvent;
import org.eclipse.che.ide.project.event.ResourceNodeDeletedEvent.ResourceNodeDeletedHandler;
import org.eclipse.che.ide.project.event.ResourceNodeRenamedEvent;
import org.eclipse.che.ide.project.node.NodeManager;
import org.eclipse.che.ide.project.node.ProjectDescriptorNode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Vlad Zhukovskiy
 */
@Singleton
public class NewProjectExplorerPresenter extends BasePresenter implements ActionDelegate,
                                                                          NewProjectExplorerPart,
                                                                          HasView {

    private final NewProjectExplorerView view;
    private final EventBus               eventBus;
    private final NodeManager            nodeManager;
    private final AppContext             appContext;

    @Inject
    public NewProjectExplorerPresenter(final NewProjectExplorerView view,
                                       final EventBus eventBus,
                                       final NodeManager nodeManager,
                                       final AppContext appContext) {
        this.view = view;
        this.eventBus = eventBus;
        this.nodeManager = nodeManager;
        this.appContext = appContext;

        view.setDelegate(this);

        initHandlers();
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

    private void initHandlers() {
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
            /** {@inheritDoc} */
            @Override
            public void onProjectOpened(ProjectActionEvent event) {
                ProjectDescriptor projectDescriptor = event.getProject();
                ProjectDescriptorNode projectDescriptorNode = nodeManager.wrap(projectDescriptor);
                view.setRootNode(projectDescriptorNode);
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
        });


        eventBus.addHandler(ResourceNodeDeletedEvent.getType(), new ResourceNodeDeletedHandler() {
            /** {@inheritDoc} */
            @Override
            public void onResourceEvent(ResourceNodeDeletedEvent evt) {
                reloadChildren(evt.getNode().getParent(), null);
            }
        });

        eventBus.addHandler(ResourceNodeRenamedEvent.getType(), new ResourceNodeRenamedEvent.ResourceNodeRenamedHandler() {
            @Override
            public void onResourceRenamedEvent(ResourceNodeRenamedEvent event) {
                reloadChildren(event.getNode().getParent(), event.getNewDataObject());
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public View getView() {
        return view;
    }

    /** {@inheritDoc} */
    @Nonnull
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
    public void reloadSelectedNodes() {
        Selection<?> selection = getSelection();
        if (selection.isEmpty()) {
            return;
        }

        List<?> nodes = selection.getAllElements();
        List<Node> nodesToReload = new ArrayList<>();

        for (Object o : nodes) {
            if (o instanceof Node && !((Node)o).isLeaf()) {
                nodesToReload.add((Node)o);
            }
        }

        if (!nodesToReload.isEmpty()) {
            view.reloadChildren(nodesToReload, null, false);
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

    public void expand(HasStorablePath node) {
        //temporary stub
    }

    public void goInto(Node node) {
        view.goInto(node);
    }

    private Operation<List<Node>> _showProjectsList() {
        return new Operation<List<Node>>() {
            @Override
            public void apply(List<Node> nodes) throws OperationException {
                view.setRootNodes(nodes);
            }
        };
    }

    public void synchronizeTree() {
        view.synchronizeTree();
    }

    public void navigate(HasStorablePath node, boolean select, boolean callAction) {
        view.navigate(node, select, callAction);
    }

    public Promise<Node> navigate(HasStorablePath node, boolean select) {
        return view.navigate(node, select);
    }

    public void reloadChildren(Node node) {
        reloadChildren(node, null);
    }

    public void reloadChildren(Node node, Object selectAfter) {
        reloadChildren(node, selectAfter, false);
    }

    public void reloadChildren(Node node, Object selectAfter, boolean callAction) {
        reloadChildren(node != null ? Collections.singletonList(node) : null, selectAfter, callAction);
    }

    public void reloadChildren(List<Node> node, Object selectAfter, boolean callAction) {
        view.reloadChildren(node, selectAfter, callAction);
    }

    public void reloadChildrenByType(Class<?> type) {
        view.reloadChildrenByType(type);
    }

    public void resetGoIntoMode() {
        view.resetGoIntoMode();
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
}
