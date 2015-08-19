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
package org.eclipse.che.ide.ui.toolbar;

import org.eclipse.che.ide.api.action.Action;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.action.ActionGroup;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.action.Presentation;
import org.eclipse.che.ide.api.action.Separator;
import org.eclipse.che.ide.api.parts.PerspectiveManager;
import org.eclipse.che.ide.util.loging.Log;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * @author <a href="mailto:evidolob@codenvy.com">Evgen Vidolob</a>
 * @author Dmitry Shnurenko
 */
public class Utils {

    /**
     * @param list
     *         this list contains expanded actions.
     * @param actionManager
     *         manager
     */
    public static void expandActionGroup(@Nonnull ActionGroup group,
                                         List<Action> list,
                                         PresentationFactory presentationFactory,
                                         @Nonnull String place,
                                         ActionManager actionManager,
                                         boolean transparentOnly,
                                         PerspectiveManager perspectiveManager) {
        Presentation presentation = presentationFactory.getPresentation(group);
        ActionEvent event = new ActionEvent(place, presentation, actionManager, perspectiveManager);

        if (!presentation.isVisible()) {
            return;
        }

        Action[] children = group.getChildren(event);

        for (Action child : children) {
            if (child == null) {
                String groupId = actionManager.getId(group);
                Log.error(Utils.class, "action is null: group=" + group + " group id=" + groupId);
                continue;
            }

            presentation = presentationFactory.getPresentation(child);
            ActionEvent e1 = new ActionEvent(place, presentation, actionManager, perspectiveManager);

            if (transparentOnly && child.isTransparentUpdate() || !transparentOnly) {
                if (!doUpdate(child, e1)) continue;
            }

            if (!presentation.isVisible()) { // don't create invisible items in the menu
                continue;
            }

            if (child instanceof ActionGroup) {
                ActionGroup actionGroup = (ActionGroup)child;
                if (actionGroup.isPopup()) { // popup menu has its own presentation
                    if (actionGroup.disableIfNoVisibleChildren()) {
                        final boolean visibleChildren = hasVisibleChildren(actionGroup,
                                                                           presentationFactory,
                                                                           actionManager,
                                                                           place,
                                                                           perspectiveManager);
                        if (actionGroup.hideIfNoVisibleChildren() && !visibleChildren) {
                            continue;
                        }
                        presentation.setEnabled(actionGroup.canBePerformed() || visibleChildren);
                    }

                    list.add(child);
                } else {
                    expandActionGroup((ActionGroup)child, list, presentationFactory, place, actionManager, perspectiveManager);
                }
            } else if (child instanceof Separator) {
                if (!list.isEmpty() && !(list.get(list.size() - 1) instanceof Separator)) {
                    list.add(child);
                }
            } else {
                list.add(child);
            }
        }
    }


    /**
     * @param list
     *         this list contains expanded actions.
     * @param actionManager
     *         manager
     */
    public static void expandActionGroup(@Nonnull ActionGroup group,
                                         List<Action> list,
                                         PresentationFactory presentationFactory,
                                         String place,
                                         ActionManager actionManager,
                                         PerspectiveManager perspectiveManager) {
        expandActionGroup(group, list, presentationFactory, place, actionManager, false, perspectiveManager);
    }

    // returns false if exception was thrown and handled
    private static boolean doUpdate(final Action action, final ActionEvent event) {
        final boolean result = true;
        action.update(event);
        return result;
    }

    public static boolean hasVisibleChildren(ActionGroup group,
                                             PresentationFactory factory,
                                             ActionManager actionManager,
                                             String place,
                                             PerspectiveManager perspectiveManager) {
        ActionEvent event = new ActionEvent(place, factory.getPresentation(group), actionManager, perspectiveManager);
//        event.setInjectedContext(group.isInInjectedContext());
        for (Action anAction : group.getChildren(event)) {
            if (anAction == null) {
                Log.error(Utils.class, "Null action found in group " + group + ", " + factory.getPresentation(group));
                continue;
            }
            if (anAction instanceof Separator) {
                continue;
            }

            final Presentation presentation = factory.getPresentation(anAction);
            updateGroupChild(place, anAction, presentation, actionManager, perspectiveManager);
            if (anAction instanceof ActionGroup) {
                ActionGroup childGroup = (ActionGroup)anAction;

                // popup menu must be visible itself
                if (childGroup.isPopup()) {
                    if (!presentation.isVisible()) {
                        continue;
                    }
                }

                if (hasVisibleChildren(childGroup, factory, actionManager, place, perspectiveManager)) {
                    return true;
                }
            } else if (presentation.isVisible()) {
                return true;
            }
        }

        return false;
    }

    public static void updateGroupChild(String place,
                                        Action anAction,
                                        final Presentation presentation,
                                        ActionManager actionManager,
                                        PerspectiveManager perspectiveManager) {
        ActionEvent event1 = new ActionEvent(place, presentation, actionManager, perspectiveManager);
        doUpdate(anAction, event1);
    }
}
