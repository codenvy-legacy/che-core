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
package org.eclipse.che.ide.part.widgets.partbutton;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import org.eclipse.che.ide.Resources;
import org.eclipse.che.ide.api.parts.PartPresenter;
import org.eclipse.che.ide.api.parts.PartStackView.TabPosition;
import org.vectomatic.dom.svg.ui.SVGImage;
import org.vectomatic.dom.svg.ui.SVGResource;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import org.eclipse.che.commons.annotation.Nullable;

import static com.google.gwt.dom.client.Style.Unit.PX;
import static org.eclipse.che.ide.api.parts.PartStackView.TabPosition.BELOW;
import static org.eclipse.che.ide.api.parts.PartStackView.TabPosition.LEFT;
import static org.eclipse.che.ide.api.parts.PartStackView.TabPosition.RIGHT;

/**
 * @author Dmitry Shnurenko
 * @author Valeriy Svydenko
 * @author Vitaliy Guliy
 */
public class PartButtonWidget extends Composite implements PartButton {

    /** Shifting left and right panels from the top */
    private final static int TOP_SHIFT = 63;

    interface PartButtonWidgetUiBinder extends UiBinder<Widget, PartButtonWidget> {
    }

    private static final PartButtonWidgetUiBinder UI_BINDER = GWT.create(PartButtonWidgetUiBinder.class);

    private final Resources resources;

    @UiField
    SimplePanel icon;

    @UiField
    Label       buttonName;

    private ActionDelegate delegate;
    private TabPosition    tabPosition;

    private Widget badgeWidget;

    private boolean selected;

    private SVGResource iconResource;

    @Inject
    public PartButtonWidget(Resources resources, @Assisted String title) {
        this.resources = resources;

        initWidget(UI_BINDER.createAndBindUi(this));
        setStyleName(resources.partStackCss().idePartStackTab());
        this.ensureDebugId("partButton-" + title);

        addDomHandler(this, ClickEvent.getType());

        buttonName.setText(title);
    }

    /** {@inheritDoc} */
    @NotNull
    @Override
    public IsWidget getView() {
        return this.asWidget();
    }

    @Override
    public Widget getIcon() {
        if (iconResource != null) {
            return new SVGImage(iconResource);
        }

        return null;
    }

    /** {@inheritDoc} */
    @NotNull
    public PartButton setTooltip(@Nullable String tooltip) {
        setTitle(tooltip);
        return this;
    }

    /** {@inheritDoc} */
    @NotNull
    @Override
    public PartButton setIcon(@Nullable SVGResource iconResource) {
        this.iconResource = iconResource;
        icon.clear();

        if (iconResource != null) {
            icon.setWidget(new SVGImage(iconResource));
        }

        return this;
    }

    /** {@inheritDoc} */
    @Override
    public void update(@NotNull PartPresenter part) {
        if (badgeWidget != null) {
            badgeWidget.getElement().removeFromParent();
            badgeWidget = null;
        }

        int unreadMessages = part.getUnreadNotificationsCount();
        if (unreadMessages == 0) {
            return;
        }

        badgeWidget = getBadge(unreadMessages);
        if (badgeWidget != null) {
            icon.getParent().getElement().appendChild(badgeWidget.asWidget().getElement());
            updateBadge();
        }
    }

    /**
     * Creates a badge widget with a message
     *
     * @param messages messages count
     * @return new badge widget
     */
    private Widget getBadge(int messages) {
        FlowPanel w = new FlowPanel();
        Style s = w.getElement().getStyle();

        s.setProperty("position", "absolute");
        s.setProperty("width", "12px");
        s.setProperty("height", "12px");

        s.setProperty("boxSizing", "border-box");
        s.setProperty("borderRadius", "8px");
        s.setProperty("textAlign", "center");

        s.setProperty("color", org.eclipse.che.ide.api.theme.Style.getBadgeFontColor());

        s.setProperty("left", "13px");
        s.setProperty("top", "3px");
        s.setProperty("borderWidth", "1.5px");
        s.setProperty("borderStyle", "solid");

        s.setProperty("fontFamily", "'Helvetica Neue', 'Myriad Pro', arial, Verdana, Verdana, sans-serif");
        s.setProperty("fontSize", "9.5px");
        s.setProperty("fontWeight", "bold");
        s.setProperty("textShadow", "none");

        s.setProperty("backgroundColor", org.eclipse.che.ide.api.theme.Style.getBadgeBackgroundColor());

        w.setStyleName("bounceOutUp");

        if (messages > 9) {
            s.setProperty("lineHeight", "5px");
            w.getElement().setInnerHTML("...");
        } else {
            s.setProperty("lineHeight", "10px");
            w.getElement().setInnerHTML("" + messages);
        }

        return w;
    }

    /** {@inheritDoc} */
    @Override
    public void onClick(@NotNull ClickEvent event) {
        delegate.onTabClicked(this);
    }

    /** {@inheritDoc} */
    @Override
    public void select() {
        selected = true;

        if (BELOW.equals(tabPosition)) {
            addStyleName(resources.partStackCss().selectedBottomTab());
        } else {
            addStyleName(resources.partStackCss().selectedRightOrLeftTab());
        }

        updateBadge();
    }

    /** {@inheritDoc} */
    @Override
    public void unSelect() {
        selected = false;

        if (BELOW.equals(tabPosition)) {
            removeStyleName(resources.partStackCss().selectedBottomTab());
        } else {
            removeStyleName(resources.partStackCss().selectedRightOrLeftTab());
        }

        updateBadge();
    }

    /**
     * Updates a badge style.
     */
    private void updateBadge() {
        if (badgeWidget == null) {
            return;
        }

        if (selected) {
            badgeWidget.getElement().getStyle().setBorderColor(org.eclipse.che.ide.api.theme.Style.theme.activeTabBackground());
        } else {
            badgeWidget.getElement().getStyle().setBorderColor(org.eclipse.che.ide.api.theme.Style.theme.tabsPanelBackground());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setTabPosition(@NotNull TabPosition tabPosition, @Min(value=0) int countWidgets) {
        this.tabPosition = tabPosition;

        if (LEFT.equals(tabPosition)) {
            addStyleName(resources.partStackCss().leftTabs());
            getElement().getStyle().setTop((countWidgets - 1) * TOP_SHIFT, PX);
        } else if (RIGHT.equals(tabPosition)) {
            addStyleName(resources.partStackCss().rightTabs());
            getElement().getStyle().setTop((countWidgets - 1) * TOP_SHIFT, PX);
        } else {
            addStyleName(resources.partStackCss().bottomTabs());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setDelegate(@NotNull ActionDelegate delegate) {
        this.delegate = delegate;
    }

}
