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

import com.google.gwt.user.client.ui.IsWidget;

/**
 * View of {@link LoaderPresenter}.
 *
 * @author Roman Nikitenko
 */
public interface LoaderView extends IsWidget {
    interface ActionDelegate {
        /**
         * Performs any actions appropriate in response to the user having clicked the 'Close' button.
         */
        void onCloseClicked();

        /**
         * Performs any actions appropriate in response to the user having clicked the 'Details'.
         */
        void onDetailsClicked();
    }

    /** Expand Details area. */
    void expandDetails();

    /** Collapse Details area. */
    void collapseDetails();

    /** Sets the delegate to receive events from this view. */
    void setDelegate(ActionDelegate delegate);

    /**
     * Print operation to operation panel and details area.
     *
     * @param info
     *         information about the operation.
     */
    void print(OperationInfo info);

    /**
     * Print operation only to details area.
     *
     * @param info
     *         information about the operation.
     */
    void printToDetails(OperationInfo info);

    /** Scrolls details area to bottom. */
    void scrollBottom();

    /** Show loader and print operation to operation panel and details area. */
    void show(OperationInfo info);

    /** Hide loader */
    void hide();

    /** Refresh details area(after change status, for example). */
    void update();

    /**
     * Change the enable state of the close button.
     *
     * @param enabled
     *         <code>true</code> to enable the button, <code>false</code> to disable it
     */
    void setEnabledCloseButton(boolean enabled);
}