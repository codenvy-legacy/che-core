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

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.eclipse.che.ide.api.action.Action;
import org.eclipse.che.ide.api.action.ActionGroup;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.action.CustomComponentAction;
import org.eclipse.che.ide.api.action.Presentation;
import org.eclipse.che.ide.api.action.Separator;
import org.eclipse.che.ide.api.keybinding.KeyBindingAgent;
import org.eclipse.che.ide.api.parts.PerspectiveManager;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * The implementation of {@link ToolbarView}
 *
 * @author Andrey Plotnikov
 * @author Dmitry Shnurenko
 * @author Vitaliy Guliy
 * @author Oleksii Orel
 */
public class ToolbarViewImpl extends FlowPanel implements ToolbarView {

    public static final int DELAY_MILLIS = 1000;

    private FlowPanel leftToolbar;
    private FlowPanel rightToolbar;

    private String          place;
    private ActionGroup     leftActionGroup;
    private ActionGroup     rightActionGroup;
    private ActionManager   actionManager;
    private KeyBindingAgent keyBindingAgent;

    private List<Utils.VisibleActionGroup> leftVisibleGroupActions;
    private Provider<PerspectiveManager>   managerProvider;

    private List<Utils.VisibleActionGroup> rightVisibleGroupActions;


    private PresentationFactory presentationFactory;
    private boolean             addSeparatorFirst;

    private ToolbarResources toolbarResources;

    private ActionDelegate delegate;

    private final Timer timer;

    /** Create view with given instance of resources. */
    @Inject
    public ToolbarViewImpl(ActionManager actionManager,
                           KeyBindingAgent keyBindingAgent,
                           ToolbarResources toolbarResources,
                           Provider<PerspectiveManager> managerProvider) {
        this.actionManager = actionManager;
        this.keyBindingAgent = keyBindingAgent;
        this.managerProvider = managerProvider;
        this.toolbarResources = toolbarResources;

        toolbarResources.toolbar().ensureInjected();

        setStyleName(toolbarResources.toolbar().toolbarPanel());

        leftVisibleGroupActions = new ArrayList<>();
        rightVisibleGroupActions = new ArrayList<>();
        presentationFactory = new PresentationFactory();
        leftToolbar = new FlowPanel();
        rightToolbar = new FlowPanel();
        timer = new Timer() {
            @Override
            public void run() {
                updateActions();
                schedule(DELAY_MILLIS);
            }
        };

        add(leftToolbar);

        rightToolbar.addStyleName(toolbarResources.toolbar().rightPanel());
        add(rightToolbar);

        if (!timer.isRunning()) {
            timer.schedule(DELAY_MILLIS);
        }
    }

