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
package org.eclipse.che.ide.ui.dialogs.input;

/**
 * The view interface for the input dialog component.
 *
 * @author Mickaël Leduque
 * @author Artem Zatsarynnyi
 */
public interface InputDialogView {

    /** Sets the action delegate. */
    void setDelegate(ActionDelegate delegate);

    /** Displays the dialog window. */
    void showDialog();

    /** Closes the dialog window. */
    void closeDialog();

    /** Fill the window with its content. */
    void setContent(String content);

    /** Sets the value to the input. */
    void setValue(String value);

    /** Sets the window title. */
    void setTitle(String title);

    /** Returns the input value. */
    String getValue();

    /** Sets the beginning index to select the value in the input, inclusive. */
    void setSelectionStartIndex(int selectionStartIndex);

    /** Sets the number of characters to be selected in the input. */
    void setSelectionLength(int selectionEndIndex);

    /**
     * Show the validation error message.
     *
     * @param message
     */
    void showErrorHint(String message);

    /** Hide the validation error message. */
    void hideErrorHint();

    /** The interface for the action delegate. */
    public interface ActionDelegate {

        /** Defines what's done when the user clicks cancel. */
        void cancelled();

        /** Defines what's done when the user clicks OK. */
        void accepted();

        /** Performs any actions appropriate in response to the user having changed the input value. */
        void inputValueChanged();

        /** Performs any actions appropriate in response to the user having clicked the Enter key. */
        void onEnterClicked();
    }
}
