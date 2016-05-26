/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.core.problemDialog;

import org.eclipse.che.commons.annotation.Nullable;

import java.util.List;

/**
 * The view interface for the problem dialog component.
 *
 * @author Roman Nikitenko.
 */
public interface ProjectProblemDialogView {

    interface ActionDelegate {
        /** Call when the user clicks on "Open as is" button. */
        void onOpenAsIs();

        /** Call when the user clicks on "Open as ..." button. */
        void onOpenAs();

        /** Call when the user clicks on "Configure" button. */
        void onConfigure();

        /** Call when the user clicks on Enter. */
        void onEnterClicked();

        void onSelectedTypeChanged(String projectType);
    }

    /**
     * Displays new dialog.
     *
     * @param projectTypes
     *         the list of estimation project types for displaying in popup window
     */
    void showDialog(@Nullable List<String> projectTypes);

    /** Hides the dialog. */
    void hide();

    /**
     * Sets message in popup window
     *
     * @param message
     *         the message that will displays to user
     */
    void setMessage(String message);

    /** Sets the title for 'Open as' button */
    void setOpenAsButtonTitle(String title);

    /** Returns the index of selected project type */
    int getSelectedTypeIndex();

    /** Sets the delegate to receive events from this view. */
    void setDelegate(ActionDelegate delegate);

}