    @Override
    public void setDelegate(ActionDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public void setPlace(@Nonnull String place) {
        this.place = place;
    }

    @Override
    public void setLeftActionGroup(@Nonnull ActionGroup leftActionGroup) {
        this.leftActionGroup = leftActionGroup;
    }

    @Override
    public void setRightActionGroup(@Nonnull ActionGroup rightActionGroup) {
        this.rightActionGroup = rightActionGroup;
    }

    /**
     * Update toolbar if visible actions are changed.
     */
    private void updateActions() {
        if (leftActionGroup != null) {
            List<Utils.VisibleActionGroup> newLeftVisibleGroupActions =
                    Utils.renderActionGroup(leftActionGroup, presentationFactory, place, actionManager, managerProvider.get());
            if (newLeftVisibleGroupActions != null && !leftVisibleGroupActions.equals(newLeftVisibleGroupActions)) {
                leftVisibleGroupActions = newLeftVisibleGroupActions;
                leftToolbar.clear();
                leftToolbar.add(createLeftToolbar(leftVisibleGroupActions));
            }
        }
        if (rightActionGroup != null) {
            List<Utils.VisibleActionGroup> newRightVisibleGroupActions =
                    Utils.renderActionGroup(rightActionGroup, presentationFactory, place, actionManager, managerProvider.get());
            if (newRightVisibleGroupActions != null && !rightVisibleGroupActions.equals(newRightVisibleGroupActions)) {
                rightVisibleGroupActions = newRightVisibleGroupActions;
                rightToolbar.clear();
                rightToolbar.add(createRightToolbar(rightVisibleGroupActions));
            }
        }
    }

    /**
     * Creates a left toolbar widget.
     *
     * @return widget
     */
    private Widget createLeftToolbar(List<Utils.VisibleActionGroup> leftVisibleActionGroupList) {
        FlowPanel leftToolbar = new FlowPanel();

        if (addSeparatorFirst) {
            final Widget firstDelimiter = createDelimiter();
            leftToolbar.add(firstDelimiter);
        }

        for (Utils.VisibleActionGroup visibleActionGroup : leftVisibleActionGroupList) {
            List<Action> actions = visibleActionGroup.getActionList();
            if (actions == null || actions.size() == 0) {
                continue;
            }
            FlowPanel actionGroupPanel = new FlowPanel();
            actionGroupPanel.setStyleName(toolbarResources.toolbar().toolbarActionGroupPanel());
            leftToolbar.add(actionGroupPanel);
            for (Action action : actions) {
                if (action instanceof Separator) {
                    int actionIndex = actions.indexOf(action);
                    if (actionIndex > 0 && actionIndex < actions.size() - 1) {
                        final Widget delimiter = createDelimiter();
                        actionGroupPanel.add(delimiter);
                    }
                } else if (action instanceof CustomComponentAction) {
                    Presentation presentation = presentationFactory.getPresentation(action);
                    Widget customComponent = ((CustomComponentAction)action).createCustomComponent(presentation);
                    actionGroupPanel.add(customComponent);
                } else if (action instanceof ActionGroup && ((ActionGroup)action).isPopup()) {
                    ActionPopupButton button = new ActionPopupButton((ActionGroup)action,
                                                                     actionManager,
                                                                     keyBindingAgent,
                                                                     presentationFactory,
                                                                     place,
                                                                     managerProvider,
                                                                     toolbarResources);
                    actionGroupPanel.add(button);
                } else {
                    final ActionButton button = createToolbarButton(action);
                    actionGroupPanel.add(button);
                }
            }
        }
        return leftToolbar;
    }

    /**
     * Creates a right toolbar widget.
     *
     * @return widget
     */
    private Widget createRightToolbar(List<Utils.VisibleActionGroup> rightVisibleActionGroupList) {
        FlowPanel rightToolbar = new FlowPanel();

        if (addSeparatorFirst) {
            final Widget firstDelimiter = createDelimiter();
            rightToolbar.add(firstDelimiter);
        }

        for (Utils.VisibleActionGroup visibleActionGroup : rightVisibleActionGroupList) {
            List<Action> actions = visibleActionGroup.getActionList();
            if (actions == null || actions.size() == 0) {
                continue;
            }
            FlowPanel actionGroupPanel = new FlowPanel();
            actionGroupPanel.setStyleName(toolbarResources.toolbar().toolbarActionGroupPanel());
            rightToolbar.add(actionGroupPanel);
            for (Action action : actions) {
                if (action instanceof Separator) {
                    int actionIndex = actions.indexOf(action);
                    if (actionIndex > 0 && actionIndex < actions.size() - 1) {
                        final Widget delimiter = createDelimiter();
                        actionGroupPanel.add(delimiter);
                    }
                } else if (action instanceof CustomComponentAction) {
                    Presentation presentation = presentationFactory.getPresentation(action);
                    Widget customComponent = ((CustomComponentAction)action).createCustomComponent(presentation);
                    actionGroupPanel.add(customComponent);
                } else if (action instanceof ActionGroup && ((ActionGroup)action).isPopup()) {
                    ActionPopupButton button = new ActionPopupButton((ActionGroup)action,
                                                                     actionManager,
                                                                     keyBindingAgent,
                                                                     presentationFactory,
                                                                     place,
                                                                     managerProvider,
                                                                     toolbarResources);
                    actionGroupPanel.add(button);
                } else {
                    final ActionButton button = createToolbarButton(action);
                    actionGroupPanel.add(button);
                }
            }
        }
        return rightToolbar;
    }

    /**
     * Creates a delimiter widget.
     *
     * @return widget
     */
    private Widget createDelimiter() {
        FlowPanel delimiter = new FlowPanel();
        delimiter.setStyleName(toolbarResources.toolbar().toolbarDelimiter());
        return delimiter;
    }

    /**
     * Creates a toolbar button.
     *
     * @return ActionButton
     */
    private ActionButton createToolbarButton(Action action) {
        return new ActionButton(action,
                                actionManager,
                                presentationFactory.getPresentation(action),
                                place,
                                managerProvider.get(),
                                toolbarResources);
    }

    @Override
    public void setAddSeparatorFirst(boolean addSeparatorFirst) {
        this.addSeparatorFirst = addSeparatorFirst;
    }
}
