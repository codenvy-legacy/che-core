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
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.analytics.client.logger.AnalyticsEventLogger;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.Resources;
import org.eclipse.che.ide.api.action.AbstractPerspectiveAction;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.event.project.CloseCurrentProjectEvent;
import org.eclipse.che.ide.api.selection.Selection;
import org.eclipse.che.ide.api.selection.SelectionAgent;
import org.eclipse.che.ide.project.node.ProjectDescriptorNode;

import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;

import static org.eclipse.che.ide.workspace.perspectives.project.ProjectPerspective.PROJECT_PERSPECTIVE_ID;

/**
 * @author Andrey Plotnikov
 * @author Dmitry Shnurenko
 */
@Singleton
public class CloseProjectAction extends AbstractPerspectiveAction {

    private final AppContext           appContext;
    private final AnalyticsEventLogger eventLogger;
    private final EventBus             eventBus;
    private final SelectionAgent       selectionAgent;

    @Inject
    public CloseProjectAction(AppContext appContext,
                              Resources resources,
                              AnalyticsEventLogger eventLogger,
                              EventBus eventBus,
                              SelectionAgent selectionAgent) {
        super(Arrays.asList(PROJECT_PERSPECTIVE_ID), "Close Project", "Close project", null, resources.closeProject());
        this.appContext = appContext;
        this.eventLogger = eventLogger;
        this.eventBus = eventBus;
        this.selectionAgent = selectionAgent;
    }

    /** {@inheritDoc} */
    @Override
    public void updateInPerspective(@NotNull ActionEvent event) {
        List<ProjectDescriptor> openedProjects = appContext.getOpenedProjects();

        event.getPresentation().setVisible(openedProjects.size() > 0);
        event.getPresentation().setVisible(openedProjects.size() > 0);

        Selection<?> selection = selectionAgent.getSelection();

        if (selection == null || selection.isEmpty()) {
            return;
        }

        List<?> selectedElements = selection.getAllElements();

        event.getPresentation().setEnabled(selectedElements.size() < 2);
        event.getPresentation().setEnabled(selectedElements.get(0) instanceof ProjectDescriptorNode);

    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(ActionEvent event) {
        eventLogger.log(this);
        eventBus.fireEvent(new CloseCurrentProjectEvent(appContext.getCurrentProject().getProjectDescription()));
    }
}
