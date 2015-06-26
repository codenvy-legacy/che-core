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

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.eclipse.che.ide.api.action.Action;
import org.eclipse.che.ide.api.action.ActionGroup;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.action.CustomComponentAction;
import org.eclipse.che.ide.api.action.Presentation;
import org.eclipse.che.ide.api.action.Separator;
import org.eclipse.che.ide.api.keybinding.KeyBindingAgent;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.collections.Collections;

import javax.annotation.Nonnull;


/**
 * The implementation of {@link ToolbarView}
 *
 * @author Andrey Plotnikov
 * @author Vitaliy Guliy
 */
public class ToolbarViewImpl extends FlowPanel implements ToolbarView {

    public static final int DELAY_MILLIS = 1000;

    public static final ToolbarResources RESOURCES = GWT.create(ToolbarResources.class);

    static {
        RESOURCES.toolbar().ensureInjected();
    }

//    Toolbar toolbar;
    private FlowPanel leftToolbar;
    private FlowPanel rightToolbar;

    private String          place;
    private ActionGroup     leftActionGroup;
    private ActionGroup     rightActionGroup;
    private ActionManager   actionManager;
    private KeyBindingAgent keyBindingAgent;

    private Array<Action> newLeftVisibleActions;
    private Array<Action> leftVisibleActions;

    private Array<Action> newRightVisibleActions;
    private Array<Action> rightVisibleActions;


    private PresentationFactory presentationFactory;
    private boolean             addSeparatorFirst;

    private ActionDelegate delegate;

    private final Timer timer = new Timer() {
        @Override
        public void run() {
            updateActions();
            schedule(DELAY_MILLIS);
        }
    };

    /** Create view with given instance of resources. */
    @Inject
    public ToolbarViewImpl(ActionManager actionManager, KeyBindingAgent keyBindingAgent) {
        this.actionManager = actionManager;
        this.keyBindingAgent = keyBindingAgent;

//        toolbar = new Toolbar();
//        initWidget(toolbar);

        setStyleName(RESOURCES.toolbar().toolbarPanel());

        leftToolbar = new FlowPanel();
        add(leftToolbar);

        rightToolbar = new FlowPanel();
        rightToolbar.addStyleName(RESOURCES.toolbar().rightPanel());
        add(rightToolbar);

        newLeftVisibleActions = Collections.createArray();
        leftVisibleActions = Collections.createArray();

        newRightVisibleActions = Collections.createArray();
        rightVisibleActions = Collections.createArray();

        presentationFactory = new PresentationFactory();
    }

    /** {@inheritDoc} */
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
        updateActions();
        if (!timer.isRunning())
            timer.schedule(DELAY_MILLIS);
    }

    @Override
    public void setRightActionGroup(@Nonnull ActionGroup rightActionGroup) {
        this.rightActionGroup = rightActionGroup;
        updateActions();
        if (!timer.isRunning())
            timer.schedule(DELAY_MILLIS);
    }

    //TODO need improve code
    private void updateActions() {
        if (leftActionGroup != null) {
            newLeftVisibleActions.clear();
            Utils.expandActionGroup(leftActionGroup, newLeftVisibleActions, presentationFactory, place, actionManager, false);
            if (!Collections.equals(newLeftVisibleActions, leftVisibleActions)) {
                final Array<Action> temp = leftVisibleActions;
                leftVisibleActions = newLeftVisibleActions;
                newLeftVisibleActions = temp;
//                toolbar.clearMainPanel();
                leftToolbar.clear();
                fillLeftToolbar(leftVisibleActions);
            }
        }
        if (rightActionGroup != null) {
            newRightVisibleActions.clear();
            Utils.expandActionGroup(rightActionGroup, newRightVisibleActions, presentationFactory, place, actionManager, false);
            if (!Collections.equals(newRightVisibleActions, rightVisibleActions)) {
                final Array<Action> temp = rightVisibleActions;
                rightVisibleActions = newRightVisibleActions;
                newRightVisibleActions = temp;
//                toolbar.clearRightPanel();
                rightToolbar.clear();
                fillRightToolbar(rightVisibleActions);
            }
        }
    }

    //TODO need improve code : dublicate code
    private void fillLeftToolbar(Array<Action> leftActions) {
        if (addSeparatorFirst) {
//            toolbar.addToMainPanel(new DelimiterItem());
//            toolbar.addToRightPanel(new DelimiterItem());
            leftToolbar.add(newDelimiter());
            rightToolbar.add(newDelimiter());
        }

        for (int i = 0; i < leftActions.size(); i++) {
            final Action action = leftActions.get(i);
            if (action instanceof Separator) {
                if (i > 0 && i < leftActions.size() - 1) {
//                    toolbar.addToMainPanel(new DelimiterItem());
                    leftToolbar.add(newDelimiter());
                }
            } else if (action instanceof CustomComponentAction) {
                Presentation presentation = presentationFactory.getPresentation(action);
                Widget customComponent = ((CustomComponentAction)action).createCustomComponent(presentation);
//                toolbar.addToMainPanel(customComponent);
                leftToolbar.add(customComponent);
            } else if (action instanceof ActionGroup && !(action instanceof CustomComponentAction) && ((ActionGroup)action).isPopup()) {
                ActionPopupButton button =
                        new ActionPopupButton((ActionGroup)action, actionManager, keyBindingAgent, presentationFactory, place);
//                toolbar.addToMainPanel(button);
                leftToolbar.add(button);
            } else {
                final ActionButton button = createToolbarButton(action);
//                toolbar.addToMainPanel(button);
                leftToolbar.add(button);
            }
        }
    }

    //TODO need improve code : dublicate code
    private void fillRightToolbar(Array<Action> rightActions) {
        for (int i = 0; i < rightActions.size(); i++) {
            final Action action = rightActions.get(i);
            if (action instanceof Separator) {
                if (i > 0 && i < rightActions.size() - 1) {
//                    toolbar.addToRightPanel(new DelimiterItem());
                    rightToolbar.add(newDelimiter());
                }
            } else if (action instanceof CustomComponentAction) {
                Presentation presentation = presentationFactory.getPresentation(action);
                Widget customComponent = ((CustomComponentAction)action).createCustomComponent(presentation);
//                toolbar.addToRightPanel(customComponent);
                rightToolbar.add(customComponent);
            } else if (action instanceof ActionGroup && !(action instanceof CustomComponentAction) && ((ActionGroup)action).isPopup()) {
                ActionPopupButton button =
                        new ActionPopupButton((ActionGroup)action, actionManager, keyBindingAgent, presentationFactory, place);
//                toolbar.addToRightPanel(button);
                rightToolbar.add(button);
            } else {
                final ActionButton button = createToolbarButton(action);
//                toolbar.addToRightPanel(button);
                rightToolbar.add(button);
            }
        }
    }

    /**
     * Creates a delimiter widget.
     *
     * @return delimiter widget
     */
    private Widget newDelimiter() {
        FlowPanel delimiter = new FlowPanel();
        delimiter.setStyleName(ToolbarViewImpl.RESOURCES.toolbar().toolbarDelimiter());
        return delimiter;
    }

    private ActionButton createToolbarButton(Action action) {
        return new ActionButton(action, actionManager, presentationFactory.getPresentation(action), place);
    }

    @Override
    public void setAddSeparatorFirst(boolean addSeparatorFirst) {
        this.addSeparatorFirst = addSeparatorFirst;
    }

}
