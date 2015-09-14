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
package org.eclipse.che.ide.actions;

import com.google.inject.Inject;

import org.eclipse.che.api.analytics.client.logger.AnalyticsEventLogger;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.Resources;
import org.eclipse.che.ide.api.action.AbstractPerspectiveAction;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.texteditor.HandlesUndoRedo;
import org.eclipse.che.ide.api.texteditor.UndoableEditor;

import javax.validation.constraints.NotNull;
import java.util.Arrays;

import static org.eclipse.che.ide.workspace.perspectives.project.ProjectPerspective.PROJECT_PERSPECTIVE_ID;

/**
 * Undo Action
 *
 * @author Roman Nikitenko
 * @author Dmitry Shnurenko
 */
public class UndoAction extends AbstractPerspectiveAction {

    private       EditorAgent          editorAgent;
    private final AnalyticsEventLogger eventLogger;

    @Inject
    public UndoAction(EditorAgent editorAgent,
                      CoreLocalizationConstant localization,
                      AnalyticsEventLogger eventLogger,
                      Resources resources) {
        super(Arrays.asList(PROJECT_PERSPECTIVE_ID), localization.undoName(), localization.undoDescription(), null, resources.undo());
        this.editorAgent = editorAgent;
        this.eventLogger = eventLogger;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        eventLogger.log(this);

        EditorPartPresenter activeEditor = editorAgent.getActiveEditor();

        if (activeEditor != null && activeEditor instanceof UndoableEditor) {
            final HandlesUndoRedo undoRedo = ((UndoableEditor)activeEditor).getUndoRedo();
            if (undoRedo != null) {
                undoRedo.undo();
            }
        }
    }

    @Override
    public void updateInPerspective(@NotNull ActionEvent event) {
        EditorPartPresenter activeEditor = editorAgent.getActiveEditor();

        boolean mustEnable = false;
        if (activeEditor != null && activeEditor instanceof UndoableEditor) {
            final HandlesUndoRedo undoRedo = ((UndoableEditor)activeEditor).getUndoRedo();
            if (undoRedo != null) {
                mustEnable = undoRedo.undoable();
            }
        }
        event.getPresentation().setEnabled(mustEnable);
    }
}
