/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.extension;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.TextAreaElement;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.Resources;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.action.DefaultActionGroup;
import org.eclipse.che.ide.api.extension.ExtensionDescription;
import org.eclipse.che.ide.ui.toolbar.ToolbarPresenter;
import org.eclipse.che.ide.util.loging.Log;

import java.util.List;

/**
 * @author <a href="mailto:evidolob@codenvy.com">Evgen Vidolob</a>
 * @version $Id:
 */
@Singleton
public class ExtensionManagerViewImpl implements ExtensionManagerView {
    private static ExtensionManagerViewImplUiBinder ourUiBinder = GWT.create(ExtensionManagerViewImplUiBinder.class);
    private final DockLayoutPanel rootElement;
    @UiField
    Style                          style;
    @UiField(provided = true)
    DataGrid<ExtensionDescription> dataGrid;
    @UiField
    TextAreaElement                descriptionArea;
    @UiField
    SimplePanel                    toolBarPanel;
    private ActionDelegate delegate;

    @Inject
    public ExtensionManagerViewImpl(ToolbarPresenter toolbarPresenter, ActionManager actionManager, Resources resources) {
        dataGrid = new DataGrid<ExtensionDescription>(100, resources);
        rootElement = ourUiBinder.createAndBindUi(this);
//        toolbarPresenter.go(toolBarPanel);
        DefaultActionGroup actionGroup = new DefaultActionGroup("extensionManager", false, actionManager);
        actionManager.registerAction("extensionManagerGroup", actionGroup);
//        SortByStatusAction sortByStatusAction = new SortByStatusAction(this, resources);
//        actionManager.registerAction("extensionManagerSortByStatus", sortByStatusAction);
//        actionGroup.add(sortByStatusAction);
        toolbarPresenter.bindMainGroup(actionGroup);
        UIObject.ensureDebugId(descriptionArea, "window-preferences-extensions-descriptionArea");

//        CheckboxCell checkboxCell = new CheckboxCell(false, false);
//        Column<ExtensionDescription, Boolean> enabledColumn = new Column<ExtensionDescription, Boolean>(checkboxCell) {
//            @Override
//            public Boolean getValue(ExtensionDescription object) {
//                return object.isEnabled();
//            }
//
//            @Override
//            public void render(Cell.Context context, ExtensionDescription object, SafeHtmlBuilder sb) {
//                sb.appendHtmlConstant(
//                        "<div id=\"" + UIObject.DEBUG_ID_PREFIX + "window-preferences-extensions-row-" + context.getIndex() + "\">");
//                super.render(context, object, sb);
//            }
//        };

//        enabledColumn.setFieldUpdater(new FieldUpdater<ExtensionDescription, Boolean>() {
//            @Override
//            public void update(int index, ExtensionDescription object, Boolean value) {
//                object.setEnabled(value);
//                delegate.setDirty();
//            }
//        });
//        enabledColumn.setCellStyleNames(style.enabledColumn());
//
//        dataGrid.addColumn(enabledColumn);
//        dataGrid.setColumnWidth(enabledColumn, 75, com.google.gwt.dom.client.Style.Unit.PX);

        Column<ExtensionDescription, String> titleColumn = new Column<ExtensionDescription, String>(new TextCell()) {
            @Override
            public String getValue(ExtensionDescription object) {
                return object.getTitle();
            }
        };
        titleColumn.setCellStyleNames(style.titleColumn());

        dataGrid.addColumn(titleColumn);
        SingleSelectionModel<ExtensionDescription> selectionModel = new SingleSelectionModel<ExtensionDescription>();
        dataGrid.setSelectionModel(selectionModel);
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {

            }
        });

    }

    /** {@inheritDoc} */
    @Override
    public void setDelegate(ActionDelegate delegate) {
        this.delegate = delegate;
    }

    /** {@inheritDoc} */
    @Override
    public Widget asWidget() {
        return rootElement;
    }

    public void sortByStatus(boolean state) {
        Log.info(getClass(), "Not implemented yet!");
    }

    /** {@inheritDoc} */
    @Override
    public void setExtensions(List<ExtensionDescription> extensions) {
        dataGrid.setRowData(extensions);
        dataGrid.redraw();
    }

    interface ExtensionManagerViewImplUiBinder
            extends UiBinder<DockLayoutPanel, ExtensionManagerViewImpl> {
    }

    interface Style extends CssResource {
        String headerTitle();

        String labelActivate();

        String labelName();

        String enabledColumn();

        String titleColumn();

        String leftSeparator();

        String chatMessageInput();

        String messageInputContainer();
    }
}