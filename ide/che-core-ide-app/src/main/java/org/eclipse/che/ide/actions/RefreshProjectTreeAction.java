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
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.Resources;
import org.eclipse.che.ide.api.action.AbstractPerspectiveAction;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.event.RefreshProjectTreeEvent;

import javax.annotation.Nonnull;
import java.util.Arrays;

import static org.eclipse.che.ide.workspace.perspectives.project.ProjectPerspective.PROJECT_PERSPECTIVE_ID;

/**
 * Refresh project tree Action
 *
 * @author Roman Nikitenko
 * @author Dmitry Shnurenko
 */
@Singleton
public class RefreshProjectTreeAction extends AbstractPerspectiveAction {

    private final EventBus             eventBus;
    private final AnalyticsEventLogger eventLogger;
    private final AppContext           appContext;

    @Inject
    public RefreshProjectTreeAction(AppContext appContext,
                                    CoreLocalizationConstant locale,
                                    EventBus eventBus,
                                    AnalyticsEventLogger eventLogger,
                                    Resources resources) {
        super(Arrays.asList(PROJECT_PERSPECTIVE_ID),
              locale.refreshProjectTreeName(),
              locale.refreshProjectTreeDescription(),
              null,
              resources.refresh());
        this.appContext = appContext;
        this.eventBus = eventBus;
        this.eventLogger = eventLogger;
    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(ActionEvent e) {
        eventLogger.log(this);

        eventBus.fireEvent(new RefreshProjectTreeEvent());
    }

    /** {@inheritDoc} */
    @Override
    public void updateInPerspective(@Nonnull ActionEvent event) {
        event.getPresentation().setEnabledAndVisible(appContext.getCurrentProject() != null);
    }
}
