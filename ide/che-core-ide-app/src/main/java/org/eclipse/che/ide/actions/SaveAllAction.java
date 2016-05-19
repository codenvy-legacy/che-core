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
package org.eclipse.che.ide.actions;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.analytics.client.logger.AnalyticsEventLogger;
import org.eclipse.che.ide.Resources;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.action.ProjectAction;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorInput;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.editor.EditorWithAutoSave;
import org.eclipse.che.ide.util.loging.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** @author Evgen Vidolob */
@Singleton
public class SaveAllAction extends ProjectAction {

    private final EditorAgent          editorAgent;
    private final AnalyticsEventLogger eventLogger;

    @Inject
    public SaveAllAction(EditorAgent editorAgent, Resources resources, AnalyticsEventLogger eventLogger) {
        super("Save All", "Save all changes for project", resources.save());
        this.editorAgent = editorAgent;
        this.eventLogger = eventLogger;
    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(ActionEvent e) {
        eventLogger.log(this);
        Collection<EditorPartPresenter> values = editorAgent.getOpenedEditors().values();
        List<EditorPartPresenter> editors = new ArrayList<>(values);
        save(editors);
    }

    private void save(final List<EditorPartPresenter> editors) {
        if (editors.isEmpty()) {
            return;
        }

        final EditorPartPresenter editorPartPresenter = editors.get(0);
        if (editorPartPresenter.isDirty()) {
            editorPartPresenter.doSave(new AsyncCallback<EditorInput>() {
                @Override
                public void onFailure(Throwable caught) {
                    Log.error(SaveAllAction.class, caught);
                    //try to save other files
                    editors.remove(editorPartPresenter);
                    save(editors);
                }

                @Override
                public void onSuccess(EditorInput result) {
                    editors.remove(editorPartPresenter);
                    save(editors);
                }
            });
        } else {
            editors.remove(editorPartPresenter);
            save(editors);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void updateProjectAction(ActionEvent e) {
//        e.getPresentation().setVisible(true);
        boolean hasDirtyEditor = false;
        for (EditorPartPresenter editor : editorAgent.getOpenedEditors().values()) {
            if(editor instanceof EditorWithAutoSave) {
                if (((EditorWithAutoSave)editor).isAutoSaveEnabled()) {
                    continue;
                }
            }
            if (editor.isDirty()) {
                hasDirtyEditor = true;
                break;
            }
        }
        e.getPresentation().setEnabledAndVisible(hasDirtyEditor);
    }
}
