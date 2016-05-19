/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.privacy;

import org.eclipse.che.ide.actions.PrivacyAction;

import org.eclipse.che.ide.actions.PrivacyAction;
import org.eclipse.che.ide.api.action.Action;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.action.DefaultActionGroup;
import org.eclipse.che.ide.api.action.IdeActions;
import org.eclipse.che.ide.api.action.Separator;
import org.eclipse.che.ide.api.constraints.Constraints;
import org.eclipse.che.ide.api.event.ProjectActionEvent;
import org.eclipse.che.ide.api.event.ProjectActionHandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

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
public class PrivacyPresenter implements ProjectActionHandler {
    private static final String PRIVACY_ACTION_ID = "privacy";

    private final ActionManager      actionManager;
    private final DefaultActionGroup rightMainMenuGroup;
    private final PrivacyAction      privacyAction;

    @Inject
    public PrivacyPresenter(ActionManager actionManager, PrivacyAction privacyAction, EventBus eventBus) {
        this.actionManager = actionManager;
        this.privacyAction = privacyAction;
        this.rightMainMenuGroup = (DefaultActionGroup)actionManager.getAction(IdeActions.GROUP_RIGHT_MAIN_MENU);

        eventBus.addHandler(ProjectActionEvent.TYPE, this);
    }

    @Override
    public void onProjectReady(ProjectActionEvent event) {
        actionManager.registerAction(PRIVACY_ACTION_ID, privacyAction);

        rightMainMenuGroup.add(privacyAction, LAST);
        rightMainMenuGroup.add(Separator.getInstance(), new Constraints(BEFORE, PRIVACY_ACTION_ID));
        rightMainMenuGroup.add(Separator.getInstance(), new Constraints(AFTER, PRIVACY_ACTION_ID));
    }

    @Override
    public void onProjectClosing(ProjectActionEvent event) {
    }

    @Override
    public void onProjectClosed(ProjectActionEvent event) {
        int index = 0;
        boolean found = false;

        final Action[] actions = rightMainMenuGroup.getChildActionsOrStubs();
        for (Action oneAction : actions) {
            if (oneAction.equals(privacyAction)) {
                found = true;
                break;
            }
            index++;
        }

        if (found) {
            final Action previousAction = actions[index - 1];
            if (previousAction instanceof Separator) {
                rightMainMenuGroup.remove(previousAction);
            }

            actionManager.unregisterAction(PRIVACY_ACTION_ID);
            rightMainMenuGroup.remove(actions[index]);

            final Action nextAction = actions[index + 1];
            if (previousAction instanceof Separator) {
                rightMainMenuGroup.remove(nextAction);
            }
        }
    }

    @Override
    public void onProjectOpened(ProjectActionEvent event) {

    }
}
