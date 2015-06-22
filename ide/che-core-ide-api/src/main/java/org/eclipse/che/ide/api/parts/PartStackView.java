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
package org.eclipse.che.ide.api.parts;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.IsWidget;

import org.eclipse.che.ide.api.mvp.View;
import org.vectomatic.dom.svg.ui.SVGResource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static com.google.gwt.user.client.ui.InsertPanel.ForIsWidget;

/** PartStack View interface */
public interface PartStackView extends View<PartStackView.ActionDelegate> {

    public enum TabPosition {
        BELOW, LEFT, RIGHT
    }

    /** Tab which can be clicked and closed */
    interface TabItem extends View<TabItem.ActionDelegate>, ClickHandler {

        IsWidget getView();

        TabItem addTooltip(@Nullable String tooltip);

        TabItem addWidget(@Nullable IsWidget widget);

        TabItem addIcon(@Nullable SVGResource resource);

        void update(@Nonnull PartPresenter part);

        void select();

        void unSelect();

        interface ActionDelegate {
            void onTabClicked(@Nonnull TabItem selectedTab, boolean isSelected);
        }
    }

    /** Add Tab */
    public void addTab(@Nonnull TabItem tabItem, @Nonnull PartPresenter presenter);

    /** Remove Tab */
    public void removeTab(@Nonnull PartPresenter presenter);

    /** Set Active Tab */
    public void setActiveTab(@Nonnull PartPresenter partPresenter);

    public void unSelectTabs();

    /** Set new Tabs positions */
    public void setTabpositions(List<Integer> partPositions);

    /** Get Content Panel */
    public ForIsWidget getContentPanel();

    /** Set PartStack focused */
    public void setFocus(boolean focused);

    /** Update Tab */
    public void updateTabItem(@Nonnull PartPresenter partPresenter);

    /** Handles Focus Request Event. It is generated, when user clicks a stack anywhere */
    public interface ActionDelegate {
        /** PartStack is being clicked and requests Focus */
        void onRequestFocus();
    }

}
