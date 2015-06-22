package org.eclipse.che.ide.part.widgets.partbutton;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import org.eclipse.che.ide.Resources;
import org.eclipse.che.ide.api.parts.PartPresenter;
import org.eclipse.che.ide.api.parts.PartStackView.TabItem;
import org.vectomatic.dom.svg.ui.SVGImage;
import org.vectomatic.dom.svg.ui.SVGResource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Dmitry Shnurenko
 */
public class PartButtonWidget extends Composite implements TabItem {
    interface PartButtonWidgetUiBinder extends UiBinder<Widget, PartButtonWidget> {
    }

    private static final PartButtonWidgetUiBinder UI_BINDER = GWT.create(PartButtonWidgetUiBinder.class);

    private final Resources resources;

    @UiField
    DockLayoutPanel main;
    @UiField
    SimplePanel     icon;
    @UiField
    Label           buttonName;

    private IsWidget       widget;
    private ActionDelegate delegate;

    private boolean isSelected;

    @Inject
    public PartButtonWidget(Resources resources, @Assisted String title) {
        this.resources = resources;
        initWidget(UI_BINDER.createAndBindUi(this));

        addDomHandler(this, ClickEvent.getType());

        buttonName.setText(title);
    }

    /** {@inheritDoc} */
    @Override
    public IsWidget getView() {
        return this.asWidget();
    }

    /** {@inheritDoc} */
    @Nonnull
    public TabItem addTooltip(@Nullable String tooltip) {
        return this;
    }

    /** {@inheritDoc} */
    @Nonnull
    public TabItem addIcon(@Nullable SVGResource resource) {
        if (resource != null) {
            SVGImage image = new SVGImage(resource);
            icon.getElement().setInnerHTML(image.toString());
        }

        return this;
    }

    /** {@inheritDoc} */
    @Override
    public void update(@Nonnull PartPresenter part) {

    }

    /** {@inheritDoc} */
    @Nonnull
    public TabItem addWidget(@Nullable IsWidget widget) {
        this.widget = widget;
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public void onClick(@Nonnull ClickEvent event) {
        isSelected = !isSelected;

        delegate.onTabClicked(this, isSelected);
    }

    /** {@inheritDoc} */
    @Override
    public void select() {
        addStyleName(resources.partStackCss().idePartStackToolTabSelected());
    }

    /** {@inheritDoc} */
    @Override
    public void unSelect() {
        removeStyleName(resources.partStackCss().idePartStackToolTabSelected());
    }

    /** {@inheritDoc} */
    @Override
    public void setDelegate(@Nonnull ActionDelegate delegate) {
        this.delegate = delegate;
    }
}