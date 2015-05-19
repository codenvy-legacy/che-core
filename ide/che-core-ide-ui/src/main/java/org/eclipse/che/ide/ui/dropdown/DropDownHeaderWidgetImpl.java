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
package org.eclipse.che.ide.ui.dropdown;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimpleLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import org.vectomatic.dom.svg.ui.SVGImage;
import org.vectomatic.dom.svg.ui.SVGResource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Class provides general view representation for header of drop down list.
 *
 * @author Valeriy Svydenko
 */
public class DropDownHeaderWidgetImpl extends Composite implements ClickHandler, MouseOutHandler, MouseOverHandler, DropDownHeaderWidget {
    private static final Resources resources = GWT.create(Resources.class);

    static {
        resources.dropdownListCss().ensureInjected();
    }

    interface HeaderWidgetImplUiBinder extends UiBinder<Widget, DropDownHeaderWidgetImpl> {
    }

    private static final HeaderWidgetImplUiBinder UI_BINDER = GWT.create(HeaderWidgetImplUiBinder.class);

    @UiField
    SimpleLayoutPanel marker;
    @UiField
    SimpleLayoutPanel selectedElementImage;
    @UiField
    Label             selectedElementName;
    @UiField
    FlowPanel         selectedElement;
    @UiField
    FlowPanel         listHeader;

    private final DropDownListMenu dropDownListMenu;
    private final String           listId;

    private String selectedName;

    @AssistedInject
    public DropDownHeaderWidgetImpl(Resources resources, DropDownListMenu dropDownListMenu, @Nonnull @Assisted String listId) {
        this.dropDownListMenu = dropDownListMenu;
        this.listId = listId;

        initWidget(UI_BINDER.createAndBindUi(this));

        listHeader.setStyleName(resources.dropdownListCss().onMouseOut());

        marker.getElement().appendChild(resources.expansionImage().getSvg().getElement());
        marker.addStyleName(resources.dropdownListCss().expandedImage());

        selectedElement.setVisible(false);

        addDomHandler(this, ClickEvent.getType());
        addDomHandler(this, MouseOutEvent.getType());
        addDomHandler(this, MouseOverEvent.getType());
    }

    /** {@inheritDoc} */
    @Override
    public void setDelegate(@Nonnull ActionDelegate delegate) {
        //do nothing
    }

    /** {@inheritDoc} */
    @Override
    public void selectElement(@Nullable SVGResource icon, @Nonnull String title) {
        selectedName = title;

        selectedElement.setVisible(icon != null && !title.isEmpty());

        selectedElementImage.clear();
        if (icon != null) {
            selectedElementImage.add(new SVGImage(icon));
        }
        selectedElementName.setText(title);
    }

    /** {@inheritDoc} */
    @Nullable
    @Override
    public String getSelectedElementName() {
        return selectedName;
    }

    /** {@inheritDoc} */
    @Override
    public void onClick(ClickEvent event) {
        int left = getAbsoluteLeft() + listHeader.getOffsetWidth();
        int top = getAbsoluteTop() + listHeader.getOffsetHeight();
        dropDownListMenu.show(left, top, listId);
    }

    /** {@inheritDoc} */
    @Override
    public void onMouseOut(MouseOutEvent event) {
        listHeader.setStyleName(resources.dropdownListCss().onMouseOut());
    }

    /** {@inheritDoc} */
    @Override
    public void onMouseOver(MouseOverEvent event) {
        listHeader.setStyleName(resources.dropdownListCss().onMouseOver());
    }

    /** Item style selectors for a categories list item. */
    public interface DropdownCss extends CssResource {
        String expandedImage();

        String onMouseOver();

        String onMouseOut();
    }

    public interface Resources extends ClientBundle {
        @Source("DropdownList.css")
        DropdownCss dropdownListCss();

        @Source("expansionIcon.svg")
        SVGResource expansionImage();
    }
}