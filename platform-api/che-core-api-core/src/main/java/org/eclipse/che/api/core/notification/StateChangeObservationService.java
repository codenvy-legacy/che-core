/*
 * CODENVY CONFIDENTIAL
 * __________________
 * 
 *  [2012] - [2014] Codenvy, S.A. 
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package org.eclipse.che.api.core.notification;

import org.eclipse.che.api.core.RuntimeApiException;

import javax.inject.Singleton;

/**
 * @author Alexander Garagatyi
 */
@Singleton
public class StateChangeObservationService {
    private final EventService eventService;

    public StateChangeObservationService() {
        this.eventService = new EventService();
    }

    public void addListner(StateChangeListener<?> stateChangeListener) {
        eventService.subscribe(stateChangeListener);
    }

    public void removeListener(StateChangeListener<?> stateChangeListener) {
        eventService.unsubscribe(stateChangeListener);
    }

    public void check(Object event) throws RuntimeApiException {
        eventService.publish(event, true);
    }
}
