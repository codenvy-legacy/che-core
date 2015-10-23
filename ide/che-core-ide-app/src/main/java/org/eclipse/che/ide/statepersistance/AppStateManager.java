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
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.api.workspace.shared.dto.UsersWorkspaceDto;
import org.eclipse.che.ide.api.action.Action;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.action.Presentation;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.event.WindowActionEvent;
import org.eclipse.che.ide.api.event.WindowActionHandler;
import org.eclipse.che.ide.api.event.project.CloseCurrentProjectEvent;
import org.eclipse.che.ide.api.event.project.CloseCurrentProjectHandler;
import org.eclipse.che.ide.api.event.project.OpenProjectEvent;
import org.eclipse.che.ide.api.event.project.OpenProjectHandler;
import org.eclipse.che.ide.api.parts.PerspectiveManager;
import org.eclipse.che.ide.api.preferences.PreferencesManager;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.statepersistance.dto.ActionDescriptor;
import org.eclipse.che.ide.statepersistance.dto.AppState;
import org.eclipse.che.ide.statepersistance.dto.ProjectState;
import org.eclipse.che.ide.statepersistance.dto.RecentProject;
import org.eclipse.che.ide.ui.toolbar.PresentationFactory;
import org.eclipse.che.ide.util.Pair;
import org.eclipse.che.ide.util.loging.Log;
import org.eclipse.che.ide.workspace.start.StopWorkspaceEvent;
import org.eclipse.che.ide.workspace.start.StopWorkspaceHandler;

import java.util.ArrayList;
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
public class AppStateManager implements WindowActionHandler,
                                        StopWorkspaceHandler,
                                        OpenProjectHandler,
                                        CloseCurrentProjectHandler {

    /** The name of the property for the mappings in user preferences. */
    public static final String PREFERENCE_PROPERTY_NAME = "CodenvyAppState";

    private final Set<PersistenceComponent>    persistenceComponents;
    private final PreferencesManager           preferencesManager;
    private final AppContext                   appContext;
    private final DtoFactory                   dtoFactory;
    private final ActionManager                actionManager;
    private final PresentationFactory          presentationFactory;
    private final Provider<PerspectiveManager> managerProvider;

    private AppState appState;

    @Inject
    public AppStateManager(Set<PersistenceComponent> persistenceComponents,
                           PreferencesManager preferencesManager,
                           AppContext appContext,
                           DtoFactory dtoFactory,
                           ActionManager actionManager,
                           PresentationFactory presentationFactory,
                           Provider<PerspectiveManager> managerProvider,
                           EventBus eventBus) {
        this.persistenceComponents = persistenceComponents;
        this.preferencesManager = preferencesManager;
        this.appContext = appContext;
        this.dtoFactory = dtoFactory;
        this.actionManager = actionManager;
        this.presentationFactory = presentationFactory;
        this.managerProvider = managerProvider;

        eventBus.addHandler(StopWorkspaceEvent.TYPE, this);
        eventBus.addHandler(WindowActionEvent.TYPE, this);
        eventBus.addHandler(OpenProjectEvent.TYPE, this);
        eventBus.addHandler(CloseCurrentProjectEvent.TYPE, this);

        readStateFromPreferences();
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
        recentProject.setWorkspaceName("");
        appState.setRecentProject(recentProject);
    }

    @Override
    public void onWindowClosing(WindowActionEvent event) {
        for (ProjectDescriptor openedProject : appContext.getOpenedProjects()) {
            persistCurrentProjectState(openedProject);
        }
    }

    private void persistCurrentProjectState(ProjectDescriptor projectDescriptor) {
        String projectPath = projectDescriptor.getPath();
        String fullProjectPath = "/" + projectDescriptor.getWorkspaceId() + projectPath;

        ProjectState projectState = dtoFactory.createDto(ProjectState.class);

        appState.getProjects().put(fullProjectPath, projectState);

        final List<ActionDescriptor> actions = projectState.getActions();

        for (PersistenceComponent persistenceComponent : persistenceComponents) {
            actions.addAll(persistenceComponent.getActions(projectPath));
        }

        writeStateToPreferences();
    }

    private void writeStateToPreferences() {
        final String json = dtoFactory.toJson(appState);
        preferencesManager.setValue(PREFERENCE_PROPERTY_NAME, json);

        flushPreferences();
    }

    private void flushPreferences() {
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

    @Override
    public void onWindowClosed(WindowActionEvent event) {
    }

    @Override
    public void onWorkspaceStopped(UsersWorkspaceDto workspace) {
        appState.setWorkspace(workspace);

        RecentProject recentProject = appState.getRecentProject();
        recentProject.setWorkspaceId(workspace.getId());
        recentProject.setWorkspaceName(workspace.getName());

        writeStateToPreferences();
    }

    @Override
    public void onCloseCurrentProject(CloseCurrentProjectEvent event) {
        persistCurrentProjectState(event.getDescriptor());
    }

    @Override
    public void onProjectOpened(OpenProjectEvent event) {
        ProjectDescriptor projectDescriptor = event.getDescriptor();

        String workspaceId = projectDescriptor.getWorkspaceId();
        String projectPath = projectDescriptor.getPath();

        String fullPath = "/" + workspaceId + projectPath;

        ProjectState projectState = appState.getProjects().get(fullPath);

        if (projectState == null) {
            return;
        }

        List<ActionDescriptor> actions = projectState.getActions();
        List<Pair<Action, ActionEvent>> actionsToPerform = new ArrayList<>(actions.size());

        for (ActionDescriptor actionDescriptor : actions) {
            final Action action = actionManager.getAction(actionDescriptor.getId());
            if (action == null) {
                continue;
            }

            final Presentation presentation = presentationFactory.getPresentation(action);

            PerspectiveManager manager = managerProvider.get();
            final ActionEvent actionEvent = new ActionEvent("", presentation, actionManager, manager, actionDescriptor.getParameters());
            actionsToPerform.add(new Pair<>(action, actionEvent));
        }

        final Promise<Void> promise = actionManager.performActions(actionsToPerform, false);
        promise.catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError arg) throws OperationException {
                Log.info(AppStateManager.class, "Failed to restore project's state");
            }
        });
    }
}
