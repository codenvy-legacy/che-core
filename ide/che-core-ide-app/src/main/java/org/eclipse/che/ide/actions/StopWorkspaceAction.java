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

import com.google.gwt.core.client.Callback;
import com.google.inject.Inject;

import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.workspace.gwt.client.WorkspaceServiceClient;
import org.eclipse.che.api.workspace.shared.dto.UsersWorkspaceDto;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.api.action.AbstractPerspectiveAction;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.bootstrap.WorkspaceComponent;
import org.eclipse.che.ide.core.Component;

import javax.validation.constraints.NotNull;
import java.util.Collections;

/**
 * The class contains business logic to stop workspace.
 *
 * @author Dmitry Shnurenko
 */
public class StopWorkspaceAction extends AbstractPerspectiveAction {

    private static final String MACHINE_PERSPECTIVE_ID = "Machine Perspective";

    private final CoreLocalizationConstant locale;
    private final AppContext               appContext;
    private final WorkspaceServiceClient   workspaceService;
    private final WorkspaceComponent       workspaceComponent;
    private final NotificationManager      notificationManager;

    @Inject
    public StopWorkspaceAction(CoreLocalizationConstant locale,
                               AppContext appContext,
                               WorkspaceServiceClient workspaceService,
                               WorkspaceComponent workspaceComponent,
                               NotificationManager notificationManager) {
        super(Collections.singletonList(MACHINE_PERSPECTIVE_ID), locale.stopWsTitle(), locale.stopWsDescription(), null, null);

        this.locale = locale;
        this.appContext = appContext;
        this.workspaceService = workspaceService;
        this.workspaceComponent = workspaceComponent;
        this.notificationManager = notificationManager;
    }

    /** {@inheritDoc} */
    @Override
    public void updateInPerspective(@NotNull ActionEvent event) {
        UsersWorkspaceDto workspace = appContext.getWorkspace();

        event.getPresentation().setEnabled(workspace != null);
    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(ActionEvent event) {
        final UsersWorkspaceDto workspace = appContext.getWorkspace();

        Promise<Void> stopWsPromise = workspaceService.stop(workspace.getId());

        stopWsPromise.then(new Operation<Void>() {
            @Override
            public void apply(Void arg) throws OperationException {
                notificationManager.showInfo(locale.stopWsNotification(workspace.getDefaultEnvName()));

                workspaceComponent.start(new Callback<Component, Exception>() {
                    @Override
                    public void onFailure(Exception reason) {
                    }

                    @Override
                    public void onSuccess(Component result) {
                    }
                });
            }
        });
    }
}
