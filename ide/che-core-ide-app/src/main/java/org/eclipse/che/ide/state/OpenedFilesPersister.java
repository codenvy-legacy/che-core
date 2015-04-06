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

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import org.eclipse.che.ide.actions.OpenFileAction;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.collections.StringMap;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.state.dto.ActionDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.eclipse.che.ide.actions.OpenFileAction.FILE_PARAM_ID;

/**
 * //
 *
 * @author Artem Zatsarynnyy
 */
@Singleton
public class OpenedFilesPersister implements Persister {
    private final Provider<EditorAgent> editorAgentProvider;
    private final ActionManager         actionManager;
    private final OpenFileAction        openFileAction;
    private final DtoFactory dtoFactory;

    @Inject
    public OpenedFilesPersister(Provider<EditorAgent> editorAgentProvider,
                                ActionManager actionManager,
                                OpenFileAction openFileAction,
                                DtoFactory dtoFactory) {
        this.editorAgentProvider = editorAgentProvider;
        this.actionManager = actionManager;
        this.openFileAction = openFileAction;
        this.dtoFactory = dtoFactory;
    }

    @Override
    public List<ActionDescriptor> persist(String projectPath) {
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
}
