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
import org.eclipse.che.ide.projectimport.wizard.presenter.ImportProjectWizardPresenter;

import javax.validation.constraints.NotNull;
import java.util.Arrays;

import static org.eclipse.che.ide.workspace.perspectives.project.ProjectPerspective.PROJECT_PERSPECTIVE_ID;

/**
 * Import project from location action
 *
 * @author Roman Nikitenko
 * @author Dmitry Shnurenko
 */
@Singleton
public class ImportProjectFromLocationAction extends AbstractPerspectiveAction {

    private final ImportProjectWizardPresenter presenter;
    private final AnalyticsEventLogger         eventLogger;
    private final AppContext                   appContext;

    @Inject
    public ImportProjectFromLocationAction(ImportProjectWizardPresenter presenter,
                                           CoreLocalizationConstant locale,
                                           AnalyticsEventLogger eventLogger,
                                           Resources resources,
                                           AppContext appContext) {
        super(Arrays.asList(PROJECT_PERSPECTIVE_ID), locale.importProjectFromLocationName(),
              locale.importProjectFromLocationDescription(),
              null,
              resources.importProject());
        this.presenter = presenter;
        this.eventLogger = eventLogger;
        this.appContext = appContext;
    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(ActionEvent event) {
        eventLogger.log(this);
        presenter.show();
    }

    /** {@inheritDoc} */
    @Override
    public void updateInPerspective(@NotNull ActionEvent event) {
        if (appContext.getCurrentProject() == null) {
            event.getPresentation().setEnabled(appContext.getCurrentUser().isUserPermanent());
        } else {
            event.getPresentation().setEnabled(!appContext.getCurrentProject().isReadOnly());
        }
    }
}
