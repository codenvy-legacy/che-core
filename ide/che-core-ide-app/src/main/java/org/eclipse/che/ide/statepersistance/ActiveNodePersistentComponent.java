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
import org.eclipse.che.ide.api.project.tree.VirtualFile;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.project.tree.TreeNode;
import org.eclipse.che.ide.api.project.tree.generic.FileNode;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.part.projectexplorer.ProjectExplorerViewImpl;
import org.eclipse.che.ide.statepersistance.dto.ActionDescriptor;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.eclipse.che.ide.actions.SelectNodeAction.SELECT_NODE_PARAM_ID;

/**
 * @author Andrienko Alexander
 */
@Singleton
public class ActiveNodePersistentComponent implements PersistenceComponent {

    private Provider<EditorAgent> editorAgentProvider;
    private DtoFactory            dtoFactory;
    private ActionManager         actionManager;
    private SelectNodeAction selectNodeAction;
    private ProjectExplorerViewImpl projectExplorerView;

    @Inject
    public ActiveNodePersistentComponent(Provider<EditorAgent> editorAgentProvider,
                                         DtoFactory dtoFactory,
                                         ActionManager actionManager,
                                         SelectNodeAction selectNodeAction,
                                         ProjectExplorerViewImpl projectExplorerView) {
        this.editorAgentProvider = editorAgentProvider;
        this.dtoFactory = dtoFactory;
        this.actionManager = actionManager;
        this.selectNodeAction = selectNodeAction;
        this.projectExplorerView = projectExplorerView;
    }

    @Override
    public List<ActionDescriptor> getActions(String projectPath) {
        EditorAgent editorAgent = editorAgentProvider.get();
        EditorPartPresenter activeEditor = editorAgent.getActiveEditor();

        final List<ActionDescriptor> actions = new ArrayList<>();

        if (activeEditor == null) {
            return actions;
        }

        VirtualFile virtualFile = activeEditor.getEditorInput().getFile();
        TreeNode<?> parentNode = getParentNode(virtualFile);

        if (parentNode == null) {
            return actions;
        }

        Array<TreeNode<?>> openedNodes = projectExplorerView.getOpenedTreeNodes();

        if (openedNodes != null && openedNodes.contains(parentNode)) {
            String path = virtualFile.getPath();
            path = path.replaceFirst(projectPath, "");

            String openNodeActionId = actionManager.getId(selectNodeAction);

            actions.add(dtoFactory.createDto(ActionDescriptor.class)
                                  .withId(openNodeActionId)
                                  .withParameters(Collections.singletonMap(SELECT_NODE_PARAM_ID, path)));
        }

        return actions;
    }

    private TreeNode<?> getParentNode(@NotNull VirtualFile virtualFile) {
        TreeNode<?> parent = null;

        if (virtualFile instanceof FileNode) {
            parent = ((FileNode)virtualFile).getParent();
        }

        return parent;
    }
}
