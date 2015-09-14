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
 */
public class EditorPartStackView extends ResizeComposite implements PartStackView, MouseDownHandler {
    interface PartStackUiBinder extends UiBinder<Widget, EditorPartStackView> {
    }

    private static final PartStackUiBinder UI_BINDER = GWT.create(PartStackUiBinder.class);

    //this margin need to stay distance for not visible component. The component is list button which appears when
    //common length of editor's tabs is more than width of panel, on which these tabs are added.
    private static final int LEFT_MARGIN = 250;
    private static final int THE_FIRST   = 1;

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

    private int addedTabsWidth;
    private int tabsPanelWidth;

    @Inject
    public EditorPartStackView(PartStackUIResources resources) {
        this.resources = resources;

        initWidget(UI_BINDER.createAndBindUi(this));

        this.tabs = new HashMap<>();
        this.contents = new LinkedList<>();

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
    public void addTab(@NotNull TabItem tabItem, @NotNull PartPresenter presenter) {
        if (contents.isEmpty()) {
            getElement().getParentElement().getStyle().setDisplay(BLOCK);

            addedTabsWidth = 0;
        }

        if (addedTabsWidth > tabsPanelWidth) {
            tabsPanel.insert(tabItem.getView(), THE_FIRST);
        } else {
            tabsPanel.add(tabItem.getView());
        }

        checkVisibleTabAmount(tabItem, OperationType.ADD);

        tabs.put(presenter, tabItem);
        contents.add(presenter);

        presenter.go(partViewContainer);

    }

    private void checkVisibleTabAmount(@NotNull TabItem tabItem, @NotNull OperationType operationType) {
        tabsPanelWidth = tabsPanel.getOffsetWidth() - LEFT_MARGIN;
        int itemWidth = tabItem.getView().asWidget().getOffsetWidth();

        if (tabsPanelWidth == itemWidth - LEFT_MARGIN) {
            return;
        }

        if (OperationType.REMOVE.equals(operationType)) {
            addedTabsWidth -= itemWidth;
        } else {
            addedTabsWidth += itemWidth;
        }

        listButton.setVisible(addedTabsWidth > tabsPanelWidth);
    }

    /** {@inheritDoc} */
    @Override
    public void removeTab(@NotNull PartPresenter presenter) {
        TabItem tab = tabs.get(presenter);

        checkVisibleTabAmount(tab, OperationType.REMOVE);

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

        boolean isWidgetExist = viewIndex != -1;

        if (!isWidgetExist) {
            partPresenter.go(partViewContainer);

            viewIndex = contentPanel.getWidgetIndex(view);
        }

        contentPanel.showWidget(viewIndex);

        setActiveTab(partPresenter);
    }

    private void setActiveTab(@NotNull PartPresenter part) {
        for (TabItem tab : tabs.values()) {
            tab.unSelect();
        }

        activeTab = tabs.get(part);
        activeTab.select();

        delegate.onRequestFocus();
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
            contentPanel.removeStyleName(resources.partStackCss().unSelectEditorBorder());

            activeTab.select();
        } else {
            contentPanel.addStyleName(resources.partStackCss().unSelectEditorBorder());

            activeTab.unSelect();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void updateTabItem(@NotNull PartPresenter partPresenter) {
        TabItem tab = tabs.get(partPresenter);

        tab.update(partPresenter);
    }

    private enum OperationType {
        ADD, REMOVE
    }
}
