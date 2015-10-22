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
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.parts.PartPresenter;
import org.eclipse.che.ide.part.editor.EditorPartStackPresenter;

import java.util.List;

/**
 * Editor tab switcher to next or previous opened editor
 * @author Alexander Andrienko
 */
@Singleton
public class TabSwitcher {
    private final EditorPartStackPresenter editorPartStackPresenter;

    @Inject
    public TabSwitcher(EditorPartStackPresenter editorPartStackPresenter) {
        this.editorPartStackPresenter = editorPartStackPresenter;
    }

    /**
     * Switch to previous tab
     */
    public void switchToPreviousTab() {
        List<PartPresenter> editors = editorPartStackPresenter.getEditors();
        PartPresenter activePart = editorPartStackPresenter.getActivePart();

        PartPresenter previousEditor;
        PartPresenter firstEditor = editors.get(0);
        int activePartIndex = editors.indexOf(activePart);
        if (activePart.equals(firstEditor)) {
            previousEditor = editors.get(editors.size() - 1);
        } else {
            previousEditor = editors.get(activePartIndex - 1);
        }

        setActiveTab(previousEditor);
    }

    /**
     * Switch to next tab
     */
    public void switchToNextTab() {
        List<PartPresenter> editors = editorPartStackPresenter.getEditors();
        PartPresenter activePart = editorPartStackPresenter.getActivePart();

        PartPresenter nextEditor;
        int activePartIndex = editors.indexOf(activePart);
        PartPresenter lastEditor = editors.get(editors.size() - 1);
        if (activePart.equals(lastEditor)) {
            nextEditor = editors.get(0);
        } else {
            nextEditor = editors.get(activePartIndex + 1);
        }

        setActiveTab(nextEditor);
    }

    private void setActiveTab(@Nullable PartPresenter editor) {
        if (editor == null || !(editor instanceof EditorPartPresenter)) {
             return;
         }
        editorPartStackPresenter.setActivePart(editor);
    }
}
