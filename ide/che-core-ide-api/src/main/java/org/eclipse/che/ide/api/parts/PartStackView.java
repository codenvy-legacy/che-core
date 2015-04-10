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

import org.eclipse.che.ide.api.mvp.View;
import org.eclipse.che.ide.collections.Array;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.HasMouseDownHandlers;
import com.google.gwt.event.logical.shared.HasCloseHandlers;
import com.google.gwt.user.client.ui.IsWidget;

import org.vectomatic.dom.svg.ui.SVGImage;

import static com.google.gwt.user.client.ui.InsertPanel.ForIsWidget;

/** PartStack View interface */
public interface PartStackView extends View<PartStackView.ActionDelegate> {

    enum TabPosition {
        BELOW, LEFT, RIGHT
    }

    /** Tab which can be clicked and closed */
    interface TabItem extends HasCloseHandlers<PartStackView.TabItem>, HasClickHandlers, HasMouseDownHandlers {
    }

    /** Add Tab */
    PartStackView.TabItem addTab(SVGImage icon, String title, String toolTip, IsWidget widget, boolean closable);

    /** Remove Tab */
    void removeTab(int index);

    /** Set Active Tab */
    void setActiveTab(int index);

    /** Set new Tabs positions */
    void setTabpositions(Array<Integer> partPositions);

    /** Get Content Panel */
    ForIsWidget getContentPanel();

    /** Set PartStack focused */
    void setFocus(boolean focused);

    /** Update Tab */
    void updateTabItem(int index, SVGImage icon, String title, String toolTip, IsWidget widget);

    /** Handles Focus Request Event. It is generated, when user clicks a stack anywhere */
    interface ActionDelegate {
        /** PartStack is being clicked and requests Focus */
        void onRequestFocus();
    }

}
