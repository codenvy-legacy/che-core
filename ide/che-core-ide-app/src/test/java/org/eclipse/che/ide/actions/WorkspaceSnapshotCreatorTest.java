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

import com.google.gwtmockito.GwtMockitoTestRunner;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.api.workspace.gwt.client.WorkspaceServiceClient;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link WorkspaceSnapshotCreator}.
 *
 * @author Yevhenii Voevodin
 */
@RunWith(GwtMockitoTestRunner.class)
public class WorkspaceSnapshotCreatorTest {

    @Mock(answer = Answers.RETURNS_MOCKS)
    private WorkspaceServiceClient workspaceService;

    @Mock
    private NotificationManager notificationManager;

    @Mock
    private Notification notification;

    @Mock
    private Promise<Void> createSnapshotPromise;

    @Mock
    private PromiseError promiseError;

    @Mock
    private CoreLocalizationConstant locale;

    @Captor
    private ArgumentCaptor<Operation<PromiseError>> errorCaptor;

    @InjectMocks
    private WorkspaceSnapshotCreator snapshotCreator;

    @Test
    public void shouldShowNotificationWhenCreatingSnapshot() {
        snapshotCreator.createSnapshot("workspace123");

        verify(notificationManager).showNotification(any());
    }

    @Test
    public void shouldChangeNotificationAfterCreationError() {
        snapshotCreator.createSnapshot("workspace123");
        snapshotCreator.notification = notification;

        snapshotCreator.creationError("Error");

        verify(notification).setType(Notification.Type.ERROR);
        verify(notification).setStatus(Notification.Status.FINISHED);
        verify(notification).setMessage("Error");
    }

    @Test
    public void shouldChangeNotificationAfterSuccessfullyCreated() {
        when(locale.createSnapshotSuccess()).thenReturn("Snapshot successfully created");
        snapshotCreator.createSnapshot("workspace123");
        snapshotCreator.notification = notification;

        snapshotCreator.successfullyCreated();

        verify(notification).setType(Notification.Type.INFO);
        verify(notification).setStatus(Notification.Status.FINISHED);
        verify(notification).setMessage("Snapshot successfully created");
    }

    @Test
    public void shouldChangeNotificationIfErrorCaughtWhileCreatingSnapshot() throws OperationException {
        when(workspaceService.createSnapshot(anyString())).thenReturn(createSnapshotPromise);
        snapshotCreator.createSnapshot("workspace123");
        snapshotCreator.notification = notification;

        verify(createSnapshotPromise).catchError(errorCaptor.capture());
        errorCaptor.getValue().apply(promiseError);

        verify(notification).setType(Notification.Type.ERROR);
        verify(notification).setStatus(Notification.Status.FINISHED);
        verify(notification).setMessage(promiseError.getMessage());
    }

    @Test
    public void shouldBeInProgressAfterCreatingSnapshot() {
        snapshotCreator.createSnapshot("workspace123");

        assertTrue(snapshotCreator.isInProgress());
    }

    @Test
    public void shouldNotBeInProgressBeforeCreatingSnapshot() {
        assertFalse(snapshotCreator.isInProgress());
    }

    @Test
    public void shouldNotBeInProgressAfterSuccessfullyCreatedSnapshot() {
        snapshotCreator.createSnapshot("workspace123");

        snapshotCreator.successfullyCreated();

        assertFalse(snapshotCreator.isInProgress());
    }

    @Test
    public void shouldNotBeInProgressAfterSnapshotCreationError() {
        snapshotCreator.createSnapshot("workspace123");

        snapshotCreator.creationError("Error");

        assertFalse(snapshotCreator.isInProgress());
    }

    @Test
    public void shouldNotBeInProgressIfErrorCaughtWhileCreatingSnapshot() throws OperationException {
        when(workspaceService.createSnapshot(anyString())).thenReturn(createSnapshotPromise);
        snapshotCreator.createSnapshot("workspace123");

        verify(createSnapshotPromise).catchError(errorCaptor.capture());
        errorCaptor.getValue().apply(promiseError);

        assertFalse(snapshotCreator.isInProgress());
    }
}
