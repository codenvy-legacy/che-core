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

import javax.annotation.Nonnull;
import java.util.List;

/** PartStack View interface */
public interface PartStackView extends View<PartStackView.ActionDelegate> {

    public enum TabPosition {
        BELOW, LEFT, RIGHT
    }

    /** Tab which can be clicked and closed */
    interface TabItem extends ClickHandler {

        @Nonnull
        IsWidget getView();

        @Nonnull
        String getTitle();

        void update(@Nonnull PartPresenter part);

        void select();

        void unSelect();

        void setTabPosition(@Nonnull TabPosition tabPosition);
    }

    /** Add Tab */
    public void addTab(@Nonnull TabItem tabItem, @Nonnull PartPresenter presenter);

    /** Remove Tab */
    public void removeTab(@Nonnull PartPresenter presenter);

    public void selectTab(@Nonnull PartPresenter partPresenter);

    /** Set new Tabs positions */
    public void setTabPositions(List<PartPresenter> partPositions);

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
