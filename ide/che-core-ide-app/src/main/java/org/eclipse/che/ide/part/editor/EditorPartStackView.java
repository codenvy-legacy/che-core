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
package org.eclipse.che.ide.part.editor;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.DeckLayoutPanel;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.eclipse.che.ide.api.parts.PartPresenter;
import org.eclipse.che.ide.api.parts.PartStackUIResources;
import org.eclipse.che.ide.api.parts.PartStackView;
import org.eclipse.che.ide.part.widgets.listtab.ListButton;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static com.google.gwt.dom.client.Style.Display.BLOCK;
import static com.google.gwt.dom.client.Style.Display.NONE;
import static com.google.gwt.dom.client.Style.Unit.PCT;

/**
 * @author Evgen Vidolob
 * @author Dmitry Shnurenko
 * @author Vitaliy Guliy
 */
public class EditorPartStackView extends ResizeComposite implements PartStackView, MouseDownHandler {

    interface PartStackUiBinder extends UiBinder<Widget, EditorPartStackView> {
    }

    private static final PartStackUiBinder UI_BINDER = GWT.create(PartStackUiBinder.class);

    @UiField
    DockLayoutPanel parent;

    @UiField
    FlowPanel       tabsPanel;

    @UiField
    DeckLayoutPanel contentPanel;

    private final Map<PartPresenter, TabItem> tabs;
    private final AcceptsOneWidget            partViewContainer;
    private final LinkedList<PartPresenter>   contents;
    private final PartStackUIResources        resources;

    private ActionDelegate delegate;
    private ListButton     listButton;
    private TabItem        activeTab;

    @Inject
    public EditorPartStackView(PartStackUIResources resources) {
        this.resources = resources;
        this.tabs = new HashMap<>();
        this.contents = new LinkedList<>();

        initWidget(UI_BINDER.createAndBindUi(this));

        partViewContainer = new AcceptsOneWidget() {
            @Override
            public void setWidget(IsWidget widget) {
                contentPanel.add(widget);
            }
        };

        addDomHandler(this, MouseDownEvent.getType());
    }

    /** {@inheritDoc} */
    @Override
    protected void onAttach() {
        super.onAttach();

        Style style = getElement().getParentElement().getStyle();
        style.setHeight(100, PCT);
        style.setWidth(100, PCT);
    }

    /**
     * Adds list button in special place on view.
     *
     * @param listButton
     *         button which will be added
     */
    public void setListButton(@NotNull ListButton listButton) {
        this.listButton = listButton;
        tabsPanel.add(listButton);
        listButton.setVisible(false);
    }

    /** {@inheritDoc} */
    @Override
    public void onMouseDown(@NotNull MouseDownEvent event) {
        delegate.onRequestFocus();
    }

    /** {@inheritDoc} */
    @Override
    public void setDelegate(ActionDelegate delegate) {
        this.delegate = delegate;
    }

    /** {@inheritDoc} */
    @Override
    public void addTab(@NotNull TabItem tabItem, @NotNull PartPresenter partPresenter) {
        /** Show editor area if it is empty and hidden */
        if (contents.isEmpty()) {
            getElement().getParentElement().getStyle().setDisplay(BLOCK);
        }

        /** Add editor tab to tab panel */
        tabsPanel.add(tabItem.getView());

        /** Process added editor tab */
        tabs.put(partPresenter, tabItem);
        contents.add(partPresenter);
        partPresenter.go(partViewContainer);
    }

    /**
     * Updates visibility of file list button.
     */
    private void updateDropdownVisibility() {
        if (tabsPanel.getWidgetCount() == 1) {
            listButton.setVisible(false);
            return;
        }

        int width = 0;
        for (int i = 0; i < tabsPanel.getWidgetCount(); i++) {
            if (listButton != null && listButton != tabsPanel.getWidget(i)) {
                if (tabsPanel.getWidget(i).isVisible()) {
                    width += tabsPanel.getWidget(i).getOffsetWidth();
                } else {
                    tabsPanel.getWidget(i).setVisible(true);
                    width += tabsPanel.getWidget(i).getOffsetWidth();
                    tabsPanel.getWidget(i).setVisible(false);
                }
            }
        }

        listButton.setVisible(width >= tabsPanel.getOffsetWidth());
    }

    /**
     * Makes active tab visible.
     */
    private void ensureActiveTabVisible() {
        if (activeTab == null) {
            return;
        }

        for (int i = 0; i < tabsPanel.getWidgetCount(); i++) {
            if (listButton != null && listButton != tabsPanel.getWidget(i)) {
                tabsPanel.getWidget(i).setVisible(true);
            }
        }

        for (int i = 0; i < tabsPanel.getWidgetCount(); i++) {
            if (listButton != null && listButton != tabsPanel.getWidget(i)) {
                if (activeTab.getView().asWidget().getAbsoluteTop() > tabsPanel.getAbsoluteTop()) {
                    tabsPanel.getWidget(i).setVisible(false);
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void removeTab(@NotNull PartPresenter presenter) {
        TabItem tab = tabs.get(presenter);
        tabsPanel.remove(tab.getView());
        contentPanel.remove(presenter.getView());

        tabs.remove(presenter);
        contents.remove(presenter);

        try {
            PartPresenter activePart = contents.getLast();
            selectTab(activePart);
        } catch (NoSuchElementException exception) {
            getElement().getParentElement().getStyle().setDisplay(NONE);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void selectTab(@NotNull PartPresenter partPresenter) {
        IsWidget view = partPresenter.getView();

        int viewIndex = contentPanel.getWidgetIndex(view);
        if (viewIndex < 0) {
            partPresenter.go(partViewContainer);
            viewIndex = contentPanel.getWidgetIndex(view);
        }

        contentPanel.showWidget(viewIndex);
        setActiveTab(partPresenter);
    }

    /**
     * Switches to specified tab.
     *
     * @param part tab part
     */
    private void setActiveTab(@NotNull PartPresenter part) {
        for (TabItem tab : tabs.values()) {
            tab.unSelect();
            tab.getView().asWidget().getElement().removeAttribute("active");
        }

        activeTab = tabs.get(part);
        activeTab.select();

        activeTab.getView().asWidget().getElement().setAttribute("active", "");

        delegate.onRequestFocus();

        updateDropdownVisibility();
        ensureActiveTabVisible();
    }

    /** {@inheritDoc} */
    @Override
    public void setTabPositions(List<PartPresenter> partPositions) {
        throw new UnsupportedOperationException("The method doesn't allowed in this class " + getClass());
    }

    /** {@inheritDoc} */
    @Override
    public void setFocus(boolean focused) {
        if (focused) {
            activeTab.select();
        } else {
            activeTab.unSelect();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void updateTabItem(@NotNull PartPresenter partPresenter) {
        TabItem tab = tabs.get(partPresenter);
        tab.update(partPresenter);
    }

    @Override
    public void onResize() {
        super.onResize();
        updateDropdownVisibility();
        ensureActiveTabVisible();
    }

}
