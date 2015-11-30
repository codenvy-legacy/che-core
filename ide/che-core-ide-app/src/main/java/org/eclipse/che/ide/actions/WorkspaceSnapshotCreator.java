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

import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.api.workspace.gwt.client.WorkspaceServiceClient;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.notification.NotificationManager;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Creates snapshot of the workspace using {@link WorkspaceServiceClient}.
 *
 * <p>This component is for managing notifications which are related to creating snapshot process.
 *
 * @author Yevhenii Voevodin
 */
@Singleton
public class WorkspaceSnapshotCreator {

    private final WorkspaceServiceClient   workspaceService;
    private final NotificationManager      notificationManager;
    private final CoreLocalizationConstant locale;

    Notification notification;

    @Inject
    public WorkspaceSnapshotCreator(WorkspaceServiceClient workspaceService,
                                    NotificationManager notificationManager,
                                    CoreLocalizationConstant locale) {
        this.workspaceService = workspaceService;
        this.notificationManager = notificationManager;
        this.locale = locale;
    }

    /**
     * Changes notification state to finished with an error.
     */
    public void creationError(String message) {
        notification.setMessage(message);
        notification.setType(Notification.Type.ERROR);
        notification.setStatus(Notification.Status.FINISHED);
        notification = null;
    }

    /**
     * Changes notification state to successfully finished.
     */
    public void successfullyCreated() {
        notification.setMessage(locale.createSnapshotSuccess());
        notification.setType(Notification.Type.INFO);
        notification.setStatus(Notification.Status.FINISHED);
        notification = null;
    }

    /**
     * Returns true if workspace creation process is not done, otherwise when it is done - returns false
     */
    public boolean isInProgress() {
        return notification != null;
    }

    /**
     * Creates snapshot from workspace with given id and shows appropriate notification.
     *
     * @param workspaceId
     *         id of the workspace to create snapshot from.
     */
    public void createSnapshot(String workspaceId) {
        notification = new Notification(locale.createSnapshotProgress(), Notification.Status.PROGRESS);
        notificationManager.showNotification(notification);
        workspaceService.createSnapshot(workspaceId)
                        .catchError(new Operation<PromiseError>() {
                            @Override
                            public void apply(PromiseError error) throws OperationException {
                                creationError(error.getMessage());
                            }
                        });
    }
}
