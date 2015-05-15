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
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;

import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ItemReference;
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
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.event.ActivePartChangedEvent;
import org.eclipse.che.ide.api.event.ActivePartChangedHandler;
import org.eclipse.che.ide.api.event.FileEvent;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.project.tree.TreeNode;
import org.eclipse.che.ide.api.project.tree.generic.FileNode;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.util.loging.Log;

import javax.inject.Inject;
import javax.inject.Singleton;

import static org.eclipse.che.ide.api.event.FileEvent.FileOperation.OPEN;
import static org.eclipse.che.ide.api.notification.Notification.Type.WARNING;
import static org.eclipse.che.api.promises.client.callback.CallbackPromiseHelper.createFromCallback;

/**
 * @author Sergii Leschenko
 */
@Singleton
public class OpenFileAction extends Action implements PromisableAction {

    /** ID of the parameter to specify file path to open. */
    public static String FILE_PARAM_ID = "file";

    private final EventBus                 eventBus;
    private final AppContext               appContext;
    private final NotificationManager      notificationManager;
    private final CoreLocalizationConstant localization;
    private final ProjectServiceClient     projectServiceClient;

    private Callback<Void, Throwable>      actionCompletedCallBack;

    @Inject
    public OpenFileAction(EventBus eventBus,
                          AppContext appContext,
                          NotificationManager notificationManager,
                          CoreLocalizationConstant localization,
                          ProjectServiceClient projectServiceClient) {
        this.eventBus = eventBus;
        this.appContext = appContext;
        this.notificationManager = notificationManager;
        this.localization = localization;
        this.projectServiceClient = projectServiceClient;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (appContext.getCurrentProject() == null || appContext.getCurrentProject().getRootProject() == null) {
            return;
        }

        final ProjectDescriptor activeProject = appContext.getCurrentProject().getRootProject();
        if (event.getParameters() == null) {
            Log.error(getClass(), localization.canNotOpenFileWithoutParams());
            return;
        }

        final String path = event.getParameters().get(FILE_PARAM_ID);
        if (path == null) {
            Log.error(getClass(), localization.fileToOpenIsNotSpecified());
            return;
        }

        final String filePathToOpen = activeProject.getPath() + (!path.startsWith("/") ? "/".concat(path) : path);

        openFileByPath(filePathToOpen, event);
    }

    private void openFileByPath(final String filePath, final ActionEvent actionEvent) {
        final CurrentProject currentProject = appContext.getCurrentProject();
        if (currentProject == null) {
            return;
        }

        currentProject.getCurrentTree().getNodeByPath(filePath, new AsyncCallback<TreeNode<?>>() {
            @Override
            public void onSuccess(TreeNode<?> result) {
                if (result instanceof FileNode) {
                    eventBus.fireEvent(new FileEvent((FileNode)result, OPEN));
                }
            }

            @Override
            public void onFailure(Throwable caught) {
                notificationManager.showNotification(new Notification(localization.unableOpenResource(filePath), WARNING));

                if (actionCompletedCallBack != null) {
                    actionCompletedCallBack.onFailure(caught);
                }
            }
        });
    }

    @Override
    public Promise<Void> promise(final ActionEvent actionEvent) {
        final CurrentProject currentProject = appContext.getCurrentProject();
        if (currentProject == null) {
            return Promises.reject(JsPromiseError.create(localization.noOpenedProject()));
        }

        final String activeProjectPath = currentProject.getRootProject().getPath();

        if (actionEvent.getParameters() == null) {
            return Promises.reject(JsPromiseError.create(localization.canNotOpenFileWithoutParams()));
        }

        String relPathToOpen = actionEvent.getParameters().get(FILE_PARAM_ID);
        if (relPathToOpen == null) {
            return Promises.reject(JsPromiseError.create(localization.fileToOpenIsNotSpecified()));
        }

        if (!relPathToOpen.startsWith("/")) {
            relPathToOpen = "/" + relPathToOpen;
        }
        final String pathToOpen = activeProjectPath + relPathToOpen;

        final Call<Void, Throwable> call = new Call<Void, Throwable>() {
            HandlerRegistration handlerRegistration;

            @Override
            public void makeCall(final Callback<Void, Throwable> callback) {
                projectServiceClient.getItem(pathToOpen, new AsyncRequestCallback<ItemReference>() {
                    @Override
                    protected void onSuccess(ItemReference result) {
                        actionCompletedCallBack = callback;

                        handlerRegistration = eventBus.addHandler(ActivePartChangedEvent.TYPE, new ActivePartChangedHandler() {
                            @Override
                            public void onActivePartChanged(ActivePartChangedEvent event) {
                                if (event.getActivePart() instanceof EditorPartPresenter) {
                                    EditorPartPresenter editor = (EditorPartPresenter)event.getActivePart();
                                    handlerRegistration.removeHandler();
                                    if ((pathToOpen).equals(editor.getEditorInput().getFile().getPath())) {
                                        callback.onSuccess(null);
                                    }
                                }
                            }
                        });

                        actionPerformed(actionEvent);
                    }

                    @Override
                    protected void onFailure(Throwable exception) {
                        callback.onFailure(exception);
                    }
                });
            }
        };

        return createFromCallback(call);
    }
}