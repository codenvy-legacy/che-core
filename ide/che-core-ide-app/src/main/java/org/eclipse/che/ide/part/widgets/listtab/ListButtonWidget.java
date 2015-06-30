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
package org.eclipse.che.ide.part.widgets.listtab;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.eclipse.che.ide.Resources;
import org.eclipse.che.ide.part.widgets.listtab.item.ListItem;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Dmitry Shnurenko
 */
public class ListButtonWidget extends Composite implements ListButton {
    interface ListButtonWidgetUiBinder extends UiBinder<Widget, ListButtonWidget> {
    }

    private static final int Y_SHIFT = 21;

    private static final String GWT_POPUP_STANDARD_STYLE = "gwt-PopupPanel";

    private static final ListButtonWidgetUiBinder UI_BINDER = GWT.create(ListButtonWidgetUiBinder.class);

    private final FlowPanel      listPanel;
    private final PopupPanel     popupPanel;
    private final List<ListItem> items;

    @UiField
    FlowPanel button;

    @UiField(provided = true)
    final Resources resources;

    private ActionDelegate delegate;
    private boolean        isShown;

    @Inject
    public ListButtonWidget(Resources resources, FlowPanel listPanel, PopupPanel popupPanel) {
        this.resources = resources;

        initWidget(UI_BINDER.createAndBindUi(this));

        this.listPanel = listPanel;
        this.listPanel.addStyleName(resources.partStackCss().listItemPanel());

        this.popupPanel = popupPanel;
        this.popupPanel.removeStyleName(GWT_POPUP_STANDARD_STYLE);
        this.popupPanel.add(listPanel);

        addDomHandler(this, ClickEvent.getType());

        this.items = new ArrayList<>();
    }

    /** {@inheritDoc} */
    @Override
    public void showList() {
        int x = button.getAbsoluteLeft() + button.getOffsetWidth();
        int y = button.getAbsoluteTop() + Y_SHIFT;

        popupPanel.show();

        //alignment popup by right side of button. By default it alignments by left side.
        popupPanel.asWidget().getElement().setAttribute("style", "right:calc(100% - " + x + "px);top:" + y + "px;position: absolute");
    }

    /** {@inheritDoc} */
    @Override
    public void addListItem(@Nonnull ListItem listItem) {
        if (items.isEmpty()) {
            removeStyleName(resources.partStackCss().listShownButtonBackground());
        }

        items.add(listItem);

        listPanel.add(listItem);
    }

    /** {@inheritDoc} */
    @Override
    public void removeListItem(@Nonnull ListItem listItem) {
        items.remove(listItem);

        listPanel.remove(listItem);
    }

    /** {@inheritDoc} */
    @Override
    public void hide() {
        popupPanel.hide();

        removeStyleName(resources.partStackCss().listShownButtonBackground());
    }

    /** {@inheritDoc} */
    @Override
    public void onClick(@Nonnull ClickEvent event) {
        if (isShown) {
            hide();
        } else {
            delegate.onListButtonClicked();

            addStyleName(resources.partStackCss().listShownButtonBackground());
        }

        isShown = !isShown;
    }

    /** {@inheritDoc} */
    @Override
    public void setDelegate(@Nonnull ActionDelegate delegate) {
        this.delegate = delegate;
    }

}