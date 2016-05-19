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

import org.eclipse.che.ide.jseditor.client.editortype.EditorType;
import org.eclipse.che.ide.util.loging.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Value object for keymaps.
 *
 * @author "Mickaël Leduque"
 */
public class Keymap {

    /** The editor type key. */
    private final String     keymapKey;
    /** The name displayed to a user. */
    private final String     displayString;
    private final EditorType editorType;


    /** The already built instances. */
    private static final Map<String, Keymap> instances = new HashMap<>();

    private Keymap(final String key, final String displayString, final EditorType editorType) {
        this.keymapKey = key;
        this.displayString = displayString;
        this.editorType = editorType;
    }

    /**
     * Returns the key for this keymap (among other keymaps for the same editor implementation).
     *
     * @return the key
     */
    public String getKey() {
        return this.keymapKey;
    }

    /**
     * Returns the value displayed to a user.
     *
     * @return the display value
     */
    public String getDisplay() {
        return this.displayString;
    }

    /**
     * Returns the editor type wjich the keymap is associated with.
     *
     * @return the editor type
     */
    public EditorType getEditorType() {
        return this.editorType;
    }

    /**
     * Retrieve an keymap instance by its key.
     *
     * @param key
     *         the keymap key
     * @return the {@link Keymap} instance
     */
    public static Keymap fromKey(final String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        final Keymap searchResult = instances.get(key);
        return searchResult;
    }

    /**
     * Creates a new keymap instance.
     *
     * @param key
     *         the key (must not already exist)
     * @param displayString
     *         the name displayed to a user
     * @param editorType
     *         the editor type this keymap is linked to
     * @return a new keymap instance
     */
    public static Keymap newKeymap(final String key, final String displayString, final EditorType editorType) {
        if (key == null) {
            throw new IllegalArgumentException("Keymap key can't be null");
        }
        if (displayString == null) {
            throw new IllegalArgumentException("Keymap display string can't be null");
        }
        if (editorType == null) {
            throw new IllegalArgumentException("Keymap editor can't be null");
        }
        if (fromKey(key) != null) {
            throw new RuntimeException("Keymap with key " + key + " already exists");
        }

        Log.debug(Keymap.class, "Creation of new keymap " + key + " for editor type " + editorType);
        Keymap keymap = new Keymap(key, displayString, editorType);
        instances.put(key, keymap);
        return keymap;
    }

    /**
     * Return all existing instances of {@link Keymap}.
     *
     * @return all instances
     */
    public static List<Keymap> getInstances() {
        return new ArrayList<>(instances.values());
    }

    /**
     * Return all instances of {@link Keymap} assicoated with the given editor type.
     *
     * @param editorType
     *         the editor type
     * @return keymaps for this editor type
     */
    public static List<Keymap> getInstances(final EditorType editorType) {
        if (editorType == null) {
            return null;
        }
        final List<Keymap> result = new ArrayList<>();
        for (final Keymap keymap : instances.values()) {
            if (editorType.equals(keymap.getEditorType())) {
                result.add(keymap);
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return this.keymapKey;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((keymapKey == null) ? 0 : keymapKey.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Keymap other = (Keymap)obj;
        if (keymapKey == null) {
            if (other.keymapKey != null) {
                return false;
            }
        } else if (!keymapKey.equals(other.keymapKey)) {
            return false;
        }
        return true;
    }
}
