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
package org.eclipse.che.api.auth;


//import org.eclipse.che.api.core.notification.EventService;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import javax.annotation.PostConstruct;
//import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author Alexander Garagatyi
 */
@Singleton
public class LogoutOnUserRemoveSubscriber {
//    private static final Logger LOG = LoggerFactory.getLogger(LogoutOnUserRemoveSubscriber.class);
//
//    @Inject
//    private EventService eventService;
//
//    @Inject
//    private TokenManager tokenManager;
//
//    @PostConstruct
//    public void start() {
//        eventService.subscribe(new EventSubscriber<RemoveUserEvent>() {
//            @Override
//            public void onEvent(RemoveUserEvent event) {
//                if (null != event && null != event.getUserId()) {
//                    String id = event.getUserId();
//                    for (AccessTicket accessTicket : ticketManager.getAccessTickets()) {
//                        if (id.equals(accessTicket.getPrincipal().getId())) {
//                            ticketManager.removeTicket(accessTicket.getAccessToken());
//                        }
//                    }
//                }
//            }
//        });
//    }
}