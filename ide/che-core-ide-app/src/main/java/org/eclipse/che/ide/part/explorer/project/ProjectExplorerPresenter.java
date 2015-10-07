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
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.machine.gwt.client.events.ExtServerStateEvent;
import org.eclipse.che.api.machine.gwt.client.events.ExtServerStateHandler;
import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.project.shared.dto.ProjectReference;
import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.ide.api.action.Action;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.action.Presentation;
import org.eclipse.che.ide.api.action.PromisableAction;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.event.ProjectActionEvent;
import org.eclipse.che.ide.api.event.ProjectActionHandler;
import org.eclipse.che.ide.api.mvp.View;
import org.eclipse.che.ide.api.parts.HasView;
import org.eclipse.che.ide.api.parts.PerspectiveManager;
import org.eclipse.che.ide.api.parts.ProjectExplorerPart;
import org.eclipse.che.ide.api.parts.base.BasePresenter;
import org.eclipse.che.ide.api.project.node.HasProjectDescriptor;
import org.eclipse.che.ide.api.project.node.HasStorablePath;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.api.selection.Selection;
import org.eclipse.che.ide.part.explorer.project.ProjectExplorerView.ActionDelegate;
import org.eclipse.che.ide.project.event.ResourceNodeRenamedEvent;
import org.eclipse.che.ide.project.event.SynchronizeProjectViewEvent;
import org.eclipse.che.ide.project.node.ModuleDescriptorNode;
import org.eclipse.che.ide.project.node.NodeManager;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.Unmarshallable;
import org.eclipse.che.ide.statepersistance.AppStateManager;
import org.eclipse.che.ide.ui.smartTree.event.CollapseNodeEvent;
import org.eclipse.che.ide.ui.smartTree.event.ExpandNodeEvent;
import org.eclipse.che.ide.ui.toolbar.PresentationFactory;
import org.eclipse.che.ide.util.loging.Log;
import org.eclipse.che.ide.workspace.BrowserQueryFieldViewer;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Vlad Zhukovskiy
 * @author Dmitry Shnurenko
 */
