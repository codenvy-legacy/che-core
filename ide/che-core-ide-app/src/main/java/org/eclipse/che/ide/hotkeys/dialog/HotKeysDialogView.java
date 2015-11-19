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
package org.eclipse.che.ide.hotkeys.dialog;

import org.eclipse.che.ide.api.mvp.View;
import org.eclipse.che.ide.hotkeys.HotKeyItem;

import java.util.List;

/**
 * This representation of widget that provides an ability to show hotKeys list for IDE and editor
 * @author Alexander Andrienko
 */
public interface HotKeysDialogView extends View<HotKeysDialogView.ActionDelegate> {
    interface ActionDelegate {
        /**
         * Show list hotKeys
         */
        void showHotKeys();

        /**
         * Perform some action in response to user's clicking 'Ok' button
         */
        void onBtnOkClicked();
    }

    /**
     * Show dialog 
     */
    void showDialog();

    /**
     * Close dialog
     */
    void close();

    /**
     * Set hotKeys list for displaying 
     * @param data hotKeys list
     */
    void setData(List<HotKeyItem> data);
}
