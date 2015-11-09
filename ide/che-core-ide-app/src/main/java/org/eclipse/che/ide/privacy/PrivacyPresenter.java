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
package org.eclipse.che.ide.privacy;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.actions.PrivacyAction;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.action.DefaultActionGroup;
import org.eclipse.che.ide.api.action.IdeActions;
import org.eclipse.che.ide.api.constraints.Constraints;

import static org.eclipse.che.ide.api.action.Separator.getInstance;
import static org.eclipse.che.ide.api.constraints.Anchor.AFTER;
import static org.eclipse.che.ide.api.constraints.Anchor.BEFORE;
import static org.eclipse.che.ide.api.constraints.Constraints.LAST;

/**
 * This presenter provides the base functionality to add and hide the privacy action with its separators. Currently there is no way to do
 * that in a simple way.
 *
 * @author Kevin Pollet
 */
@Singleton
public class PrivacyPresenter{
    private static final String PRIVACY_ACTION_ID = "privacy";

    @Inject
    public PrivacyPresenter(ActionManager actionManager, PrivacyAction privacyAction) {
        DefaultActionGroup rightMainMenuGroup = (DefaultActionGroup)actionManager.getAction(IdeActions.GROUP_RIGHT_MAIN_MENU);

        actionManager.registerAction(PRIVACY_ACTION_ID, privacyAction);

        rightMainMenuGroup.add(getInstance(), new Constraints(BEFORE, PRIVACY_ACTION_ID));
        rightMainMenuGroup.add(privacyAction, LAST);
        rightMainMenuGroup.add(getInstance(), new Constraints(AFTER, PRIVACY_ACTION_ID));

    }
}
