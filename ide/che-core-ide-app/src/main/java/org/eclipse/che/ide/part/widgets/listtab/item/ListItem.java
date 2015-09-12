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
package org.eclipse.che.ide.part.widgets.listtab.item;

import org.eclipse.che.ide.api.mvp.View;

import javax.validation.constraints.NotNull;

/**
 * @author Dmitry Shnurenko
 */
public interface ListItem extends View<ListItem.ActionDelegate> {

    @NotNull
    String getTitle();

    interface ActionDelegate {
        void onCloseItemClicked(@NotNull ListItem listItem);
    }
}
