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

import org.eclipse.che.ide.workspace.perspectives.general.Perspective.Type;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

import static org.eclipse.che.ide.workspace.perspectives.general.Perspective.Type.PROJECT;

/**
 * The class stores current perspective type. Contains listeners which do some actions when type is changed. By default PROJECT
 * perspective type is set.
 *
 * @author Dmitry Shnurenko
 */
@Singleton
public class PerspectiveType {

    private final List<PerspectiveTypeListener> listeners;

    private Type type;

    @Inject
    public PerspectiveType() {
        listeners = new ArrayList<>();

        type = PROJECT;
    }

    /**
     * Changes perspective type and notifies listeners.
     *
     * @param type
     *         type which need set
     */
    public void setType(@Nonnull Type type) {
        this.type = type;

        notifyListeners();
    }

    /** Returns current perspective type. */
    @Nonnull
    public Type getType() {
        return type;
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
