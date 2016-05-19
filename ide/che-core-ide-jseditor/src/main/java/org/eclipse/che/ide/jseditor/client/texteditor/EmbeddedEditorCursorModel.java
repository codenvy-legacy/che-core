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
package org.eclipse.che.ide.jseditor.client.texteditor;

import org.eclipse.che.ide.api.text.Position;
import org.eclipse.che.ide.jseditor.client.document.EmbeddedDocument;
import org.eclipse.che.ide.jseditor.client.events.CursorActivityEvent;
import org.eclipse.che.ide.jseditor.client.events.CursorActivityHandler;
import org.eclipse.che.ide.jseditor.client.text.TextPosition;
import org.eclipse.che.ide.texteditor.selection.CursorModelWithHandler;

import org.eclipse.che.ide.util.ListenerManager;
import org.eclipse.che.ide.util.ListenerManager.Dispatcher;
import org.eclipse.che.ide.util.ListenerRegistrar.Remover;

/**
 * {@link CursorModelWithHandler} implementation for the embedded editors.
 *
 * @author "Mickaël Leduque"
 */
class EmbeddedEditorCursorModel implements CursorModelWithHandler, CursorActivityHandler {

    private final EmbeddedDocument document;
    private final ListenerManager<CursorHandler> cursorHandlerManager = ListenerManager.create();

    public EmbeddedEditorCursorModel(final EmbeddedDocument document) {
        this.document = document;
        this.document.addCursorHandler(this);
    }

    @Override
    public void setCursorPosition(int offset) {
        TextPosition position = document.getPositionFromIndex(offset);
        document.setCursorPosition(position);
    }

    @Override
    public Position getCursorPosition() {
        TextPosition position = document.getCursorPosition();
        int offset = document.getIndexFromPosition(position);
        return new Position(offset);
    }

    @Override
    public Remover addCursorHandler(CursorHandler handler) {
        return this.cursorHandlerManager.add(handler);
    }

    private void dispatchCursorChange(final boolean isExplicitChange) {
        final TextPosition position = this.document.getCursorPosition();


        cursorHandlerManager.dispatch(new Dispatcher<CursorModelWithHandler.CursorHandler>() {
            @Override
            public void dispatch(CursorHandler listener) {
                listener.onCursorChange(position.getLine(), position.getCharacter(), isExplicitChange);
            }
        });
    }

    @Override
    public void onCursorActivity(final CursorActivityEvent event) {
        dispatchCursorChange(true);
    }
}
