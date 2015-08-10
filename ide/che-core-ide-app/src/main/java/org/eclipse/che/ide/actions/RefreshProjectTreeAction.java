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
//package org.eclipse.che.ide.actions;
//
//import com.google.inject.Inject;
//import com.google.inject.Singleton;
//import com.google.web.bindery.event.shared.EventBus;
//
//import org.eclipse.che.api.analytics.client.logger.AnalyticsEventLogger;
//import org.eclipse.che.ide.CoreLocalizationConstant;
//import org.eclipse.che.ide.Resources;
//import org.eclipse.che.ide.api.action.ActionEvent;
//import org.eclipse.che.ide.api.action.ProjectAction;
//import org.eclipse.che.ide.api.event.RefreshProjectTreeEvent;
//
///**
// * Refresh project tree Action
// *
// * @author Roman Nikitenko
// */
//@Singleton
//public class RefreshProjectTreeAction extends ProjectAction {
//
//    private final EventBus             eventBus;
//    private final AnalyticsEventLogger eventLogger;
//
//    @Inject
//    public RefreshProjectTreeAction(CoreLocalizationConstant locale,
//                                    EventBus eventBus,
//                                    AnalyticsEventLogger eventLogger,
//                                    Resources resources) {
//        super(locale.refreshProjectTreeName(), locale.refreshProjectTreeDescription(), resources.refresh());
//        this.eventBus = eventBus;
//        this.eventLogger = eventLogger;
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    public void actionPerformed(ActionEvent e) {
//        eventLogger.log(this);
//
//        eventBus.fireEvent(new RefreshProjectTreeEvent());
//    }
//
//    /** {@inheritDoc} */
//    @Override
//    public void updateProjectAction(ActionEvent event) {
//        event.getPresentation().setEnabledAndVisible(true);
//    }
//}
