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
import org.eclipse.che.ide.api.action.AbstractPerspectiveAction;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.project.tree.generic.StorableNode;
import org.eclipse.che.ide.api.selection.Selection;
import org.eclipse.che.ide.api.selection.SelectionAgent;
import org.eclipse.che.ide.part.projectexplorer.DeleteNodeHandler;

import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;

import static org.eclipse.che.ide.workspace.perspectives.project.ProjectPerspective.PROJECT_PERSPECTIVE_ID;

/**
 * Action for deleting an item which is selected in 'Project Explorer'.
 *
 * @author Artem Zatsarynnyy
 * @author Dmitry Shnurenko
 */
@Singleton
public class DeleteItemAction extends AbstractPerspectiveAction {
    private final AnalyticsEventLogger eventLogger;
    private       SelectionAgent       selectionAgent;
    private       DeleteNodeHandler    deleteNodeHandler;
    private       AppContext           appContext;

    @Inject
    public DeleteItemAction(Resources resources,
                            AnalyticsEventLogger eventLogger,
                            SelectionAgent selectionAgent,
                            DeleteNodeHandler deleteNodeHandler, CoreLocalizationConstant localization, AppContext appContext) {
        super(Arrays.asList(PROJECT_PERSPECTIVE_ID),
              localization.deleteItemActionText(),
              localization.deleteItemActionDescription(),
              null,
              resources.delete());
        this.selectionAgent = selectionAgent;
        this.eventLogger = eventLogger;
        this.deleteNodeHandler = deleteNodeHandler;
        this.appContext = appContext;
    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(ActionEvent e) {
        eventLogger.log(this);

        Selection<?> selection = selectionAgent.getSelection();
        if (selection != null && !selection.isEmpty() & selection.getHeadElement() instanceof StorableNode) {
            if (selection.isSingleSelection()) {
                deleteNodeHandler.delete((StorableNode)selection.getHeadElement());
            } else {
                deleteNodeHandler.deleteNodes((List<StorableNode>)selection.getAllElements());
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void updateInPerspective(@NotNull ActionEvent event) {
        if ((appContext.getCurrentProject() == null && !appContext.getCurrentUser().isUserPermanent()) ||
            (appContext.getCurrentProject() != null && appContext.getCurrentProject().isReadOnly())) {
            event.getPresentation().setVisible(true);
            event.getPresentation().setEnabled(false);
            return;
        }

        boolean isEnabled = false;
        Selection<?> selection = selectionAgent.getSelection();
        if (selection != null && selection.getFirstElement() instanceof StorableNode) {
            isEnabled = ((StorableNode)selection.getFirstElement()).isDeletable();
        }
        event.getPresentation().setEnabled(isEnabled);
    }
}
