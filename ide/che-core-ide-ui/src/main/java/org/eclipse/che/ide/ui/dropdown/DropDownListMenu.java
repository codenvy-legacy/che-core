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
package org.eclipse.che.ide.ui.dropdown;

import com.google.gwt.core.client.Scheduler;
import com.google.inject.Inject;

import org.eclipse.che.ide.api.action.Action;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.action.ActionGroup;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.action.ActionPlaces;
import org.eclipse.che.ide.api.action.ActionSelectedHandler;
import org.eclipse.che.ide.api.action.DefaultActionGroup;
import org.eclipse.che.ide.api.action.Presentation;
import org.eclipse.che.ide.api.keybinding.KeyBindingAgent;
import org.eclipse.che.ide.ui.toolbar.MenuLockLayer;
import org.eclipse.che.ide.ui.toolbar.PopupMenu;
import org.eclipse.che.ide.ui.toolbar.PresentationFactory;

import javax.annotation.Nonnull;

import static com.google.gwt.dom.client.Style.Unit.PX;

/**
 * Class describes the popup window which contains all elements of list.
 *
 * @author Valeriy Svydenko
 */
public class DropDownListMenu implements ActionSelectedHandler {
    private static final String place = ActionPlaces.DROPDOWN_MENU;

    private final ActionManager       actionManager;
    private final KeyBindingAgent     keyBindingAgent;
    private final PresentationFactory presentationFactory;
    private final DefaultActionGroup  actions;

    private PopupMenu     popupMenu;
    private MenuLockLayer lockLayer;

    @Inject
    public DropDownListMenu(ActionManager actionManager, KeyBindingAgent keyBindingAgent) {
        this.actionManager = actionManager;
        this.keyBindingAgent = keyBindingAgent;

        presentationFactory = new PresentationFactory();
        actions = new DefaultActionGroup(actionManager);
    }

    /** {@inheritDoc} */
    @Override
    public void onActionSelected(Action action) {
        hide();
    }

    /**
     * Shows a content menu and moves it to specified position.
     *
     * @param x
     *         horizontal position
     * @param y
     *         vertical position
     * @param itemIdPrefix
     *         list identifier
     */
    public void show(final int x, final int y, @Nonnull String itemIdPrefix) {
        hide();
        updateActions(itemIdPrefix);

        lockLayer = new MenuLockLayer();
        popupMenu =
                new PopupMenu(actions, actionManager, place, presentationFactory, lockLayer, this, keyBindingAgent, itemIdPrefix);
        lockLayer.add(popupMenu);

        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                int left = x - popupMenu.getOffsetWidth();
                if (left < 0) {
                    left = 0;
                }

                popupMenu.getElement().getStyle().setTop(y, PX);
                popupMenu.getElement().getStyle().setLeft(left, PX);
            }
        });
    }

    /**
     * Updates the list of visible actions.
     *
     * @param listId
     *         identifier of action group which contains elements of list
     */
    private void updateActions(@Nonnull String listId) {
        actions.removeAll();

        ActionGroup mainActionGroup = (ActionGroup)actionManager.getAction(listId);
        if (mainActionGroup == null) {
            return;
        }

        Action[] children = mainActionGroup.getChildren(null);
        for (Action action : children) {
            Presentation presentation = presentationFactory.getPresentation(action);
            ActionEvent e = new ActionEvent(ActionPlaces.DROPDOWN_MENU, presentation, actionManager, 0);

            action.update(e);
            if (presentation.isVisible()) {
                actions.add(action);
            }
        }
    }

    /** Hides opened content menu. */
    public void hide() {
        if (popupMenu != null) {
            popupMenu.removeFromParent();
            popupMenu = null;
        }

        if (lockLayer != null) {
            lockLayer.removeFromParent();
            lockLayer = null;
        }
    }
}
