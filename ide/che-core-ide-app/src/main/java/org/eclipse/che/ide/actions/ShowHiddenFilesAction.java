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

import org.eclipse.che.api.analytics.client.logger.AnalyticsEventLogger;
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
import org.eclipse.che.ide.part.explorer.project.ProjectExplorerPresenter;
import org.eclipse.che.ide.util.loging.Log;

import static org.eclipse.che.api.promises.client.callback.CallbackPromiseHelper.createFromCallback;

/** @author Artem Zatsarynnyy */
@Singleton
public class ShowHiddenFilesAction extends Action implements PromisableAction {

    public static final String SHOW_HIDDEN_FILES_PARAM_ID = "showHiddenFiles";
    private final AppContext               appContext;
    private final AnalyticsEventLogger     eventLogger;
    private final CoreLocalizationConstant localizationConstant;
    private final ProjectExplorerPresenter projectExplorerPresenter;

    @Inject
    public ShowHiddenFilesAction(AppContext appContext,
                                 AnalyticsEventLogger eventLogger,
                                 CoreLocalizationConstant localizationConstant,
                                 ProjectExplorerPresenter projectExplorerPresenter) {
        super(localizationConstant.actionShowHiddenFilesTitle(), localizationConstant.actionShowHiddenFilesDescription(), null, null);
        this.appContext = appContext;
        this.eventLogger = eventLogger;
        this.localizationConstant = localizationConstant;
        this.projectExplorerPresenter = projectExplorerPresenter;
    }

    /** {@inheritDoc} */
    @Override
    public void update(ActionEvent e) {
        e.getPresentation().setVisible(appContext.getCurrentProject() != null);
    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(ActionEvent e) {
        eventLogger.log(this);

        boolean isShow = projectExplorerPresenter.isShowHiddenFiles();
        projectExplorerPresenter.showHiddenFiles(!isShow);
    }

    @Override
    public Promise<Void> promise(final ActionEvent event) {
        final CurrentProject currentProject = appContext.getCurrentProject();
        if (currentProject == null) {
            return Promises.reject(JsPromiseError.create(localizationConstant.noOpenedProject()));
        }


        if (event.getParameters() == null || event.getParameters().get(SHOW_HIDDEN_FILES_PARAM_ID) == null) {
            Log.error(getClass(), "No show hidden files parameter");
        }

        String showHiddenFilesKey = event.getParameters().get(SHOW_HIDDEN_FILES_PARAM_ID);
        final boolean isShowHiddenFiles = Boolean.valueOf(showHiddenFilesKey);

        final CallbackPromiseHelper.Call<Void, Throwable> call = new CallbackPromiseHelper.Call<Void, Throwable>() {

            @Override
            public void makeCall(final Callback<Void, Throwable> callback) {
                projectExplorerPresenter.showHiddenFiles(isShowHiddenFiles);

                callback.onSuccess(null);
            }
        };

        return createFromCallback(call);
    }
}
