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
package org.eclipse.che.api.core.notification;

/**
 * Receives notification events from EventService.
 *
 * @author andrew00x
 * @see EventService
 */
public interface EventSubscriber<T> {
    /**
     * Receives notification that an event has been published to the EventService.
     * If the method throws an unchecked exception it is ignored.
     */
    void onEvent(T event);
}
