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
package org.eclipse.che.ide.statepersistance;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import org.eclipse.che.ide.actions.OpenFileAction;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.project.tree.VirtualFile;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.statepersistance.dto.ActionDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.eclipse.che.ide.actions.OpenFileAction.FILE_PARAM_ID;

/**
 * Component provides sequence of actions which should be performed
 * in order to restore active file for the particular project.
 *
 * @author Artem Zatsarynnyy
 */
@Singleton
public class ActiveFilePersistenceComponent implements PersistenceComponent {
    private final Provider<EditorAgent> editorAgentProvider;
    private final ActionManager         actionManager;
    private final OpenFileAction        openFileAction;
    private final DtoFactory            dtoFactory;

    @Inject
    public ActiveFilePersistenceComponent(Provider<EditorAgent> editorAgentProvider,
                                          ActionManager actionManager,
                                          OpenFileAction openFileAction,
                                          DtoFactory dtoFactory) {
        this.editorAgentProvider = editorAgentProvider;
        this.actionManager = actionManager;
        this.openFileAction = openFileAction;
        this.dtoFactory = dtoFactory;
    }

    @Override
    public List<ActionDescriptor> getActions(String projectPath) {
        EditorAgent editorAgent = editorAgentProvider.get();
        EditorPartPresenter activeEditor = editorAgent.getActiveEditor();

        final List<ActionDescriptor> actions = new ArrayList<>();

        if (activeEditor == null) {
            return Collections.emptyList();
        }

        VirtualFile virtualFile = activeEditor.getEditorInput().getFile();

        String openNodeActionId = actionManager.getId(openFileAction);

        actions.add(dtoFactory.createDto(ActionDescriptor.class)
                              .withId(openNodeActionId)
                              .withParameters(Collections.singletonMap(FILE_PARAM_ID, virtualFile.getPath())));
        return actions;
    }
}
