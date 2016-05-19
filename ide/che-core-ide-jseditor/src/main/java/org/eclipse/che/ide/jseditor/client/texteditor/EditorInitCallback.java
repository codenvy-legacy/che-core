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

import org.eclipse.che.ide.jseditor.client.JsEditorConstants;
import org.eclipse.che.ide.jseditor.client.document.DocumentStorage.EmbeddedDocumentCallback;
import org.eclipse.che.ide.rest.AsyncRequestLoader;

/**
 * Composite callback that waits for both the editor module initialization and the document content.
 * @param <T> the type of the editor widget
 */
abstract class EditorInitCallback<T extends EditorWidget> implements EmbeddedDocumentCallback, EditorModule.EditorModuleReadyCallback {

    /** Loader used to wait for editor impl initialization. */
    private final AsyncRequestLoader loader;
    /** The message displayed while waiting for the editor init. */
    private final String waitEditorMessageString;

    /** Flag that tells if the editor initialization was finished. */
    private boolean editorModuleReady;
    /** The content of the document to open. */
    private String receivedContent;

    /** Tells if editor init loader was shown. */
    private boolean loaderWasShown = false;

    /**
     * Constructor.
     * @param moduleAlreadyReady if set to true, the callback will not wait for editor module initialization.
     * @param loader loader used to wait for editor impl initialization
     */
    public EditorInitCallback(final boolean moduleAlreadyReady,
                              final AsyncRequestLoader loader,
                              final JsEditorConstants constants) {
        this.editorModuleReady = moduleAlreadyReady;
        this.loader = loader;
        this.waitEditorMessageString = constants.waitEditorInitMessage();
    }

    @Override
    public void onEditorModuleReady() {
        this.editorModuleReady = true;
        checkReadyAndContinue();
    }

    @Override
    public void onEditorModuleError() {
        if (this.loaderWasShown) {
            this.loader.hide(this.waitEditorMessageString);
        }
        onError();
    }

    @Override
    public void onDocumentReceived(final String content) {
        if (content != null) {
            this.receivedContent = content;
        } else {
            this.receivedContent = "";
        }
        checkReadyAndContinue();
    }

    @Override
    public void onDocumentLoadFailure(final Throwable cause) {
        if (this.loaderWasShown) {
            this.loader.hide(this.waitEditorMessageString);
        }
        onFileError();
    }

    private void checkReadyAndContinue() {
        if (this.receivedContent != null && this.editorModuleReady) {
            if (this.loaderWasShown) {
                this.loader.hide(this.waitEditorMessageString);
            }
            onReady(this.receivedContent);
        } else if (! this.editorModuleReady) {
            // Show a loader for the editor preparation
            this.loaderWasShown = true;
            this.loader.show(this.waitEditorMessageString);
        }
    }

    /**
     * Action when the editor is ready AND we have the document content.
     * @param content the content
     */
    public abstract void onReady(final String content);

    /**
     * Action when editor init failed.
     */
    public abstract void onError();

    /**
     * Action when file load failed.
     */
    public abstract void onFileError();

}
