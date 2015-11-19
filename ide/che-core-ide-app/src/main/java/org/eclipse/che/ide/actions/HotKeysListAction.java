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

import com.google.inject.Inject;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.api.action.Action;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.hotkeys.dialog.HotKeysDialogPresenter;

/**
 * Show hotKeys list for IDE and editor
 * @author Alexander Andrienko
 */
public class HotKeysListAction extends Action {

    private HotKeysDialogPresenter hotKeysDialogPresenter;

    @Inject
    public HotKeysListAction(HotKeysDialogPresenter hotKeysDialogPresenter, CoreLocalizationConstant locale) {
        super(locale.hotKeysActionName(), locale.hotKeysActionDescription(), null, null);
        this.hotKeysDialogPresenter = hotKeysDialogPresenter;
    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(ActionEvent e) {
        hotKeysDialogPresenter.showHotKeys();
    }
}
