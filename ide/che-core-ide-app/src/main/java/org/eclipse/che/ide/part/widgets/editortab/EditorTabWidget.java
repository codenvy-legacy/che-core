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
package org.eclipse.che.ide.part.widgets.editortab;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import org.eclipse.che.ide.api.parts.PartPresenter;
import org.eclipse.che.ide.api.parts.PartStackUIResources;
import org.eclipse.che.ide.api.parts.PartStackView.TabPosition;
import org.vectomatic.dom.svg.ui.SVGImage;
import org.vectomatic.dom.svg.ui.SVGResource;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * @author Dmitry Shnurenko
 */
public class EditorTabWidget extends Composite implements EditorTab {
    interface EditorTabWidgetUiBinder extends UiBinder<Widget, EditorTabWidget> {
    }

    private static final EditorTabWidgetUiBinder UI_BINDER = GWT.create(EditorTabWidgetUiBinder.class);

    @UiField
    SimplePanel icon;
    @UiField
    Label       title;
    @UiField
    Image       closeIcon;

    @UiField(provided = true)
    final PartStackUIResources resources;

    private ActionDelegate delegate;

    @Inject
    public EditorTabWidget(PartStackUIResources resources, @Assisted SVGResource icon, @Assisted String title) {
        this.resources = resources;
        initWidget(UI_BINDER.createAndBindUi(this));

        this.title.setText(title);

        SVGImage image = new SVGImage(icon);
        this.icon.getElement().setInnerHTML(image.toString());

        addDomHandler(this, ClickEvent.getType());
        addDomHandler(this, DoubleClickEvent.getType());
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull
    public String getTitle() {
        return title.getText();
    }

    /** {@inheritDoc} */
    @Nonnull
    @Override
    public IsWidget getView() {
        return this.asWidget();
    }

    /** {@inheritDoc} */
    @Override
    public void update(@Nonnull PartPresenter part) {

    }

    /** {@inheritDoc} */
    @Override
    public void select() {
        closeIcon.addStyleName(resources.partStackCss().opacity());
        title.addStyleName(resources.partStackCss().activeTabTextColor());

        addStyleName(resources.partStackCss().selectEditorTab());
    }

    /** {@inheritDoc} */
    @Override
    public void unSelect() {
        closeIcon.removeStyleName(resources.partStackCss().opacity());
        title.removeStyleName(resources.partStackCss().activeTabTextColor());

        removeStyleName(resources.partStackCss().selectEditorTab());
    }

    /** {@inheritDoc} */
    @Override
    public void setTabPosition(@Nonnull TabPosition tabPosition) {
        throw new UnsupportedOperationException("This method doesn't allow in this class " + getClass());
    }

    /** {@inheritDoc} */
    @Override
    public void setErrorMark(boolean isVisible) {
        if (isVisible) {
            title.addStyleName(resources.partStackCss().lineError());
        } else {
            title.removeStyleName(resources.partStackCss().lineError());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setWarningMark(boolean isVisible) {
        if (isVisible) {
            title.addStyleName(resources.partStackCss().lineWarning());
        } else {
            title.removeStyleName(resources.partStackCss().lineWarning());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onClick(@Nonnull ClickEvent event) {
        delegate.onTabClicked(this);
    }

    /** {@inheritDoc} */
    @Override
    public void onDoubleClick(@Nonnull DoubleClickEvent event) {
        expandEditor();
    }

    private native void expandEditor() /*-{
        try {
            $wnd.IDE.eventHandlers.expandEditor();
        } catch (e) {
            console.log(e.message);
        }
    }-*/;

    /** {@inheritDoc} */
    @Override
    public void setDelegate(ActionDelegate delegate) {
        this.delegate = delegate;
    }

    @UiHandler("closeIcon")
    public void onCloseButtonClicked(@SuppressWarnings("UnusedParameters") ClickEvent event) {
        delegate.onTabClose(this);
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;

        EditorTabWidget that = (EditorTabWidget)other;

        return Objects.equals(title.getText(), that.title.getText());
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hashCode(title.getText());
    }
}