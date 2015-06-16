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
import org.eclipse.che.ide.Resources;
import org.eclipse.che.ide.api.action.AbstractPerspectiveAction;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.navigation.NavigateToFilePresenter;

import javax.annotation.Nonnull;
import java.util.Arrays;

import static org.eclipse.che.ide.workspace.perspectives.project.ProjectPerspective.PROJECT_PERSPECTIVE_ID;

/**
 * Action for finding file by name and opening it.
 *
 * @author Ann Shumilova
 * @author Dmitry Shnurenko
 */
@Singleton
public class NavigateToFileAction extends AbstractPerspectiveAction {

    private final NavigateToFilePresenter presenter;
    private final AppContext              appContext;
    private final AnalyticsEventLogger    eventLogger;

    @Inject
    public NavigateToFileAction(NavigateToFilePresenter presenter,
                                AppContext appContext,
                                AnalyticsEventLogger eventLogger, Resources resources) {
        super(Arrays.asList(PROJECT_PERSPECTIVE_ID), "Navigate to File", "Navigate to file", null, resources.navigateToFile());
        this.presenter = presenter;
        this.appContext = appContext;
        this.eventLogger = eventLogger;
    }


    /** {@inheritDoc} */
    @Override
    public void actionPerformed(ActionEvent e) {
        eventLogger.log(this);
        presenter.showDialog();
    }

    /** {@inheritDoc} */
    @Override
    public void updatePerspective(@Nonnull ActionEvent event) {
        event.getPresentation().setEnabled(appContext.getCurrentProject() != null);
    }
}
