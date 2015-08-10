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
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.event.ProjectActionEvent;
import org.eclipse.che.ide.api.event.ProjectActionHandler;
import org.eclipse.che.ide.api.mvp.View;
import org.eclipse.che.ide.api.parts.HasView;
import org.eclipse.che.ide.api.parts.base.BasePresenter;
import org.eclipse.che.ide.api.project.node.HasProjectDescriptor;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.api.project.node.event.ProjectPartLoadEvent;
import org.eclipse.che.ide.api.project.node.event.ProjectPartLoadEvent.ProjectPartLoadHandler;
import org.eclipse.che.ide.api.selection.Selection;
import org.eclipse.che.ide.part.explorer.project.NewProjectExplorerView.ActionDelegate;
import org.eclipse.che.ide.project.event.ResourceNodeEvent;
import org.eclipse.che.ide.project.event.ResourceNodeEvent.Event;
import org.eclipse.che.ide.project.event.ResourceNodeEvent.ResourceNodeHandler;
import org.eclipse.che.ide.project.node.ProjectDescriptorNode;
import org.eclipse.che.ide.project.node.ResourceNodeManager;
import org.eclipse.che.ide.api.project.node.HasStorablePath;
import org.eclipse.che.ide.util.loging.Log;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.eclipse.che.ide.project.event.ResourceNodeEvent.Event.CREATED;
import static org.eclipse.che.ide.project.event.ResourceNodeEvent.Event.DELETED;
import static org.eclipse.che.ide.project.event.ResourceNodeEvent.Event.RENAMED;

/**
 * @author Vlad Zhukovskiy
 */
@Singleton
public class NewProjectExplorerPresenter extends BasePresenter implements ActionDelegate,
                                                                          NewProjectExplorerPart,
                                                                          HasView {

    private final NewProjectExplorerView view;
    private final EventBus               eventBus;
    private final ResourceNodeManager resourceNodeManager;
    private final AppContext appContext;

    @Inject
    public NewProjectExplorerPresenter(final NewProjectExplorerView view,
                                       final EventBus eventBus,
                                       final ResourceNodeManager resourceNodeManager,
                                       final AppContext appContext) {
        this.view = view;
        this.eventBus = eventBus;
        this.resourceNodeManager = resourceNodeManager;
        this.appContext = appContext;

        view.setDelegate(this);

        initHandlers();
    }

    @Override
    public void onOpen() {
        super.onOpen();
        if (appContext.getCurrentProject() == null) {
            resourceNodeManager.getProjects()
                               .then(showProjectsList());
        } else {
            ProjectDescriptor rootProject = appContext.getCurrentProject().getRootProject();
            ProjectDescriptorNode projectDescriptorNode = resourceNodeManager.wrap(rootProject);
            view.setRootNode(projectDescriptorNode);
        }
    }

    private void initHandlers() {
        eventBus.addHandler(ProjectPartLoadEvent.getType(), new ProjectPartLoadHandler() {
            /** {@inheritDoc} */
            @Override
            public void onProjectPartLoad(ProjectPartLoadEvent event) {
                if (appContext.getCurrentProject() == null) {
                    resourceNodeManager.getProjects()
                                       .then(showProjectsList());
                } else {
                    ProjectDescriptor rootProject = appContext.getCurrentProject().getRootProject();
                    ProjectDescriptorNode projectDescriptorNode = resourceNodeManager.wrap(rootProject);
                    view.setRootNode(projectDescriptorNode);
                }
            }
        });


        eventBus.addHandler(ProjectActionEvent.TYPE, new ProjectActionHandler() {
            /** {@inheritDoc} */
            @Override
            public void onProjectOpened(ProjectActionEvent event) {
                ProjectDescriptor projectDescriptor = event.getProject();
                ProjectDescriptorNode projectDescriptorNode = resourceNodeManager.wrap(projectDescriptor);
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
                resourceNodeManager.getProjects()
                                   .then(showProjectsList());
            }
        });


        eventBus.addHandler(ResourceNodeEvent.getType(), new ResourceNodeHandler() {
            /** {@inheritDoc} */
            @Override
            public void onResourceEvent(ResourceNodeEvent evt) {
                Event event = evt.getEvent();

                if (event == CREATED) {
                    //process created
                    view.onChildrenCreated(evt.getParent(), evt.getNode());
                } else if (event == DELETED) {
                    //process deleted
                    view.onChildrenRemoved(evt.getNode());
                } else if (event == RENAMED) {
                    //process renamed
                }
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
        return 230;
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
        Set<ProjectDescriptor> selectedDescriptors = new HashSet<>();

        for (Node node : nodes) {
            if (node instanceof HasProjectDescriptor) {
                ProjectDescriptor descriptor = ((HasProjectDescriptor)node).getProjectDescriptor();
                selectedDescriptors.add(descriptor);
            }
        }
    }

    public void expand(HasStorablePath node) {

    }

    public void goInto(Node node) {
        view.goInto(node);
    }

    private Operation<List<Node>> showProjectsList() {
        return new Operation<List<Node>>() {
            @Override
            public void apply(List<Node> nodes) throws OperationException {
                view.setRootNodes(nodes);
            }
        };
    }

    public void scrollFromSource(Object object) {

    }
}
