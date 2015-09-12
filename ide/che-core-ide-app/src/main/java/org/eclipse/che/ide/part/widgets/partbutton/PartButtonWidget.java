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
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
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

    @Inject
    public PartButtonWidget(Resources resources, @Assisted String title) {
        this.resources = resources;

        initWidget(UI_BINDER.createAndBindUi(this));
        setStyleName(resources.partStackCss().idePartStackTab());

        addDomHandler(this, ClickEvent.getType());

        buttonName.setText(title);
    }

    /** {@inheritDoc} */
    @NotNull
    @Override
    public IsWidget getView() {
        return this.asWidget();
    }

    /** {@inheritDoc} */
    @NotNull
    public PartButton addTooltip(@Nullable String tooltip) {
        setTitle(tooltip);
        return this;
    }

    /** {@inheritDoc} */
    @NotNull
    public PartButton addIcon(@Nullable SVGResource resource) {
        icon.getElement().setInnerHTML(resource == null ? "" : new SVGImage(resource).toString());
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public void update(@NotNull PartPresenter part) {
        icon.add(part.getTitleWidget());
    }

    /** {@inheritDoc} */
    @Override
    public void onClick(@NotNull ClickEvent event) {
        delegate.onTabClicked(this);
    }

    /** {@inheritDoc} */
    @Override
    public void select() {
        if (BELOW.equals(tabPosition)) {
            addStyleName(resources.partStackCss().selectedBottomTab());
        } else {
            addStyleName(resources.partStackCss().selectedRightOrLeftTab());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void unSelect() {
        if (BELOW.equals(tabPosition)) {
            removeStyleName(resources.partStackCss().selectedBottomTab());
        } else {
            removeStyleName(resources.partStackCss().selectedRightOrLeftTab());
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
