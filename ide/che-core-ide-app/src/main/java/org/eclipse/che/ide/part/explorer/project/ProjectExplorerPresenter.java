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

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.machine.gwt.client.events.ExtServerStateEvent;
import org.eclipse.che.api.machine.gwt.client.events.ExtServerStateHandler;
import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.Constants;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.api.promises.client.callback.AsyncPromiseHelper;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.Resources;
import org.eclipse.che.ide.api.action.Action;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.action.Presentation;
import org.eclipse.che.ide.api.action.PromisableAction;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.event.ConfigureProjectEvent;
import org.eclipse.che.ide.api.event.ConfigureProjectHandler;
import org.eclipse.che.ide.api.event.ModuleCreatedEvent;
import org.eclipse.che.ide.api.event.ModuleCreatedEvent.ModuleCreatedHandler;
import org.eclipse.che.ide.api.event.project.CreateProjectEvent;
import org.eclipse.che.ide.api.event.project.CreateProjectHandler;
import org.eclipse.che.ide.api.event.project.CurrentProjectChangedEvent;
import org.eclipse.che.ide.api.event.project.DeleteProjectEvent;
import org.eclipse.che.ide.api.event.project.DeleteProjectHandler;
import org.eclipse.che.ide.api.event.project.ProjectUpdatedEvent;
import org.eclipse.che.ide.api.event.project.ProjectUpdatedEvent.ProjectUpdatedHandler;
import org.eclipse.che.ide.api.mvp.View;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.parts.HasView;
import org.eclipse.che.ide.api.parts.PerspectiveManager;
import org.eclipse.che.ide.api.parts.ProjectExplorerPart;
import org.eclipse.che.ide.api.parts.base.BasePresenter;
import org.eclipse.che.ide.api.project.node.HasProjectConfig;
import org.eclipse.che.ide.api.project.node.HasStorablePath;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.api.selection.Selection;
import org.eclipse.che.ide.core.problemDialog.ProjectProblemDialog;
import org.eclipse.che.ide.part.explorer.project.ProjectExplorerView.ActionDelegate;
import org.eclipse.che.ide.project.event.ProjectExplorerLoadedEvent;
import org.eclipse.che.ide.project.event.ResourceNodeDeletedEvent;
import org.eclipse.che.ide.project.event.ResourceNodeDeletedEvent.ResourceNodeDeletedHandler;
import org.eclipse.che.ide.project.event.ResourceNodeRenamedEvent;
import org.eclipse.che.ide.project.event.ResourceNodeRenamedEvent.ResourceNodeRenamedHandler;
import org.eclipse.che.ide.project.node.ItemReferenceBasedNode;
import org.eclipse.che.ide.project.node.ModuleNode;
import org.eclipse.che.ide.project.node.NodeManager;
import org.eclipse.che.ide.project.node.ProjectNode;
import org.eclipse.che.ide.projecttype.wizard.presenter.ProjectWizardPresenter;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.ui.smartTree.event.BeforeExpandNodeEvent;
import org.eclipse.che.ide.ui.smartTree.event.CollapseNodeEvent;
import org.eclipse.che.ide.ui.smartTree.event.ExpandNodeEvent;
import org.eclipse.che.ide.ui.toolbar.PresentationFactory;
import org.eclipse.che.ide.util.loging.Log;
import org.eclipse.che.ide.workspace.BrowserQueryFieldRenderer;
import org.vectomatic.dom.svg.ui.SVGResource;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.che.api.promises.client.callback.PromiseHelper.newCallback;
import static org.eclipse.che.api.promises.client.callback.PromiseHelper.newPromise;

/**
 * Project explorer presenter. Handle basic logic to control project tree display.
 *
 * @author Vlad Zhukovskiy
 * @author Dmitry Shnurenko
 */
