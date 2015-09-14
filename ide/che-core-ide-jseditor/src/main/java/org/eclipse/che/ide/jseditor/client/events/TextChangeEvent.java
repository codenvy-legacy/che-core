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
package org.eclipse.che.ide.jseditor.client.events;

import javax.validation.constraints.NotNull;

import org.eclipse.che.ide.jseditor.client.changeintercept.TextChange;
import com.google.gwt.event.shared.GwtEvent;

/**
 * An event describing a change in the text of an editor.
 */
public class TextChangeEvent extends GwtEvent<TextChangeHandler> {

    /** The type instance for this event. */
    public static final Type<TextChangeHandler> TYPE = new Type<>();

    /** The text change. */
    private final TextChange change;

    /** The updater. */
    private final ChangeUpdater updater;

    public TextChangeEvent(@NotNull final TextChange change, @NotNull final ChangeUpdater updater) {
        this.change = change;
        this.updater = updater;
    }

    @Override
    public Type<TextChangeHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(final TextChangeHandler handler) {
        handler.onTextChange(this);
    }

    public TextChange getChange() {
        return this.change;
    }

    public void update(final TextChange updatedChange) {
        this.updater.update(updatedChange);
    }

    @Override
    public String toString() {
        return "TextChangeEvent [change=" + change + "]";
    }

    /** Action to update the change event. */
    public interface ChangeUpdater {
        void update(TextChange updatedChange);
    }
}
