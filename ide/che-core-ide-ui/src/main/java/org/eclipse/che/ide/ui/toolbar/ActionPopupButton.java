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

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.InlineLabel;

import org.eclipse.che.ide.api.action.Action;
import org.eclipse.che.ide.api.action.ActionGroup;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.action.ActionSelectedHandler;
import org.eclipse.che.ide.api.keybinding.KeyBindingAgent;
import org.vectomatic.dom.svg.ui.SVGImage;
import org.vectomatic.dom.svg.ui.SVGResource;

/**
 * @author <a href="mailto:evidolob@codenvy.com">Evgen Vidolob</a>
 * @version $Id:
 */
public class ActionPopupButton extends Composite implements CloseMenuHandler, ActionSelectedHandler {

    private static final ToolbarResources.Css css = Toolbar.RESOURCES.toolbar();
    private final ActionGroup         action;
    private final ActionManager       actionManager;
    private final Element             tooltip;
    private       KeyBindingAgent     keyBindingAgent;
    private       PresentationFactory presentationFactory;
    private final String              place;

    /** Enabled state. True as default. */
    private boolean enabled = true;
    /** Lock Layer uses for locking rest of the screen, which does not covered by Popup Menu. */
    private MenuLockLayer lockLayer;
    /** Popup Menu button panel (<div> HTML element). */
    private ButtonPanel   panel;
    /** Has instance if Popup Menu is opened. */
    private PopupMenu     popupMenu;

    /** Create Popup Menu Button with specified icons for enabled and disabled states. */
    public ActionPopupButton(final ActionGroup action, ActionManager actionManager, KeyBindingAgent keyBindingAgent,
                             final PresentationFactory presentationFactory,
                             String place) {
        this.action = action;
        this.actionManager = actionManager;
        this.keyBindingAgent = keyBindingAgent;
        this.presentationFactory = presentationFactory;
        this.place = place;

        panel = new ButtonPanel();
        tooltip = DOM.createSpan();
        initWidget(panel);
        panel.setStyleName(css.popupButtonPanel());
        SVGResource icon = presentationFactory.getPresentation(action).getSVGIcon();
        if (icon != null) {
            SVGImage image = new SVGImage(icon);
            image.getElement().setAttribute("class", css.popupButtonIcon());
            panel.add(image);
        } else if (presentationFactory.getPresentation(action).getIcon() != null) {
            Image image = new Image(presentationFactory.getPresentation(action).getIcon());
            image.setStyleName(css.popupButtonIcon());
            panel.add(image);
        }
        renderIcon();
        InlineLabel caret = new InlineLabel("");
        caret.setStyleName(css.caret());
        panel.add(caret);
        final String description = presentationFactory.getPresentation(action).getDescription();
        if (description != null) {
            tooltip.setInnerText(description);
            tooltip.addClassName(css.tooltip());
            panel.getElement().appendChild(tooltip);
        }
        this.ensureDebugId(place + "/" + action.getTemplatePresentation().getText());
    }

    /** Closes Popup Menu ( if opened ) and sets style of this Popup Menu Button to default. */
    protected void closePopupMenu() {
        if (popupMenu != null) {
            popupMenu.removeFromParent();
            popupMenu = null;
        }

        if (lockLayer != null) {
            lockLayer.removeFromParent();
            lockLayer = null;
        }

        panel.setStyleName(css.popupButtonPanel());
    }

    /**
     * Get is this button enabled.
     *
     * @return is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Set is enabled.
     *
     * @param enabled
     *         is enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        renderIcon();
    }

    /** {@inheritDoc} */
    public void onCloseMenu() {
        closePopupMenu();
    }

    /** Mouse Down handler. */
    private void onMouseDown() {
        panel.setStyleName(css.popupButtonPanelDown());
    }

    /** Mouse Out Handler. */
    private void onMouseOut() {
        if (popupMenu != null) {
            return;
        }

        panel.setStyleName(css.popupButtonPanel());
    }

    private void onMouseClick() {
        openPopupMenu();
    }

    /** Mouse Over handler. */
    private void onMouseOver() {
        tooltip.getStyle().setProperty("top", (panel.getAbsoluteTop() + panel.getOffsetHeight() + 9) + "px");
        tooltip.getStyle().setProperty("left", (panel.getAbsoluteLeft() + panel.getOffsetWidth() / 2 - 11) + "px");
        panel.setStyleName(css.popupButtonPanelOver());
    }

    /** Mouse Up handler. */
    private void onMouseUp() {
        panel.setStyleName(css.popupButtonPanelOver());
    }

    /** Opens Popup Menu. */
    public void openPopupMenu() {
        lockLayer = new MenuLockLayer(this);

        popupMenu = new PopupMenu(action, actionManager, place, presentationFactory, lockLayer, this, keyBindingAgent, "toolbar");
        lockLayer.add(popupMenu);

        int left = getAbsoluteLeft();
        int top = getAbsoluteTop() + 24;
        popupMenu.getElement().getStyle().setTop(top, com.google.gwt.dom.client.Style.Unit.PX);
        popupMenu.getElement().getStyle().setLeft(left, com.google.gwt.dom.client.Style.Unit.PX);
    }

    /** Redraw icon. */
    private void renderIcon() {
        if (enabled) {
            panel.getElement().removeClassName(css.disabled());
        } else {
            panel.getElement().addClassName(css.disabled());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onActionSelected(Action action) {
        closePopupMenu();
    }

    /** This class uses to handling mouse events on Popup Button. */
    private class ButtonPanel extends FlowPanel {

        public ButtonPanel() {
            sinkEvents(Event.ONMOUSEOVER | Event.ONMOUSEOUT | Event.ONMOUSEDOWN | Event.ONMOUSEUP | Event.ONCLICK);
        }

        /** Handle browser's events. */
        @Override
        public void onBrowserEvent(Event event) {
            if (!enabled) {
                return;
            }

            switch (DOM.eventGetType(event)) {
                case Event.ONMOUSEOVER:
                    onMouseOver();
                    break;

                case Event.ONMOUSEOUT:
                    onMouseOut();
                    break;

                case Event.ONMOUSEDOWN:
                    if (event.getButton() == Event.BUTTON_LEFT) {
                        onMouseDown();
                    }
                    break;

                case Event.ONMOUSEUP:
                    if (event.getButton() == Event.BUTTON_LEFT) {
                        onMouseUp();
                    }
                    break;

                case Event.ONCLICK:
                    onMouseClick();
                    break;

            }
        }

    }
}
