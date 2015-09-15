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
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;

import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
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
import org.eclipse.che.ide.api.project.node.HasStorablePath;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.part.explorer.project.NewProjectExplorerPresenter;
import org.eclipse.che.ide.project.node.FileReferenceNode;
import org.eclipse.che.ide.util.loging.Log;

import javax.inject.Inject;
import javax.inject.Singleton;

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
    private final CoreLocalizationConstant localization;
    private final NewProjectExplorerPresenter projectExplorer;

    private Callback<Void, Throwable> actionCompletedCallBack;

    @Inject
    public OpenFileAction(EventBus eventBus,
                          AppContext appContext,
                          CoreLocalizationConstant localization,
                          NewProjectExplorerPresenter projectExplorer) {
        this.eventBus = eventBus;
        this.appContext = appContext;
        this.localization = localization;
        this.projectExplorer = projectExplorer;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (appContext.getCurrentProject() == null || appContext.getCurrentProject().getRootProject() == null) {
            return;
        }

        if (event.getParameters() == null) {
            Log.error(getClass(), localization.canNotOpenFileWithoutParams());
            return;
        }

        final String path = event.getParameters().get(FILE_PARAM_ID);
        if (path == null) {
            Log.error(getClass(), localization.fileToOpenIsNotSpecified());
            return;
        }

        projectExplorer.getNodeByPath(new HasStorablePath.StorablePath(path))
                       .then(selectNode())
                       .then(openNode()).then(new Operation<Node>() {
            @Override
            public void apply(Node arg) throws OperationException {
                if (actionCompletedCallBack != null) {
                    actionCompletedCallBack.onSuccess(null);
                }
            }
        });
    }

    protected Function<Node, Node> selectNode() {
        return new Function<Node, Node>() {
            @Override
            public Node apply(Node node) throws FunctionException {
                projectExplorer.select(node, false);

                return node;
            }
        };
    }

    protected Function<Node, Node> openNode() {
        return new Function<Node, Node>() {
            @Override
            public Node apply(Node node) throws FunctionException {
                if (node instanceof FileReferenceNode) {
                    ((FileReferenceNode)node).actionPerformed();
                }

                return node;
            }
        };
    }

    @Override
    public Promise<Void> promise(final ActionEvent actionEvent) {
        final CurrentProject currentProject = appContext.getCurrentProject();
        if (currentProject == null) {
            return Promises.reject(JsPromiseError.create(localization.noOpenedProject()));
        }

//        final String activeProjectPath = currentProject.getRootProject().getPath();

        if (actionEvent.getParameters() == null) {
            return Promises.reject(JsPromiseError.create(localization.canNotOpenFileWithoutParams()));
        }

        final String relPathToOpen = actionEvent.getParameters().get(FILE_PARAM_ID);
        if (relPathToOpen == null) {
            return Promises.reject(JsPromiseError.create(localization.fileToOpenIsNotSpecified()));
        }

//        if (!relPathToOpen.startsWith("/")) {
//            relPathToOpen = "/" + relPathToOpen;
//        }
//        final String pathToOpen = activeProjectPath + relPathToOpen;

        final Call<Void, Throwable> call = new Call<Void, Throwable>() {
            HandlerRegistration handlerRegistration;

            @Override
            public void makeCall(final Callback<Void, Throwable> callback) {

                actionCompletedCallBack = callback;
//                callback.onSuccess(null);
                handlerRegistration = eventBus.addHandler(ActivePartChangedEvent.TYPE, new ActivePartChangedHandler() {
                    @Override
                    public void onActivePartChanged(ActivePartChangedEvent event) {
                        if (event.getActivePart() instanceof EditorPartPresenter) {
                            EditorPartPresenter editor = (EditorPartPresenter)event.getActivePart();
                            handlerRegistration.removeHandler();
                            if ((relPathToOpen).equals(editor.getEditorInput().getFile().getPath())) {
                                callback.onSuccess(null);
                            }
                        }
                    }
                });
                actionPerformed(actionEvent);



//                projectServiceClient.getItem(pathToOpen, new AsyncRequestCallback<ItemReference>() {
//                    @Override
//                    protected void onSuccess(ItemReference result) {
//                        actionCompletedCallBack = callback;
//

//
//                        actionPerformed(actionEvent);
//                    }
//
//                    @Override
//                    protected void onFailure(Throwable exception) {
//                        callback.onFailure(exception);
//                    }
//                });
            }
        };

        return createFromCallback(call);
    }
}