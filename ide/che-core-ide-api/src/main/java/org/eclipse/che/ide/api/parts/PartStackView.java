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

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.List;

/** PartStack View interface */
public interface PartStackView extends View<PartStackView.ActionDelegate> {

    enum TabPosition {
        BELOW, LEFT, RIGHT
    }

    /** Tab which can be clicked and closed */
    interface TabItem extends ClickHandler {

        @NotNull
        IsWidget getView();

        @NotNull
        String getTitle();

        void update(@NotNull PartPresenter part);

        void select();

        void unSelect();

        /**
         * Determines position of the tab.
         *
         * @param tabPosition
         *         orientation of the Tab (e.g. LEFT or RIGHT)
         * @param countWidgets
         *         number of widgets(tabs) which are including in the current part. It is necessary for ranking the tab.
         */
        void setTabPosition(@NotNull TabPosition tabPosition, @Min(value=0) int countWidgets);
    }

    /** Add Tab */
    void addTab(@NotNull TabItem tabItem, @NotNull PartPresenter presenter);

    /** Remove Tab */
    void removeTab(@NotNull PartPresenter presenter);

    void selectTab(@NotNull PartPresenter partPresenter);

    /** Set new Tabs positions */
    void setTabPositions(List<PartPresenter> partPositions);

    /** Set PartStack focused */
    void setFocus(boolean focused);

    /** Update Tab */
    void updateTabItem(@NotNull PartPresenter partPresenter);

    /** Handles Focus Request Event. It is generated, when user clicks a stack anywhere */
    interface ActionDelegate {
        /** PartStack is being clicked and requests Focus */
        void onRequestFocus();
    }

}
