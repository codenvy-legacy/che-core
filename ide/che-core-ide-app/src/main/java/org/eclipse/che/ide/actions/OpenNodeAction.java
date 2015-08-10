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
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.callback.CallbackPromiseHelper.Call;
import org.eclipse.che.api.promises.client.js.JsPromiseError;
import org.eclipse.che.api.promises.client.js.Promises;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.api.action.Action;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.action.PromisableAction;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.event.NodeExpandedEvent;
import org.eclipse.che.ide.api.event.NodeExpandedEventHandler;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.part.explorer.project.NewProjectExplorerPresenter;
import org.eclipse.che.ide.api.project.node.HasStorablePath;
import org.eclipse.che.ide.util.loging.Log;

import javax.annotation.Nonnull;

import static org.eclipse.che.api.promises.client.callback.CallbackPromiseHelper.createFromCallback;

/**
 * @author Andrienko Alexander
 */
@Singleton
public class OpenNodeAction extends Action implements PromisableAction {

    /** ID of the parameter to specify node path to open. */
    public static String NODE_PARAM_ID = "node";

    private final EventBus                    eventBus;
    private final AppContext                  appContext;
    private final NotificationManager         notificationManager;
    private final CoreLocalizationConstant    localization;
    private final NewProjectExplorerPresenter projectExplorerPartPresenter;

    private Callback<Void, Throwable> actionCompletedCallBack;

    @Inject
    public OpenNodeAction(EventBus eventBus,
                          AppContext appContext,
                          NotificationManager notificationManager,
                          CoreLocalizationConstant localization,
                          NewProjectExplorerPresenter projectExplorerPartPresenter) {
        this.eventBus = eventBus;
        this.appContext = appContext;
        this.notificationManager = notificationManager;
        this.localization = localization;
        this.projectExplorerPartPresenter = projectExplorerPartPresenter;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        final CurrentProject currentProject = appContext.getCurrentProject();

        if (currentProject == null || currentProject.getRootProject() == null) {
            return;
        }

        final ProjectDescriptor activeProject = currentProject.getRootProject();

        if (event.getParameters() == null) {
            Log.error(getClass(), localization.canNotOpenNodeWithoutParams());
            return;
        }

        final String path = event.getParameters().get(NODE_PARAM_ID);
        if (path == null || path.equals("")) {
            Log.error(getClass(), localization.nodeToOpenIsNotSpecified());
            return;
        }

        final String nodePathToOpen = activeProject.getPath() + (!path.startsWith("/") ? "/".concat(path) : path);

        projectExplorerPartPresenter.expand(new HasStorablePath() {
            @Nonnull
            @Override
            public String getStorablePath() {
                return nodePathToOpen;
            }
        });

//        openNodeByPath(nodePathToOpen, currentProject, event);
    }

//    private void openNodeByPath(final String path, CurrentProject currentProject, final ActionEvent event) {
//        currentProject.getCurrentTree().getNodeByPath(path, new AsyncCallback<TreeNode<?>>() {
//
//            @Override
//            public void onSuccess(TreeNode<?> treeNode) {
//                projectExplorerPartPresenter.expandNode(treeNode);
//            }
//
//            @Override
//            public void onFailure(Throwable throwable) {
//                notificationManager.showNotification(new Notification(localization.unableOpenResource(path), WARNING));
//
//                if (actionCompletedCallBack != null) {
//                    actionCompletedCallBack.onFailure(throwable);
//                }
//            }
//        });
//    }

    @Override
    public Promise<Void> promise(final ActionEvent event) {
        final CurrentProject currentProject = appContext.getCurrentProject();

        if (currentProject == null) {
            return Promises.reject(JsPromiseError.create(localization.noOpenedProject()));
        }

        final Call<Void, Throwable> call = new Call<Void, Throwable>() {

            private HandlerRegistration handlerRegistration;

            @Override
            public void makeCall(final Callback<Void, Throwable> callback) {
                actionCompletedCallBack = callback;

                handlerRegistration = eventBus.addHandler(NodeExpandedEvent.TYPE, new NodeExpandedEventHandler() {
                    @Override
                    public void onNodeExpanded() {
                        handlerRegistration.removeHandler();
                        callback.onSuccess(null);
                    }
                });

                actionPerformed(event);
            }
        };

        return createFromCallback(call);
    }

}
