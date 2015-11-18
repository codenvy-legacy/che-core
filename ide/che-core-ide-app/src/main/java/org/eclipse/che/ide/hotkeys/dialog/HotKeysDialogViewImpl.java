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
package org.eclipse.che.ide.hotkeys.dialog;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.inject.Inject;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.hotkeys.HotKeyItem;
import org.eclipse.che.ide.ui.window.Window;

import java.util.List;

/**
 * Implementation {@link HotKeysDialogView}
 * @author Alexander Andrienko
 */
public class HotKeysDialogViewImpl extends Window implements HotKeysDialogView {

    interface KeyMapViewImplUiBinder extends UiBinder<Widget, HotKeysDialogViewImpl> {
    }
    
    private final CoreLocalizationConstant locale;

    private ActionDelegate delegate;

    Button okButton;
    @UiField(provided = true)
    DataGrid<HotKeyItem> dataGrid;

    @Inject
    public HotKeysDialogViewImpl(KeyMapViewImplUiBinder uiBinder, CoreLocalizationConstant locale, org.eclipse.che.ide.Resources res) {
        dataGrid = new DataGrid<>(50, res);
        this.locale = locale;
        this.setTitle(locale.hotKeysDialogTitle());
        this.setWidget(uiBinder.createAndBindUi(this));

        okButton = createButton(locale.ok(), "hot-keys-dialog-ok-btn", new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                delegate.onBtnOkClicked();
            }
        });
        addButtonToFooter(okButton);
        createTable();
    }

    private void createTable() {
        TextColumn<HotKeyItem> actionColumn = new TextColumn<HotKeyItem>() {
            @Override
            public String getValue(HotKeyItem hotKeyItem) {
                return hotKeyItem.getActionDescription();
            }
        };
        Header<String> actionColumnHeader = new Header<String>(new TextCell()) {
            @Override
            public String getValue() {
                return locale.hotKeysTableActionDescriptionTitle();
            }
        };
        dataGrid.addColumn(actionColumn, actionColumnHeader);

        TextColumn<HotKeyItem> keyColumn = new TextColumn<HotKeyItem>() {
            @Override
            public String getValue(HotKeyItem hotKeyItem) {
                return hotKeyItem.getHotKey();
            }
        };
        Header<String> hotKeyColumnHeader = new Header<String>(new TextCell()) {
            @Override
            public String getValue() {
                return locale.hotKeysTableItemTitle();
            }
        };
        dataGrid.addColumn(keyColumn, hotKeyColumnHeader);

        SingleSelectionModel<HotKeyItem> selectionModel = new SingleSelectionModel<>();
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
            }
        });
        dataGrid.setSelectionModel(selectionModel);
    }

    /** {@inheritDoc} */
    @Override
    public void setDelegate(ActionDelegate delegate) {
        this.delegate = delegate;
    }

    /** {@inheritDoc} */
    @Override
    public void showDialog() {
        dataGrid.redraw();
        this.show();
    }
    
    /** {@inheritDoc} */
    @Override
    public void close() {
        this.hide();
    }

    /** {@inheritDoc} */
    @Override
    public void setData(List<HotKeyItem> data) {
        dataGrid.setRowData(data);
    }
}
