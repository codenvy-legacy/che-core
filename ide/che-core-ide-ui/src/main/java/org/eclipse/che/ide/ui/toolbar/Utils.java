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
import org.eclipse.che.ide.collections.ListHelper;
import org.eclipse.che.ide.util.loging.Log;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:evidolob@codenvy.com">Evgen Vidolob</a>
 * @author Dmitry Shnurenko
 * @author Oleksii Orel
 */
public class Utils {

    /**
     * Returns the list of visible action group.
     *
     * @param group
     *         action group
     * @param presentationFactory
     *         presentation factory
     * @param place
     *         position name on the page
     * @param actionManager
     *         action manager
     * @param perspectiveManager
     *         perspective manager
     * @return list of visible action group
     */
    public static List<VisibleActionGroup> renderActionGroup(@NotNull ActionGroup group,
                                                             PresentationFactory presentationFactory,
                                                             @NotNull String place,
                                                             ActionManager actionManager,
                                                             PerspectiveManager perspectiveManager) {
        Presentation presentation = presentationFactory.getPresentation(group);
        ActionEvent event = new ActionEvent(place, presentation, actionManager, perspectiveManager);

        if (!presentation.isVisible()) { // don't process invisible groups
            return null;
        }

        Action[] children = group.getChildren(event);
        List<VisibleActionGroup> currentVisibleActionGroupList = new ArrayList<>();
        List<Action> currentActionList = new ArrayList<>();
        String currentGroupId = actionManager.getId(group);
        for (Action child : children) {
            if (child == null) {
                Log.error(Utils.class, "action is null: group=" + group + " group id=" + currentGroupId);
                continue;
            }

            presentation = presentationFactory.getPresentation(child);
            child.update(new ActionEvent(place, presentation, actionManager, perspectiveManager));

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
                    currentActionList.add(child);
                } else {
                    List<VisibleActionGroup> newVisibleActionGroupList = renderActionGroup((ActionGroup)child,
                                                                                           presentationFactory,
                                                                                           place,
                                                                                           actionManager,
                                                                                           perspectiveManager);
                    currentVisibleActionGroupList.addAll(newVisibleActionGroupList);
                }
            } else if (child instanceof Separator) {
                if (!currentActionList.isEmpty() && !(currentActionList.get(currentActionList.size() - 1) instanceof Separator)) {
                    currentActionList.add(child);
                }
            } else {
                currentActionList.add(child);
            }
        }
        currentVisibleActionGroupList.add(0, new VisibleActionGroup(currentGroupId, currentActionList));

        return currentVisibleActionGroupList;
    }


    /**
     * Returns true if action group has visible children.
     *
     * @param group
     *         action group
     * @param factory
     *         presentation factory
     * @param actionManager
     *         action manager
     * @param place
     *         position name on the page
     * @param perspectiveManager
     *         perspective manager
     * @return boolean
     */
    public static boolean hasVisibleChildren(ActionGroup group,
                                             PresentationFactory factory,
                                             ActionManager actionManager,
                                             String place,
                                             PerspectiveManager perspectiveManager) {
        ActionEvent event = new ActionEvent(place, factory.getPresentation(group), actionManager, perspectiveManager);
        for (Action anAction : group.getChildren(event)) {
            if (anAction == null) {
                Log.error(Utils.class, "Null action found in group " + group + ", " + factory.getPresentation(group));
                continue;
            }
            if (anAction instanceof Separator) {
                continue;
            }

            final Presentation presentation = factory.getPresentation(anAction);
            anAction.update(new ActionEvent(place, presentation, actionManager, perspectiveManager));
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


    public static class VisibleActionGroup {
        private String       groupId;
        private List<Action> actionList;

        /**
         * Creates a new <code>VisibleActionGroup</code> with groupId set to <code>null</code> and
         * actionList set to <code>null</code>.
         */
        VisibleActionGroup() {
            this(null, null);
        }

        /**
         * Creates a new <code>VisibleActionGroup</code> with the specified groupId
         * and actionList.
         *
         * @param groupId
         *         Action group ID
         * @param actionList
         *         List of actions
         */
        VisibleActionGroup(String groupId, List<Action> actionList) {
            this.groupId = groupId;
            this.actionList = actionList;
        }

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public List<Action> getActionList() {
            return actionList;
        }

        public void setActionList(List<Action> actionList) {
            this.actionList = actionList;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof VisibleActionGroup)) {
                return false;
            }
            VisibleActionGroup other = (VisibleActionGroup)o;
            if (this.groupId != null) {
                if (!this.groupId.equals(other.groupId)) {
                    return false;
                }
            } else {
                if (other.groupId != null) {
                    return false;
                }
            }
            if (this.actionList != null) {
                if (!ListHelper.equals(this.actionList, other.actionList)) {
                    return false;
                }
            } else {
                if (other.actionList != null) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = hash * 31 + (groupId != null ? groupId.hashCode() : 0);
            hash = hash * 31 + (actionList != null ? actionList.hashCode() : 0);
            return hash;
        }
    }
}
