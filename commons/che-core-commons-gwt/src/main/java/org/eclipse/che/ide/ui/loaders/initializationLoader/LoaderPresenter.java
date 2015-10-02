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
package org.eclipse.che.ide.ui.loaders.initializationLoader;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Loader for displaying information about the operation.
 *
 * @author Roman Nikitenko
 */
@Singleton
public class LoaderPresenter implements OperationInfo.StatusListener, LoaderView.ActionDelegate {

    private final LoaderView view;
    private       boolean    expandPanelState;

    @Inject
    public LoaderPresenter(LoaderView view) {
        this.view = view;
        view.setDelegate(this);
        expandPanelState = false;
    }

    /**
     * Show loader and print operation to operation panel and details area.
     *
     * @param info
     *         information about the operation.
     */
    public void show(OperationInfo info) {
        view.show(info);
        view.printToDetails(info);
    }

    /**
     * Hide loader and clean it.
     */
    public void hide() {
        view.print(new OperationInfo("The operations completed!", OperationInfo.Status.EMPTY));
        view.setEnabledCloseButton(true);
        if (!expandPanelState) {
            view.hide();
        }
    }

    /**
     * Print operation to operation panel and details area.
     *
     * @param info
     *         information about the operation.
     */
    public void print(OperationInfo info) {
        view.print(info);
        view.printToDetails(info);
        view.scrollBottom();
    }

    /**
     * Print operation only to details area.
     *
     * @param info
     *         information about the operation.
     */
    public void printToDetails(OperationInfo info) {
        view.printToDetails(info);
        view.scrollBottom();
    }

    @Override
    public void onStatusChanged() {
        view.update();
    }

    @Override
    public void onCloseClicked() {
        view.hide();
    }

    @Override
    public void onDetailsClicked() {
        if (expandPanelState) {
            view.collapseDetails();
        } else {
            view.expandDetails();
        }
        expandPanelState = !expandPanelState;
    }
}
