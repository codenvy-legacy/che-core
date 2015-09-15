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
package org.eclipse.che.ide.part.widgets.editortab;

import com.google.gwt.event.dom.client.DoubleClickHandler;

import org.eclipse.che.ide.api.mvp.View;
import org.eclipse.che.ide.api.parts.PartStackView.TabItem;

import javax.validation.constraints.NotNull;

/**
 * @author Dmitry Shnurenko
 */
public interface EditorTab extends View<EditorTab.ActionDelegate>, TabItem, DoubleClickHandler {

    void setErrorMark(boolean isVisible);

    void setWarningMark(boolean isVisible);

    interface ActionDelegate {
        void onTabClicked(@NotNull TabItem tab);

        void onTabClose(@NotNull TabItem tab);
    }
}
