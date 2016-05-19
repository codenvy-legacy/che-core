/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
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
import org.eclipse.che.ide.api.action.Action;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.projecttype.wizard.presenter.ProjectWizardPresenter;

/** @author Evgen Vidolob */
@Singleton
public class NewProjectAction extends Action {

    private final ProjectWizardPresenter wizard;
    private final AnalyticsEventLogger   eventLogger;
    private final AppContext             appContext;

    @Inject
    public NewProjectAction(Resources resources, ProjectWizardPresenter wizard, AnalyticsEventLogger eventLogger, AppContext appContext) {
        super("Project...", "Create new project", null);
        this.wizard = wizard;
        this.eventLogger = eventLogger;
        this.appContext = appContext;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        eventLogger.log(this);
        wizard.show();
    }

    @Override
    public void update(ActionEvent e) {
        if (appContext.getCurrentProject() == null) {
            e.getPresentation().setEnabled(appContext.getCurrentUser().isUserPermanent());
        } else {
            e.getPresentation().setEnabled(!appContext.getCurrentProject().isReadOnly());
        }
    }
}
