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
package org.eclipse.che.ide.jseditor.client.preference;

import org.eclipse.che.ide.api.mvp.Presenter;

/** Presenter for a section of the editor preferences page. */
public interface EditorPreferenceSection extends Presenter {

    /** Tells if the content of the section has been changed. */
    boolean isDirty();

    /** Sets the editor page presenter that owns the section. */
    void setParent(ParentPresenter parent);

    /**
     * Stores changes to preferences.
     */
    void storeChanges();

    /**
     * Reads changes from preferences and updates the view.
     */
    void refresh();

    /** Interface for the parent presenter that owns the section. */
    public interface ParentPresenter {
        /** Asks to trigger a dirty state action. */
        void signalDirtyState();
    }
}
