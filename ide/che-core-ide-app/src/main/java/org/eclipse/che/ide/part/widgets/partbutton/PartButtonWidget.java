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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.eclipse.che.ide.api.parts.PartStackView.TabPosition.BELOW;
import static org.eclipse.che.ide.api.parts.PartStackView.TabPosition.LEFT;

/**
 * @author Dmitry Shnurenko
 */
public class PartButtonWidget extends Composite implements PartButton {
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

        addDomHandler(this, ClickEvent.getType());

        buttonName.setText(title);
    }

    /** {@inheritDoc} */
    @Nonnull
    @Override
    public IsWidget getView() {
        return this.asWidget();
    }

    /** {@inheritDoc} */
    @Nonnull
    public PartButton addTooltip(@Nullable String tooltip) {
        setTitle(tooltip);
        return this;
    }

    /** {@inheritDoc} */
    @Nonnull
    public PartButton addIcon(@Nullable SVGResource resource) {
        icon.getElement().setInnerHTML(getSvgDiv(resource));
        return this;
    }

    private String getSvgDiv(@Nullable SVGResource resource) {
        if (resource == null) {
            return "";
        }

        SVGImage image = new SVGImage(resource);

        return image.toString();
    }

    /** {@inheritDoc} */
    @Override
    public void update(@Nonnull PartPresenter part) {
        icon.add(part.getTitleWidget());
    }

    /** {@inheritDoc} */
    @Override
    public void onClick(@Nonnull ClickEvent event) {
        delegate.onTabClicked(this);
    }

    /** {@inheritDoc} */
    @Override
    public void select() {
        if (!BELOW.equals(tabPosition)) {
            addStyleName(resources.partStackCss().selectedRightOrLeftTab());

            return;
        }

        addStyleName(resources.partStackCss().idePartStackToolTabSelected());
    }

    /** {@inheritDoc} */
    @Override
    public void unSelect() {
        if (!BELOW.equals(tabPosition)) {
            removeStyleName(resources.partStackCss().selectedRightOrLeftTab());

            return;
        }

        removeStyleName(resources.partStackCss().idePartStackToolTabSelected());
    }

    /** {@inheritDoc} */
    @Override
    public void setTabPosition(@Nonnull TabPosition tabPosition) {
        this.tabPosition = tabPosition;

        if (LEFT.equals(tabPosition)) {
            addStyleName(resources.partStackCss().leftTabBorders());

            return;
        }

        addStyleName(resources.partStackCss().tabBordersDefault());
    }

    /** {@inheritDoc} */
    @Override
    public void setDelegate(@Nonnull ActionDelegate delegate) {
        this.delegate = delegate;
    }
}