@Singleton
public class ProjectExplorerPresenter extends BasePresenter implements ActionDelegate,
                                                                       ProjectExplorerPart,
                                                                       HasView,
                                                                       ExtServerStateHandler,
                                                                       ProjectActionHandler,
                                                                       SynchronizeProjectViewEvent.SynchronizeProjectViewHandler,
                                                                       ResourceNodeRenamedEvent.ResourceNodeRenamedHandler {
    private final ProjectExplorerView          view;
    private final NodeManager                  nodeManager;
    private final AppContext                   appContext;
    private final ActionManager                actionManager;
    private final Provider<PerspectiveManager> managerProvider;
    private final ProjectServiceClient         projectService;
    private final DtoUnmarshallerFactory       dtoUnmarshallerFactory;
    private final List<ProjectDescriptor>      existingProjects;
    private final BrowserQueryFieldViewer      queryFieldViewer;
    private final Provider<AppStateManager>    appStateManagerProvider;

    private int             countOfExistingProjects;
    private AppStateManager appStateManager;

    @Inject
    public ProjectExplorerPresenter(ProjectExplorerView view,
                                    EventBus eventBus,
                                    NodeManager nodeManager,
                                    AppContext appContext,
                                    ActionManager actionManager,
                                    Provider<PerspectiveManager> managerProvider,
                                    ProjectServiceClient projectService,
                                    DtoUnmarshallerFactory dtoUnmarshallerFactory,
                                    BrowserQueryFieldViewer queryFieldViewer,
                                    Provider<AppStateManager> appStateManagerProvider) {
        this.view = view;
        this.view.setDelegate(this);

        this.nodeManager = nodeManager;
        this.appContext = appContext;
        this.actionManager = actionManager;
        this.managerProvider = managerProvider;
        this.projectService = projectService;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.queryFieldViewer = queryFieldViewer;
        this.appStateManagerProvider = appStateManagerProvider;

        this.existingProjects = new ArrayList<>();

        eventBus.addHandler(ExtServerStateEvent.TYPE, this);
        eventBus.addHandler(ProjectActionEvent.TYPE, this);
        eventBus.addHandler(SynchronizeProjectViewEvent.getType(), this);
        eventBus.addHandler(ResourceNodeRenamedEvent.getType(), this);
    }

    /** {@inheritDoc} */
    @Override
    public void onExtServerStarted(ExtServerStateEvent event) {
        takeAllProjects();
    }

    private void takeAllProjects() {
        Unmarshallable<List<ProjectReference>> unmarshaller = dtoUnmarshallerFactory.newListUnmarshaller(ProjectReference.class);

        projectService.getProjects(new AsyncRequestCallback<List<ProjectReference>>(unmarshaller) {
            @Override
            protected void onSuccess(List<ProjectReference> references) {
                countOfExistingProjects = references.size();

                appStateManager = appStateManagerProvider.get();

                for (ProjectReference reference : references) {
                    takeCertainProject(reference);
                }
            }

            @Override
            protected void onFailure(Throwable exception) {
                Log.error(ProjectExplorerPresenter.class, exception);
            }
        });
    }

    private void takeCertainProject(ProjectReference reference) {
        Unmarshallable<ProjectDescriptor> unmarshaller = dtoUnmarshallerFactory.newUnmarshaller(ProjectDescriptor.class);

        projectService.getProject(reference.getName(), new AsyncRequestCallback<ProjectDescriptor>(unmarshaller) {
            @Override
            protected void onSuccess(ProjectDescriptor project) {
                existingProjects.add(project);

                if (countOfExistingProjects == existingProjects.size()) {
                    showProjects(existingProjects);
                }
            }

            @Override
            protected void onFailure(Throwable throwable) {
                Log.error(ProjectExplorerPresenter.class, throwable);
            }
        });
    }

    private void showProjects(List<ProjectDescriptor> projects) {
        List<Node> projectNodes = new ArrayList<>();

        for (ProjectDescriptor projectDescriptor : projects) {
            projectNodes.add(nodeManager.wrap(projectDescriptor));

            CurrentProject currentProject = new CurrentProject(projectDescriptor);

            appContext.addProject(currentProject);

            appContext.setCurrentProject(currentProject);
        }

        view.setRootNodes(projectNodes);

        for (final ProjectDescriptor projectDescriptor : projects) {
            appStateManager.restoreProjectState(projectDescriptor);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onExtServerStopped(ExtServerStateEvent event) {
        view.setVisible(false);
    }

    /** {@inheritDoc} */
    @Override
    public void onProjectDeleted(ProjectActionEvent event) {
        ProjectDescriptor deletedProject = event.getProject();

        existingProjects.remove(deletedProject);

        showProjects(existingProjects);

        appContext.removeProject(deletedProject);
    }

    /** {@inheritDoc} */
    @Override
    public void onProjectCreated(ProjectActionEvent event) {
        ProjectDescriptor createdProject = event.getProject();

        if (existingProjects.contains(createdProject)) {
            return;
        }

        existingProjects.add(createdProject);

        showProjects(existingProjects);

        CurrentProject currentProject = new CurrentProject(createdProject);

        appContext.addProject(currentProject);

        appContext.setCurrentProject(currentProject);
    }

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
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

    @Override
    public void setVisible(boolean visible) {
        view.setVisible(visible);
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

    private void updateAppContext(List<Node> nodes) {
        for (Node node : nodes) {
            if (node instanceof HasProjectDescriptor) {
                ProjectDescriptor descriptor = ((HasProjectDescriptor)node).getProjectDescriptor();
                appContext.getCurrentProject().setProjectDescription(descriptor);

                String projectName = descriptor.getName();

                queryFieldViewer.setProjectName(projectName);

                return;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onDeleteKeyPressed() {
        final Action deleteItemAction = actionManager.getAction("deleteItem");

        final Presentation presentation = new PresentationFactory().getPresentation(deleteItemAction);
        final PerspectiveManager manager = managerProvider.get();
        final ActionEvent event = new ActionEvent("", presentation, actionManager, manager);


        if (deleteItemAction instanceof PromisableAction) {
            ((PromisableAction)deleteItemAction).promise(event);
        } else {
            deleteItemAction.actionPerformed(event);
        }
    }

    public void setExpanded(Node node, boolean expand) {
        view.setExpanded(node, expand);
    }

    public void goInto(Node node) {
        view.setGoIntoModeOn(node);
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
