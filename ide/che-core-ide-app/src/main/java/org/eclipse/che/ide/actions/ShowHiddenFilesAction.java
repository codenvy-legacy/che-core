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
import org.eclipse.che.ide.api.event.RefreshProjectTreeEvent;
import org.eclipse.che.ide.api.project.tree.TreeSettings;
import org.eclipse.che.ide.util.loging.Log;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import static org.eclipse.che.api.promises.client.callback.CallbackPromiseHelper.createFromCallback;

/** @author Artem Zatsarynnyy */
@Singleton
public class ShowHiddenFilesAction extends Action implements PromisableAction {

    public static final String SHOW_HIDDEN_FILES_PARAM_ID = "showHiddenFiles";
    private final AppContext           appContext;
    private final AnalyticsEventLogger eventLogger;
    private final EventBus             eventBus;
    private final CoreLocalizationConstant localizationConstant;

    @Inject
    public ShowHiddenFilesAction(AppContext appContext, AnalyticsEventLogger eventLogger, EventBus eventBus,
                                 CoreLocalizationConstant localizationConstant) {
        super(localizationConstant.actionShowHiddenFilesTitle(), localizationConstant.actionShowHiddenFilesDescription(), null, null);
        this.appContext = appContext;
        this.eventLogger = eventLogger;
        this.eventBus = eventBus;
        this.localizationConstant = localizationConstant;
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
        CurrentProject currentProject = appContext.getCurrentProject();
        if (currentProject != null) {
            TreeSettings treeSettings = currentProject.getCurrentTree().getSettings();
            treeSettings.setShowHiddenItems(!treeSettings.isShowHiddenItems());
            eventBus.fireEvent(new RefreshProjectTreeEvent());
        }
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
                currentProject.getCurrentTree().getSettings().setShowHiddenItems(isShowHiddenFiles);

                callback.onSuccess(null);
            }
        };

        return createFromCallback(call);
    }
}
