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
package org.eclipse.che.ide.context;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.factory.shared.dto.Factory;
import org.eclipse.che.api.machine.gwt.client.events.ExtServerStateEvent;
import org.eclipse.che.api.machine.gwt.client.events.ExtServerStateHandler;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.api.workspace.shared.dto.UsersWorkspaceDto;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.app.CurrentUser;
import org.eclipse.che.ide.api.app.StartUpAction;
import org.eclipse.che.ide.api.event.SelectionChangedEvent;
import org.eclipse.che.ide.api.event.SelectionChangedHandler;
import org.eclipse.che.ide.api.event.project.CurrentProjectChangedEvent;
import org.eclipse.che.ide.api.event.project.ProjectUpdatedEvent;
import org.eclipse.che.ide.api.event.project.ProjectUpdatedEvent.ProjectUpdatedHandler;
import org.eclipse.che.ide.api.project.ProjectImpl;
import org.eclipse.che.ide.api.project.node.HasProjectConfig;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.api.selection.Selection;
import org.eclipse.che.ide.project.WorkspaceProjects;
import org.eclipse.che.ide.project.node.ProjectNode;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of {@link AppContext}.
 *
 * @author Vitaly Parfonov
 * @author Artem Zatsarynnyi
 */
@Singleton
public class AppContextImpl implements AppContext, SelectionChangedHandler, ExtServerStateHandler, ProjectUpdatedHandler {

    private final WorkspaceProjects         projectFactory;
    private final EventBus                  eventBus;
    private final BrowserQueryFieldRenderer browserQueryFieldRenderer;
    private final List<String>              projectsInImport;

    private UsersWorkspaceDto   workspace;
    private CurrentProject      currentProject;
    private ProjectImpl         activeProject;
    private CurrentUser         currentUser;
    private Factory             factory;
    private String              devMachineId;
    private String              projectsRoot;
    /**
     * List of actions with parameters which comes from startup URL.
     * Can be processed after IDE initialization as usual after starting
     * Extension Server and Project API initialization.
     */
    private List<StartUpAction> startAppActions;

    @Inject
    public AppContextImpl(WorkspaceProjects projectFactory, EventBus eventBus, BrowserQueryFieldRenderer browserQueryFieldRenderer) {
        this.projectFactory = projectFactory;
        this.eventBus = eventBus;
        this.browserQueryFieldRenderer = browserQueryFieldRenderer;

        projectsInImport = new ArrayList<>();

        eventBus.addHandler(SelectionChangedEvent.TYPE, this);
        eventBus.addHandler(ExtServerStateEvent.TYPE, this);
        eventBus.addHandler(ProjectUpdatedEvent.getType(), this);
    }

    private static ProjectConfigDto getRootConfig(Node selectedNode) {
        Node parent = selectedNode.getParent();
        if (parent == null) {
            if (selectedNode instanceof ProjectNode) {
                return ((ProjectNode)selectedNode).getData();
            }
            return null;
        }

        return getRootConfig(parent);
    }

    @Override
    public UsersWorkspaceDto getWorkspace() {
        return workspace;
    }

    @Override
    public void setWorkspace(UsersWorkspaceDto workspace) {
        this.workspace = workspace;
    }

    @Override
    public String getWorkspaceId() {
        if (workspace == null) {
            throw new IllegalArgumentException(getClass() + " Workspace can not be null.");
        }

        return workspace.getId();
    }

    @Override
    public CurrentProject getCurrentProject() {
        return currentProject;
    }

    /**
     * TODO experimental
     *
     * @return
     */
    public ProjectImpl getActiveProject() {
        return activeProject;
    }

    /**
     * * TODO experimental
     * * @param configDto
     */
    public void setActiveProject(String projectName) {
        //this.activeProject = projectFactory.createProject(configDto);
        this.activeProject = projectFactory.getProject(projectName);
    }

    @Override
    public CurrentUser getCurrentUser() {
        return currentUser;
    }

    @Override
    public void setCurrentUser(CurrentUser currentUser) {
        this.currentUser = currentUser;
    }

    @Override
    public List<String> getImportingProjects() {
        return projectsInImport;
    }

    @Override
    public void addProjectToImporting(String pathToProject) {
        projectsInImport.add(pathToProject);
    }

    @Override
    public void removeProjectFromImporting(String pathToProject) {
        projectsInImport.remove(pathToProject);
    }

    @Override
    public List<StartUpAction> getStartAppActions() {
        return startAppActions;
    }

    @Override
    public void setStartUpActions(List<StartUpAction> startUpActions) {
        this.startAppActions = startUpActions;
    }

    @Override
    public Factory getFactory() {
        return factory;
    }

    @Override
    public void setFactory(Factory factory) {
        this.factory = factory;
    }

    @Override
    public String getDevMachineId() {
        return devMachineId;
    }

    @Override
    public void setDevMachineId(String id) {
        this.devMachineId = id;
    }

    @Override
    public String getProjectsRoot() {
        return projectsRoot;
    }

    @Override
    public void setProjectsRoot(String projectsRoot) {
        this.projectsRoot = projectsRoot;
    }

    @Override
    public void onSelectionChanged(SelectionChangedEvent event) {
        final Selection<?> selection = event.getSelection();
        if (selection instanceof Selection.NoSelectionProvided) {
            return;
        }

        if (selection == null) {
            currentProject = null;
            browserQueryFieldRenderer.setProjectName("");
            return;
        }

        final Object headElement = selection.getHeadElement();
        if (headElement == null) {
            currentProject = null;
            browserQueryFieldRenderer.setProjectName("");
            return;
        }

        currentProject = new CurrentProject();

        if (headElement instanceof HasProjectConfig) {
            final HasProjectConfig hasProjectConfig = (HasProjectConfig)headElement;
            final ProjectConfigDto module = (hasProjectConfig).getProjectConfig();
            currentProject.setProjectConfig(module);

            // TODO experimental
            setActiveProject(module.getName());

            eventBus.fireEvent(new CurrentProjectChangedEvent(module));
        }

        if (headElement instanceof Node) {
            ProjectConfigDto rootConfig = getRootConfig((Node)headElement);
            if (rootConfig == null) {
                rootConfig = currentProject.getProjectConfig();
            }
            currentProject.setRootProject(rootConfig);
            browserQueryFieldRenderer.setProjectName(rootConfig.getName());
        }
    }

    @Override
    public void onExtServerStarted(ExtServerStateEvent event) {
        projectFactory.init(workspace.getProjects());
    }

    @Override
    public void onExtServerStopped(ExtServerStateEvent event) {
        currentProject = null;
        browserQueryFieldRenderer.setProjectName("");
    }

    @Override
    public void onProjectUpdated(ProjectUpdatedEvent event) {
        final ProjectConfigDto updatedProjectDescriptor = event.getUpdatedProjectDescriptor();
        final String updatedProjectDescriptorPath = updatedProjectDescriptor.getPath();

        if (updatedProjectDescriptorPath.equals(currentProject.getProjectConfig().getPath())) {
            currentProject.setProjectConfig(updatedProjectDescriptor);

            // TODO experimental
            setActiveProject(updatedProjectDescriptor.getName());

            eventBus.fireEvent(new CurrentProjectChangedEvent(updatedProjectDescriptor));
        }

        if (updatedProjectDescriptorPath.equals(currentProject.getRootProject().getPath())) {
            currentProject.setRootProject(updatedProjectDescriptor);
            browserQueryFieldRenderer.setProjectName(updatedProjectDescriptor.getName());
        }
    }
}
