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
package org.eclipse.che.ide.jseditor.client.texteditor;

import org.eclipse.che.ide.api.editor.EditorWithErrors;
import org.eclipse.che.ide.jseditor.client.codeassist.CompletionsSource;
import org.eclipse.che.ide.jseditor.client.editortype.EditorType;
import org.eclipse.che.ide.jseditor.client.keymap.Keymap;
import org.eclipse.che.ide.jseditor.client.text.TextPosition;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;

/**
 * View interface for the embedded editors components.
 *
 * @author "Mickaël Leduque"
 */
public interface EmbeddedTextEditorPartView extends RequiresResize,
                                                    IsWidget {

    /**
     * Invoke the code complete dialog.
     *
     * @param editorWidget the editor widget
     * @param completionsSource the completion source
     */
    void showCompletionProposals(EditorWidget editorWidget, CompletionsSource completionsSource);

    /**
     * Invoke the code complete dialog with default completion.
     * @param editorWidget the editor widget
     */
    void showCompletionProposals(EditorWidget editorWidget);

    /**
     * Sets the view delegate.
     * @param delegate the delegate
     */
    void setDelegate(Delegate delegate);

    /**
     * Sets the editor widget.
     * @param editorWidget the widget
     */
    void setEditorWidget(EditorWidget editorWidget);

    /**
     * Display a placeholder in place of the editor widget.
     * @param placeHolder the widget to display
     */
    void showPlaceHolder(Widget placeHolder);

    /**
     * Sets the initial state of the info panel.
     * @param mode the file mode
     * @param editorType the editor implementation
     * @param keymap the current keymap
     * @param lineCount the number of lines
     * @param tabSize the tab size in this editor
     */
    void initInfoPanel(String mode, EditorType editorType, Keymap keymap, int lineCount, int tabSize);

    /**
     * Update the location displayed in the info panel.
     * @param position the new position
     */
    void updateInfoPanelPosition(TextPosition position);

    /**
     * Update the values in the info panel for when the editor is not focused (i.e. show line count and not char part).
     * @param linecount the number of lines in the file
     */
    void updateInfoPanelUnfocused(int linecount);

    /** Delegate interface for this view. */
    interface Delegate extends EditorWithErrors, RequiresResize {
        /** Reaction on loss of focus. */
        void editorLostFocus();
        /** Reaction when the editor gains focus. */
        void editorGotFocus();
        /** Reaction when the cursor position changes. */
        void editorCursorPositionChanged();
    }
}
