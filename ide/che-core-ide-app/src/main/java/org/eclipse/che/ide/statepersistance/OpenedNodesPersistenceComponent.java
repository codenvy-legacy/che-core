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
import com.google.inject.Singleton;

import org.eclipse.che.ide.api.project.tree.generic.FileNode;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.actions.OpenNodeAction;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.project.tree.TreeNode;
import org.eclipse.che.ide.api.project.tree.generic.StorableNode;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.part.projectexplorer.ProjectExplorerViewImpl;
import org.eclipse.che.ide.statepersistance.dto.ActionDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static org.eclipse.che.ide.actions.OpenNodeAction.NODE_PARAM_ID;

/**
 * @author Andrienko Alexander
 */
@Singleton
public class OpenedNodesPersistenceComponent implements PersistenceComponent {

    private final ProjectExplorerViewImpl projectExplorerView;
    private final OpenNodeAction openNodeAction;
    private final ActionManager actionManager;
    private final DtoFactory dtoFactory;

    @Inject
    public OpenedNodesPersistenceComponent(ProjectExplorerViewImpl projectExplorerView,
                                           ActionManager actionManager,
                                           OpenNodeAction openNodeAction,
                                           DtoFactory dtoFactory) {
        this.projectExplorerView = projectExplorerView;
        this.actionManager = actionManager;
        this.openNodeAction = openNodeAction;
        this.dtoFactory = dtoFactory;
    }

    @Override
    public List<ActionDescriptor> getActions(String projectPath) {
        Array<TreeNode<?>> openedNodes = projectExplorerView.getOpenedTreeNodes();
        final List<ActionDescriptor> actions = new ArrayList<>();

        if (openedNodes == null || openedNodes.isEmpty()) {
            return actions;
        }

        String actionId = actionManager.getId(openNodeAction);

        for (TreeNode<?> openedNode: openedNodes.asIterable()) {
            if (openedNode instanceof StorableNode && !(openedNode instanceof FileNode)) {
                String relNodePath = ((StorableNode)openedNode).getPath();

                relNodePath = relNodePath.replaceFirst(projectPath, "");

                if (relNodePath.equals("")) {
                    continue;
                }

                ActionDescriptor actionDescriptor = dtoFactory.createDto(ActionDescriptor.class)
                                                              .withId(actionId)
                                                              .withParameters(Collections.singletonMap(NODE_PARAM_ID, relNodePath));
                actions.add(actionDescriptor);
            }
        }
        return actions;
    }

}
