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
package org.eclipse.che.ide.api.project.wizard;

import org.eclipse.che.ide.api.notification.Notification;

/**
 * Client service that subscribes a project to import project output notifications. Default implementation get the output stream of the
 * remote import process through a Websocket channel.
 */
public interface ImportProjectNotificationSubscriber {

    /**
     * Subscribe to display the import output notifications. To be called before triggering the import. Reusing already shown notification.
     *
     * @param projectName
     * @param notification existing already shown notification.
     */
    void subscribe(String projectName, Notification notification);

    /**
     * Subscribe to display the import output notifications. To be called before triggering the import.
     *
     * @param projectName
     */
    void subscribe(String projectName);

    /**
     * Updates the notifications when the import has been successfully performed. This also unsubscribes.
     */
    void onSuccess();

    /**
     * Updates the notifications when the import is failing. Display the error message. This also unsubscribes.
     *
     * @param errorMessage
     */
    void onFailure(String errorMessage);

}
