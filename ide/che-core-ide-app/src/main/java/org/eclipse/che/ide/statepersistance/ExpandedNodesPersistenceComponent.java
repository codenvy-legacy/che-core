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

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.actions.ExpandNodeAction;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.project.node.HasStorablePath;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.part.explorer.project.NewProjectExplorerPresenter;
import org.eclipse.che.ide.statepersistance.dto.ActionDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Vlad Zhukovskiy
 */
@Singleton
public class ExpandedNodesPersistenceComponent implements PersistenceComponent {

    private final ExpandNodeAction            expandNodeAction;
    private final ActionManager               actionManager;
    private final DtoFactory                  dtoFactory;
    private final NewProjectExplorerPresenter projectExplorer;

    @Inject
    public ExpandedNodesPersistenceComponent(ActionManager actionManager,
                                             ExpandNodeAction expandNodeAction,
                                             DtoFactory dtoFactory,
                                             NewProjectExplorerPresenter projectExplorer) {
        this.actionManager = actionManager;
        this.expandNodeAction = expandNodeAction;
        this.dtoFactory = dtoFactory;
        this.projectExplorer = projectExplorer;
    }

    @Override
    public List<ActionDescriptor> getActions(String projectPath) {
        List<Node> visible = projectExplorer.getVisibleNodes();

        if (visible == null || visible.isEmpty()) {
            return Collections.emptyList();
        }

        final List<ActionDescriptor> actions = new ArrayList<>();

        String actionId = actionManager.getId(expandNodeAction);

        for (Node node : visible) {
            if (node instanceof HasStorablePath && projectExplorer.isExpanded(node)) {
                String nodePath = ((HasStorablePath)node).getStorablePath();

                if (Strings.isNullOrEmpty(nodePath) || nodePath.equals(projectPath)) {
                    continue;
                }

                ActionDescriptor actionDescriptor = dtoFactory.createDto(ActionDescriptor.class)
                                                              .withId(actionId)
                                                              .withParameters(
                                                                      Collections.singletonMap(ExpandNodeAction.NODE_PARAM_ID, nodePath));
                actions.add(actionDescriptor);
            }
        }
        return actions;
    }

}
