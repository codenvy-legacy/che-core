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
package org.eclipse.che.ide.part.widgets.listtab.item;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import javax.annotation.Nonnull;

/**
 * @author Dmitry Shnurenko
 */
public class ListItemWidget extends Composite implements ListItem {
    interface ListItemWidgetUiBinder extends UiBinder<Widget, ListItemWidget> {
    }

    private static final ListItemWidgetUiBinder UI_BINDER = GWT.create(ListItemWidgetUiBinder.class);

    @UiField
    Label title;
    @UiField
    Image closeIcon;

    private ActionDelegate delegate;

    public static ListItem create(@Nonnull String title) {
        return new ListItemWidget(title);
    }

    public ListItemWidget(@Nonnull String title) {
        initWidget(UI_BINDER.createAndBindUi(this));

        this.title.setText(title);
    }


    /** {@inheritDoc} */
    @Override
    public void setDelegate(ActionDelegate delegate) {
        this.delegate = delegate;
    }

    @UiHandler("closeIcon")
    public void onCloseButtonClicked(@SuppressWarnings("UnusedParameters") ClickEvent event) {
        delegate.onCloseItemClicked(this);
    }
}