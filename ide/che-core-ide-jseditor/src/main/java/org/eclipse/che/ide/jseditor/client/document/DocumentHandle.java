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

/**
 * Handle on an editor document.
 */
public interface DocumentHandle {

    /**
     * tells if the handles point to the same document.
     * @param documentHandle the other document handle to compare
     * @return true iff the pointed document is the same.
     */
    boolean isSameAs(DocumentHandle documentHandle);

    /**
     * Returns the private event bus for the pointed editor.
     * @return the private event bus
     */
    DocumentEventBus getDocEventBus();

    /**
     * Returns the pointed document
     * @return the document
     */
    EmbeddedDocument getDocument();
}
