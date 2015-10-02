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
package org.eclipse.che.ide.ui.loaders.initializationLoader;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.safehtml.shared.SimpleHtmlSanitizer;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.view.client.ListDataProvider;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.ui.loaders.LoaderResources;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of {@link LoaderView}.
 *
 * @author Roman Nikitenko
 */
@Singleton
public class LoaderViewImpl extends PopupPanel implements LoaderView, ResizeHandler {

    private static final String PRE_STYLE       = "style='margin:0px;'";
    private static final int    EXPAND_WIDTH    = 650;
    private static final int    EXPAND_HEIGHT   = 321;
    private static final int    COLLAPSE_WIDTH  = 400;
    private static final int    COLLAPSE_HEIGHT = 65;
    private static final int    DELTA_WIDTH     = (EXPAND_WIDTH - COLLAPSE_WIDTH) / 2;
    private static final int    DELTA_HEIGHT    = (EXPAND_HEIGHT - COLLAPSE_HEIGHT) / 2;

    @UiField
    SimplePanel operationPanel;
    @UiField
    FlowPanel   expandHolder;
    @UiField
    FlowPanel   expandPanel;
    @UiField
    ScrollPanel scroll;
    @UiField
    FlowPanel   detailsArea;
    @UiField
    Button      closeButton;

    @UiField(provided = true)
    CellTable<OperationInfo> detailsTable;

    private       HandlerRegistration             resizeHandler;
    private       ListDataProvider<OperationInfo> dataProvider;
    private       List<OperationInfo>             operationData;
    private final DivElement                      expander;
    private       LoaderResources                 resources;
    private       ActionDelegate                  delegate;

    @Inject
    public LoaderViewImpl(LoaderViewImplUiBinder uiBinder,
                          LoaderResources resources) {
        this.resources = resources;
        resources.Css().ensureInjected();

        this.setPixelSize(COLLAPSE_WIDTH, COLLAPSE_HEIGHT);

        createTable();

        DockLayoutPanel rootElement = uiBinder.createAndBindUi(this);
        this.add(rootElement);

        expander = Document.get().createDivElement();
        expander.appendChild(resources.expansionImage().getSvg().getElement());
        expander.setClassName(resources.Css().expandControl());
        expandHolder.getElement().appendChild(expander);
        expandPanel.sinkEvents(Event.ONCLICK);
        expandPanel.addDomHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                delegate.onDetailsClicked();
            }
        }, ClickEvent.getType());

        detailsArea.setVisible(false);
        closeButton.setEnabled(false);

        setGlassEnabled(true);
        getGlassElement().getStyle().setOpacity(0);
        getGlassElement().getStyle().setZIndex(9999998);
        getElement().getStyle().setZIndex(9999999);
    }

    @UiHandler("closeButton")
    public void onCloseClicked(ClickEvent event) {
        delegate.onCloseClicked();
    }

    @Override
    public void expandDetails() {
        resize(EXPAND_WIDTH, EXPAND_HEIGHT, true);
        detailsArea.setVisible(true);
        expander.addClassName(resources.Css().expandedImage());
        scrollBottom();
    }

    @Override
    public void collapseDetails() {
        resize(COLLAPSE_WIDTH, COLLAPSE_HEIGHT, false);
        detailsArea.setVisible(false);
        expander.removeClassName(resources.Css().expandedImage());
    }

    /** {@inheritDoc} */
    @Override
    public void setDelegate(ActionDelegate delegate) {
        this.delegate = delegate;
    }

    /** {@inheritDoc} */
    @Override
    public void print(OperationInfo info) {
        final HTML html = new HTML(buildSafeHtmlMessage(info.getOperation()));
        html.getElement().getStyle().setPaddingLeft(2, Style.Unit.PX);
        html.getElement().getStyle().setPaddingTop(9, Style.Unit.PX);
        operationPanel.clear();
        operationPanel.add(html);
    }

    /** {@inheritDoc} */
    @Override
    public void printToDetails(OperationInfo info) {
        operationData.add(info);
        dataProvider.refresh();
    }

    /** {@inheritDoc} */
    @Override
    public void scrollBottom() {
        scroll.getElement().setScrollTop(scroll.getElement().getScrollHeight());
    }

    /** {@inheritDoc} */
    @Override
    public void show(OperationInfo info) {
        closeButton.setEnabled(false);
        center();
        print(info);

        if (resizeHandler == null) {
            resizeHandler = Window.addResizeHandler(this);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void hide() {
        if (resizeHandler != null) {
            resizeHandler.removeHandler();
        }
        resizeHandler = null;
        super.hide();

        clearContent();
    }

    private void clearContent() {
        operationPanel.clear();
        operationData.clear();
    }

    @Override
    public void update() {
        dataProvider.refresh();
    }

    @Override
    public void setEnabledCloseButton(boolean enabled) {
        closeButton.setEnabled(enabled);
    }

    /** Return sanitized message (with all restricted HTML-tags escaped) in {@link SafeHtml}. */
    private SafeHtml buildSafeHtmlMessage(String message) {
        return new SafeHtmlBuilder()
                .appendHtmlConstant("<pre " + PRE_STYLE + ">")
                .append(SimpleHtmlSanitizer.sanitizeHtml(message))
                .appendHtmlConstant("</pre>")
                .toSafeHtml();
    }

    /** {@inheritDoc} */
    @Override
    public void onResize(ResizeEvent event) {
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                center();
            }
        });
    }

    private void resize(final int width, final int height, final boolean expanded) {
        this.setPixelSize(width, height);

        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                int left = (expanded) ? getPopupLeft() - DELTA_WIDTH : getPopupLeft() + DELTA_WIDTH;
                int top = (expanded) ? getPopupTop() - DELTA_HEIGHT : getPopupTop() + DELTA_HEIGHT;
                setPopupPosition(left, top);
            }
        });

    }

    private void createTable() {
        operationData = new ArrayList<>();
        detailsTable = new CellTable<>();
        dataProvider = new ListDataProvider<>();
        dataProvider.addDataDisplay(detailsTable);
        operationData = dataProvider.getList();

        Column<OperationInfo, SafeHtml> operationColumn = new Column<OperationInfo, SafeHtml>(new SafeHtmlCell()) {
            @Override
            public SafeHtml getValue(final OperationInfo item) {
                return SafeHtmlUtils.fromString(item.getOperation());
            }
        };

        Column<OperationInfo, SafeHtml> statusColumn = new Column<OperationInfo, SafeHtml>(new SafeHtmlCell()) {
            @Override
            public SafeHtml getValue(final OperationInfo item) {
                return SafeHtmlUtils.fromString(item.getStatus().getValue());
            }

            @Override
            public String getCellStyleNames(Cell.Context context, OperationInfo info) {
                switch (info.getStatus()) {
                    case ERROR:
                        return resources.Css().errorStatus();
                    case IN_PROGRESS:
                        return resources.Css().inProgressStatus();
                    default:
                        return resources.Css().successStatus();
                }
            }
        };

        detailsTable.setColumnWidth(statusColumn, 100, Style.Unit.PX);
        detailsTable.setColumnWidth(operationColumn, 250, Style.Unit.PX);
        detailsTable.addColumn(operationColumn);
        detailsTable.addColumn(statusColumn);
        detailsTable.setVisibleRange(0, 10000);
    }

    interface LoaderViewImplUiBinder extends UiBinder<DockLayoutPanel, LoaderViewImpl> {
    }
}
