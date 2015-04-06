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
package org.eclipse.che.ide.state;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.api.user.shared.dto.ProfileDescriptor;
import org.eclipse.che.ide.actions.OpenFileAction;
import org.eclipse.che.ide.api.action.Action;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.action.Presentation;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.event.OpenProjectEvent;
import org.eclipse.che.ide.api.event.ProjectActionEvent;
import org.eclipse.che.ide.api.event.ProjectActionHandler;
import org.eclipse.che.ide.api.event.WindowActionEvent;
import org.eclipse.che.ide.api.event.WindowActionHandler;
import org.eclipse.che.ide.api.preferences.PreferencesManager;
import org.eclipse.che.ide.collections.StringMap;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.state.dto.ActionDescriptor;
import org.eclipse.che.ide.state.dto.AppState;
import org.eclipse.che.ide.state.dto.ProjectState;
import org.eclipse.che.ide.toolbar.PresentationFactory;
import org.eclipse.che.ide.util.Pair;
import org.eclipse.che.ide.util.loging.Log;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.eclipse.che.ide.actions.OpenFileAction.FILE_PARAM_ID;

/**
 * Responsible for persisting and restoring the Codenvy application's state across sessions.
 * Uses user preferences as storage for serialized state.
 *
 * @author Artem Zatsarynnyy
 */
@Singleton
public class AppStateManager implements WindowActionHandler, ProjectActionHandler {

    /** The name of the property for the mappings in user preferences. */
    private static final String PREFERENCE_PROPERTY_NAME = "CodenvyAppState";

    private AppState myAppState;

    private final Set<Persister> persisters;
    private final EventBus eventBus;
    private final PreferencesManager    preferencesManager;
    private final AppContext            appContext;
    private final Provider<EditorAgent> editorAgentProvider;
    private final DtoFactory            dtoFactory;
    private final ActionManager         actionManager;
    private final PresentationFactory   presentationFactory;
    private final OpenFileAction        openFileAction;

    @Inject
    public AppStateManager(Set<Persister> persisters,
                           EventBus eventBus,
                           PreferencesManager preferencesManager,
                           AppContext appContext,
                           Provider<EditorAgent> editorAgentProvider,
                           DtoFactory dtoFactory,
                           ActionManager actionManager,
                           PresentationFactory presentationFactory,
                           OpenFileAction openFileAction) {
        this.persisters = persisters;
        this.eventBus = eventBus;
        this.preferencesManager = preferencesManager;
        this.appContext = appContext;
        this.editorAgentProvider = editorAgentProvider;
        this.dtoFactory = dtoFactory;
        this.actionManager = actionManager;
        this.presentationFactory = presentationFactory;
        this.openFileAction = openFileAction;
    }

    @Override
    public void onWindowClosing(WindowActionEvent event) {
        final CurrentProject currentProject = appContext.getCurrentProject();
        if (currentProject == null) {
            myAppState.setLastProjectPath("");
        } else {
            final String projectPath = currentProject.getRootProject().getPath();
            myAppState.setLastProjectPath(projectPath);
            persistCurrentProjectState();
        }
        writeStateToPreferences();
    }

    @Override
    public void onWindowClosed(WindowActionEvent event) {
    }

    @Override
    public void onProjectOpened(ProjectActionEvent event) {
        final String projectPath = event.getProject().getPath();
        final ProjectState projectState = myAppState.getProjects().get(projectPath);
        if (projectState != null) {
            restoreCurrentProjectState(projectState);
        }
    }

    @Override
    public void onProjectClosing(ProjectActionEvent event) {
        persistCurrentProjectState();
        writeStateToPreferences();
    }

    @Override
    public void onProjectClosed(ProjectActionEvent event) {
    }

    public void start(boolean openLastProject) {
        readStateFromPreferences();

        // don't re-open last project if some project name was provided
        if (openLastProject) {
            openLastProject();
        }
    }

    private void readStateFromPreferences() {
        final String json = preferencesManager.getValue(PREFERENCE_PROPERTY_NAME);
        if (json == null) {
            myAppState = dtoFactory.createDto(AppState.class);
        } else {
            try {
                myAppState = dtoFactory.createDtoFromJson(json, AppState.class);
            } catch (Exception e) {
                // create 'clear' state if there's any error
                myAppState = dtoFactory.createDto(AppState.class);
            }
        }
    }

    private void writeStateToPreferences() {
        final String json = dtoFactory.toJson(myAppState);
        preferencesManager.setValue(PREFERENCE_PROPERTY_NAME, json);
        preferencesManager.flushPreferences(new AsyncCallback<ProfileDescriptor>() {
            @Override
            public void onSuccess(ProfileDescriptor result) {
            }

            @Override
            public void onFailure(Throwable caught) {
                Log.error(AppStateManager.class, "Failed to store app's state to user's preferences");
            }
        });
    }

    private void openLastProject() {
        final String lastProjectPath = myAppState.getLastProjectPath();
        if (lastProjectPath != null && !lastProjectPath.isEmpty()) {
            eventBus.fireEvent(new OpenProjectEvent(lastProjectPath));
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
    private void persistCurrentProjectState() {
        final CurrentProject currentProject = appContext.getCurrentProject();
        if (currentProject == null) {
            return;
        }

        final String projectPath = currentProject.getRootProject().getPath();
        final ProjectState projectState = dtoFactory.createDto(ProjectState.class);
        myAppState.getProjects().put(projectPath, projectState);

        for (Persister persister : persisters) {
            projectState.getActions().addAll(persister.persist(projectPath));
        }

//        projectState.getActions().addAll(saveOpenedFiles(projectPath));
//        projectState.getActions().addAll(saveActiveFile(projectPath));
    }

    private List<ActionDescriptor> saveOpenedFiles(String projectPath) {
        final EditorAgent editorAgent = editorAgentProvider.get();
        final List<ActionDescriptor> actions = new ArrayList<>();
        final String openFileActionId = actionManager.getId(openFileAction);
        final StringMap<EditorPartPresenter> openedEditors = editorAgent.getOpenedEditors();

        for (String filePath : openedEditors.getKeys().asIterable()) {
            final String relFilePath = filePath.replaceFirst(projectPath, "");

            actions.add(dtoFactory.createDto(ActionDescriptor.class)
                                  .withId(openFileActionId)
                                  .withParameters(Collections.singletonMap(FILE_PARAM_ID, relFilePath)));
        }
        return actions;
    }

    private List<ActionDescriptor> saveActiveFile(String projectPath) {
        final EditorAgent editorAgent = editorAgentProvider.get();
        final List<ActionDescriptor> actions = new ArrayList<>();
        final String openFileActionId = actionManager.getId(openFileAction);
        final StringMap<EditorPartPresenter> openedEditors = editorAgent.getOpenedEditors();
        final EditorPartPresenter activeEditor = editorAgent.getActiveEditor();

        if (activeEditor != null) {
            final String activeFilePath = activeEditor.getEditorInput().getFile().getPath();
            // save active file only if it's not the last opened file
            if (openedEditors.getKeys().indexOf(activeFilePath) < openedEditors.getKeys().size() - 1) {
                final String activeFileRelPath = activeFilePath.replaceFirst(projectPath, "");

                actions.add(dtoFactory.createDto(ActionDescriptor.class)
                                      .withId(openFileActionId)
                                      .withParameters(Collections.singletonMap(FILE_PARAM_ID, activeFileRelPath)));
            }
        }
        return actions;
    }
}
