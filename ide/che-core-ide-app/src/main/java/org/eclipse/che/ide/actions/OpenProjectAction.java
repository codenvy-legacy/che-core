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
import org.eclipse.che.ide.openproject.OpenProjectPresenter;

import javax.annotation.Nonnull;
import java.util.Arrays;

import static org.eclipse.che.ide.workspace.perspectives.project.ProjectPerspective.PROJECT_PERSPECTIVE_ID;

/**
 * @author Evgen Vidolob
 * @author Dmitry Shnurenko
 */
@Singleton
public class OpenProjectAction extends AbstractPerspectiveAction {

    private final OpenProjectPresenter presenter;
    private final AnalyticsEventLogger eventLogger;

    @Inject
    public OpenProjectAction(OpenProjectPresenter presenter, AnalyticsEventLogger eventLogger, Resources resources) {
        super(Arrays.asList(PROJECT_PERSPECTIVE_ID), "Open Project", "Open project", null, resources.openProject());
        this.presenter = presenter;
        this.eventLogger = eventLogger;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        eventLogger.log(this);
        presenter.showDialog();
    }

    @Override
    public void updatePerspective(@Nonnull ActionEvent event) {
        //to do nothing
    }
}