@Singleton
public class ProjectExplorerPresenter extends BasePresenter implements ActionDelegate,
                                                                       ProjectExplorerPart,
                                                                       HasView,
                                                                       ExtServerStateHandler,
                                                                       ProjectUpdatedHandler,
                                                                       CreateProjectHandler,
                                                                       DeleteProjectHandler,
                                                                       ConfigureProjectHandler,
                                                                       ResourceNodeRenamedHandler,
                                                                       ResourceNodeDeletedHandler,
                                                                       ModuleCreatedHandler {
    private final ProjectExplorerView          view;
    private final EventBus                     eventBus;
    private final NodeManager                  nodeManager;
    private final AppContext                   appContext;
    private final ActionManager                actionManager;
    private final Provider<PerspectiveManager> managerProvider;
    private final BrowserQueryFieldRenderer    queryFieldViewer;
    private final ProjectWizardPresenter       projectConfigWizard;
    private final CoreLocalizationConstant     locale;
    private final Resources                    resources;
    private final DtoUnmarshallerFactory       dtoUnmarshaller;
    private final ProjectServiceClient         projectService;
    private final NotificationManager          notificationManager;
    private final CurrentProject               currentProject;

    public static final int PART_SIZE = 250;

    private boolean hiddenFilesAreShown;

    @Inject
    public ProjectExplorerPresenter(ProjectExplorerView view,
                                    EventBus eventBus,
                                    NodeManager nodeManager,
                                    AppContext appContext,
                                    ActionManager actionManager,
                                    Provider<PerspectiveManager> managerProvider,
                                    BrowserQueryFieldRenderer queryFieldViewer,
                                    ProjectWizardPresenter projectConfigWizard,
                                    CoreLocalizationConstant locale,
                                    Resources resources,
                                    DtoUnmarshallerFactory dtoUnmarshaller,
                                    ProjectServiceClient projectService,
                                    NotificationManager notificationManager) {
        this.view = view;
        this.view.setDelegate(this);

        this.eventBus = eventBus;
        this.nodeManager = nodeManager;
        this.appContext = appContext;
        this.actionManager = actionManager;
        this.managerProvider = managerProvider;
        this.queryFieldViewer = queryFieldViewer;
        this.projectConfigWizard = projectConfigWizard;
        this.locale = locale;
        this.resources = resources;
        this.dtoUnmarshaller = dtoUnmarshaller;
        this.projectService = projectService;
        this.notificationManager = notificationManager;
        this.currentProject = new CurrentProject();

        eventBus.addHandler(CreateProjectEvent.TYPE, this);
        eventBus.addHandler(DeleteProjectEvent.TYPE, this);
        eventBus.addHandler(ConfigureProjectEvent.TYPE, this);
        eventBus.addHandler(ProjectUpdatedEvent.getType(), this);
        eventBus.addHandler(ExtServerStateEvent.TYPE, this);
        eventBus.addHandler(ResourceNodeRenamedEvent.getType(), this);
        eventBus.addHandler(ResourceNodeDeletedEvent.getType(), this);
        eventBus.addHandler(ModuleCreatedEvent.getType(), this);
    }

    /** {@inheritDoc} */
    @Override
    public void onExtServerStarted(ExtServerStateEvent event) {
        reloadProjectTree();
    }

    /** {@inheritDoc} */
    @Override
    public void onProjectCreated(CreateProjectEvent event) {
        final ProjectConfigDto projectConfig = event.getProjectConfig();

        if (projectConfig == null) {
            return;
        }

        if (view.isGoIntoActivated()) {
            view.resetGoIntoMode();
        }

        final ProjectNode node = nodeManager.wrap(projectConfig);

        view.addNode(null, node);
        view.select(node, false);

        if (!projectConfig.getProblems().isEmpty()) {
            notificationManager.notify("Project", locale.projectExplorerDetectedUnconfiguredProject(), projectConfig);
            askUserToSetUpProject(projectConfig);
        }

    }

    //TODO: temporary fix to make accept factory working
    public void reloadProjectTree() {
        nodeManager.getProjectNodes().then(new Operation<List<Node>>() {
            @Override
            public void apply(List<Node> nodes) throws OperationException {
                view.removeAllNodes();
                view.addNodes(null, nodes);
                //actually we don't need to setup current project in application context
                //because when we apply selection to first node, then tree will fires
                //selection changed event and app context will be filled in method
                //updateAppContext(List<Nodes>)

                eventBus.fireEvent(new ProjectExplorerLoadedEvent(nodes));
            }
        }).catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError arg) throws OperationException {
                notificationManager.notify("Project", locale.projectExplorerProjectsLoadFailed());
            }
        });
    }

    private void askUserToSetUpProject(final ProjectConfigDto descriptor) {
        ProjectProblemDialog.AskHandler askHandler = new ProjectProblemDialog.AskHandler() {
            @Override
            public void onConfigure() {
                projectConfigWizard.show(descriptor);
            }

            @Override
            public void onKeepBlank() {
                descriptor.setType(Constants.BLANK_ID);

                updateProject(descriptor).then(new Operation<ProjectConfigDto>() {
                    @Override
                    public void apply(ProjectConfigDto updatedConfig) throws OperationException {
                        eventBus.fireEvent(new ProjectUpdatedEvent(updatedConfig.getPath(), updatedConfig));
                    }
                }).catchError(new Operation<PromiseError>() {
                    @Override
                    public void apply(PromiseError arg) throws OperationException {
                        notificationManager.notify("Project", locale.projectExplorerProjectConfigurationFailed(), descriptor);
                        Log.warn(getClass(), arg.getMessage());
                    }
                });
            }
        };

        new ProjectProblemDialog(locale.projectProblemTitle(),
                                 locale.projectProblemMessage(),
                                 askHandler).show();
    }

    /** {@inheritDoc} */
    @Override
    public void onProjectUpdated(ProjectUpdatedEvent event) {
        ProjectNode node = null;

        for (Node seekNode : view.getAllNodes()) {
            if (seekNode instanceof ProjectNode &&
                ((ProjectNode)seekNode).getData().getPath().equals(event.getPath())) {
                node = (ProjectNode)seekNode;
                break;
            }
        }

        if (node == null) {
            return;
        }

        final String oldNodeId = view.getNodeIdProvider().getKey(node);

        Map<String, Node> oldIdToNode = new HashMap<>();
        for (Node n : view.getAllNodes(node)) {
            oldIdToNode.put(view.getNodeIdProvider().getKey(n), n);
        }

        final ProjectConfigDto updatedDescriptor = event.getUpdatedProjectDescriptor();

        node.setData(updatedDescriptor);
        node.setProjectConfig(updatedDescriptor);

        if (!view.reIndex(oldNodeId, node)) {
            Log.info(getClass(), "Node wasn't re-indexed");
        }

        for (Map.Entry<String, Node> entry : oldIdToNode.entrySet()) {
            if (!view.reIndex(entry.getKey(), entry.getValue())) {
                Log.info(getClass(), "Node wasn't re-indexed");
            }
        }

        view.refresh(node);

        if (view.isExpanded(node) && view.isLoaded(node)) {
            view.reloadChildren(node, true);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onProjectDeleted(DeleteProjectEvent event) {
        ProjectNode toDelete = null;

        for (Node node : view.getRootNodes()) {
            if (node instanceof ProjectNode &&
                ((ProjectNode)node).getData().getPath().equals(event.getProjectConfig().getPath())) {
                toDelete = (ProjectNode)node;
            }
        }

        if (toDelete == null) {
            return;
        }

        view.removeNode(toDelete, true);
    }

    /** {@inheritDoc} */
    @Override
    public void onResourceRenamedEvent(final ResourceNodeRenamedEvent event) {
        //Here we have old node with old data object and new renamed data object
        //so we need fetch storable path from it and call tree to find node by this
        //storable path. When tree will find our node by path, the old node will be
        //deleted automatically. Otherwise if we can't determine storable path from
        //node, then we just take parent node and try navigate on it in the tree.

        final String oldNodeId = view.getNodeIdProvider().getKey(event.getNode());

        Map<String, Node> oldIdToNode = new HashMap<>();
        for (Node n : view.getAllNodes(event.getNode())) {
            oldIdToNode.put(view.getNodeIdProvider().getKey(n), n);
        }

        if (event.getNode() instanceof ItemReferenceBasedNode) {
            ItemReference newDTO = (ItemReference)event.getNewDataObject();
            ItemReferenceBasedNode node = (ItemReferenceBasedNode)event.getNode();

            node.setData(newDTO);
        } else if (event.getNode() instanceof ModuleNode) {
            ProjectConfigDto newDTO = (ProjectConfigDto)event.getNewDataObject();
            ModuleNode node = (ModuleNode)event.getNode();
            node.setData(newDTO);
        }

        if (!view.reIndex(oldNodeId, event.getNode())) {
            Log.info(getClass(), "Node wasn't re-indexed");
        }

        for (Map.Entry<String, Node> entry : oldIdToNode.entrySet()) {
            if (!view.reIndex(entry.getKey(), entry.getValue())) {
                Log.info(getClass(), "Node wasn't re-indexed");
            }
        }

        view.refresh(event.getNode());

        if (!event.getNode().isLeaf() && view.isLoaded(event.getNode())) {
            view.reloadChildren(event.getNode(), true);
        }

        //here we have possible odd behaviour: after renaming directory we should perform checking structure of the expanded directories
        //cause some of them after renaming may become source directory and we need to replace them with correct nodes, possible solution
        //is to use node interceptors to intercept expanded nodes
    }

    /** {@inheritDoc} */
    @Override
    public void onResourceEvent(ResourceNodeDeletedEvent event) {
        view.removeNode(event.getNode(), true);
    }

    /** {@inheritDoc} */
    @Override
    public void onExtServerStopped(ExtServerStateEvent event) {
        view.removeAllNodes();
        appContext.setCurrentProject(null);
        queryFieldViewer.setProjectName("");
        notificationManager.notify("Project", locale.projectExplorerExtensionServerStopped());
    }

    /** {@inheritDoc} */
    @Override
    public void onModuleCreated(ModuleCreatedEvent event) {
        if (isGoIntoActivated()) {
            resetGoIntoMode();
        }

        ProjectConfigDto createdModule = event.getModule();

        String pathToModule = createdModule.getPath();

        String pathToParent = pathToModule.substring(0, pathToModule.lastIndexOf("/"));

        ProjectConfigDto projectConfig = appContext.getCurrentProject().getProjectConfig();

        ProjectConfigDto parentModule = projectConfig.findModule(pathToParent);

        if (parentModule == null) {
            projectConfig.getModules().add(createdModule);
        } else {
            parentModule.getModules().add(createdModule);
        }

        reloadChildren();
    }

    /** {@inheritDoc} */
    @Override
    public void onConfigureProject(ConfigureProjectEvent event) {
        final ProjectConfigDto toConfigure = event.getProject();
        if (toConfigure != null) {
            projectConfigWizard.show(toConfigure);
        }
    }

    private Promise<ProjectConfigDto> updateProject(final ProjectConfigDto project) {
        return newPromise(new AsyncPromiseHelper.RequestCall<ProjectConfigDto>() {
            @Override
            public void makeCall(AsyncCallback<ProjectConfigDto> callback) {
                projectService.updateProject(project.getName(),
                                             project,
                                             newCallback(callback, dtoUnmarshaller.newUnmarshaller(ProjectConfigDto.class)));
            }
        }).catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError arg) throws OperationException {
                Log.warn(getClass(), arg.getMessage());
                notificationManager.notify("Project", locale.projectExplorerProjectUpdateFailed(), project);
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public View getView() {
        return view;
    }

    /** {@inheritDoc} */
    @Override
    public void setVisible(boolean visible) {
        view.setVisible(visible);
    }

    /** {@inheritDoc} */
    @NotNull
    @Override
    public String getTitle() {
        return locale.projectExplorerButtonTitle();
    }

    /** {@inheritDoc} */
    @Override
    public SVGResource getTitleSVGImage() {
        return resources.projectExplorerPartIcon();
    }

    /** {@inheritDoc} */
    @Nullable
    @Override
    public String getTitleToolTip() {
        return locale.projectExplorerPartTooltip();
    }

    /** {@inheritDoc} */
    @Override
    public int getSize() {
        return PART_SIZE;
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
            appContext.setCurrentProject(null);
            queryFieldViewer.setProjectName("");
            return;
        } else if (nodes.size() > 1) {
            return;
        }

        Node selectedNode = nodes.get(0);

        if (selectedNode != null && selectedNode instanceof HasProjectConfig) {
            ProjectConfigDto selectedProjectConfig = ((HasProjectConfig)selectedNode).getProjectConfig();

            ProjectConfigDto rootProjectConfig = getRootConfig(selectedNode);

            if (rootProjectConfig == null) {
                Log.error(ProjectExplorerPresenter.class, "Can't set root project config. App context contains not valid project configs.");

                return;
            }

            currentProject.setRootProject(rootProjectConfig);
            currentProject.setProjectConfig(selectedProjectConfig);

            appContext.setCurrentProject(currentProject);

            eventBus.fireEvent(new CurrentProjectChangedEvent(selectedProjectConfig));

            queryFieldViewer.setProjectName(rootProjectConfig.getName());
        }
    }

    private ProjectConfigDto getRootConfig(Node selectedNode) {
        Node parent = selectedNode.getParent();
        if (parent == null && selectedNode instanceof ProjectNode) {
            return ((ProjectNode)selectedNode).getData();
        }

        return getRootConfig(parent);
    }

    /** {@inheritDoc} */
    @Override
    public void onDeleteKeyPressed() {
        final Action deleteItemAction = actionManager.getAction("deleteItem");

        final Presentation presentation = new PresentationFactory().getPresentation(deleteItemAction);
        final PerspectiveManager manager = managerProvider.get();
        final ActionEvent event = new ActionEvent(presentation, actionManager, manager);


        if (deleteItemAction instanceof PromisableAction) {
            ((PromisableAction)deleteItemAction).promise(event);
        } else {
            deleteItemAction.actionPerformed(event);
        }
    }

    /**
     * Set node expand state.
     *
     * @param node
     *         node to expand
     * @param expand
     *         true - if node should be expanded, otherwise - collapsed
     */
    public void setExpanded(Node node, boolean expand) {
        view.setExpanded(node, expand);
    }

    /**
     * Activate "Go Into" mode on specified node if.
     * Node should support this mode. See {@link Node#supportGoInto()}.
     *
     * @param node
     *         node which should be activated in "Go Into" mode
     */
    public void goInto(Node node) {
        view.setGoIntoModeOn(node);
    }

    public void reloadChildren() {
        view.reloadChildren(null, true);
    }

    public void reloadChildren(Node node) {
        view.reloadChildren(node);
    }

    /**
     * Reload children by node type.
     * Useful method if you want to reload specified nodes, e.g. External Liraries.
     *
     * @param type
     *         node type to update
     */
    public void reloadChildrenByType(Class<?> type) {
        view.reloadChildrenByType(type);
    }

    /**
     * Reset "Go Into" mode. If tree wasn't in "Go Into" mode than this method will do nothing.
     */
    public void resetGoIntoMode() {
        view.resetGoIntoMode();
    }

    /**
     * Get "Go Into" state on current tree.
     *
     * @return true - if "Go Into" mode has been activated.
     */
    public boolean isGoIntoActivated() {
        return view.isGoIntoActivated();
    }

    /**
     * Expand all non-leaf nodes.
     * <p/>
     * CAUTION! Use this method for your own risk, because it may took a lot of traffic to the server.
     */
    public void expandAll() {
        view.expandAll();
    }

    /**
     * Collapse all non-leaf nodes.
     */
    public void collapseAll() {
        view.collapseAll();
    }

    /**
     * Get all rendered and visible nodes.
     *
     * @return list of visible nodes
     */
    public List<Node> getVisibleNodes() {
        return view.getVisibleNodes();
    }

    /**
     * Configure tree to show or hide files that starts with ".", e.g. hidden files.
     *
     * @param show
     *         true - if those files should be shown, otherwise - false
     */
    public void showHiddenFiles(boolean show) {
        hiddenFilesAreShown = show;
        view.showHiddenFilesForAllExpandedNodes(show);
    }

    /**
     * Retrieve status of showing hidden files.
     *
     * @return true - if hidden files are shown, otherwise - false
     */
    public boolean isShowHiddenFiles() {
        return hiddenFilesAreShown;
    }

    /**
     * Search node in the project explorer tree by storable path.
     *
     * @param path
     *         path to node
     * @return promise object with found node or promise error if node wasn't found
     */
    public Promise<Node> getNodeByPath(HasStorablePath path) {
        return view.getNodeByPath(path, false, true);
    }

    /**
     * Search node in the project explorer tree by storable path.
     *
     * @param path
     *         path to node
     * @param forceUpdate
     *         force children reload
     * @return promise object with found node or promise error if node wasn't found
     */
    public Promise<Node> getNodeByPath(HasStorablePath path, boolean forceUpdate) {
        return view.getNodeByPath(path, forceUpdate, true);
    }

    /**
     * Search node in the project explorer tree by storable path.
     *
     * @param path
     *         path to node
     * @param forceUpdate
     *         force children reload
     * @param closeMissingFiles
     *         allow editor to close removed files if they were opened
     * @return promise object with found node or promise error if node wasn't found
     */
    public Promise<Node> getNodeByPath(HasStorablePath path, boolean forceUpdate, boolean closeMissingFiles) {
        return view.getNodeByPath(path, forceUpdate, closeMissingFiles);
    }

    /**
     * Set selection on node in project tree.
     *
     * @param item
     *         node which should be selected
     * @param keepExisting
     *         keep current selection or reset it
     */
    public void select(Node item, boolean keepExisting) {
        view.select(item, keepExisting);
    }

    /**
     * Set selection on nodes in project tree.
     *
     * @param items
     *         nodes which should be selected
     * @param keepExisting
     *         keep current selection or reset it
     */
    public void select(List<Node> items, boolean keepExisting) {
        view.select(items, keepExisting);
    }

    /**
     * Check if node is expanded or not.
     *
     * @param node
     *         node to check
     * @return true - if node expanded, otherwise - false
     */
    public boolean isExpanded(Node node) {
        return view.isExpanded(node);
    }

    /**
     * Check if node is loaded or not.
     *
     * @param node
     *         node to check
     * @return true - if node is loaded, otherwise - false
     */
    public boolean isLoaded(Node node) {
        return view.isLoaded(node);
    }

    /**
     * Navigate to the storable source node in the project tree.
     * Perform node search and setting selection.
     *
     * @param path
     *         path to search
     */
    public void scrollFromSource(HasStorablePath path) {
        view.scrollFromSource(path);
    }

    /**
     * Register node expand handler to allow custom functionality retrieve expand event from the project tree.
     *
     * @param handler
     *         expand handler
     * @return handler registration
     */
    public HandlerRegistration addExpandHandler(ExpandNodeEvent.ExpandNodeHandler handler) {
        return view.addExpandHandler(handler);
    }

    /**
     * Register befor node expand handler to allow custom functionality retrieve before expand event from the project tree.
     *
     * @param handler
     *         before expand handler
     * @return handler registration
     */
    public HandlerRegistration addBeforeExpandHandler(BeforeExpandNodeEvent.BeforeExpandNodeHandler handler) {
        return view.addBeforeExpandHandler(handler);
    }

    /**
     * Register node collapse handler to allow custom functionality retrieve collapse event from the project tree.
     *
     * @param handler
     *         collapse handler
     * @return handler registration
     */
    public HandlerRegistration addCollapseHandler(CollapseNodeEvent.CollapseNodeHandler handler) {
        return view.addCollapseHandler(handler);
    }

    /**
     * Remove node from the project tree.
     *
     * @param node
     *         node which should be remove
     * @param closeMissingFiles
     *         true if opened nodes in editor part should be closed
     */
    public void removeNode(Node node, boolean closeMissingFiles) {
        view.removeNode(node, closeMissingFiles);
    }

    /**
     * Perform visual scroll to node in the Project Explorer.
     *
     * @param node
     *         node
     */
    public void scrollToNode(Node node) {
        view.scrollToNode(node);
    }

    /** Returns all root nodes. */
    public List<Node> getRootNodes() {
        return view.getRootNodes();
    }

}
