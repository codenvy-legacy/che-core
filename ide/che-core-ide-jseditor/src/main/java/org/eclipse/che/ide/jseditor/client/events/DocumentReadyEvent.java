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
package org.eclipse.che.ide.jseditor.client.events;

import org.eclipse.che.ide.jseditor.client.document.EmbeddedDocument;
import org.eclipse.che.ide.jseditor.client.texteditor.EditorHandle;
import com.google.gwt.event.shared.GwtEvent;

public class DocumentReadyEvent extends GwtEvent<DocumentReadyHandler> {

    /** The type instance for this event. */
    public static final Type<DocumentReadyHandler> TYPE = new Type<>();

    /** The editor. */
    private final EditorHandle editorHandle;
    /** The document. */
    private final EmbeddedDocument document;

    public DocumentReadyEvent(final EditorHandle editorHandle, final EmbeddedDocument document) {
        this.editorHandle = editorHandle;
        this.document = document;
    }

    @Override
    public Type<DocumentReadyHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(final DocumentReadyHandler handler) {
        handler.onDocumentReady(this);
    }

    public EmbeddedDocument getDocument() {
        return document;
    }

    public EditorHandle getEditorHandle() {
        return editorHandle;
    }
}
