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
package org.eclipse.che.ide.jseditor.client.keymap;

import com.google.gwt.event.shared.GwtEvent;

/**
 * Event type for change in keymap preference.
 *
 * @author "Mickaël Leduque"
 */
public class KeymapChangeEvent extends GwtEvent<KeymapChangeHandler> {
    /** Type instance for the event. */
    public static final Type<KeymapChangeHandler> TYPE = new Type<>();

    /** The key of the editor type. */
    private final String editorTypeKey;
    /** The key of the new keymap. */
    private final String keymapKey;

    /**
     * Creates a new keymap change event
     *
     * @param editorTypeKey
     *         the editor type which had a keymap change
     * @param keymapKey
     *         the new keymap
     */
    public KeymapChangeEvent(final String editorTypeKey, final String keymapKey) {
        this.editorTypeKey = editorTypeKey;
        this.keymapKey = keymapKey;
    }

    @Override
    public Type<KeymapChangeHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(final KeymapChangeHandler handler) {
        handler.onKeymapChanged(this);
    }

    /**
     * Returns the editor type.
     *
     * @return the editor type
     */
    public String getEditorTypeKey() {
        return editorTypeKey;
    }

    /**
     * Returns the keymap key.
     *
     * @return the keymap key
     */
    public String getKeymapKey() {
        return keymapKey;
    }

}
