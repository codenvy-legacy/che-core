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
package org.eclipse.che.ide.statepersistance;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.ide.api.action.Action;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.action.Presentation;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.event.OpenProjectEvent;
import org.eclipse.che.ide.api.event.PersistProjectTreeStateEvent;
import org.eclipse.che.ide.api.event.PersistProjectTreeStateHandler;
import org.eclipse.che.ide.api.event.ProjectActionEvent;
import org.eclipse.che.ide.api.event.ProjectActionHandler;
import org.eclipse.che.ide.api.event.RestoreProjectTreeStateEvent;
import org.eclipse.che.ide.api.event.RestoreProjectTreeStateHandler;
import org.eclipse.che.ide.api.event.WindowActionEvent;
import org.eclipse.che.ide.api.event.WindowActionHandler;
import org.eclipse.che.ide.api.preferences.PreferencesManager;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.statepersistance.dto.ActionDescriptor;
import org.eclipse.che.ide.statepersistance.dto.AppState;
import org.eclipse.che.ide.statepersistance.dto.ProjectState;
import org.eclipse.che.ide.statepersistance.dto.RecentProject;
import org.eclipse.che.ide.ui.toolbar.PresentationFactory;
import org.eclipse.che.ide.util.Pair;
import org.eclipse.che.ide.util.loging.Log;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Responsible for persisting and restoring the Codenvy application's state across sessions.
 * Uses user preferences as storage for serialized state.
 *
 * @author Artem Zatsarynnyy
 */
@Singleton
public class AppStateManager implements WindowActionHandler, ProjectActionHandler {

    /** The name of the property for the mappings in user preferences. */
    protected static final String PREFERENCE_PROPERTY_NAME = "CodenvyAppState";

    private AppState appState;

    private final Set<PersistenceComponent>         persistenceComponents;
    private final Map<String, PersistenceComponent> projectTreePersistenceComponents;
    private final EventBus                          eventBus;
    private final PreferencesManager                preferencesManager;
    private final AppContext                        appContext;
    private final DtoFactory                        dtoFactory;
    private final ActionManager                     actionManager;
    private final PresentationFactory               presentationFactory;
    private final ProjectServiceClient              projectServiceClient;

    @Inject
    public AppStateManager(Set<PersistenceComponent> persistenceComponents,
                           Map<String, PersistenceComponent> projectTreePersistenceComponents,
                           EventBus eventBus,
                           PreferencesManager preferencesManager,
                           AppContext appContext,
                           DtoFactory dtoFactory,
                           ActionManager actionManager,
                           PresentationFactory presentationFactory,
                           ProjectServiceClient projectServiceClient) {
        this.persistenceComponents = persistenceComponents;
        this.projectTreePersistenceComponents = projectTreePersistenceComponents;
        this.eventBus = eventBus;
        this.preferencesManager = preferencesManager;
        this.appContext = appContext;
        this.dtoFactory = dtoFactory;
        this.actionManager = actionManager;
        this.presentationFactory = presentationFactory;
        this.projectServiceClient = projectServiceClient;

        bind();
    }

    private void bind() {
        eventBus.addHandler(PersistProjectTreeStateEvent.TYPE, new PersistProjectTreeStateHandler() {
            @Override
            public void onPersist(PersistProjectTreeStateEvent event) {
                persistCurrentProjectState(projectTreePersistenceComponents.values());
            }
        });

        eventBus.addHandler(RestoreProjectTreeStateEvent.TYPE, new RestoreProjectTreeStateHandler() {
            @Override
            public void onRestore(RestoreProjectTreeStateEvent event) {
                final String projectPath = event.getProjectPath();
                final ProjectState projectState = appState.getProjects().get(projectPath);
                restoreCurrentProjectState(projectState);
            }
        });
    }

    @Override
    public void onWindowClosing(WindowActionEvent event) {
        final CurrentProject currentProject = appContext.getCurrentProject();
        final RecentProject recentProject = appState.getRecentProject();

        if (currentProject != null) {
            ProjectDescriptor descriptor = currentProject.getRootProject();
            final String workspaceId = appContext.getWorkspace().getId();
            final String projectPath = "/" + descriptor.getWorkspaceName() + descriptor.getPath();

            recentProject.setWorkspaceId(workspaceId);
            recentProject.setPath(projectPath);

            persistCurrentProjectState(persistenceComponents);
        } else { //when none opened projects
            recentProject.setWorkspaceId("");
            recentProject.setPath("");
        }
        writeStateToPreferences();
    }

    @Override
    public void onWindowClosed(WindowActionEvent event) {
    }

    @Override
    public void onProjectOpened(ProjectActionEvent event) {
        CurrentProject rootProject = appContext.getCurrentProject();
        if (rootProject == null) {
            return;
        }
        String workspaceName = rootProject.getRootProject().getWorkspaceName();
        final String fullProjectPath = "/" + workspaceName + event.getProject().getPath();

        final ProjectState projectState = appState.getProjects().get(fullProjectPath);
        if (projectState != null) {
            restoreCurrentProjectState(projectState);
        }
    }

