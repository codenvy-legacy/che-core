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
package org.eclipse.che.ide.actions.find;

import org.eclipse.che.api.analytics.client.logger.AnalyticsEventLogger;

import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.Resources;

import org.eclipse.che.ide.api.action.Action;
import org.eclipse.che.ide.api.action.ActionEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Action fo find action action
 *
 * @author Evgen Vidolob
 */
@Singleton
public class FindActionAction extends Action {

    private       FindActionPresenter  presenter;
    private final AnalyticsEventLogger eventLogger;

    @Inject
    public FindActionAction(FindActionPresenter presenter,
                            CoreLocalizationConstant localization,
                            AnalyticsEventLogger eventLogger,
                            Resources resources) {
        super(localization.actionFindActionDescription(), localization.actionFindActionTitle(), null, resources.findActions());
        this.presenter = presenter;
        this.eventLogger = eventLogger;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        eventLogger.log(this);
        presenter.show();
    }
}
