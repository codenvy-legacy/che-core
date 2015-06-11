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
import org.eclipse.che.api.analytics.client.logger.AnalyticsEventLogger;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.Resources;
import org.eclipse.che.ide.api.action.Action;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.project.tree.generic.StorableNode;
import org.eclipse.che.ide.api.selection.Selection;
import org.eclipse.che.ide.api.selection.SelectionAgent;

import java.util.ArrayList;
import java.util.List;

/**
 * Action for moving items.
 *
 * @author Vitaliy Guliy
 */
@Singleton
public class CutAction extends Action {
    private final AnalyticsEventLogger eventLogger;
    private       SelectionAgent       selectionAgent;
    private       AppContext           appContext;

    private       PasteAction          pasteAction;

    @Inject
    public CutAction(Resources resources,
                     AnalyticsEventLogger eventLogger,
                     SelectionAgent selectionAgent, CoreLocalizationConstant localization, AppContext appContext,
                     PasteAction pasteAction) {
        super(localization.cutItemsActionText(), localization.cutItemsActionDescription(), null, resources.cut());
        this.selectionAgent = selectionAgent;
        this.eventLogger = eventLogger;
        this.appContext = appContext;
        this.pasteAction = pasteAction;
    }

    /** {@inheritDoc} */
    @Override
    public void update(ActionEvent e) {
        if ((appContext.getCurrentProject() == null && !appContext.getCurrentUser().isUserPermanent()) ||
            (appContext.getCurrentProject() != null && appContext.getCurrentProject().isReadOnly())) {
            e.getPresentation().setVisible(true);
            e.getPresentation().setEnabled(false);
            return;
        }

        e.getPresentation().setEnabled(canMoveSelection());
    }

    /**
     * Determines whether the selection can be moved.
     *
     * @return <b>true</b> if the selection can be moved, otherwise returns <b>false</b>
     */
    private boolean canMoveSelection() {
        Selection<?> selection = selectionAgent.getSelection();
        if (selection == null || selection.isEmpty()) {
            return false;
        }

        if (appContext.getCurrentProject() == null || appContext.getCurrentProject().getRootProject() == null) {
            return false;
        }

        String projectPath = appContext.getCurrentProject().getRootProject().getPath();

        List<?> selectedItems = selection.getAllElements();
        for (int i = 0; i < selectedItems.size(); i++) {
            Object o = selectedItems.get(i);

            if (!(o instanceof StorableNode)) {
                return false;
            }

            if (projectPath.equals(((StorableNode)o).getPath())) {
                return false;
            }
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(ActionEvent e) {
        eventLogger.log(this);

        if (!canMoveSelection()) {
            return;
        }

        List<StorableNode> moveItems = new ArrayList<StorableNode>();
        List<?> selectionItems = selectionAgent.getSelection().getAllElements();
        for (int i = 0; i < selectionItems.size(); i++) {
            moveItems.add(((StorableNode) selectionItems.get(i)));
        }
        pasteAction.moveItems(moveItems);
    }

}
