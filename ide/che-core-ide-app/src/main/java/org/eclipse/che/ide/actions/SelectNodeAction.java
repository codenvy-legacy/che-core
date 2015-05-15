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
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.callback.CallbackPromiseHelper;
import org.eclipse.che.api.promises.client.js.JsPromiseError;
import org.eclipse.che.api.promises.client.js.Promises;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.api.action.Action;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.action.PromisableAction;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.project.tree.TreeNode;
import org.eclipse.che.ide.part.projectexplorer.ProjectExplorerView;
import org.eclipse.che.ide.util.loging.Log;
import static org.eclipse.che.ide.api.notification.Notification.Type.WARNING;
import static org.eclipse.che.api.promises.client.callback.CallbackPromiseHelper.createFromCallback;

/**
 * @author Andrienko Alexander
 */
@Singleton
public class SelectNodeAction extends Action implements PromisableAction {

    /** ID of the parameter to specify node path to select. */
    public static String SELECT_NODE_PARAM_ID = "selectSomeNode";

    private final AppContext appContext;
    private final NotificationManager notificationManager;
    private final CoreLocalizationConstant localization;
    private final ProjectExplorerView projectExplorerView;

    @Inject
    public SelectNodeAction(AppContext appContext,
                            NotificationManager notificationManager,
                            CoreLocalizationConstant localization,
                            ProjectExplorerView projectExplorerView) {
        this.appContext = appContext;
        this.notificationManager = notificationManager;
        this.localization = localization;
        this.projectExplorerView = projectExplorerView;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        final CurrentProject currentProject = appContext.getCurrentProject();

        if (currentProject == null || currentProject.getRootProject() == null) {
            return;
        }

        final ProjectDescriptor activeProject = currentProject.getRootProject();

        if (event.getParameters() == null) {
            Log.error(getClass(), localization.canNotSelectNodeWithoutParams());
            return;
        }

        final String path = event.getParameters().get(SELECT_NODE_PARAM_ID);
        if (path == null || path.equals("")) {
            Log.error(getClass(), localization.nodeToSelectIsNotSpecified());
            return;
        }

        String nodePathToOpen = activeProject.getPath() + (!path.startsWith("/") ? "/".concat(path) : path);

        selectNodeByPath(nodePathToOpen, currentProject);
    }

    private void selectNodeByPath(final String path, CurrentProject currentProject) {
        currentProject.getCurrentTree().getNodeByPath(path, new AsyncCallback<TreeNode<?>>() {

            @Override
            public void onSuccess(TreeNode<?> treeNode) {
                projectExplorerView.selectNode(treeNode);
            }

            @Override
            public void onFailure(Throwable throwable) {
                notificationManager.showNotification(new Notification(localization.unableSelectResource(path), WARNING));
            }
        });
    }

    @Override
    public Promise<Void> promise(final ActionEvent event) {
        final CurrentProject currentProject = appContext.getCurrentProject();

        if (currentProject == null) {
            return Promises.reject(JsPromiseError.create(localization.noOpenedProject()));
        }

        final CallbackPromiseHelper.Call<Void, Throwable> call = new CallbackPromiseHelper.Call<Void, Throwable>() {
            @Override
            public void makeCall(final Callback<Void, Throwable> callback) {
                actionPerformed(event);
                callback.onSuccess(null);
            }
        };

        return createFromCallback(call);
    }
}
