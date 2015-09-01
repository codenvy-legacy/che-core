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

import org.eclipse.che.ide.actions.OpenNodeAction;
import org.eclipse.che.ide.actions.SelectNodeAction;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.project.node.HasStorablePath;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.part.explorer.project.NewProjectExplorerPresenter;
import org.eclipse.che.ide.part.projectexplorer.ProjectExplorerView;
import org.eclipse.che.ide.project.node.FileReferenceNode;
import org.eclipse.che.ide.statepersistance.dto.ActionDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Andrienko Alexander
 */
@Singleton
public class OpenedNodesPersistenceComponent implements PersistenceComponent {

    private final OpenNodeAction openNodeAction;
    private final ActionManager  actionManager;
    private final DtoFactory     dtoFactory;
    private final NewProjectExplorerPresenter projectExplorer;

    @Inject
    public OpenedNodesPersistenceComponent(ActionManager actionManager,
                                           OpenNodeAction openNodeAction,
                                           DtoFactory dtoFactory,
                                           NewProjectExplorerPresenter projectExplorer) {
        this.actionManager = actionManager;
        this.openNodeAction = openNodeAction;
        this.dtoFactory = dtoFactory;
        this.projectExplorer = projectExplorer;
    }

    @Override
    public List<ActionDescriptor> getActions(String projectPath) {
//        List<Node> visible = projectExplorer.getVisibleNodes();
        final List<ActionDescriptor> actions = new ArrayList<>();

//        if (visible == null || visible.isEmpty()) {
//            return actions;
//        }
//
//        String actionId = actionManager.getId(openNodeAction);
//
//        for (Node openedNode : visible) {
//            if (openedNode instanceof HasStorablePath && !(openedNode instanceof FileReferenceNode)) {
//                String relNodePath = ((HasStorablePath)openedNode).getStorablePath();
//
//                relNodePath = relNodePath.replaceFirst(projectPath, "");
//
//                if (relNodePath.equals("")) {
//                    continue;
//                }
//
//                ActionDescriptor actionDescriptor = dtoFactory.createDto(ActionDescriptor.class)
//                                                              .withId(actionId)
//                                                              .withParameters(
//                                                                      Collections.singletonMap(SelectNodeAction.NODE_PARAM_ID, relNodePath));
//                actions.add(actionDescriptor);
//            }
//        }
        return actions;
    }

}
