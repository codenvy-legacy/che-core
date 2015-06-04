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
package org.eclipse.che.ide.workspace.perspectives.general;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.eclipse.che.ide.workspace.perspectives.project.ProjectPerspective.PROJECT_PERSPECTIVE_ID;

/**
 * The class stores current perspective type. Contains listeners which do some actions when type is changed. By default PROJECT
 * perspective type is set.
 *
 * @author Dmitry Shnurenko
 */
@Singleton
public class PerspectiveManager {

    private final List<PerspectiveTypeListener> listeners;
    private final Map<String, Perspective>      perspectives;

    private String currentPerspectiveId;

    @Inject
    public PerspectiveManager(Map<String, Perspective> perspectives) {
        this.listeners = new ArrayList<>();
        this.perspectives = perspectives;

        //perspective by default
        currentPerspectiveId = PROJECT_PERSPECTIVE_ID;
    }

    /** Returns current active perspective. The method can return null, if current perspective isn't found. */
    @Nullable
    public Perspective getActivePerspective() {
        return perspectives.get(currentPerspectiveId);
    }

    /**
     * Changes perspective type and notifies listeners.
     *
     * @param perspectiveId
     *         type which need set
     */
    public void setPerspectiveId(@Nonnull String perspectiveId) {
        currentPerspectiveId = perspectiveId;

        notifyListeners();
    }

    /** Returns current perspective type. */
    @Nonnull
    public String getPerspectiveId() {
        return currentPerspectiveId;
    }

    /**
     * Adds listeners which will react on changing of perspective type.
     *
     * @param listener
     *         listener which need add
     */
    public void addListener(@Nonnull PerspectiveTypeListener listener) {
        listeners.add(listener);
    }

    /** Notifies all listeners of perspective type. */
    public void notifyListeners() {
        for (PerspectiveTypeListener container : listeners) {
            container.onPerspectiveChanged();
        }
    }

    /** The interface which must be implemented by all elements who need react on perspective changing. */
    public interface PerspectiveTypeListener {
        /** Performs some action when perspective was changed. */
        void onPerspectiveChanged();
    }
}
