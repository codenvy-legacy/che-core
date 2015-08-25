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
import org.eclipse.che.ide.api.action.Action;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.action.Presentation;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.event.PersistProjectTreeStateEvent;
import org.eclipse.che.ide.api.event.PersistProjectTreeStateHandler;
import org.eclipse.che.ide.api.event.RestoreProjectTreeStateEvent;
import org.eclipse.che.ide.api.event.RestoreProjectTreeStateHandler;
import org.eclipse.che.ide.api.event.WindowActionEvent;
import org.eclipse.che.ide.api.event.WindowActionHandler;
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
public class AppStateManager implements WindowActionHandler, RestoreProjectTreeStateHandler, PersistProjectTreeStateHandler {

    /** The name of the property for the mappings in user preferences. */
    protected static final String PREFERENCE_PROPERTY_NAME = "CodenvyAppState";

    private AppState appState;

    private final Set<PersistenceComponent>         persistenceComponents;
    private final EventBus                          eventBus;
    private final PreferencesManager                preferencesManager;
    private final AppContext                        appContext;
    private final DtoFactory                        dtoFactory;
    private final ActionManager                     actionManager;
    private final PresentationFactory               presentationFactory;
    private final Provider<PerspectiveManager>      managerProvider;
    private final Map<String, PersistenceComponent> projectTreePersistenceComponents;

    @Inject
    public AppStateManager(Set<PersistenceComponent> persistenceComponents,
                           final Map<String, PersistenceComponent> projectTreePersistenceComponents,
                           EventBus eventBus,
                           PreferencesManager preferencesManager,
                           final AppContext appContext,
                           DtoFactory dtoFactory,
                           ActionManager actionManager,
                           PresentationFactory presentationFactory,
                           Provider<PerspectiveManager> managerProvider) {
        this.persistenceComponents = persistenceComponents;
        this.eventBus = eventBus;
        this.preferencesManager = preferencesManager;
        this.appContext = appContext;
        this.dtoFactory = dtoFactory;
        this.actionManager = actionManager;
        this.presentationFactory = presentationFactory;
        this.managerProvider = managerProvider;
        this.projectTreePersistenceComponents = projectTreePersistenceComponents;

        this.eventBus.addHandler(PersistProjectTreeStateEvent.TYPE, this);
        this.eventBus.addHandler(RestoreProjectTreeStateEvent.TYPE, this);
    }

    @Override
    public void onPersist(PersistProjectTreeStateEvent event) {
        persistOpenedProjectsState(projectTreePersistenceComponents.values());
    }

    @Override
    public void onRestore(RestoreProjectTreeStateEvent event) {
        ProjectState projectState = appState.getProjects().get(event.getProjectPath());

        restoreOpenedProjectState(projectState);
    }

    /** Restores state of the currently opened project. */
    private void restoreOpenedProjectState(@Nonnull ProjectState projectState) {
        final List<ActionDescriptor> actions = projectState.getActions();
        final List<Pair<Action, ActionEvent>> actionsToPerform = new ArrayList<>(actions.size());

        for (ActionDescriptor actionDescriptor : actions) {
            Action action = actionManager.getAction(actionDescriptor.getId());
            if (action == null) {
                continue;
            }

            final Presentation presentation = presentationFactory.getPresentation(action);

            PerspectiveManager manager = managerProvider.get();
            final ActionEvent event = new ActionEvent("", presentation, actionManager, manager, actionDescriptor.getParameters());
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

    @Override
    public void onWindowClosed(WindowActionEvent event) {
    }

    @Override
    public void onWindowClosing(WindowActionEvent event) {
        persistOpenedProjectsState(persistenceComponents);

        writeStateToPreferences();
    }

    /** Persist state of the currently opened project. */
    private void persistOpenedProjectsState(Collection<PersistenceComponent> persistenceComponents) {
        List<ProjectDescriptor> openedProjects = appContext.getOpenedProjects();

        for (ProjectDescriptor descriptor : openedProjects) {
            final String projectPath = descriptor.getPath();
            final String fullProjectPath = "/" + descriptor.getWorkspaceId() + projectPath;

            final ProjectState projectState = dtoFactory.createDto(ProjectState.class);

            appState.getProjects().put(fullProjectPath, projectState);

            final List<ActionDescriptor> actions = projectState.getActions();

            for (PersistenceComponent persistenceComponent : persistenceComponents) {
                actions.addAll(persistenceComponent.getActions(projectPath));
            }
        }
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

    /** Start the manager. */
    public void start() {
        readStateFromPreferences();

        eventBus.addHandler(WindowActionEvent.TYPE, this);
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

}
