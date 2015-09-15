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

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import org.eclipse.che.ide.actions.SelectNodeAction;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.project.tree.VirtualFile;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.statepersistance.dto.ActionDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Andrienko Alexander
 */
@Singleton
public class LastOpenedFilePersistentComponent implements PersistenceComponent {

    private Provider<EditorAgent> editorAgentProvider;
    private DtoFactory            dtoFactory;
    private ActionManager         actionManager;
    private SelectNodeAction      selectNodeAction;

    @Inject
    public LastOpenedFilePersistentComponent(Provider<EditorAgent> editorAgentProvider,
                                             DtoFactory dtoFactory,
                                             ActionManager actionManager,
                                             SelectNodeAction selectNodeAction) {
        this.editorAgentProvider = editorAgentProvider;
        this.dtoFactory = dtoFactory;
        this.actionManager = actionManager;
        this.selectNodeAction = selectNodeAction;
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

        String openNodeActionId = actionManager.getId(selectNodeAction);

        actions.add(dtoFactory.createDto(ActionDescriptor.class)
                              .withId(openNodeActionId)
                              .withParameters(Collections.singletonMap(SelectNodeAction.SELECT_NODE_PARAM_ID, virtualFile.getPath())));

        return actions;
    }
}
