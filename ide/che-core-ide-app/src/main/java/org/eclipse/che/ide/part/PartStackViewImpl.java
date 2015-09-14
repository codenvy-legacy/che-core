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
package org.eclipse.che.ide.part;

import com.google.gwt.event.dom.client.ContextMenuEvent;
import com.google.gwt.event.dom.client.ContextMenuHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.DeckLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import org.eclipse.che.ide.api.parts.PartPresenter;
import org.eclipse.che.ide.api.parts.PartStackUIResources;
import org.eclipse.che.ide.api.parts.PartStackView;

import javax.validation.constraints.NotNull;
import java.util.HashMap;

import java.util.List;
import java.util.Map;

/**
 * PartStack view class. Implements UI that manages Parts organized in a Tab-like widget.
 *
 * @author Nikolay Zamosenchuk
 * @author Dmitry Shnurenko
 * @author Valeriy Svydenko
 */
public class PartStackViewImpl extends ResizeComposite implements PartStackView, MouseDownHandler, ContextMenuHandler {
    private final Map<PartPresenter, TabItem> tabs;
    private final AcceptsOneWidget            partViewContainer;
    private final DeckLayoutPanel             contentPanel;
    private final FlowPanel                   tabsPanel;
    private final FlowPanel                   tabsRotationPanel;
    private final TabPosition                 tabPosition;

    private ActionDelegate delegate;
    private Widget         focusedWidget;

    @Inject
    public PartStackViewImpl(PartStackUIResources resources,
                             FlowPanel tabsRotationPanel,
                             final DeckLayoutPanel contentPanel,
                             @Assisted @NotNull TabPosition tabPosition,
                             @Assisted @NotNull FlowPanel tabsPanel) {
        this.tabsPanel = tabsPanel;
        this.tabPosition = tabPosition;
        this.tabsRotationPanel = tabsRotationPanel;

        this.contentPanel = contentPanel;
        this.contentPanel.setStyleName(resources.partStackCss().idePartStackContent());
        initWidget(contentPanel);

        this.tabs = new HashMap<>();

        tabsPanel.add(tabsRotationPanel);

        partViewContainer = new AcceptsOneWidget() {
            @Override
            public void setWidget(IsWidget widget) {
                contentPanel.add(widget);
            }
        };

        addDomHandler(this, MouseDownEvent.getType());
        addDomHandler(this, ContextMenuEvent.getType());
    }

       /** {@inheritDoc} */
    @Override
    public void onMouseDown(@NotNull MouseDownEvent event) {
        delegate.onRequestFocus();
    }

    /** {@inheritDoc} */
    @Override
    public void onContextMenu(@NotNull ContextMenuEvent event) {
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
        tabsRotationPanel.add(tabItem.getView());
        presenter.go(partViewContainer);

        tabs.put(presenter, tabItem);
        tabItem.setTabPosition(tabPosition, tabsRotationPanel.getWidgetCount());
    }

    /** {@inheritDoc} */
    @Override
    public void removeTab(@NotNull PartPresenter presenter) {
        TabItem tab = tabs.get(presenter);

        tabsPanel.remove(tab.getView());
        contentPanel.remove(presenter.getView());

        tabs.remove(presenter);
    }

    /** {@inheritDoc} */
    @Override
    public void setTabPositions(List<PartPresenter> presenters) {
        for (PartPresenter partPresenter : presenters) {
            int tabIndex = presenters.indexOf(partPresenter);

            TabItem tabItem = tabs.get(partPresenter);

            tabsRotationPanel.insert(tabItem.getView(), tabIndex);
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

        tabs.get(part).select();

        delegate.onRequestFocus();
    }

    /** {@inheritDoc} */
    @Override
    public void setFocus(boolean focused) {
        if (focusedWidget != null) {
            focusedWidget.getElement().removeAttribute("active");
        }

        focusedWidget = contentPanel.getVisibleWidget();

        if (focused && focusedWidget != null) {
            focusedWidget.getElement().setAttribute("active", "");
        }
    }

    /** {@inheritDoc} */
    @Override
    public void updateTabItem(@NotNull PartPresenter partPresenter) {
        TabItem tabItem = tabs.get(partPresenter);

        tabItem.update(partPresenter);
    }
}
