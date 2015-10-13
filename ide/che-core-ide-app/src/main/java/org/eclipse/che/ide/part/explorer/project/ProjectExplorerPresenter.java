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

import org.eclipse.che.api.core.rest.shared.dto.ServiceError;
import org.eclipse.che.api.machine.gwt.client.events.ExtServerStateEvent;
import org.eclipse.che.api.machine.gwt.client.events.ExtServerStateHandler;
import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.Constants;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.project.shared.dto.ProjectReference;
import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.api.action.Action;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.action.Presentation;
import org.eclipse.che.ide.api.action.PromisableAction;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.event.project.CloseCurrentProjectEvent;
import org.eclipse.che.ide.api.event.project.CloseCurrentProjectHandler;
import org.eclipse.che.ide.api.event.project.CreateProjectEvent;
import org.eclipse.che.ide.api.event.project.CreateProjectHandler;
import org.eclipse.che.ide.api.event.project.DeleteProjectEvent;
import org.eclipse.che.ide.api.event.project.DeleteProjectHandler;
import org.eclipse.che.ide.api.event.project.OpenProjectEvent;
import org.eclipse.che.ide.api.event.project.OpenProjectHandler;
import org.eclipse.che.ide.api.event.project.ProjectReadyEvent;
import org.eclipse.che.ide.api.mvp.View;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.parts.HasView;
import org.eclipse.che.ide.api.parts.PerspectiveManager;
import org.eclipse.che.ide.api.parts.ProjectExplorerPart;
import org.eclipse.che.ide.api.parts.base.BasePresenter;
import org.eclipse.che.ide.api.project.node.HasProjectDescriptor;
import org.eclipse.che.ide.api.project.node.HasStorablePath;
import org.eclipse.che.ide.api.project.node.MutableNode;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.api.selection.Selection;
import org.eclipse.che.ide.core.problemDialog.ProjectProblemDialog;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.part.explorer.project.ProjectExplorerView.ActionDelegate;
import org.eclipse.che.ide.project.event.ResourceNodeRenamedEvent;
import org.eclipse.che.ide.project.event.SynchronizeProjectViewEvent;
import org.eclipse.che.ide.project.node.ModuleDescriptorNode;
import org.eclipse.che.ide.project.node.NodeManager;
import org.eclipse.che.ide.project.node.ProjectDescriptorNode;
import org.eclipse.che.ide.projecttype.wizard.presenter.ProjectWizardPresenter;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.Unmarshallable;
import org.eclipse.che.ide.statepersistance.AppStateManager;
import org.eclipse.che.ide.ui.smartTree.event.CollapseNodeEvent;
import org.eclipse.che.ide.ui.smartTree.event.ExpandNodeEvent;
import org.eclipse.che.ide.ui.smartTree.event.GoIntoStateEvent;
import org.eclipse.che.ide.ui.toolbar.PresentationFactory;
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
                                                                       OpenProjectHandler,
                                                                       CloseCurrentProjectHandler,
                                                                       ExtServerStateHandler,
                                                                       CreateProjectHandler,
                                                                       DeleteProjectHandler,
                                                                       ResourceNodeRenamedEvent.ResourceNodeRenamedHandler,
                                                                       SynchronizeProjectViewEvent.SynchronizeProjectViewHandler {
    private final ProjectExplorerView          view;
    private final EventBus                     eventBus;
    private final NodeManager                  nodeManager;
    private final AppContext                   appContext;
    private final ActionManager                actionManager;
    private final Provider<PerspectiveManager> managerProvider;
    private final BrowserQueryFieldViewer      queryFieldViewer;
    private final Provider<AppStateManager>    appStateManagerProvider;
    private final List<Node>                   existingProjects;
    private final ProjectWizardPresenter       projectWizardPresenter;
    private final CoreLocalizationConstant     locale;
    private final DtoUnmarshallerFactory       dtoUnmarshallerFactory;
    private final ProjectServiceClient         projectServiceClient;
    private final DtoFactory                   dtoFactory;
    private final NotificationManager          notificationManager;

    private HandlerRegistration handlerRegistration;

    @Inject
    public ProjectExplorerPresenter(ProjectExplorerView view,
                                    EventBus eventBus,
                                    NodeManager nodeManager,
                                    AppContext appContext,
                                    ActionManager actionManager,
                                    Provider<PerspectiveManager> managerProvider,
                                    BrowserQueryFieldViewer queryFieldViewer,
                                    Provider<AppStateManager> appStateManagerProvider,
                                    ProjectWizardPresenter projectWizardPresenter,
                                    CoreLocalizationConstant locale,
                                    DtoUnmarshallerFactory dtoUnmarshallerFactory,
                                    ProjectServiceClient projectServiceClient,
                                    DtoFactory dtoFactory,
                                    NotificationManager notificationManager) {
        this.view = view;
        this.view.setDelegate(this);

        this.eventBus = eventBus;
        this.nodeManager = nodeManager;
        this.appContext = appContext;
        this.actionManager = actionManager;
        this.managerProvider = managerProvider;
        this.queryFieldViewer = queryFieldViewer;
        this.appStateManagerProvider = appStateManagerProvider;
        this.projectWizardPresenter = projectWizardPresenter;
        this.locale = locale;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.projectServiceClient = projectServiceClient;
        this.dtoFactory = dtoFactory;
        this.notificationManager = notificationManager;

        this.existingProjects = new ArrayList<>();

        eventBus.addHandler(OpenProjectEvent.TYPE, this);
        eventBus.addHandler(CloseCurrentProjectEvent.TYPE, this);

        eventBus.addHandler(CreateProjectEvent.TYPE, this);
        eventBus.addHandler(DeleteProjectEvent.TYPE, this);

        eventBus.addHandler(ExtServerStateEvent.TYPE, this);

        eventBus.addHandler(ResourceNodeRenamedEvent.getType(), this);

        eventBus.addHandler(SynchronizeProjectViewEvent.getType(), this);
    }

    /** {@inheritDoc} */
    @Override
    public void onExtServerStarted(ExtServerStateEvent event) {
        Promise<List<Node>> nodesPromise = nodeManager.getProjects();

        nodesPromise.then(new Operation<List<Node>>() {
            @Override
            public void apply(List<Node> nodes) throws OperationException {
                for (Node node : nodes) {
                    if (node instanceof ProjectDescriptorNode) {
                        CurrentProject currentProject = new CurrentProject(((ProjectDescriptorNode)node).getProjectDescriptor());

                        appContext.setCurrentProject(currentProject);
                    }

                    existingProjects.add(node);
                }

                view.setRootNodes(nodes);
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void onProjectCreated(CreateProjectEvent event) {
        ProjectDescriptor createdProject = event.getDescriptor();

        ProjectDescriptorNode descriptorNode = nodeManager.wrap(createdProject);

        existingProjects.add(descriptorNode);

        view.addNode(descriptorNode);

        view.select(descriptorNode, false);
    }

    /** {@inheritDoc} */
    @Override
    public void onProjectDeleted(DeleteProjectEvent event) {
        ProjectDescriptor deletedProject = event.getDescriptor();

        for (Node node : existingProjects) {
            if (node instanceof ProjectDescriptorNode && deletedProject.equals(((ProjectDescriptorNode)node).getData())) {
                existingProjects.remove(node);

                view.removeNode(node);
            }
        }

        if (existingProjects.isEmpty()) {
            queryFieldViewer.setProjectName("");

            return;
        }

        ProjectDescriptorNode firstProjectNode = (ProjectDescriptorNode)existingProjects.get(0);

        view.select(firstProjectNode, false);
    }

    /** {@inheritDoc} */
    @Override
    public void onExtServerStopped(ExtServerStateEvent event) {
        view.setVisible(false);
    }

    /** {@inheritDoc} */
    @Override
    public void onProjectOpened(final OpenProjectEvent event) {
        final ProjectDescriptor descriptor = event.getDescriptor();

        if (!Strings.isNullOrEmpty(descriptor.getContentRoot())) {
            handlerRegistration = view.addGoIntoStateHandler(new GoIntoStateEvent.GoIntoStateHandler() {
                @Override
                public void onGoIntoStateChanged(GoIntoStateEvent event) {
                    if (event.getState() == GoIntoStateEvent.State.ACTIVATED) {
                        eventBus.fireEvent(ProjectReadyEvent.createReadyEvent(
                                ((HasProjectDescriptor)event.getNode()).getProjectDescriptor()));
                        handlerRegistration.removeHandler();
                    }
                }
            });
        } else {
            view.getNodeByPath(new HasStorablePath.StorablePath(descriptor.getPath()), false).then(new Operation<Node>() {
                @Override
                public void apply(final Node node) throws OperationException {
                    openNode(node, descriptor);
                    eventBus.fireEvent(ProjectReadyEvent.createReadyEvent(descriptor));
                }
            });
        }
    }

    private void openNode(Node node, ProjectDescriptor descriptor) {
        appContext.addOpenedProject(descriptor);

        CurrentProject currentProject = new CurrentProject(descriptor);

        appContext.setCurrentProject(currentProject);

        boolean isProjectDescriptorNode = node instanceof ProjectDescriptorNode;

        if (isProjectDescriptorNode) {
            ProjectDescriptorNode descriptorNode = (ProjectDescriptorNode)node;

            descriptorNode.setProjectDescriptor(descriptor);
            descriptorNode.setData(descriptor);
        }

        boolean isMutableNode = node instanceof MutableNode;

        if (isMutableNode) {
            MutableNode mutableNode = (MutableNode)node;

            mutableNode.setLeaf(false);

            if (hasProblems(descriptor)) {
                openProblemProject(descriptor);

                return;
            }

            setExpanded(node, true);
        }

        AppStateManager appStateManager = appStateManagerProvider.get();

        appStateManager.restoreCurrentProjectState(descriptor);
    }

    private boolean hasProblems(ProjectDescriptor descriptor) {
        return !descriptor.getProblems().isEmpty();
    }

    private void openProblemProject(final ProjectDescriptor descriptor) {
        ProjectProblemDialog.AskHandler askHandler = new ProjectProblemDialog.AskHandler() {
            @Override
            public void onConfigure() {
                projectWizardPresenter.show(descriptor);
            }

            @Override
            public void onKeepBlank() {
                descriptor.setType(Constants.BLANK_ID);
                updateProject(descriptor);
            }
        };

        ProjectProblemDialog dialog = new ProjectProblemDialog(locale.projectProblemTitle(),
                                                               locale.projectProblemMessage(),
                                                               askHandler);
        dialog.show();
    }

    private void updateProject(final ProjectDescriptor project) {
        final Unmarshallable<ProjectDescriptor> unmarshaller = dtoUnmarshallerFactory.newUnmarshaller(ProjectDescriptor.class);
        projectServiceClient.updateProject(project.getName(), project, new AsyncRequestCallback<ProjectDescriptor>(unmarshaller) {
            @Override
            protected void onSuccess(ProjectDescriptor result) {
                onProjectOpened(new OpenProjectEvent(result));
            }

            @Override
            protected void onFailure(Throwable exception) {
                final String message = dtoFactory.createDtoFromJson(exception.getMessage(), ServiceError.class).getMessage();
                notificationManager.showError(message);
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void onCloseCurrentProject(CloseCurrentProjectEvent event) {
        ProjectDescriptor descriptor = event.getDescriptor();

        AppStateManager appStateManager = appStateManagerProvider.get();

        appStateManager.persistCurrentProjectState(descriptor);

        appContext.removeOpenedProject(descriptor);

        ProjectDescriptorNode descriptorNode = nodeManager.wrap(descriptor);

        if (view.isGoIntoActivated()) {
            view.resetGoIntoMode();
        }

        view.replaceParentNode(descriptorNode);

        view.select(descriptorNode, false);
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
        if (nodes.isEmpty()) {
            return;
        }

        Node selectedNode = nodes.get(0);

        if (selectedNode != null && selectedNode instanceof HasProjectDescriptor) {
            CurrentProject currentProject = appContext.getCurrentProject();

            if (currentProject == null) {
                return;
            }

            ProjectDescriptor descriptor = ((HasProjectDescriptor)selectedNode).getProjectDescriptor();

            currentProject.setProjectDescription(descriptor);

            String projectName = descriptor.getName();

            queryFieldViewer.setProjectName(projectName);

            view.setProjectTitle(projectName);
        }
    }

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
