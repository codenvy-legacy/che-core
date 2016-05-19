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
package org.eclipse.che.ide.jseditor.client.document;

import org.eclipse.che.ide.api.project.tree.VirtualFile;
import org.eclipse.che.ide.api.text.RegionImpl;
import org.eclipse.che.ide.jseditor.client.text.LinearRange;
import org.eclipse.che.ide.jseditor.client.text.TextRange;

/** An abstract implementation of {@link EmbeddedDocument}. */
public abstract class AbstractEmbeddedDocument implements EmbeddedDocument, DocumentHandle {

    /** The document event bus. */
    private final DocumentEventBus eventBus = new DocumentEventBus();

    /** The file holding the document. */
    private VirtualFile file;

    @Override
    public DocumentEventBus getDocEventBus() {
        return this.eventBus;
    }

    @Override
    public boolean isSameAs(final DocumentHandle document) {
        return (this.equals(document));
    }

    @Override
    public EmbeddedDocument getDocument() {
        return this;
    }

    public DocumentHandle getDocumentHandle() {
        return this;
    }

    @Override
    public void setFile(VirtualFile fileNode) {
        this.file = fileNode;
    }

    @Override
    public VirtualFile getFile() {
        return this.file;
    }

    @Override
    public void replace(final int offset, final int length, String text) {
        replace(new RegionImpl(offset, length), text);
    }

    @Override
    public ReadOnlyDocument getReadOnlyDocument() {
        return this;
    }

    @Override
    public void setSelectedRange(final TextRange range) {
        setSelectedRange(range, false);
    }

    @Override
    public void setSelectedRange(final TextRange range, final boolean show) {
        // does nothing by default
    }

    @Override
    public void setSelectedRange(final LinearRange range) {
        setSelectedRange(range, false);
    }
    
    @Override
    public void setSelectedRange(final LinearRange range, final boolean show) {
     // does nothing by default
    }

}
