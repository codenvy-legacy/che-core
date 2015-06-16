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
import org.eclipse.che.ide.Resources;
import org.eclipse.che.ide.api.action.AbstractPerspectiveAction;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.event.CloseCurrentProjectEvent;

import javax.annotation.Nonnull;
import java.util.Arrays;

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

    @Inject
    public CloseProjectAction(AppContext appContext,
                              Resources resources,
                              AnalyticsEventLogger eventLogger,
                              EventBus eventBus) {
        super(Arrays.asList(PROJECT_PERSPECTIVE_ID), "Close Project", "Close project", null, resources.closeProject());
        this.appContext = appContext;
        this.eventLogger = eventLogger;
        this.eventBus = eventBus;
    }

    /** {@inheritDoc} */
    @Override
    public void updateInPerspective(@Nonnull ActionEvent event) {
        event.getPresentation().setVisible(appContext.getCurrentProject() != null);
    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(ActionEvent e) {
        eventLogger.log(this);
        eventBus.fireEvent(new CloseCurrentProjectEvent());
    }
}
