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
package org.eclipse.che.ide.part.widgets.listtab;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.inject.ImplementedBy;

import org.eclipse.che.ide.api.mvp.View;
import org.eclipse.che.ide.part.widgets.listtab.item.ListItem;

import javax.annotation.Nonnull;

/**
 * @author Dmitry Shnurenko
 */
@ImplementedBy(ListButtonWidget.class)
public interface ListButton extends View<ListButton.ActionDelegate>, ClickHandler {

    void showList();

    void addListItem(@Nonnull ListItem listItem);

    void removeListItem(@Nonnull ListItem listItem);

    void hide();

    void setVisible(boolean visible);

    interface ActionDelegate {
        void onListButtonClicked();
    }
}
