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

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.gwt.user.client.ui.InsertPanel.ForIsWidget;

/**
 * PartStack view class. Implements UI that manages Parts organized in a Tab-like widget.
 *
 * @author Nikolay Zamosenchuk
 * @author Dmitry Shnurenko
 */
public class PartStackViewImpl extends ResizeComposite implements PartStackView {

    private final PartStackUIResources        resources;
    private final Map<PartPresenter, TabItem> tabs;
    private final AcceptsOneWidget            partViewContainer;
    private final DeckLayoutPanel             contentPanel;
    private final FlowPanel                   tabsPanel;

    private ActionDelegate delegate;
    private TabPosition    tabPosition;

    @Inject
    public PartStackViewImpl(PartStackUIResources resources,
                             @Assisted TabPosition tabPosition,
                             @Assisted FlowPanel tabsPanel) {
        this.resources = resources;
        this.tabPosition = tabPosition;
        this.tabsPanel = tabsPanel;

        this.tabs = new HashMap<>();
        contentPanel = new DeckLayoutPanel();

        partViewContainer = new AcceptsOneWidget() {
            @Override
            public void setWidget(IsWidget widget) {
                contentPanel.add(widget);
            }
        };

        contentPanel.setStyleName(resources.partStackCss().idePartStackContent());
        initWidget(contentPanel);

        addDomHandler(new MouseDownHandler() {
            @Override
            public void onMouseDown(MouseDownEvent event) {
                if (delegate != null) {
                    delegate.onRequestFocus();
                }
            }
        }, MouseDownEvent.getType());

        addDomHandler(new ContextMenuHandler() {
            @Override
            public void onContextMenu(ContextMenuEvent event) {
                if (delegate != null) {
                    delegate.onRequestFocus();
                }
            }
        }, ContextMenuEvent.getType());
    }

    /** {@inheritDoc} */
    @Override
    public void setDelegate(ActionDelegate delegate) {
        this.delegate = delegate;
    }

    /** {@inheritDoc} */
    @Override
    public ForIsWidget getContentPanel() {
        return contentPanel;
    }

    /** {@inheritDoc} */
    @Override
    public void addTab(@Nonnull TabItem tabItem, @Nonnull PartPresenter presenter) {
        tabsPanel.add(tabItem.getView());
        presenter.go(partViewContainer);

        tabs.put(presenter, tabItem);
    }

    /** {@inheritDoc} */
    @Override
    public void removeTab(@Nonnull PartPresenter presenter) {
        TabItem tab = tabs.get(presenter);

        tabsPanel.remove(tab.getView());
        contentPanel.remove(presenter.getView());
    }

    /** {@inheritDoc} */
    @Override
    public void setTabpositions(List<Integer> partPositions) {
        //TODO need add ability add tab in special position
    }

    /** {@inheritDoc} */
    @Override
    public void setActiveTab(@Nonnull PartPresenter part) {
        unSelectTabs();

        tabs.get(part).select();
    }

    /** {@inheritDoc} */
    @Override
    public void unSelectTabs() {
        for (TabItem tab : tabs.values()) {
            tab.unSelect();
        }
    }


    private Widget focusedWidget;

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

        /* the style doesn't change the style of the panel, was left for future */
        if (focused) {
            contentPanel.addStyleName(resources.partStackCss().idePartStackFocused());
        } else {
            contentPanel.removeStyleName(resources.partStackCss().idePartStackFocused());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void updateTabItem(@Nonnull PartPresenter partPresenter) {
        TabItem tabItem = tabs.get(partPresenter);

        tabItem.update(partPresenter);
    }
}
