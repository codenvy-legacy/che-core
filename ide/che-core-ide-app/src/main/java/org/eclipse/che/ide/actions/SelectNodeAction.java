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

import com.google.common.base.Strings;
import com.google.gwt.core.client.Callback;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
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
import org.eclipse.che.ide.api.project.node.HasStorablePath;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.part.explorer.project.NewProjectExplorerPresenter;
import org.eclipse.che.ide.util.loging.Log;

import static org.eclipse.che.api.promises.client.callback.CallbackPromiseHelper.createFromCallback;

/**
 * @author Andrienko Alexander
 */
@Singleton
public class SelectNodeAction extends Action implements PromisableAction {

    /** ID of the parameter to specify node path to select. */
    public static String SELECT_NODE_PARAM_ID = "selectSomeNode";

    private final AppContext                  appContext;
    private final CoreLocalizationConstant    localization;
    private final NewProjectExplorerPresenter projectExplorer;

    private Callback<Void, Throwable> actionCompletedCallBack;

    @Inject
    public SelectNodeAction(AppContext appContext,
                            CoreLocalizationConstant localization,
                            NewProjectExplorerPresenter projectExplorer) {
        this.appContext = appContext;
        this.localization = localization;
        this.projectExplorer = projectExplorer;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        final CurrentProject currentProject = appContext.getCurrentProject();

        if (currentProject == null || currentProject.getRootProject() == null) {
            return;
        }

        if (event.getParameters() == null) {
            Log.error(getClass(), localization.canNotSelectNodeWithoutParams());
            return;
        }

        final String path = event.getParameters().get(SELECT_NODE_PARAM_ID);
        if (Strings.isNullOrEmpty(path)) {
            Log.error(getClass(), localization.nodeToSelectIsNotSpecified());
            return;
        }

        projectExplorer.getNodeByPath(new HasStorablePath.StorablePath(path)).then(new Operation<Node>() {
            @Override
            public void apply(Node arg) throws OperationException {
                projectExplorer.select(arg, false);

                if (actionCompletedCallBack != null) {
                    actionCompletedCallBack.onSuccess(null);
                }
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
                actionCompletedCallBack = callback;
                actionPerformed(event);
            }
        };

        return createFromCallback(call);
    }
}
