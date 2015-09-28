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
package org.eclipse.che.ide.actions;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.action.ProjectAction;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.part.explorer.project.ProjectExplorerPresenter;

import java.util.List;

/**
 * @author Vlad Zhukovskiy
 */
@Singleton
public class GoIntoAction extends ProjectAction {

    private final ProjectExplorerPresenter projectExplorer;

    @Inject
    public GoIntoAction(ProjectExplorerPresenter projectExplorer) {
        super("Go into");
        this.projectExplorer = projectExplorer;
    }

    @Override
    protected void updateProjectAction(ActionEvent e) {
        List<?> selection = projectExplorer.getSelection().getAllElements();

        e.getPresentation().setEnabledAndVisible(!projectExplorer.isGoIntoActivated()
                                                 && selection.size() == 1
                                                 && isNodeSupportGoInto(selection.get(0)));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        List<?> selection = projectExplorer.getSelection().getAllElements();

        if (selection.isEmpty() || selection.size() > 1) {
            throw new IllegalArgumentException("Node isn't selected");
        }

        Object node = selection.get(0);

        if (isNodeSupportGoInto(node)) {
            projectExplorer.goInto((Node)node);
        }
    }

    private boolean isNodeSupportGoInto(Object node) {
        return node != null && node instanceof Node && ((Node)node).getParent() != null && ((Node)node).supportGoInto();
    }
}
