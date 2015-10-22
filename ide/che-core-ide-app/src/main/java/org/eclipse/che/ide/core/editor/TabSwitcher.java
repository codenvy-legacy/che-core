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
package org.eclipse.che.ide.core.editor;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.parts.PartPresenter;
import org.eclipse.che.ide.part.editor.EditorPartStackPresenter;

import java.util.Collection;
import java.util.Iterator;

/**
 * Editor tab switcher to next or previous opened editor
 * @author Alexander Andrienko
 */
@Singleton
public class TabSwitcher {
    private final EditorAgent              editorAgent;
    private final EditorPartStackPresenter editorPartStackPresenter;


    @Inject
    public TabSwitcher(EditorAgent editorAgent, EditorPartStackPresenter editorPartStackPresenter) {
        this.editorAgent = editorAgent;
        this.editorPartStackPresenter = editorPartStackPresenter;
    }

    /**
     * Switch to previous tab
     */
    public void switchToPreviousTab() {
        EditorPartPresenter activeEditor = editorAgent.getActiveEditor();
        Collection<EditorPartPresenter> openedEditors = editorAgent.getOpenedEditors().values();
        EditorPartPresenter previousEditor = null;
        for (EditorPartPresenter editor : openedEditors) {
            if (activeEditor.equals(editor) && previousEditor != null) {
                break;
            }
            previousEditor = editor;
        }

        if (previousEditor == null) {
            previousEditor = getLastEditor(openedEditors);
        }
        setActiveTab(previousEditor);
    }

    /**
     * Switch to next tab
     */
    public void switchToNextTab() {
        EditorPartPresenter activeEditor = editorAgent.getActiveEditor();
        EditorPartPresenter nextPart = null;
        Collection<EditorPartPresenter> openedEditors = editorAgent.getOpenedEditors().values();
        Iterator<EditorPartPresenter> iterator = openedEditors.iterator();
        while (iterator.hasNext()) {
            EditorPartPresenter editor = iterator.next();
            if (activeEditor.equals(editor) && iterator.hasNext()) {
                nextPart = iterator.next();
                break;
            }
        }

        if (nextPart == null) {
            nextPart = getFirstEditor(openedEditors);
        }
        setActiveTab(nextPart);
    }

    private void setActiveTab(@Nullable PartPresenter editor) {
        if (editor == null || !(editor instanceof EditorPartPresenter)) {
             return;
         }
        editorPartStackPresenter.setActivePart(editor);
    }

    private EditorPartPresenter getLastEditor(Collection<EditorPartPresenter> openedEditors) {
        EditorPartPresenter result = null;
        for (EditorPartPresenter editor: openedEditors) {
            result = editor;
        }
        return result;
    }

    private EditorPartPresenter getFirstEditor(Collection<EditorPartPresenter> openedEditors) {
        Iterator<EditorPartPresenter> editorIterator = openedEditors.iterator();
        return editorIterator.hasNext() ? editorIterator.next() : null;
    }
}