    @Override
    public void onProjectClosing(ProjectActionEvent event) {
        persistCurrentProjectState(persistenceComponents);
        writeStateToPreferences();
    }

    @Override
    public void onProjectClosed(ProjectActionEvent event) {
    }

    /**
     * Start the manager.
     *
     * @param openRecentProject
     *         specifies whether the recent opened project should be re-opened
     */
    public void start(boolean openRecentProject) {
        eventBus.addHandler(WindowActionEvent.TYPE, this);
        eventBus.addHandler(ProjectActionEvent.TYPE, this);

        readStateFromPreferences();
        openRecentProject(openRecentProject);
    }

    private void openRecentProject(final boolean openRecentProject) {
        // don't re-open recent project if some project name was provided
        if (!openRecentProject) {
            return;
        }
        final RecentProject recentProject = appState.getRecentProject();
        final String recentProjectPath = recentProject.getPath();

        if (recentProjectPath != null && !recentProjectPath.isEmpty()) {

            int start = recentProjectPath.lastIndexOf("/");
            String projectPath = recentProjectPath.substring(start);

            projectServiceClient.getProject(projectPath, new AsyncRequestCallback<ProjectDescriptor>() {
                @Override
                protected void onSuccess(ProjectDescriptor result) {
                    openRecentProject();
                }

                @Override
                protected void onFailure(Throwable exception) {
                    appState.getProjects().remove(recentProjectPath);
                    recentProject.setPath("");
                    recentProject.setWorkspaceId("");
                    writeStateToPreferences();
                }
            });
        }
    }

    private void readStateFromPreferences() {
        final String json = preferencesManager.getValue(PREFERENCE_PROPERTY_NAME);
        if (json == null) {
            appState = dtoFactory.createDto(AppState.class);
            initClearRecentProject(appState);
        } else {
            try {
                appState = dtoFactory.createDtoFromJson(json, AppState.class);
                if (appState.getRecentProject() == null) {
                    initClearRecentProject(appState);
                }
            } catch (Exception e) {
                // create 'clear' state if there's any error
                appState = dtoFactory.createDto(AppState.class);
                initClearRecentProject(appState);
            }
        }
    }

    private void initClearRecentProject(AppState appState) {
        final RecentProject recentProject = dtoFactory.createDto(RecentProject.class);
        recentProject.setPath("");
        recentProject.setWorkspaceId("");
        appState.setRecentProject(recentProject);
    }

    private void writeStateToPreferences() {
        final String json = dtoFactory.toJson(appState);
        preferencesManager.setValue(PREFERENCE_PROPERTY_NAME, json);
        preferencesManager.flushPreferences(new AsyncCallback<Map<String, String>>() {
            @Override
            public void onSuccess(Map<String, String> result) {
            }

            @Override
            public void onFailure(Throwable caught) {
                Log.error(AppStateManager.class, "Failed to store app's state to user's preferences");
            }
        });
    }

    private void openRecentProject() {
        RecentProject recentProject = appState.getRecentProject();
        if (recentProject != null) {
            final String projectPath = recentProject.getPath();

            if (projectPath != null && !projectPath.isEmpty()) {

                int start = projectPath.lastIndexOf("/");
                String projectName = projectPath.substring(start);

                eventBus.fireEvent(new OpenProjectEvent(projectName));
            }
        }
    }

    /** Restores state of the currently opened project. */
    private void restoreCurrentProjectState(@Nonnull ProjectState projectState) {
        final List<ActionDescriptor> actions = projectState.getActions();
        final List<Pair<Action, ActionEvent>> actionsToPerform = new ArrayList<>(actions.size());

        for (ActionDescriptor a : actions) {
            final Action action = actionManager.getAction(a.getId());
            if (action == null) {
                continue;
            }

            final Presentation presentation = presentationFactory.getPresentation(action);
            final ActionEvent event = new ActionEvent("", presentation, actionManager, 0, a.getParameters());
            actionsToPerform.add(new Pair<>(action, event));
        }

        final Promise<Void> promise = actionManager.performActions(actionsToPerform, false);
        promise.catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError arg) throws OperationException {
                Log.info(AppStateManager.class, "Failed to restore project's state");
            }
        });
    }

    /** Persist state of the currently opened project. */
    private void persistCurrentProjectState(Collection<PersistenceComponent> persistenceComponents) {
        final CurrentProject currentProject = appContext.getCurrentProject();
        if (currentProject == null) {
            return;
        }

        ProjectDescriptor descriptor = currentProject.getRootProject();

        final String projectPath = descriptor.getPath();
        final String fullProjectPath = "/" + descriptor.getWorkspaceName() + projectPath;

        final ProjectState projectState = dtoFactory.createDto(ProjectState.class);

        appState.getProjects().put(fullProjectPath, projectState);

        final List<ActionDescriptor> actions = projectState.getActions();

        for (PersistenceComponent persistenceComponent : persistenceComponents) {
            actions.addAll(persistenceComponent.getActions(projectPath));
        }
    }
}
