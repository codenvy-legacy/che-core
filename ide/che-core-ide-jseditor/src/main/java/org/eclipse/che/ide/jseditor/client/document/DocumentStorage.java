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
package org.eclipse.che.ide.jseditor.client.document;


import org.eclipse.che.ide.api.editor.EditorInput;
import org.eclipse.che.ide.api.project.tree.VirtualFile;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.ImplementedBy;

import javax.validation.constraints.NotNull;
import org.eclipse.che.commons.annotation.Nullable;

/**
 * Interface for file retrieval and storage operations.
 */

@ImplementedBy(DocumentStorageImpl.class)
public interface DocumentStorage {

    /**
     * Retrieves the file content.
     * @param file the file
     * @param callback operation to do when the content is ready
     */
    void getDocument(@NotNull VirtualFile file,
                     @NotNull final EmbeddedDocumentCallback callback);

    /**
     * Saves the file content.
     * @param editorInput the editor input
     * @param document the document
     * @param overwrite
     * @param callback operation to do when the content is ready
     */
    void saveDocument(@Nullable final EditorInput editorInput,
                      @NotNull Document document,
                      boolean overwrite,
                      @NotNull final AsyncCallback<EditorInput> callback);

    /**
     * Action taken when the document is closed.
     * @param document the document
     */
    public void documentClosed(@NotNull Document document);

    /**
     * Action taken when retrieve action is successful.
     */
    public interface EmbeddedDocumentCallback {
        /**
         * Action taken when retrieve action is successful.
         * @param content the content that was received
         */
        void onDocumentReceived(String content);

        /**
         * Action taken when retrieve action fails.
         * @param caught the exception
         */
        void onDocumentLoadFailure(Throwable caught);
    }
}
